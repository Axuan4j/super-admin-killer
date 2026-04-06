import request from './request'
import type { PageResponse } from './types'

export interface OperLogItem {
  id: number
  bizNo?: string
  logType?: string
  subType?: string
  operator?: string
  action?: string
  extra?: string
  ip?: string
  method?: string
  requestUrl?: string
  requestMethod?: string
  executionTime?: number
  success?: number
  errMsg?: string
  createTime?: string
}

export const getOperLogs = (params: {
  operator?: string
  logType?: string
  action?: string
  success?: number
  current: number
  size: number
}) =>
  request.get<unknown, PageResponse<OperLogItem>>('/system/logs', { params })
