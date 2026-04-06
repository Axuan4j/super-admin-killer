package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sak.service.dto.DictItemResponse;
import com.sak.service.entity.SysDictItem;
import com.sak.service.mapper.SysDictItemMapper;
import com.sak.service.service.SysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysDictServiceImpl implements SysDictService {

    private final SysDictItemMapper sysDictItemMapper;

    @Override
    @Cacheable(cacheNames = "sys-dicts-all")
    public Map<String, List<DictItemResponse>> getAllDictMap() {
        List<SysDictItem> dictItems = sysDictItemMapper.selectList(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getStatus, "0")
                .orderByAsc(SysDictItem::getDictType, SysDictItem::getOrderNum, SysDictItem::getId));

        return dictItems.stream()
                .map(item -> new DictItemResponse(
                        item.getDictType(),
                        item.getDictLabel(),
                        item.getDictValue(),
                        item.getTagType(),
                        item.getTagColor(),
                        item.getOrderNum()
                ))
                .collect(Collectors.groupingBy(
                        DictItemResponse::getDictType,
                        Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
                                .sorted(Comparator.comparing(DictItemResponse::getOrderNum, Comparator.nullsLast(Integer::compareTo))
                                        .thenComparing(DictItemResponse::getValue, Comparator.nullsLast(String::compareTo)))
                                .toList())
                ));
    }
}
