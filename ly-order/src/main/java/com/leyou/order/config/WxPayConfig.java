package com.leyou.order.config;

import com.github.wxpay.sdk.WXPayConfig;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.InputStream;
@Component
@EnableConfigurationProperties(PayProperties.class)
public class WxPayConfig implements WXPayConfig {

    @Autowired
    private PayProperties payProperties;

    public String getNotifyUrl() {
        return payProperties.getNotifyUrl();
    } // 通知地址

    @Override
    public String getAppID() {
        return payProperties.getAppId();
    }

    @Override
    public String getMchID() {
        return payProperties.getMchId();
    }

    @Override
    public String getKey() {
        return payProperties.getKey();
    }

    @Override
    public InputStream getCertStream() {
        return null;
    }

    @Override
    public int getHttpConnectTimeoutMs() {
        return payProperties.getConnectTimeoutMs();
    }

    @Override
    public int getHttpReadTimeoutMs() {
        return payProperties.getReadTimeoutMs();
    }
}
