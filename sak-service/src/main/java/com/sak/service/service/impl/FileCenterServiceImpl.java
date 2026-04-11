package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sak.service.config.StaticResourceConfig;
import com.sak.service.dto.FileRecordQueryRequest;
import com.sak.service.dto.FileRecordResponse;
import com.sak.service.dto.FileShareLinkRequest;
import com.sak.service.dto.FileShareLinkResponse;
import com.sak.service.dto.PageResponse;
import com.sak.service.dto.PublicFilePayload;
import com.sak.service.entity.SysFileRecord;
import com.sak.service.mapper.SysFileRecordMapper;
import com.sak.service.service.FileCenterService;
import com.sak.service.util.FileShareSignatureUtil;
import com.sak.service.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileCenterServiceImpl implements FileCenterService {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Map<String, String> FILE_RECORD_SORT_FIELDS = new LinkedHashMap<>();

    static {
        FILE_RECORD_SORT_FIELDS.put("id", "id");
        FILE_RECORD_SORT_FIELDS.put("bizType", "biz_type");
        FILE_RECORD_SORT_FIELDS.put("fileName", "file_name");
        FILE_RECORD_SORT_FIELDS.put("fileSize", "file_size");
        FILE_RECORD_SORT_FIELDS.put("operator", "operator");
        FILE_RECORD_SORT_FIELDS.put("createTime", "create_time");
    }

    private final SysFileRecordMapper sysFileRecordMapper;
    private final StaticResourceConfig staticResourceConfig;

    @Value("${server.port:8080}")
    private String serverPort;

    @Override
    public PageResponse<FileRecordResponse> listFiles(FileRecordQueryRequest request) {
        LambdaQueryWrapper<SysFileRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getBizType())) {
            wrapper.eq(SysFileRecord::getBizType, normalizeBizType(request.getBizType()));
        }
        if (StringUtils.hasText(request.getOperator())) {
            wrapper.like(SysFileRecord::getOperator, request.getOperator().trim());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            String keyword = request.getKeyword().trim();
            wrapper.and(q -> q.like(SysFileRecord::getFileName, keyword)
                    .or()
                    .like(SysFileRecord::getStorageName, keyword)
                    .or()
                    .like(SysFileRecord::getRemark, keyword)
                    .or()
                    .like(SysFileRecord::getFilePath, keyword));
        }

        Page<SysFileRecord> page = sysFileRecordMapper.selectPage(
                PageUtils.buildPage(request, FILE_RECORD_SORT_FIELDS, "id", "desc"),
                wrapper
        );
        return PageUtils.toResponse(page, page.getRecords().stream().map(this::toResponse).toList(), request);
    }

    @Override
    @Transactional
    public FileRecordResponse uploadFile(MultipartFile file, String bizType, String remark, String operator) {
        validateFile(file);

        String normalizedBizType = normalizeBizType(bizType);
        String originalFileName = normalizeOriginalFileName(file.getOriginalFilename());
        String fileExt = resolveFileExt(originalFileName);
        String storageName = buildStorageName(fileExt);
        String directoryName = buildStorageDirectory(normalizedBizType);
        Path targetPath = staticResourceConfig.createFileCenterFile(directoryName + "/" + storageName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("文件上传失败", ex);
        }

        SysFileRecord record = new SysFileRecord();
        LocalDateTime now = LocalDateTime.now();
        record.setBizType(normalizedBizType);
        record.setFileName(originalFileName);
        record.setStorageName(storageName);
        record.setFilePath(staticResourceConfig.toStorageRelativePath(targetPath));
        record.setContentType(StringUtils.hasText(file.getContentType()) ? file.getContentType().trim() : "application/octet-stream");
        record.setFileExt(fileExt);
        record.setFileSize(file.getSize());
        record.setOperator(StringUtils.hasText(operator) ? operator.trim() : "系统");
        record.setRemark(StringUtils.hasText(remark) ? remark.trim() : null);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        sysFileRecordMapper.insert(record);
        return toResponse(record);
    }

    @Override
    @Transactional
    public void deleteFile(Long id) {
        SysFileRecord record = requireRecord(id);
        try {
            Files.deleteIfExists(staticResourceConfig.resolveStorageRelativePath(record.getFilePath()));
        } catch (IOException ex) {
            throw new IllegalStateException("删除文件失败", ex);
        }
        sysFileRecordMapper.deleteById(id);
    }

    @Override
    public FileShareLinkResponse createShareLink(Long id, FileShareLinkRequest request) {
        SysFileRecord record = requireRecord(id);
        FileShareLinkRequest resolvedRequest = request == null ? new FileShareLinkRequest() : request;
        boolean permanent = Boolean.TRUE.equals(resolvedRequest.getPermanent());
        int expireDays = resolveExpireDays(resolvedRequest.getExpireDays(), permanent);
        Long expires = permanent ? null : System.currentTimeMillis() + expireDays * 24L * 60L * 60L * 1000L;
        String signature = buildSignature(id, expires);

        FileShareLinkResponse response = new FileShareLinkResponse();
        response.setFileId(record.getId());
        response.setFileName(record.getFileName());
        response.setPermanent(permanent);
        response.setExpireDays(permanent ? null : expireDays);
        response.setExpireAt(expires == null ? null : LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(expires), ZoneId.systemDefault()));
        response.setShareUrl(buildShareUrl(id, expires, signature));
        return response;
    }

    @Override
    public PublicFilePayload loadSharedFile(Long id, Long expires, String signature) {
        SysFileRecord record = requireRecord(id);
        validateShareSignature(id, expires, signature);

        Path filePath = staticResourceConfig.resolveStorageRelativePath(record.getFilePath());
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("分享文件不存在");
        }
        return new PublicFilePayload(filePath, record.getFileName(), record.getContentType(), record.getFileSize());
    }

    private SysFileRecord requireRecord(Long id) {
        SysFileRecord record = sysFileRecordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("文件记录不存在");
        }
        return record;
    }

    private FileRecordResponse toResponse(SysFileRecord record) {
        FileRecordResponse response = new FileRecordResponse();
        response.setId(record.getId());
        response.setBizType(record.getBizType());
        response.setFileName(record.getFileName());
        response.setStorageName(record.getStorageName());
        response.setFilePath(record.getFilePath());
        response.setDownloadUrl(staticResourceConfig.getStorageAccessPrefix() + normalizeRelativePath(record.getFilePath()));
        response.setContentType(record.getContentType());
        response.setFileExt(record.getFileExt());
        response.setFileSize(record.getFileSize());
        response.setOperator(record.getOperator());
        response.setRemark(record.getRemark());
        response.setCreateTime(record.getCreateTime());
        return response;
    }

    private int resolveExpireDays(Integer expireDays, boolean permanent) {
        if (permanent) {
            return 0;
        }
        int defaultExpireDays = Math.max(1, staticResourceConfig.getShareDefaultExpireDays());
        int maxExpireDays = Math.max(defaultExpireDays, staticResourceConfig.getShareMaxExpireDays());
        int requestedDays = expireDays == null ? defaultExpireDays : expireDays;
        if (requestedDays < 1 || requestedDays > maxExpireDays) {
            throw new IllegalArgumentException("有效天数必须在1到" + maxExpireDays + "天之间");
        }
        return requestedDays;
    }

    private void validateShareSignature(Long id, Long expires, String signature) {
        if (!StringUtils.hasText(signature)) {
            throw new IllegalArgumentException("缺少分享签名");
        }
        if (expires != null && expires < System.currentTimeMillis()) {
            throw new IllegalArgumentException("分享链接已过期");
        }
        String expectedSignature = buildSignature(id, expires);
        if (!expectedSignature.equals(signature)) {
            throw new IllegalArgumentException("分享签名无效");
        }
    }

    private String buildShareUrl(Long id, Long expires, String signature) {
        StringBuilder url = new StringBuilder();
        url.append(resolvePublicBaseUrl())
                .append("/system/files/public/")
                .append(id)
                .append("?signature=")
                .append(signature);
        if (expires != null) {
            url.append("&expires=").append(expires);
        }
        return url.toString();
    }

    private String resolvePublicBaseUrl() {
        String configuredBaseUrl = staticResourceConfig.getSharePublicBaseUrl();
        if (StringUtils.hasText(configuredBaseUrl)) {
            return configuredBaseUrl.replaceAll("/+$", "");
        }
        return "http://localhost:" + serverPort;
    }

    private String buildSignature(Long id, Long expires) {
        String payload = id + ":" + (expires == null ? "permanent" : expires);
        return FileShareSignatureUtil.sign(staticResourceConfig.getShareSecret(), payload);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择上传文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("单个文件大小不能超过 50MB");
        }
    }

    private String normalizeBizType(String bizType) {
        if (!StringUtils.hasText(bizType)) {
            return "COMMON";
        }
        return bizType.trim().toUpperCase(Locale.ROOT);
    }

    private String buildStorageDirectory(String bizType) {
        return sanitizeDirectoryName(bizType) + "/" + DATE_PATH_FORMATTER.format(LocalDate.now());
    }

    private String sanitizeDirectoryName(String bizType) {
        String sanitized = bizType.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "-").replaceAll("^-+|-+$", "");
        return sanitized.isBlank() ? "common" : sanitized;
    }

    private String buildStorageName(String fileExt) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        if (!StringUtils.hasText(fileExt)) {
            return uuid;
        }
        return uuid + "." + fileExt;
    }

    private String normalizeOriginalFileName(String originalFileName) {
        if (!StringUtils.hasText(originalFileName)) {
            return "unnamed-file";
        }
        String normalized = originalFileName.replace("\\", "/");
        int lastSlashIndex = normalized.lastIndexOf('/');
        return lastSlashIndex >= 0 ? normalized.substring(lastSlashIndex + 1) : normalized;
    }

    private String resolveFileExt(String originalFileName) {
        if (!StringUtils.hasText(originalFileName) || !originalFileName.contains(".")) {
            return null;
        }
        return originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeRelativePath(String filePath) {
        return filePath.replace("\\", "/").replaceAll("^/+", "");
    }
}
