package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.FileRecordQueryRequest;
import com.sak.service.dto.FileRecordResponse;
import com.sak.service.dto.FileShareLinkRequest;
import com.sak.service.dto.FileShareLinkResponse;
import com.sak.service.dto.PageResponse;
import com.sak.service.dto.PublicFilePayload;
import com.sak.service.service.FileCenterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/system/files")
@RequiredArgsConstructor
public class FileCenterController {

    private final FileCenterService fileCenterService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:file:view')")
    public Result<PageResponse<FileRecordResponse>> listFiles(FileRecordQueryRequest request) {
        return Result.success(fileCenterService.listFiles(request));
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('system:file:upload')")
    public Result<FileRecordResponse> uploadFile(Authentication authentication,
                                                 @RequestParam("file") MultipartFile file,
                                                 @RequestParam(value = "bizType", required = false) String bizType,
                                                 @RequestParam(value = "remark", required = false) String remark) {
        String operator = authentication == null ? "系统" : authentication.getName();
        return Result.success(fileCenterService.uploadFile(file, bizType, remark, operator));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:file:remove')")
    public Result<Void> deleteFile(@PathVariable Long id) {
        fileCenterService.deleteFile(id);
        return Result.success();
    }

    @PostMapping("/{id}/share")
    @PreAuthorize("hasAuthority('system:file:view')")
    public Result<FileShareLinkResponse> createShareLink(@PathVariable Long id,
                                                         @Valid @RequestBody(required = false) FileShareLinkRequest request) {
        return Result.success(fileCenterService.createShareLink(id, request == null ? new FileShareLinkRequest() : request));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<Resource> accessSharedFile(@PathVariable Long id,
                                                     @RequestParam(value = "expires", required = false) Long expires,
                                                     @RequestParam("signature") String signature) {
        PublicFilePayload payload = fileCenterService.loadSharedFile(id, expires, signature);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (payload.getContentType() != null && !payload.getContentType().isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(payload.getContentType());
            } catch (Exception ignored) {
            }
        }
        Resource resource = new FileSystemResource(payload.getPath());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(payload.getFileSize() == null ? 0L : payload.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(payload.getFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(resource);
    }
}
