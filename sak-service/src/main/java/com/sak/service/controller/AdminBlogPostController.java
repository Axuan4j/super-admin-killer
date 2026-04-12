package com.sak.service.controller;

import com.sak.service.common.Result;
import com.sak.service.dto.BlogOptionResponse;
import com.sak.service.dto.BlogPostDetailResponse;
import com.sak.service.dto.BlogPostListResponse;
import com.sak.service.dto.BlogPostQueryRequest;
import com.sak.service.dto.BlogPostSaveRequest;
import com.sak.service.dto.PageResponse;
import com.sak.service.service.AdminBlogPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/blog")
@RequiredArgsConstructor
public class AdminBlogPostController {

    private final AdminBlogPostService adminBlogPostService;

    @GetMapping("/posts")
    @PreAuthorize("hasAuthority('blog:post:view')")
    public Result<PageResponse<BlogPostListResponse>> listPosts(BlogPostQueryRequest request) {
        return Result.success(adminBlogPostService.listPosts(request));
    }

    @GetMapping("/posts/{id}")
    @PreAuthorize("hasAuthority('blog:post:view')")
    public Result<BlogPostDetailResponse> getPost(@PathVariable Long id) {
        return Result.success(adminBlogPostService.getPostDetail(id));
    }

    @PostMapping("/posts")
    @PreAuthorize("hasAuthority('blog:post:add')")
    public Result<BlogPostDetailResponse> createPost(Authentication authentication,
                                                     @Valid @RequestBody BlogPostSaveRequest request) {
        String operator = authentication == null ? "系统" : authentication.getName();
        return Result.success(adminBlogPostService.createPost(operator, request));
    }

    @PutMapping("/posts/{id}")
    @PreAuthorize("hasAnyAuthority('blog:post:add','blog:post:edit')")
    public Result<BlogPostDetailResponse> updatePost(@PathVariable Long id,
                                                     @Valid @RequestBody BlogPostSaveRequest request) {
        return Result.success(adminBlogPostService.updatePost(id, request));
    }

    @PutMapping("/posts/{id}/publish")
    @PreAuthorize("hasAuthority('blog:post:publish')")
    public Result<BlogPostDetailResponse> publishPost(@PathVariable Long id) {
        return Result.success(adminBlogPostService.publishPost(id));
    }

    @PutMapping("/posts/{id}/offline")
    @PreAuthorize("hasAuthority('blog:post:publish')")
    public Result<BlogPostDetailResponse> offlinePost(@PathVariable Long id) {
        return Result.success(adminBlogPostService.offlinePost(id));
    }

    @DeleteMapping("/posts/{id}")
    @PreAuthorize("hasAuthority('blog:post:remove')")
    public Result<Void> deletePost(@PathVariable Long id) {
        adminBlogPostService.deletePost(id);
        return Result.success();
    }

    @GetMapping("/categories/options")
    @PreAuthorize("hasAuthority('blog:post:view')")
    public Result<List<BlogOptionResponse>> listCategoryOptions() {
        return Result.success(adminBlogPostService.listCategoryOptions());
    }

    @GetMapping("/tags/options")
    @PreAuthorize("hasAuthority('blog:post:view')")
    public Result<List<BlogOptionResponse>> listTagOptions() {
        return Result.success(adminBlogPostService.listTagOptions());
    }
}
