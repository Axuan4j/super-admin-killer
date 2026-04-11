package com.sak.service.service;

import com.sak.service.dto.DictAdminQueryRequest;
import com.sak.service.dto.DictAdminResponse;
import com.sak.service.dto.DictSaveRequest;
import com.sak.service.dto.PageResponse;

public interface AdminDictService {
    PageResponse<DictAdminResponse> listDictItems(DictAdminQueryRequest request);

    DictAdminResponse createDictItem(DictSaveRequest request);

    DictAdminResponse updateDictItem(Long id, DictSaveRequest request);

    void deleteDictItem(Long id);
}
