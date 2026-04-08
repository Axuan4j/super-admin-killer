package com.sak.service.dto;

import lombok.Data;

@Data
public class PageQuery {
    private Long pageNum = 1L;
    private Long pageSize = 10L;
    private String orderField;
    private String orderDirection = "desc";
    private Boolean searchCount = true;

    public void setCurrent(Long current) {
        if (current != null) {
            this.pageNum = current;
        }
    }

    public void setSize(Long size) {
        if (size != null) {
            this.pageSize = size;
        }
    }
}
