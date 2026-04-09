package com.sak.service.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationRecordDetailResponse extends NotificationRecordResponse {
    private List<RecipientDetail> recipientDetails = new ArrayList<>();

    @Data
    public static class RecipientDetail {
        private Long userId;
        private String username;
        private String nickName;
        private String status;
        private List<ChannelDetail> channels = new ArrayList<>();
    }

    @Data
    public static class ChannelDetail {
        private String channelCode;
        private String status;
        private String message;
    }
}
