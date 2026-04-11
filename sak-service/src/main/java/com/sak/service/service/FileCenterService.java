package com.sak.service.service;

import com.sak.service.dto.FileRecordQueryRequest;
import com.sak.service.dto.FileRecordResponse;
import com.sak.service.dto.FileShareLinkRequest;
import com.sak.service.dto.FileShareLinkResponse;
import com.sak.service.dto.PageResponse;
import com.sak.service.dto.PublicFilePayload;
import org.springframework.web.multipart.MultipartFile;

public interface FileCenterService {

    PageResponse<FileRecordResponse> listFiles(FileRecordQueryRequest request);

    FileRecordResponse uploadFile(MultipartFile file, String bizType, String remark, String operator);

    void deleteFile(Long id);

    FileShareLinkResponse createShareLink(Long id, FileShareLinkRequest request);

    PublicFilePayload loadSharedFile(Long id, Long expires, String signature);
}
