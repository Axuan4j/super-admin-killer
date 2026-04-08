package com.sak.service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ScheduledTaskTypeOptionResponse {
    private String code;
    private String name;
    private String description;
    private List<ScheduledTaskFormFieldResponse> formSchema;

    public ScheduledTaskTypeOptionResponse(String code, String name, String description, List<ScheduledTaskFormFieldResponse> formSchema) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.formSchema = formSchema;
    }
}
