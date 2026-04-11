package com.sak.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.file.Path;

@Data
@AllArgsConstructor
public class PublicFilePayload {
    private Path path;
    private String fileName;
    private String contentType;
    private Long fileSize;
}
