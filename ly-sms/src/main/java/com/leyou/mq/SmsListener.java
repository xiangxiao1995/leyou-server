package com.leyou.mq;

import com.leyou.common.utils.JsonUtils;
import com.leyou.config.SmsConfig;
import com.leyou.utils.SmsUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Map;

@Component
@EnableConfigurationProperties(SmsConfig.class)
public class SmsListener {

    @Autowired
    private SmsUtils smsUtils;
    @Autowired
    private SmsConfig smsConfig;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "sms.verify.code.queue",durable = "true"),
            exchange = @Exchange(value = "ly.sms.exchange",type = ExchangeTypes.TOPIC),
            key = "sms.verify.code"
    ))
    public void listenVerify(Map<String,String> msg) {
        if (CollectionUtils.isEmpty(msg)) {
            return;
        }
        String phone = msg.remove("phone");
        if (StringUtils.isBlank(phone)) {
            return;
        }
        smsUtils.sendSms(phone,smsConfig.getSignName(),smsConfig.getVerifyCodeTemplate(), JsonUtils.serialize(msg));

    }
}
