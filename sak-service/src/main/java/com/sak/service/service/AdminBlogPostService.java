package com.sak.service.service;

import com.sak.service.dto.BlogOptionResponse;
import com.sak.service.dto.BlogPostDetailResponse;
import com.sak.service.dto.BlogPostListResponse;
import com.sak.service.dto.BlogPostQueryRequest;
import com.sak.service.dto.BlogPostSaveRequest;
import com.sak.service.dto.PageResponse;

import java.util.List;

public interface AdminBlogPostService {
    PageResponse<BlogPostListResponse> listPosts(BlogPostQueryRequest request);

    BlogPostDetailResponse getPostDetail(Long id);

    BlogPostDetailResponse createPost(String operator, BlogPostSaveRequest request);

    BlogPostDetailResponse updatePost(Long id, BlogPostSaveRequest request);

    BlogPostDetailResponse publishPost(Long id);

    BlogPostDetailResponse offlinePost(Long id);

    void deletePost(Long id);

    List<BlogOptionResponse> listCategoryOptions();

    List<BlogOptionResponse> listTagOptions();
}
