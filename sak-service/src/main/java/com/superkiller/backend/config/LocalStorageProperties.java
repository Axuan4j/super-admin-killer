package com.superkiller.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "storage")
public class LocalStorageProperties {
    private String rootDir = "./storage";
    private String avatarDir = "avatars";
    private String accessPath = "/storage/";
}
