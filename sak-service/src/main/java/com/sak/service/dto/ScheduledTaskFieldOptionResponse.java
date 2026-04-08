package com.sak.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTaskFieldOptionResponse {
    private String label;
    private Object value;
}
