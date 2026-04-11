import request from '@/api/request'

export interface MonitorOverview {
  snapshotTime?: string
  security?: {
    onlineSessionCount?: number
    todayLoginSuccessCount?: number
    todayLoginFailureCount?: number
  }
  scheduledTask?: {
    totalCount?: number
    failureCount?: number
    latestFailureTaskName?: string
    latestFailureTime?: string
  }
  redis?: {
    connected: boolean
    usedMemoryHuman?: string
  }
  database?: {
    connected: boolean
    databaseName?: string
  }
  cpu?: {
    usagePercent?: number
  }
  memory?: {
    usagePercent?: number
  }
}

export const getMonitorOverview = () =>
  request.get<unknown, MonitorOverview>('/system/monitor/overview')
