package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.BlogCategoryQueryRequest;
import com.sak.service.dto.BlogCategoryResponse;
import com.sak.service.dto.BlogCategorySaveRequest;
import com.sak.service.dto.PageResponse;
import com.sak.service.service.AdminBlogCategoryService;
import com.sak.service.vo.BlogCategorySaveVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/blog/categories")
@RequiredArgsConstructor
public class AdminBlogCategoryController {

    private final AdminBlogCategoryService adminBlogCategoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('blog:category:view')")
    public Result<PageResponse<BlogCategoryResponse>> listCategories(BlogCategoryQueryRequest request) {
        return Result.success(adminBlogCategoryService.listCategories(request));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('blog:category:add')")
    public Result<BlogCategoryResponse> createCategory(@Valid @RequestBody BlogCategorySaveVO request) {
        return Result.success(adminBlogCategoryService.createCategory(toRequest(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('blog:category:edit')")
    public Result<BlogCategoryResponse> updateCategory(@PathVariable Long id,
                                                       @Valid @RequestBody BlogCategorySaveVO request) {
        return Result.success(adminBlogCategoryService.updateCategory(id, toRequest(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('blog:category:remove')")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        adminBlogCategoryService.deleteCategory(id);
        return Result.success();
    }

    private BlogCategorySaveRequest toRequest(BlogCategorySaveVO request) {
        BlogCategorySaveRequest saveRequest = new BlogCategorySaveRequest();
        saveRequest.setCategoryName(request.getCategoryName());
        saveRequest.setSlug(request.getSlug());
        saveRequest.setDescription(request.getDescription());
        saveRequest.setCoverImage(request.getCoverImage());
        saveRequest.setOrderNum(request.getOrderNum());
        saveRequest.setStatus(request.getStatus());
        saveRequest.setRemark(request.getRemark());
        return saveRequest;
    }
}
