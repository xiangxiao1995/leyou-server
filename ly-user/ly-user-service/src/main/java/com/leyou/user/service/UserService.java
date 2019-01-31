package com.leyou.user.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.NumberUtils;
import com.leyou.item.pojo.User;
import com.leyou.user.config.UserConfig;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.utils.CodecUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
@EnableConfigurationProperties(UserConfig.class)
public class UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AmqpTemplate amqpTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private UserConfig userConfig;

    private static final String KEY_PREFIX = "user:verify:phone:";

    /**
     * 校验数据
     * @param data 要校验的数据
     * @param type 要校验的数据类型：1，用户名；2，手机；
     * @return
     */
    public Boolean checkData(String data, Integer type) {
        // 判断数据类型
        User record = new User();
        switch (type) {
            case 1: // 校验用户名
                record.setUsername(data);
                break;
            case 2: // 校验手机号
                record.setPhone(data);
                break;
            default:
                throw new LyException(ExceptionEnum.INVALID_USER_DATA_TYPE);
        }
        // 判断数据是否存在
        return userMapper.selectCount(record) == 0;
    }

    /**
     * 根据用户输入的手机号，生成随机验证码，长度为6位，纯数字。并且调用短信服务，发送验证码到用户手机。
     * @param phone 手机号
     * @return
     */
    public void sendCode(String phone) {
        String key = KEY_PREFIX + phone;
        // 生成6位数的随机验证码
        String code = NumberUtils.generateCode(6);
        // 发送消息到MQ，调用短信服务
        Map<String, String> msg = new HashMap<>();
        msg.put("phone", phone);
        msg.put("code", code);
        amqpTemplate.convertAndSend(userConfig.getExchange(),userConfig.getRoutingKey(),msg);
        // 存储验证码到redis中
        redisTemplate.opsForValue().set(key,code,userConfig.getTimeOutInMinutes(), TimeUnit.MINUTES);
    }

    /**
     * 用户注册
     * @param user 用户信息
     * @param code 验证码
     * @return
     */
    public void register(User user, String code) {
        // 校验验证码
        String cacheCode = redisTemplate.opsForValue().get(KEY_PREFIX + user.getPhone());
        if (!StringUtils.equals(cacheCode, code)) {
            throw new LyException(ExceptionEnum.INVALID_VERIFY_CODE);
        }
        // 密码加密
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);
        user.setPassword(CodecUtils.md5Hex(user.getPassword(), salt));
        // 存储用户信息
        userMapper.insert(user);
    }

    public User queryUser(String username, String password) {
        // 查询用户
        User record = new User();
        record.setUsername(username);
        User user = userMapper.selectOne(record);
        if (user == null) {
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
        // 验证密码
        if (!StringUtils.equals(user.getPassword(), CodecUtils.md5Hex(password, user.getSalt()))) {
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
        return user;
    }
}
