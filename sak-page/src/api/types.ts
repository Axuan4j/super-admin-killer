export interface PageResponse<T> {
  records: T[]
  total: number
  current: number
  size: number
}
