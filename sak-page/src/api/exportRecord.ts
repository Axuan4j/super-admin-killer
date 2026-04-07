import request from './request'
import type { PageResponse } from './types'

export interface ExportRecordItem {
  id: number
  bizType: string
  fileName: string
  queryCondition?: string
  status: string
  filePath?: string
  downloadUrl?: string
  fileSize?: number
  totalCount?: number
  operator?: string
  errMsg?: string
  finishTime?: string
  createTime?: string
}

export const getExportRecords = (params: { bizType?: string; status?: string; current: number; size: number }) =>
  request.get<unknown, PageResponse<ExportRecordItem>>('/system/export-records', { params })
