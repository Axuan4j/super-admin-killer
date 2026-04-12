import request from './request'
import type { PageResponse } from './types'

export interface BlogOptionItem {
  id: number
  name: string
  slug: string
  color?: string
}

export interface BlogPostListItem {
  id: number
  title: string
  slug: string
  summary?: string
  coverImage?: string
  status: string
  categoryId?: number
  categoryName?: string
  allowComment: number
  isTop: number
  isRecommend: number
  wordCount: number
  readingMinutes: number
  publishTime?: string
  updateTime?: string
}

export interface BlogPostDetail extends BlogPostListItem {
  contentMarkdown: string
  contentHtml?: string
  seoTitle?: string
  seoKeywords?: string
  seoDescription?: string
  sourceType?: string
  sourceUrl?: string
  tagIds: number[]
  createTime?: string
}

export interface BlogPostSavePayload {
  title: string
  slug?: string
  summary?: string
  coverImage?: string
  categoryId?: number
  tagIds?: number[]
  contentMarkdown: string
  seoTitle?: string
  seoKeywords?: string
  seoDescription?: string
  sourceType?: string
  sourceUrl?: string
  allowComment?: number
  isTop?: number
  isRecommend?: number
}

export const getBlogPosts = (params: {
  title?: string
  status?: string
  categoryId?: number
  pageNum: number
  pageSize: number
  orderField?: string
  orderDirection?: 'asc' | 'desc'
  searchCount?: boolean
}) => request.get<unknown, PageResponse<BlogPostListItem>>('/blog/posts', { params })

export const getBlogPostDetail = (id: number) => request.get<unknown, BlogPostDetail>(`/blog/posts/${id}`)

export const createBlogPost = (data: BlogPostSavePayload) =>
  request.post<unknown, BlogPostDetail>('/blog/posts', data)

export const updateBlogPost = (id: number, data: BlogPostSavePayload) =>
  request.put<unknown, BlogPostDetail>(`/blog/posts/${id}`, data)

export const publishBlogPost = (id: number) =>
  request.put<unknown, BlogPostDetail>(`/blog/posts/${id}/publish`)

export const offlineBlogPost = (id: number) =>
  request.put<unknown, BlogPostDetail>(`/blog/posts/${id}/offline`)

export const deleteBlogPost = (id: number) => request.delete<unknown, void>(`/blog/posts/${id}`)

export const getBlogCategoryOptions = () =>
  request.get<unknown, BlogOptionItem[]>('/blog/categories/options')

export const getBlogTagOptions = () =>
  request.get<unknown, BlogOptionItem[]>('/blog/tags/options')

export interface BlogCategoryItem {
  id: number
  categoryName: string
  slug: string
  description?: string
  coverImage?: string
  orderNum: number
  status: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface BlogCategorySavePayload {
  categoryName: string
  slug?: string
  description?: string
  coverImage?: string
  orderNum?: number
  status?: string
  remark?: string
}

export const getBlogCategories = (params: {
  keyword?: string
  status?: string
  pageNum: number
  pageSize: number
  orderField?: string
  orderDirection?: 'asc' | 'desc'
  searchCount?: boolean
}) => request.get<unknown, PageResponse<BlogCategoryItem>>('/blog/categories', { params })

export const createBlogCategory = (data: BlogCategorySavePayload) =>
  request.post<unknown, BlogCategoryItem>('/blog/categories', data)

export const updateBlogCategory = (id: number, data: BlogCategorySavePayload) =>
  request.put<unknown, BlogCategoryItem>(`/blog/categories/${id}`, data)

export const deleteBlogCategory = (id: number) =>
  request.delete<unknown, void>(`/blog/categories/${id}`)
