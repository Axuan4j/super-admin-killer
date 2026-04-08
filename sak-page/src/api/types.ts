export interface PageResponse<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  searchCount: boolean
  hasMore: boolean
  current: number
  size: number
}
