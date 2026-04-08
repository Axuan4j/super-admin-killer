package com.sak.service.service;

import com.sak.service.dto.FileRecordQueryRequest;
import com.sak.service.dto.FileRecordResponse;
import com.sak.service.dto.PageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileCenterService {

    PageResponse<FileRecordResponse> listFiles(FileRecordQueryRequest request);

    FileRecordResponse uploadFile(MultipartFile file, String bizType, String remark, String operator);

    void deleteFile(Long id);
}
