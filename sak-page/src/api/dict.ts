import request from './request'

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
