package com.sak.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> records;
    private long total;
    private long pageNum;
    private long pageSize;
    private boolean searchCount;
    private boolean hasMore;
    private long current;
    private long size;
}
