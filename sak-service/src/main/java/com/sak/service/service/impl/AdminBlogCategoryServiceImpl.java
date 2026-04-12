package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.sak.service.dto.BlogCategoryQueryRequest;
import com.sak.service.dto.BlogCategoryResponse;
import com.sak.service.dto.BlogCategorySaveRequest;
import com.sak.service.dto.PageResponse;
import com.sak.service.entity.BlogCategory;
import com.sak.service.mapper.BlogCategoryMapper;
import com.sak.service.service.AdminBlogCategoryService;
import com.sak.service.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminBlogCategoryServiceImpl implements AdminBlogCategoryService {

    private static final Map<String, String> CATEGORY_SORT_FIELDS = new LinkedHashMap<>();

    static {
        CATEGORY_SORT_FIELDS.put("id", "id");
        CATEGORY_SORT_FIELDS.put("categoryName", "category_name");
        CATEGORY_SORT_FIELDS.put("slug", "slug");
        CATEGORY_SORT_FIELDS.put("orderNum", "order_num");
        CATEGORY_SORT_FIELDS.put("status", "status");
        CATEGORY_SORT_FIELDS.put("createTime", "create_time");
        CATEGORY_SORT_FIELDS.put("updateTime", "update_time");
    }

    private final BlogCategoryMapper blogCategoryMapper;

    @Override
    public PageResponse<BlogCategoryResponse> listCategories(BlogCategoryQueryRequest request) {
        LambdaQueryWrapper<BlogCategory> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(BlogCategory::getCategoryName, request.getKeyword())
                    .or()
                    .like(BlogCategory::getSlug, request.getKeyword())
                    .or()
                    .like(BlogCategory::getDescription, request.getKeyword()));
        }
        if (StringUtils.hasText(request.getStatus())) {
            queryWrapper.eq(BlogCategory::getStatus, request.getStatus());
        }

        Page<BlogCategory> page = blogCategoryMapper.selectPage(
                PageUtils.buildPage(request, CATEGORY_SORT_FIELDS, "order_num", "asc"),
                queryWrapper
        );
        List<BlogCategoryResponse> records = page.getRecords().stream().map(this::toResponse).toList();
        return PageUtils.toResponse(page, records, request);
    }

    @Override
    @LogRecord(success = "新增博客分类：{{#p0.categoryName}}", fail = "新增博客分类失败：{{#p0.categoryName}}", type = "BLOG_CATEGORY", subType = "CREATE", bizNo = "{{#_ret.id}}")
    public BlogCategoryResponse createCategory(BlogCategorySaveRequest request) {
        validateUnique(request.getSlug(), null);
        BlogCategory category = new BlogCategory();
        applyRequest(category, request);
        blogCategoryMapper.insert(category);
        return toResponse(category);
    }

    @Override
    @LogRecord(success = "编辑博客分类：{{#p1.categoryName}}", fail = "编辑博客分类失败：{{#p1.categoryName}}", type = "BLOG_CATEGORY", subType = "UPDATE", bizNo = "{{#p0}}")
    public BlogCategoryResponse updateCategory(Long id, BlogCategorySaveRequest request) {
        BlogCategory category = requireCategory(id);
        validateUnique(request.getSlug(), id);
        applyRequest(category, request);
        blogCategoryMapper.updateById(category);
        return toResponse(category);
    }

    @Override
    @LogRecord(success = "删除博客分类：{{#p0}}", fail = "删除博客分类失败：{{#p0}}", type = "BLOG_CATEGORY", subType = "DELETE", bizNo = "{{#p0}}")
    public void deleteCategory(Long id) {
        requireCategory(id);
        blogCategoryMapper.deleteById(id);
    }

    private void applyRequest(BlogCategory category, BlogCategorySaveRequest request) {
        category.setCategoryName(request.getCategoryName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());
        category.setCoverImage(request.getCoverImage());
        category.setOrderNum(request.getOrderNum() == null ? 0 : request.getOrderNum());
        category.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "0");
        category.setRemark(request.getRemark());
    }

    private BlogCategoryResponse toResponse(BlogCategory category) {
        BlogCategoryResponse response = new BlogCategoryResponse();
        response.setId(category.getId());
        response.setCategoryName(category.getCategoryName());
        response.setSlug(category.getSlug());
        response.setDescription(category.getDescription());
        response.setCoverImage(category.getCoverImage());
        response.setOrderNum(category.getOrderNum());
        response.setStatus(category.getStatus());
        response.setRemark(category.getRemark());
        response.setCreateTime(category.getCreateTime());
        response.setUpdateTime(category.getUpdateTime());
        return response;
    }

    private BlogCategory requireCategory(Long id) {
        BlogCategory category = blogCategoryMapper.selectById(id);
        if (category == null) {
            throw new IllegalArgumentException("分类不存在");
        }
        return category;
    }

    private void validateUnique(String slug, Long excludeId) {
        if (!StringUtils.hasText(slug)) {
            return;
        }
        BlogCategory existing = blogCategoryMapper.selectOne(new LambdaQueryWrapper<BlogCategory>()
                .eq(BlogCategory::getSlug, slug)
                .last("limit 1"));
        if (existing != null && !Objects.equals(existing.getId(), excludeId)) {
            throw new IllegalArgumentException("分类别名已存在");
        }
    }
}
