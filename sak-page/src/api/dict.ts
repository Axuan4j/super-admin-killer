import request from './request'
import type { PageResponse } from './types'

export interface DictItem {
  dictType: string
  label: string
  value: string
  tagType?: string
  tagColor?: string
  orderNum?: number
}

export type DictMap = Record<string, DictItem[]>

export const getAllDicts = () => {
  return request.get<unknown, DictMap>('/system/dicts/all')
}

export interface DictManageItem {
  id: number
  dictType: string
  dictLabel: string
  dictValue: string
  tagType?: string
  tagColor?: string
  orderNum?: number
  status: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface DictSavePayload {
  dictType: string
  dictLabel: string
  dictValue: string
  tagType?: string
  tagColor?: string
  orderNum?: number
  status: string
  remark?: string
}

export const getDictItems = (params: {
  keyword?: string
  dictType?: string
  status?: string
  pageNum: number
  pageSize: number
  orderField?: string
  orderDirection?: 'asc' | 'desc'
  searchCount?: boolean
}) => request.get<unknown, PageResponse<DictManageItem>>('/system/dicts', { params })

export const createDictItem = (data: DictSavePayload) => request.post<unknown, DictManageItem>('/system/dicts', data)
export const updateDictItem = (id: number, data: DictSavePayload) => request.put<unknown, DictManageItem>(`/system/dicts/${id}`, data)
export const deleteDictItem = (id: number) => request.delete(`/system/dicts/${id}`)
