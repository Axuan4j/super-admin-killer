package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.DictItemResponse;
import com.sak.service.service.SysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/dicts")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictService sysDictService;

    @GetMapping("/all")
    public Result<Map<String, List<DictItemResponse>>> getAllDicts() {
        return Result.success(sysDictService.getAllDictMap());
    }
}
