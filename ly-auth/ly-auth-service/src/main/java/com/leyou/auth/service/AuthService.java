package com.leyou.auth.service;

import com.leyou.auth.client.AuthClient;
import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.item.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {

    @Autowired
    private AuthClient authClient;
    @Autowired
    private JwtProperties properties;

    public String login(String username, String password) {
        try {
            // 校验用户信息
            User user = authClient.queryUser(username, password);
            if (user == null) {
                throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
            }
            // 生成token
            return JwtUtils.generateToken(new UserInfo(), properties.getPrivateKey(), properties.getExpire());
        } catch (Exception e) {
            log.error("[授权中心] 用户凭证生成失败，用户名：{}",username,e);
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
    }
}
