package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.sak.service.dto.BlogOptionResponse;
import com.sak.service.dto.BlogPostDetailResponse;
import com.sak.service.dto.BlogPostListResponse;
import com.sak.service.dto.BlogPostQueryRequest;
import com.sak.service.dto.BlogPostSaveRequest;
import com.sak.service.dto.PageResponse;
import com.sak.service.entity.BlogCategory;
import com.sak.service.entity.BlogPost;
import com.sak.service.entity.BlogPostStat;
import com.sak.service.entity.BlogPostTag;
import com.sak.service.entity.BlogTag;
import com.sak.service.entity.SysUser;
import com.sak.service.mapper.BlogCategoryMapper;
import com.sak.service.mapper.BlogPostMapper;
import com.sak.service.mapper.BlogPostStatMapper;
import com.sak.service.mapper.BlogPostTagMapper;
import com.sak.service.mapper.BlogTagMapper;
import com.sak.service.mapper.SysUserMapper;
import com.sak.service.service.AdminBlogPostService;
import com.sak.service.util.MarkdownRenderUtils;
import com.sak.service.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminBlogPostServiceImpl implements AdminBlogPostService {

    private static final Map<String, String> BLOG_POST_SORT_FIELDS = new LinkedHashMap<>();

    static {
        BLOG_POST_SORT_FIELDS.put("id", "id");
        BLOG_POST_SORT_FIELDS.put("title", "title");
        BLOG_POST_SORT_FIELDS.put("status", "status");
        BLOG_POST_SORT_FIELDS.put("publishTime", "publish_time");
        BLOG_POST_SORT_FIELDS.put("updateTime", "update_time");
        BLOG_POST_SORT_FIELDS.put("wordCount", "word_count");
        BLOG_POST_SORT_FIELDS.put("readingMinutes", "reading_minutes");
    }

    private final BlogPostMapper blogPostMapper;
    private final BlogCategoryMapper blogCategoryMapper;
    private final BlogTagMapper blogTagMapper;
    private final BlogPostTagMapper blogPostTagMapper;
    private final BlogPostStatMapper blogPostStatMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResponse<BlogPostListResponse> listPosts(BlogPostQueryRequest request) {
        LambdaQueryWrapper<BlogPost> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getTitle())) {
            queryWrapper.like(BlogPost::getTitle, request.getTitle().trim());
        }
        if (StringUtils.hasText(request.getStatus())) {
            queryWrapper.eq(BlogPost::getStatus, request.getStatus().trim());
        }
        if (request.getCategoryId() != null) {
            queryWrapper.eq(BlogPost::getCategoryId, request.getCategoryId());
        }

        Page<BlogPost> page = blogPostMapper.selectPage(
                PageUtils.buildPage(request, BLOG_POST_SORT_FIELDS, "update_time", "desc"),
                queryWrapper
        );
        Map<Long, String> categoryNameMap = loadCategoryNameMap(page.getRecords());
        List<BlogPostListResponse> records = page.getRecords().stream()
                .map(post -> toListResponse(post, categoryNameMap.get(post.getCategoryId())))
                .toList();
        return PageUtils.toResponse(page, records, request);
    }

    @Override
    public BlogPostDetailResponse getPostDetail(Long id) {
        BlogPost post = requirePost(id);
        Map<Long, String> categoryNameMap = loadCategoryNameMap(List.of(post));
        return toDetailResponse(post, categoryNameMap.get(post.getCategoryId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(success = "新增博客文章：{{#p1.title}}", fail = "新增博客文章失败：{{#p1.title}}", type = "BLOG_POST", subType = "CREATE", bizNo = "{{#_ret.id}}")
    public BlogPostDetailResponse createPost(String operator, BlogPostSaveRequest request) {
        BlogPost post = new BlogPost();
        applySaveRequest(post, request, resolveOperatorUserId(operator), true);
        blogPostMapper.insert(post);
        syncTags(post.getId(), request.getTagIds());
        initPostStat(post.getId());
        return getPostDetail(post.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(success = "编辑博客文章：{{#p1.title}}", fail = "编辑博客文章失败：{{#p1.title}}", type = "BLOG_POST", subType = "UPDATE", bizNo = "{{#p0}}")
    public BlogPostDetailResponse updatePost(Long id, BlogPostSaveRequest request) {
        BlogPost post = requirePost(id);
        applySaveRequest(post, request, post.getAuthorUserId(), false);
        post.setVersionNo((post.getVersionNo() == null ? 1 : post.getVersionNo()) + 1);
        blogPostMapper.updateById(post);
        syncTags(id, request.getTagIds());
        return getPostDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(success = "发布博客文章：{{#p0}}", fail = "发布博客文章失败：{{#p0}}", type = "BLOG_POST", subType = "PUBLISH", bizNo = "{{#p0}}")
    public BlogPostDetailResponse publishPost(Long id) {
        BlogPost post = requirePost(id);
        if (!StringUtils.hasText(post.getTitle()) || !StringUtils.hasText(post.getContentMarkdown())) {
            throw new IllegalArgumentException("文章标题和正文不能为空");
        }
        post.setStatus("PUBLISHED");
        post.setPublishTime(LocalDateTime.now());
        post.setContentHtml(MarkdownRenderUtils.render(post.getContentMarkdown()));
        post.setWordCount(countWords(post.getContentMarkdown()));
        post.setReadingMinutes(calculateReadingMinutes(post.getWordCount()));
        post.setVersionNo((post.getVersionNo() == null ? 1 : post.getVersionNo()) + 1);
        blogPostMapper.updateById(post);
        return getPostDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(success = "下线博客文章：{{#p0}}", fail = "下线博客文章失败：{{#p0}}", type = "BLOG_POST", subType = "OFFLINE", bizNo = "{{#p0}}")
    public BlogPostDetailResponse offlinePost(Long id) {
        BlogPost post = requirePost(id);
        post.setStatus("OFFLINE");
        post.setVersionNo((post.getVersionNo() == null ? 1 : post.getVersionNo()) + 1);
        blogPostMapper.updateById(post);
        return getPostDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(success = "删除博客文章：{{#p0}}", fail = "删除博客文章失败：{{#p0}}", type = "BLOG_POST", subType = "DELETE", bizNo = "{{#p0}}")
    public void deletePost(Long id) {
        requirePost(id);
        blogPostTagMapper.delete(new LambdaQueryWrapper<BlogPostTag>().eq(BlogPostTag::getPostId, id));
        blogPostStatMapper.deleteById(id);
        blogPostMapper.deleteById(id);
    }

    @Override
    public List<BlogOptionResponse> listCategoryOptions() {
        return blogCategoryMapper.selectList(new LambdaQueryWrapper<BlogCategory>()
                        .eq(BlogCategory::getStatus, "0")
                        .orderByAsc(BlogCategory::getOrderNum, BlogCategory::getId))
                .stream()
                .map(this::toCategoryOption)
                .toList();
    }

    @Override
    public List<BlogOptionResponse> listTagOptions() {
        return blogTagMapper.selectList(new LambdaQueryWrapper<BlogTag>()
                        .eq(BlogTag::getStatus, "0")
                        .orderByAsc(BlogTag::getOrderNum, BlogTag::getId))
                .stream()
                .map(this::toTagOption)
                .toList();
    }

    private void applySaveRequest(BlogPost post, BlogPostSaveRequest request, Long authorUserId, boolean createMode) {
        validateCategory(request.getCategoryId());
        validateTags(request.getTagIds());

        String normalizedTitle = request.getTitle().trim();
        String normalizedMarkdown = request.getContentMarkdown().replace("\r\n", "\n").trim();
        post.setTitle(normalizedTitle);
        post.setSlug(resolveUniqueSlug(request.getSlug(), normalizedTitle, createMode ? null : post.getId()));
        post.setSummary(resolveSummary(request.getSummary(), normalizedMarkdown));
        post.setCoverImage(trimToNull(request.getCoverImage()));
        post.setCategoryId(request.getCategoryId());
        post.setAuthorUserId(authorUserId);
        post.setContentMarkdown(normalizedMarkdown);
        post.setContentHtml(MarkdownRenderUtils.render(normalizedMarkdown));
        post.setSeoTitle(trimToNull(request.getSeoTitle()));
        post.setSeoKeywords(trimToNull(request.getSeoKeywords()));
        post.setSeoDescription(trimToNull(request.getSeoDescription()));
        post.setSourceType(StringUtils.hasText(request.getSourceType()) ? request.getSourceType().trim() : "ORIGINAL");
        post.setSourceUrl(trimToNull(request.getSourceUrl()));
        post.setAllowComment(normalizeFlag(request.getAllowComment(), 1));
        post.setIsTop(normalizeFlag(request.getIsTop(), 0));
        post.setIsRecommend(normalizeFlag(request.getIsRecommend(), 0));
        post.setWordCount(countWords(normalizedMarkdown));
        post.setReadingMinutes(calculateReadingMinutes(post.getWordCount()));
        if (createMode) {
            post.setStatus("DRAFT");
            post.setVersionNo(1);
        }
    }

    private void syncTags(Long postId, List<Long> tagIds) {
        blogPostTagMapper.delete(new LambdaQueryWrapper<BlogPostTag>().eq(BlogPostTag::getPostId, postId));
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        Set<Long> distinctTagIds = new LinkedHashSet<>(tagIds);
        for (Long tagId : distinctTagIds) {
            BlogPostTag relation = new BlogPostTag();
            relation.setPostId(postId);
            relation.setTagId(tagId);
            blogPostTagMapper.insert(relation);
        }
    }

    private void initPostStat(Long postId) {
        BlogPostStat existing = blogPostStatMapper.selectById(postId);
        if (existing != null) {
            return;
        }
        BlogPostStat stat = new BlogPostStat();
        stat.setPostId(postId);
        stat.setViewCount(0L);
        stat.setUniqueViewCount(0L);
        stat.setCommentCount(0L);
        stat.setLikeCount(0L);
        stat.setFavoriteCount(0L);
        stat.setShareCount(0L);
        blogPostStatMapper.insert(stat);
    }

    private void validateCategory(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        BlogCategory category = blogCategoryMapper.selectById(categoryId);
        if (category == null || !"0".equals(category.getStatus())) {
            throw new IllegalArgumentException("分类不存在或已停用");
        }
    }

    private void validateTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<BlogTag> tags = blogTagMapper.selectBatchIds(new ArrayList<>(new LinkedHashSet<>(tagIds)));
        if (tags.size() != new LinkedHashSet<>(tagIds).size()) {
            throw new IllegalArgumentException("标签不存在");
        }
        boolean disabled = tags.stream().anyMatch(tag -> !"0".equals(tag.getStatus()));
        if (disabled) {
            throw new IllegalArgumentException("存在已停用标签");
        }
    }

    private String resolveUniqueSlug(String requestedSlug, String title, Long excludeId) {
        String baseSlug = StringUtils.hasText(requestedSlug) ? requestedSlug.trim() : slugify(title);
        if (!StringUtils.hasText(baseSlug)) {
            baseSlug = "post";
        }
        String candidate = baseSlug;
        int index = 1;
        while (slugExists(candidate, excludeId)) {
            candidate = baseSlug + "-" + index++;
        }
        return candidate;
    }

    private boolean slugExists(String slug, Long excludeId) {
        BlogPost existing = blogPostMapper.selectOne(new LambdaQueryWrapper<BlogPost>()
                .eq(BlogPost::getSlug, slug)
                .last("limit 1"));
        return existing != null && !Objects.equals(existing.getId(), excludeId);
    }

    private String slugify(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String slug = value.trim().toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        return slug.length() > 220 ? slug.substring(0, 220) : slug;
    }

    private String resolveSummary(String summary, String markdown) {
        if (StringUtils.hasText(summary)) {
            return summary.trim();
        }
        String plainText = markdown
                .replaceAll("!\\[(.*?)]\\((.+?)\\)", " ")
                .replaceAll("\\[(.*?)]\\((.+?)\\)", "$1")
                .replaceAll("[#>*`\\-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (!StringUtils.hasText(plainText)) {
            return null;
        }
        return plainText.length() <= 180 ? plainText : plainText.substring(0, 180) + "...";
    }

    private Integer countWords(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return 0;
        }
        return markdown.replaceAll("\\s+", "").length();
    }

    private Integer calculateReadingMinutes(Integer wordCount) {
        if (wordCount == null || wordCount <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(wordCount / 400.0d));
    }

    private Integer normalizeFlag(Integer value, int defaultValue) {
        return value == null ? defaultValue : (value == 0 ? 0 : 1);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Long resolveOperatorUserId(String operator) {
        if (!StringUtils.hasText(operator)) {
            return null;
        }
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, operator.trim())
                .last("limit 1"));
        return user == null ? null : user.getId();
    }

    private BlogPost requirePost(Long id) {
        BlogPost post = blogPostMapper.selectById(id);
        if (post == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        return post;
    }

    private Map<Long, String> loadCategoryNameMap(List<BlogPost> posts) {
        Set<Long> categoryIds = posts.stream()
                .map(BlogPost::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return blogCategoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(BlogCategory::getId, BlogCategory::getCategoryName));
    }

    private BlogPostListResponse toListResponse(BlogPost post, String categoryName) {
        BlogPostListResponse response = new BlogPostListResponse();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setSlug(post.getSlug());
        response.setSummary(post.getSummary());
        response.setCoverImage(post.getCoverImage());
        response.setStatus(post.getStatus());
        response.setCategoryId(post.getCategoryId());
        response.setCategoryName(categoryName);
        response.setAllowComment(post.getAllowComment());
        response.setIsTop(post.getIsTop());
        response.setIsRecommend(post.getIsRecommend());
        response.setWordCount(post.getWordCount());
        response.setReadingMinutes(post.getReadingMinutes());
        response.setPublishTime(post.getPublishTime());
        response.setUpdateTime(post.getUpdateTime());
        return response;
    }

    private BlogPostDetailResponse toDetailResponse(BlogPost post, String categoryName) {
        BlogPostDetailResponse response = new BlogPostDetailResponse();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setSlug(post.getSlug());
        response.setSummary(post.getSummary());
        response.setCoverImage(post.getCoverImage());
        response.setCategoryId(post.getCategoryId());
        response.setCategoryName(categoryName);
        response.setTagIds(loadTagIds(post.getId()));
        response.setContentMarkdown(post.getContentMarkdown());
        response.setContentHtml(post.getContentHtml());
        response.setSeoTitle(post.getSeoTitle());
        response.setSeoKeywords(post.getSeoKeywords());
        response.setSeoDescription(post.getSeoDescription());
        response.setSourceType(post.getSourceType());
        response.setSourceUrl(post.getSourceUrl());
        response.setAllowComment(post.getAllowComment());
        response.setIsTop(post.getIsTop());
        response.setIsRecommend(post.getIsRecommend());
        response.setStatus(post.getStatus());
        response.setWordCount(post.getWordCount());
        response.setReadingMinutes(post.getReadingMinutes());
        response.setPublishTime(post.getPublishTime());
        response.setCreateTime(post.getCreateTime());
        response.setUpdateTime(post.getUpdateTime());
        return response;
    }

    private List<Long> loadTagIds(Long postId) {
        return blogPostTagMapper.selectList(new LambdaQueryWrapper<BlogPostTag>()
                        .eq(BlogPostTag::getPostId, postId)
                        .orderByAsc(BlogPostTag::getId))
                .stream()
                .map(BlogPostTag::getTagId)
                .toList();
    }

    private BlogOptionResponse toCategoryOption(BlogCategory category) {
        BlogOptionResponse response = new BlogOptionResponse();
        response.setId(category.getId());
        response.setName(category.getCategoryName());
        response.setSlug(category.getSlug());
        return response;
    }

    private BlogOptionResponse toTagOption(BlogTag tag) {
        BlogOptionResponse response = new BlogOptionResponse();
        response.setId(tag.getId());
        response.setName(tag.getTagName());
        response.setSlug(tag.getSlug());
        response.setColor(tag.getColor());
        return response;
    }
}
