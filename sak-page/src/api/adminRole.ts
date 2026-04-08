import request from './request'
import type { PageResponse } from './types'

export interface RoleItem {
  id: number
  roleName: string
  roleKey: string
  roleSort?: number
  status?: string
  remark?: string
}

export interface RoleSavePayload {
  roleName: string
  roleKey: string
  roleSort: number
  status: string
  remark?: string
}

export const getRoles = (params: { keyword?: string; status?: string; pageNum: number; pageSize: number; orderField?: string; orderDirection?: 'asc' | 'desc'; searchCount?: boolean }) =>
  request.get<unknown, PageResponse<RoleItem>>('/system/roles', { params })
export const createRole = (data: RoleSavePayload) => request.post<unknown, RoleItem>('/system/roles', data)
export const updateRole = (id: number, data: RoleSavePayload) => request.put<unknown, RoleItem>(`/system/roles/${id}`, data)
export const deleteRole = (id: number) => request.delete(`/system/roles/${id}`)
export const getRoleMenuIds = (id: number) => request.get<unknown, number[]>(`/system/roles/${id}/menu-ids`)
export const updateRoleMenuIds = (id: number, menuIds: number[]) => request.put(`/system/roles/${id}/menu-ids`, menuIds)
