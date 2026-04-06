package com.superkiller.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@EnableConfigurationProperties(LocalStorageProperties.class)
public class StaticResourceConfig implements WebMvcConfigurer {

    private final LocalStorageProperties storageProperties;

    public StaticResourceConfig(LocalStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
        ensureStorageDirectory();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String accessPattern = normalizeAccessPath(storageProperties.getAccessPath()) + "**";
        String location = Paths.get(storageProperties.getRootDir()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(accessPattern).addResourceLocations(location);
    }

    public Path getAvatarStorageDir() {
        Path root = Paths.get(storageProperties.getRootDir()).toAbsolutePath().normalize();
        Path avatarDir = root.resolve(storageProperties.getAvatarDir()).normalize();
        if (!avatarDir.startsWith(root)) {
            throw new IllegalStateException("Avatar storage directory is outside the configured root directory");
        }
        return avatarDir;
    }

    public String getAvatarAccessPrefix() {
        return normalizeAccessPath(storageProperties.getAccessPath()) + trimSlashes(storageProperties.getAvatarDir()) + "/";
    }

    private void ensureStorageDirectory() {
        try {
            Files.createDirectories(getAvatarStorageDir());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize local storage directory", e);
        }
    }

    private String normalizeAccessPath(String accessPath) {
        String normalized = accessPath == null || accessPath.isBlank() ? "/storage/" : accessPath.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }

    private String trimSlashes(String value) {
        return value == null ? "" : value.replace("\\", "/").replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
