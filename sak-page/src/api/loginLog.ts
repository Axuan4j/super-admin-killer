import request from './request'
import type { PageResponse } from './types'

export interface LoginLogItem {
  id: number
  username?: string
  loginIp?: string
  loginLocation?: string
  userAgent?: string
  browser?: string
  os?: string
  deviceType?: string
  status?: number
  message?: string
  loginTime?: string
}

export const getLoginLogs = (params: {
  username?: string
  loginIp?: string
  status?: number
  pageNum: number
  pageSize: number
  orderField?: string
  orderDirection?: 'asc' | 'desc'
  searchCount?: boolean
}) =>
  request.get<unknown, PageResponse<LoginLogItem>>('/system/login-logs', { params })
