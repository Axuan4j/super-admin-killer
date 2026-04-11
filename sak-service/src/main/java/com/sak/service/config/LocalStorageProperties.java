package com.sak.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "storage")
public class LocalStorageProperties {
    private String rootDir = "./storage";
    private String avatarDir = "avatars";
    private String fileDir = "files";
    private String accessPath = "/storage/";
    private Share share = new Share();

    @Data
    public static class Share {
        private String secret = "superkiller-file-share-secret-change-me";
        private String publicBaseUrl = "";
        private Integer defaultExpireDays = 7;
        private Integer maxExpireDays = 3650;
    }
}
