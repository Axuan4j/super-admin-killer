package com.sak.service.service;

import com.sak.service.dto.DictItemResponse;

import java.util.List;
import java.util.Map;

public interface SysDictService {
    Map<String, List<DictItemResponse>> getAllDictMap();
}
