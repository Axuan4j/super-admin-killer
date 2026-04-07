package com.sak.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

    private final Wxpusher wxpusher = new Wxpusher();

    @Data
    public static class Wxpusher {
        private boolean enabled;
        private String appToken;
        private String baseUrl = "https://wxpusher.zjiecode.com";
        private Integer contentType = 1;
    }
}
