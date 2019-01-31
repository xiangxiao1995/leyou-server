package com.leyou.upload.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 上传的属性文件，从application.yml中读取前缀为ly.upload的属性，并注入
 */
@ConfigurationProperties(prefix = "ly.upload")
@Data
public class UploadProperties {

    private String baseUrl;
    private List<String> allowTypes;
}
