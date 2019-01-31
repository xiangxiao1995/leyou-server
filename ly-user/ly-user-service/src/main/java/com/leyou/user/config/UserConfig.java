package com.leyou.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ly.user")
@Data
public class UserConfig {

    private String exchange;
    private String routingKey;
    private Long timeOutInMinutes;// 验证码过期时间
}
