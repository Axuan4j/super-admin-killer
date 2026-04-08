package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.FileRecordQueryRequest;
import com.sak.service.dto.FileRecordResponse;
import com.sak.service.dto.PageResponse;
import com.sak.service.service.FileCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    public Result<Void> deleteFile(@PathVariable("id") Long id) {
        fileCenterService.deleteFile(id);
        return Result.success();
    }
}
