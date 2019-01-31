package com.leyou.utils;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.leyou.config.SmsConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@EnableConfigurationProperties({SmsConfig.class})
public class SmsUtils {

    @Autowired
    private SmsConfig smsConfig;
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 产品名称:云通信短信API产品,开发者无需替换
    static final String product = "Dysmsapi";
    // 产品域名,开发者无需替换
    static final String domain = "dysmsapi.aliyuncs.com";
    // 短信微服务在Redis中存储的前缀
    private static final String KEY_PREFIX = "sms:phone:";

    /**
     *
     * @param phoneNumber 电话号码
     * @param signName 短信签名
     * @param templateCode 短信模板名称
     * @param templateParams 短信需要的参数，例如验证码，JSON格式的字符串
     * @return
     */
    public SendSmsResponse sendSms(String phoneNumber,String signName,String templateCode,String templateParams)  {
        String key = KEY_PREFIX + phoneNumber;
        // 从Redis中获取号码上次发送时间
        String lastTime = redisTemplate.opsForValue().get(key);
        if (StringUtils.isNoneBlank(lastTime)) {
            // 如果不为空，则号码还未过期，不能发送短信，直接返回
            log.info("[短信服务] 消息发送频率过高，电话:{}",phoneNumber);
            return null;
        }
        try {
            //可自助调整超时时间
            System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
            System.setProperty("sun.net.client.defaultReadTimeout", "10000");

            //初始化acsClient,暂不支持region化
            IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", smsConfig.getAccessKeyId(), smsConfig.getAccessKeySecret());
            DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", product, domain);
            IAcsClient acsClient = new DefaultAcsClient(profile);

            //组装请求对象-具体描述见控制台-文档部分内容
            SendSmsRequest request = new SendSmsRequest();
            request.setMethod(MethodType.POST);
            //必填:待发送手机号
            request.setPhoneNumbers(phoneNumber);
            //必填:短信签名-可在短信控制台中找到
            request.setSignName(signName);
            //必填:短信模板-可在短信控制台中找到
            request.setTemplateCode(templateCode);
            //可选:模板中的变量替换JSON串,如模板内容为"亲爱的${name},您的验证码为${code}"时,此处的值为
            request.setTemplateParam(templateParams);

            //选填-上行短信扩展码(无特殊需求用户请忽略此字段)
            //request.setSmsUpExtendCode("90997");

            //可选:outId为提供给业务方扩展字段,最终在短信回执消息中将此值带回给调用者
            request.setOutId("123456");

            //hint 此处可能会抛出异常，注意catch
            SendSmsResponse sendSmsResponse = acsClient.getAcsResponse(request);

            if (!"ok".equals(sendSmsResponse.getMessage())) {
                log.info("[短信服务] 消息发送异常，电话:{}",phoneNumber);
            }
            // 发送成功，将号码记录到Redis中，过期时间为1分钟，并记录日志
            redisTemplate.opsForValue().set(key,String.valueOf(System.currentTimeMillis()),1, TimeUnit.MINUTES);
            log.info("[短信服务] 消息发送成功，电话:{}",phoneNumber);
            return sendSmsResponse;
        } catch (ClientException e) {
            log.error("[短信服务] 消息发送失败，电话:{}",phoneNumber,e);
            return null;
        }
    }
}
