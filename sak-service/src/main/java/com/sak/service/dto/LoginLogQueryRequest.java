package com.sak.service.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LoginLogQueryRequest extends PageQuery {
    private String username;
    private String loginIp;
    private Integer status;
}
