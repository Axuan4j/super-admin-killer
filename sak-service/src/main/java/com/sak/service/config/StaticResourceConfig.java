package com.sak.service.config;

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

    public Path getStorageRootDir() {
        return Paths.get(storageProperties.getRootDir()).toAbsolutePath().normalize();
    }

    public Path getExportStorageDir() {
        return resolveStorageSubDirectory("exports", "Export storage directory is outside the configured root directory");
    }

    public Path getFileCenterStorageDir() {
        return resolveStorageSubDirectory(storageProperties.getFileDir(), "File center storage directory is outside the configured root directory");
    }

    public String getAvatarAccessPrefix() {
        return normalizeAccessPath(storageProperties.getAccessPath()) + trimSlashes(storageProperties.getAvatarDir()) + "/";
    }

    public String getStorageAccessPrefix() {
        return normalizeAccessPath(storageProperties.getAccessPath());
    }

    public Path createExportFile(String relativePath) {
        return createStorageFile(relativePath, "Export file path is outside the configured root directory", "Failed to initialize export storage directory");
    }

    public Path createFileCenterFile(String relativePath) {
        String fileDir = trimSlashes(storageProperties.getFileDir());
        String normalizedRelativePath = trimSlashes(relativePath);
        String combinedPath = fileDir.isEmpty() ? normalizedRelativePath : fileDir + "/" + normalizedRelativePath;
        return createStorageFile(combinedPath, "File center path is outside the configured root directory", "Failed to initialize file center storage directory");
    }

    public String getFileCenterAccessPrefix() {
        return normalizeAccessPath(storageProperties.getAccessPath()) + trimSlashes(storageProperties.getFileDir()) + "/";
    }

    public Path resolveStorageRelativePath(String relativePath) {
        Path root = getStorageRootDir();
        Path normalized = root.resolve(trimSlashes(relativePath)).normalize();
        if (!normalized.startsWith(root)) {
            throw new IllegalStateException("Storage file path is outside the configured root directory");
        }
        return normalized;
    }

    public String toStorageRelativePath(Path filePath) {
        return getStorageRootDir().relativize(filePath.toAbsolutePath().normalize()).toString().replace("\\", "/");
    }

    private void ensureStorageDirectory() {
        try {
            Files.createDirectories(getAvatarStorageDir());
            Files.createDirectories(getExportStorageDir());
            Files.createDirectories(getFileCenterStorageDir());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize local storage directory", e);
        }
    }

    private Path resolveStorageSubDirectory(String relativeDir, String outsideRootMessage) {
        Path root = getStorageRootDir();
        Path targetDir = root.resolve(trimSlashes(relativeDir)).normalize();
        if (!targetDir.startsWith(root)) {
            throw new IllegalStateException(outsideRootMessage);
        }
        return targetDir;
    }

    private Path createStorageFile(String relativePath, String outsideRootMessage, String initializationErrorMessage) {
        Path normalized = resolveStorageRelativePath(relativePath);
        if (!normalized.startsWith(getStorageRootDir())) {
            throw new IllegalStateException(outsideRootMessage);
        }
        try {
            Files.createDirectories(normalized.getParent());
        } catch (Exception e) {
            throw new IllegalStateException(initializationErrorMessage, e);
        }
        return normalized;
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
