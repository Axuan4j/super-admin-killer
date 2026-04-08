package com.sak.service.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScheduledTaskFormFieldResponse {
    private String field;
    private String label;
    private String component;
    private String placeholder;
    private Boolean required;
    private Object defaultValue;
    private Integer maxLength;
    private Integer minRows;
    private Integer maxRows;
    private String helpText;
    private String visibleWhenField;
    private Object visibleWhenEquals;
    private List<ScheduledTaskFieldOptionResponse> options;
}
