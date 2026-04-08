import request from './request'
import type { PageResponse } from './types'

export type ScheduledExecutionType = 'CRON' | 'FIXED_RATE' | 'FIXED_DELAY'
export type ScheduledIntervalUnit = 'SECONDS' | 'MINUTES' | 'HOURS' | 'DAYS'

export interface ScheduledTaskFieldOption {
  label: string
  value: string | number | boolean
}

export interface ScheduledTaskFormField {
  field: string
  label: string
  component: 'input' | 'textarea' | 'checkbox-group' | 'radio-group' | 'user-select'
  placeholder?: string
  required?: boolean
  defaultValue?: unknown
  maxLength?: number
  minRows?: number
  maxRows?: number
  helpText?: string
  visibleWhenField?: string
  visibleWhenEquals?: string | number | boolean
  options?: ScheduledTaskFieldOption[]
}

export interface ScheduledTaskTypeOption {
  code: string
  name: string
  description: string
  formSchema: ScheduledTaskFormField[]
}

export interface ScheduledTaskConfig {
  [key: string]: any
}

export interface ScheduledTaskItem {
  id: number
  taskName: string
  taskType: string
  executionType: ScheduledExecutionType
  intervalValue?: number
  intervalUnit?: ScheduledIntervalUnit
  cronExpression?: string
  status: string
  taskSummary?: string
  taskConfig: ScheduledTaskConfig
  successNotify: boolean
  failureNotify: boolean
  notifyChannels: string[]
  notifyUserIds: number[]
  runCount: number
  operator?: string
  remark?: string
  lastRunTime?: string
  nextRunTime?: string
  lastSuccessTime?: string
  lastFailureTime?: string
  lastErrorMessage?: string
  createTime?: string
}

export interface ScheduledTaskPayload {
  taskName: string
  taskType: string
  executionType: ScheduledExecutionType
  intervalValue?: number
  intervalUnit?: ScheduledIntervalUnit
  cronExpression?: string
  status: 'SCHEDULED' | 'PAUSED'
  taskConfig: ScheduledTaskConfig
  successNotify: boolean
  failureNotify: boolean
  notifyChannels: string[]
  notifyUserIds: number[]
  remark?: string
}

export const getScheduledTaskTypes = () =>
  request.get<unknown, ScheduledTaskTypeOption[]>('/system/schedules/task-types')

export const refreshScheduledTaskTypes = () =>
  request.post<unknown, ScheduledTaskTypeOption[]>('/system/schedules/task-types/refresh')

export const getScheduledTasks = (params: {
  keyword?: string
  status?: string
  taskType?: string
  pageNum: number
  pageSize: number
  orderField?: string
  orderDirection?: 'asc' | 'desc'
  searchCount?: boolean
}) => request.get<unknown, PageResponse<ScheduledTaskItem>>('/system/schedules', { params })

export const getScheduledTask = (id: number) =>
  request.get<unknown, ScheduledTaskItem>(`/system/schedules/${id}`)

export const createScheduledTask = (data: ScheduledTaskPayload) =>
  request.post<unknown, ScheduledTaskItem>('/system/schedules', data)

export const updateScheduledTask = (id: number, data: ScheduledTaskPayload) =>
  request.put<unknown, ScheduledTaskItem>(`/system/schedules/${id}`, data)

export const startScheduledTask = (id: number) =>
  request.post<unknown, void>(`/system/schedules/${id}/start`)

export const pauseScheduledTask = (id: number) =>
  request.post<unknown, void>(`/system/schedules/${id}/pause`)

export const cancelScheduledTask = (id: number) =>
  request.post<unknown, void>(`/system/schedules/${id}/cancel`)

export const runScheduledTask = (id: number) =>
  request.post<unknown, void>(`/system/schedules/${id}/run`)

export const deleteScheduledTask = (id: number) =>
  request.delete<unknown, void>(`/system/schedules/${id}`)
