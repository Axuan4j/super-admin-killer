import request from './request'
import type { PageResponse } from './types'

export interface RoleOption {
  id: number
  roleName: string
  roleKey: string
  roleSort?: number
  status?: string
  remark?: string
}

export interface UserItem {
  id: number
  username: string
  nickName: string
  email?: string
  wxPusherUid?: string
  phone?: string
  status: string
  remark?: string
  roleIds: number[]
  roleNames: string[]
}

export interface UserSavePayload {
  username: string
  password?: string
  nickName: string
  email?: string
  wxPusherUid?: string
  phone?: string
  status: string
  remark?: string
  roleIds: number[]
}

export const getUsers = (params: { keyword?: string; status?: string; pageNum: number; pageSize: number; orderField?: string; orderDirection?: 'asc' | 'desc'; searchCount?: boolean }) =>
  request.get<unknown, PageResponse<UserItem>>('/system/users', { params })
export const getUserRoleOptions = () => request.get<unknown, RoleOption[]>('/system/users/role-options')
export const createUser = (data: UserSavePayload) => request.post<unknown, UserItem>('/system/users', data)
export const updateUser = (id: number, data: UserSavePayload) => request.put<unknown, UserItem>(`/system/users/${id}`, data)
export const deleteUser = (id: number) => request.delete(`/system/users/${id}`)
