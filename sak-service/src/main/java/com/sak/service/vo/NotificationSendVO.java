package com.sak.service.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class NotificationSendVO {

    private Boolean sendAll;

    private List<Long> userIds;

    private List<String> channels;

    @NotBlank(message = "通知标题不能为空")
    @Size(max = 128, message = "通知标题长度不能超过128位")
    private String title;

    @NotBlank(message = "通知内容不能为空")
    @Size(max = 5000, message = "通知内容长度不能超过5000位")
    private String content;
}
