import request from './request'

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

export const getManageMenus = (params?: { keyword?: string; menuType?: string }) =>
  request.get<unknown, MenuManageItem[]>('/system/menus', { params })
export const createMenu = (data: MenuSavePayload) => request.post<unknown, MenuManageItem>('/system/menus', data)
export const updateMenu = (id: number, data: MenuSavePayload) => request.put<unknown, MenuManageItem>(`/system/menus/${id}`, data)
export const deleteMenu = (id: number) => request.delete(`/system/menus/${id}`)
