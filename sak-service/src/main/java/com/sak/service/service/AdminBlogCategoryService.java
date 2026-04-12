package com.sak.service.service;

import com.sak.service.dto.BlogCategoryQueryRequest;
import com.sak.service.dto.BlogCategoryResponse;
import com.sak.service.dto.BlogCategorySaveRequest;
import com.sak.service.dto.PageResponse;

public interface AdminBlogCategoryService {
    PageResponse<BlogCategoryResponse> listCategories(BlogCategoryQueryRequest request);

    BlogCategoryResponse createCategory(BlogCategorySaveRequest request);

    BlogCategoryResponse updateCategory(Long id, BlogCategorySaveRequest request);

    void deleteCategory(Long id);
}
