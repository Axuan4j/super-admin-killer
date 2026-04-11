import request from '@/api/request'

export interface MonitorOverview {
  snapshotTime?: string
  cacheTtlSeconds?: number
  server?: {
    hostName?: string
    hostAddress?: string
    osName?: string
    osVersion?: string
    osArch?: string
    processCount?: number
    threadCount?: number
    bootTime?: string
    uptimeSeconds?: number
  }
  security?: {
    onlineSessionCount?: number
    todayLoginSuccessCount?: number
    todayLoginFailureCount?: number
    mfaEnabledUserCount?: number
  }
  scheduledTask?: {
    totalCount?: number
    scheduledCount?: number
    pausedCount?: number
    runningCount?: number
    failureCount?: number
    latestFailureTaskName?: string
    latestFailureTime?: string
    latestFailureMessage?: string
    nearestNextRunTime?: string
    nearestNextRunTaskName?: string
  }
  redis?: {
    connected: boolean
    version?: string
    mode?: string
    usedMemoryHuman?: string
    uptimeSeconds?: number
    connectedClients?: number
    dbSize?: number
    message?: string
  }
  database?: {
    connected: boolean
    productName?: string
    productVersion?: string
    driverName?: string
    url?: string
    username?: string
    databaseName?: string
    activeConnections?: number
    idleConnections?: number
    totalConnections?: number
    maxPoolSize?: number
    message?: string
  }
  cpu?: {
    model?: string
    physicalPackages?: number
    physicalCores?: number
    logicalCores?: number
    usagePercent?: number
    loadAverage1m?: number
    loadAverage5m?: number
    loadAverage15m?: number
  }
  memory?: {
    total?: number
    available?: number
    used?: number
    usagePercent?: number
    swapTotal?: number
    swapUsed?: number
  }
  jvm?: {
    name?: string
    vendor?: string
    version?: string
    startTime?: string
    uptimeMs?: number
    heapUsed?: number
    heapCommitted?: number
    heapMax?: number
    nonHeapUsed?: number
    threadCount?: number
  }
  disks?: Array<{
    name?: string
    mount?: string
    type?: string
    description?: string
    total?: number
    usable?: number
    used?: number
    usagePercent?: number
  }>
}

export const getMonitorOverview = () =>
  request.get<unknown, MonitorOverview>('/system/monitor/overview')
