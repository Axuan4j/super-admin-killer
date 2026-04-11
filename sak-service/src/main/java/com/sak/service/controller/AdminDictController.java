package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.DictAdminQueryRequest;
import com.sak.service.dto.DictAdminResponse;
import com.sak.service.dto.DictSaveRequest;
import com.sak.service.dto.PageResponse;
import com.sak.service.service.AdminDictService;
import com.sak.service.vo.DictSaveVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/dicts")
@RequiredArgsConstructor
public class AdminDictController {

    private final AdminDictService adminDictService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:dict:view')")
    public Result<PageResponse<DictAdminResponse>> listDictItems(DictAdminQueryRequest request) {
        return Result.success(adminDictService.listDictItems(request));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:dict:add')")
    public Result<DictAdminResponse> createDictItem(@Valid @RequestBody DictSaveVO request) {
        return Result.success(adminDictService.createDictItem(toRequest(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dict:edit')")
    public Result<DictAdminResponse> updateDictItem(@PathVariable("id") Long id, @Valid @RequestBody DictSaveVO request) {
        return Result.success(adminDictService.updateDictItem(id, toRequest(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dict:remove')")
    public Result<Void> deleteDictItem(@PathVariable("id") Long id) {
        adminDictService.deleteDictItem(id);
        return Result.success();
    }

    private DictSaveRequest toRequest(DictSaveVO request) {
        DictSaveRequest saveRequest = new DictSaveRequest();
        saveRequest.setDictType(request.getDictType());
        saveRequest.setDictLabel(request.getDictLabel());
        saveRequest.setDictValue(request.getDictValue());
        saveRequest.setTagType(request.getTagType());
        saveRequest.setTagColor(request.getTagColor());
        saveRequest.setOrderNum(request.getOrderNum());
        saveRequest.setStatus(request.getStatus());
        saveRequest.setRemark(request.getRemark());
        return saveRequest;
    }
}
