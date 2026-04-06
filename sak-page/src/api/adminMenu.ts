import request from './request'
import type { PageResponse } from './types'

export interface MenuManageItem {
  id: number
  menuName: string
  parentId: number
  orderNum: number
  path?: string
  component?: string
  menuType: string
  visible: string
  perms?: string
  icon?: string
  remark?: string
}

export interface MenuSavePayload {
  menuName: string
  parentId: number
  orderNum: number
  path?: string
  component?: string
  menuType: string
  visible: string
  perms?: string
  icon?: string
  remark?: string
}

export const getManageMenus = (params?: { keyword?: string; menuType?: string; current?: number; size?: number }) =>
  request.get<unknown, PageResponse<MenuManageItem>>('/system/menus', { params })
export const createMenu = (data: MenuSavePayload) => request.post<unknown, MenuManageItem>('/system/menus', data)
export const updateMenu = (id: number, data: MenuSavePayload) => request.put<unknown, MenuManageItem>(`/system/menus/${id}`, data)
export const deleteMenu = (id: number) => request.delete(`/system/menus/${id}`)
