package com.sak.service.dto;

import lombok.Data;

@Data
public class NotificationRecipientResponse {
    private Long id;
    private String username;
    private String nickName;
    private String email;
    private String status;
}
