package com.leyou.config;

import com.leyou.auth.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.security.PublicKey;

@Data
@ConfigurationProperties(prefix = "ly.jwt")
public class JwtProperties {
    private String pubKeyPath;
    private String cookieName;
    private PublicKey publicKey;
    @PostConstruct
    public void init() throws Exception {
        // 存在则初始化公钥和私钥
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
    }
}
