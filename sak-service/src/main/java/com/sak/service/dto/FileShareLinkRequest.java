package com.sak.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class FileShareLinkRequest {

    private Boolean permanent;

    @Min(value = 1, message = "有效天数不能小于1天")
    @Max(value = 3650, message = "有效天数不能超过3650天")
    private Integer expireDays;
}
