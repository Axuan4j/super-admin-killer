package com.sak.service.service;

import com.sak.service.dto.NotificationRecordDetailResponse;
import com.sak.service.dto.NotificationRecordQueryRequest;
import com.sak.service.dto.NotificationRecordResponse;
import com.sak.service.dto.PageResponse;

public interface NotificationRecordService {

    void saveRecord(NotificationRecordDetailResponse record);

    void updateRecord(NotificationRecordDetailResponse record);

    PageResponse<NotificationRecordResponse> listRecords(NotificationRecordQueryRequest request);

    NotificationRecordDetailResponse getRecordDetail(Long id);
}
