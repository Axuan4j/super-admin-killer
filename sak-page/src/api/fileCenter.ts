import request from './request'
import type { PageResponse } from './types'

export interface FileRecordItem {
  id: number
  bizType: string
  fileName: string
  storageName: string
  filePath: string
  downloadUrl: string
  contentType?: string
  fileExt?: string
  fileSize: number
  operator?: string
  remark?: string
  createTime?: string
}

export const getFileRecords = (params: {
  bizType?: string
  keyword?: string
  operator?: string
  pageNum: number
  pageSize: number
  orderField?: string
  orderDirection?: 'asc' | 'desc'
  searchCount?: boolean
}) => request.get<unknown, PageResponse<FileRecordItem>>('/system/files', { params })

export const uploadCenterFile = (payload: { file: File; bizType?: string; remark?: string }) => {
  const formData = new FormData()
  formData.append('file', payload.file)
  if (payload.bizType) {
    formData.append('bizType', payload.bizType)
  }
  if (payload.remark) {
    formData.append('remark', payload.remark)
  }
  return request.post<unknown, FileRecordItem>('/system/files/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const deleteCenterFile = (id: number) => request.delete<unknown, void>(`/system/files/${id}`)
