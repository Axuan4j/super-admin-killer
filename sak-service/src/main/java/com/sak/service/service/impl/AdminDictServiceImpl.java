package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.sak.service.dto.DictAdminQueryRequest;
import com.sak.service.dto.DictAdminResponse;
import com.sak.service.dto.DictSaveRequest;
import com.sak.service.dto.PageResponse;
import com.sak.service.entity.SysDictItem;
import com.sak.service.mapper.SysDictItemMapper;
import com.sak.service.service.AdminDictService;
import com.sak.service.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminDictServiceImpl implements AdminDictService {

    private static final String SYS_DICTS_ALL_CACHE = "sys-dicts-all";
    private static final Map<String, String> DICT_SORT_FIELDS = new LinkedHashMap<>();

    static {
        DICT_SORT_FIELDS.put("id", "id");
        DICT_SORT_FIELDS.put("dictType", "dict_type");
        DICT_SORT_FIELDS.put("dictLabel", "dict_label");
        DICT_SORT_FIELDS.put("dictValue", "dict_value");
        DICT_SORT_FIELDS.put("orderNum", "order_num");
        DICT_SORT_FIELDS.put("status", "status");
        DICT_SORT_FIELDS.put("createTime", "create_time");
        DICT_SORT_FIELDS.put("updateTime", "update_time");
    }

    private final SysDictItemMapper sysDictItemMapper;
    private final CacheManager cacheManager;

    @Override
    public PageResponse<DictAdminResponse> listDictItems(DictAdminQueryRequest request) {
        LambdaQueryWrapper<SysDictItem> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(SysDictItem::getDictType, request.getKeyword())
                    .or()
                    .like(SysDictItem::getDictLabel, request.getKeyword())
                    .or()
                    .like(SysDictItem::getDictValue, request.getKeyword())
                    .or()
                    .like(SysDictItem::getRemark, request.getKeyword()));
        }
        if (StringUtils.hasText(request.getDictType())) {
            queryWrapper.eq(SysDictItem::getDictType, request.getDictType());
        }
        if (StringUtils.hasText(request.getStatus())) {
            queryWrapper.eq(SysDictItem::getStatus, request.getStatus());
        }

        Page<SysDictItem> page = sysDictItemMapper.selectPage(
                PageUtils.buildPage(request, DICT_SORT_FIELDS, "dict_type", "asc"),
                queryWrapper
        );
        List<DictAdminResponse> records = page.getRecords().stream().map(this::toResponse).toList();
        return PageUtils.toResponse(page, records, request);
    }

    @Override
    @LogRecord(success = "新增字典项：{{#p0.dictType}}/{{#p0.dictValue}}", fail = "新增字典项失败：{{#p0.dictType}}/{{#p0.dictValue}}", type = "DICT", subType = "CREATE", bizNo = "{{#_ret.id}}")
    public DictAdminResponse createDictItem(DictSaveRequest request) {
        validateUnique(request.getDictType(), request.getDictValue(), null);
        SysDictItem item = new SysDictItem();
        applyRequest(item, request);
        sysDictItemMapper.insert(item);
        clearDictCache();
        return toResponse(item);
    }

    @Override
    @LogRecord(success = "编辑字典项：{{#p1.dictType}}/{{#p1.dictValue}}", fail = "编辑字典项失败：{{#p1.dictType}}/{{#p1.dictValue}}", type = "DICT", subType = "UPDATE", bizNo = "{{#p0}}")
    public DictAdminResponse updateDictItem(Long id, DictSaveRequest request) {
        SysDictItem item = requireDictItem(id);
        validateUnique(request.getDictType(), request.getDictValue(), id);
        applyRequest(item, request);
        sysDictItemMapper.updateById(item);
        clearDictCache();
        return toResponse(item);
    }

    @Override
    @LogRecord(success = "删除字典项：{{#p0}}", fail = "删除字典项失败：{{#p0}}", type = "DICT", subType = "DELETE", bizNo = "{{#p0}}")
    public void deleteDictItem(Long id) {
        requireDictItem(id);
        sysDictItemMapper.deleteById(id);
        clearDictCache();
    }

    private void applyRequest(SysDictItem item, DictSaveRequest request) {
        item.setDictType(request.getDictType());
        item.setDictLabel(request.getDictLabel());
        item.setDictValue(request.getDictValue());
        item.setTagType(request.getTagType());
        item.setTagColor(request.getTagColor());
        item.setOrderNum(request.getOrderNum() == null ? 0 : request.getOrderNum());
        item.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "0");
        item.setRemark(request.getRemark());
    }

    private DictAdminResponse toResponse(SysDictItem item) {
        DictAdminResponse response = new DictAdminResponse();
        response.setId(item.getId());
        response.setDictType(item.getDictType());
        response.setDictLabel(item.getDictLabel());
        response.setDictValue(item.getDictValue());
        response.setTagType(item.getTagType());
        response.setTagColor(item.getTagColor());
        response.setOrderNum(item.getOrderNum());
        response.setStatus(item.getStatus());
        response.setRemark(item.getRemark());
        response.setCreateTime(item.getCreateTime());
        response.setUpdateTime(item.getUpdateTime());
        return response;
    }

    private SysDictItem requireDictItem(Long id) {
        SysDictItem item = sysDictItemMapper.selectById(id);
        if (item == null) {
            throw new IllegalArgumentException("字典项不存在");
        }
        return item;
    }

    private void validateUnique(String dictType, String dictValue, Long excludeId) {
        SysDictItem existing = sysDictItemMapper.selectOne(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictType, dictType)
                .eq(SysDictItem::getDictValue, dictValue)
                .last("limit 1"));
        if (existing != null && !Objects.equals(existing.getId(), excludeId)) {
            throw new IllegalArgumentException("同一字典类型下键值已存在");
        }
    }

    private void clearDictCache() {
        Cache cache = cacheManager.getCache(SYS_DICTS_ALL_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }
}
