package com.sak.service.util;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sak.service.dto.PageQuery;
import com.sak.service.dto.PageResponse;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PageUtils {

    private static final long DEFAULT_PAGE_NUM = 1L;
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    private PageUtils() {
    }

    public static <T> Page<T> buildPage(PageQuery query, Map<String, String> sortFieldMap, String defaultColumn, String defaultDirection) {
        long pageNum = normalizePageNum(query == null ? null : query.getPageNum());
        long pageSize = normalizePageSize(query == null ? null : query.getPageSize());
        boolean searchCount = shouldSearchCount(query);
        long querySize = searchCount ? pageSize : pageSize + 1;
        Page<T> page = new Page<>(pageNum, querySize, searchCount);

        String orderColumn = resolveOrderColumn(query == null ? null : query.getOrderField(), sortFieldMap, defaultColumn);
        if (StringUtils.hasText(orderColumn)) {
            String direction = normalizeDirection(query == null ? null : query.getOrderDirection(), defaultDirection);
            page.addOrder(isAsc(direction) ? OrderItem.asc(orderColumn) : OrderItem.desc(orderColumn));
        }
        return page;
    }

    public static <T> PageResponse<T> toResponse(Page<?> page, List<T> records, PageQuery query) {
        long pageNum = normalizePageNum(query == null ? null : query.getPageNum());
        long pageSize = normalizePageSize(query == null ? null : query.getPageSize());
        boolean searchCount = shouldSearchCount(query);
        List<T> finalRecords = records;
        boolean hasMore;
        long total;

        if (searchCount) {
            total = page.getTotal();
            hasMore = pageNum * pageSize < total;
        } else {
            hasMore = records.size() > pageSize;
            finalRecords = hasMore ? records.subList(0, (int) pageSize) : records;
            total = 0L;
        }

        return new PageResponse<>(
                finalRecords,
                total,
                pageNum,
                pageSize,
                searchCount,
                hasMore,
                pageNum,
                pageSize
        );
    }

    private static long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum <= 0 ? DEFAULT_PAGE_NUM : pageNum;
    }

    private static long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private static String resolveOrderColumn(String requestedField, Map<String, String> sortFieldMap, String defaultColumn) {
        if (StringUtils.hasText(requestedField) && sortFieldMap != null) {
            String column = sortFieldMap.get(requestedField.trim());
            if (StringUtils.hasText(column)) {
                return column;
            }
        }
        return defaultColumn;
    }

    private static String normalizeDirection(String requestedDirection, String defaultDirection) {
        if (!StringUtils.hasText(requestedDirection)) {
            return StringUtils.hasText(defaultDirection) ? defaultDirection.trim().toLowerCase(Locale.ROOT) : "desc";
        }
        return requestedDirection.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isAsc(String direction) {
        return "asc".equalsIgnoreCase(direction);
    }

    private static boolean shouldSearchCount(PageQuery query) {
        return query == null || query.getSearchCount() == null || query.getSearchCount();
    }
}
