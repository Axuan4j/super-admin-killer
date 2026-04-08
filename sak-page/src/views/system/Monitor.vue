<template>
  <div class="page-container">
    <a-space direction="vertical" fill :size="16">
      <a-card title="硬件监控">
        <template #extra>
          <a-space>
            <span class="snapshot-text">快照时间：{{ formatDateTime(overview?.snapshotTime) }}</span>
            <span class="snapshot-text">缓存：{{ overview?.cacheTtlSeconds || 0 }}s</span>
            <span class="snapshot-text">自动刷新</span>
            <a-switch v-model="autoRefresh" />
            <a-button type="primary" :loading="loading" @click="handleRefresh">刷新</a-button>
          </a-space>
        </template>

        <a-row :gutter="16">
          <a-col :xs="24" :sm="12" :lg="6">
            <a-card class="stat-card" bordered>
              <a-statistic title="CPU 使用率" :value="overview?.cpu?.usagePercent || 0" :precision="2" suffix="%" />
            </a-card>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="6">
            <a-card class="stat-card" bordered>
              <a-statistic title="内存使用率" :value="overview?.memory?.usagePercent || 0" :precision="2" suffix="%" />
            </a-card>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="6">
            <a-card class="stat-card" bordered>
              <a-statistic title="JVM Heap 已用" :value="((overview?.jvm?.heapUsed || 0) / 1024 / 1024)" :precision="1" suffix="MB" />
              <div class="stat-sub-text">最大 {{ formatBytes(overview?.jvm?.heapMax) }}</div>
            </a-card>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="6">
            <a-card class="stat-card" bordered>
              <a-statistic title="磁盘分区" :value="overview?.disks?.length || 0" />
              <div class="stat-sub-text">Redis: {{ overview?.redis?.connected ? '正常' : '异常' }}</div>
            </a-card>
          </a-col>
        </a-row>
      </a-card>

      <a-row :gutter="16">
        <a-col :xs="24" :lg="12">
          <a-card title="系统信息" class="full-height">
            <a-descriptions :column="1" bordered>
              <a-descriptions-item label="主机名">{{ overview?.server?.hostName || '-' }}</a-descriptions-item>
              <a-descriptions-item label="主机地址">{{ overview?.server?.hostAddress || '-' }}</a-descriptions-item>
              <a-descriptions-item label="操作系统">
                {{ [overview?.server?.osName, overview?.server?.osVersion, overview?.server?.osArch].filter(Boolean).join(' / ') || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="系统启动">{{ formatDateTime(overview?.server?.bootTime) }}</a-descriptions-item>
              <a-descriptions-item label="系统运行">{{ formatDurationSeconds(overview?.server?.uptimeSeconds) }}</a-descriptions-item>
              <a-descriptions-item label="进程 / 线程">
                {{ overview?.server?.processCount ?? '-' }} / {{ overview?.server?.threadCount ?? '-' }}
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-card title="CPU / 内存" class="full-height">
            <a-descriptions :column="1" bordered>
              <a-descriptions-item label="CPU 型号">{{ overview?.cpu?.model || '-' }}</a-descriptions-item>
              <a-descriptions-item label="物理 / 逻辑核心">
                {{ overview?.cpu?.physicalCores ?? '-' }} / {{ overview?.cpu?.logicalCores ?? '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="CPU 使用率">{{ formatPercent(overview?.cpu?.usagePercent) }}</a-descriptions-item>
              <a-descriptions-item label="Load Average">
                {{ formatLoadAverage(overview?.cpu?.loadAverage1m, overview?.cpu?.loadAverage5m, overview?.cpu?.loadAverage15m) }}
              </a-descriptions-item>
              <a-descriptions-item label="内存占用">
                {{ formatBytes(overview?.memory?.used) }} / {{ formatBytes(overview?.memory?.total) }}
              </a-descriptions-item>
              <a-descriptions-item label="可用内存">{{ formatBytes(overview?.memory?.available) }}</a-descriptions-item>
              <a-descriptions-item label="Swap 占用">
                {{ formatBytes(overview?.memory?.swapUsed) }} / {{ formatBytes(overview?.memory?.swapTotal) }}
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :xs="24" :lg="12">
          <a-card title="JVM 信息" class="full-height">
            <a-descriptions :column="1" bordered>
              <a-descriptions-item label="JVM">{{ overview?.jvm?.name || '-' }}</a-descriptions-item>
              <a-descriptions-item label="厂商 / 版本">
                {{ [overview?.jvm?.vendor, overview?.jvm?.version].filter(Boolean).join(' / ') || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="启动时间">{{ formatDateTime(overview?.jvm?.startTime) }}</a-descriptions-item>
              <a-descriptions-item label="运行时长">{{ formatDurationMs(overview?.jvm?.uptimeMs) }}</a-descriptions-item>
              <a-descriptions-item label="Heap 已用 / 已提交">
                {{ formatBytes(overview?.jvm?.heapUsed) }} / {{ formatBytes(overview?.jvm?.heapCommitted) }}
              </a-descriptions-item>
              <a-descriptions-item label="Heap 最大值">{{ formatBytes(overview?.jvm?.heapMax) }}</a-descriptions-item>
              <a-descriptions-item label="Non-Heap 已用">{{ formatBytes(overview?.jvm?.nonHeapUsed) }}</a-descriptions-item>
              <a-descriptions-item label="线程数">{{ overview?.jvm?.threadCount ?? '-' }}</a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-space direction="vertical" fill :size="16">
            <a-card title="数据库信息">
              <a-alert
                :type="overview?.database?.connected ? 'success' : 'warning'"
                :content="overview?.database?.message || (overview?.database?.connected ? '连接正常' : '连接异常')"
                show-icon
              />
              <a-descriptions :column="1" bordered class="metric-descriptions">
                <a-descriptions-item label="数据库">{{ overview?.database?.productName || '-' }}</a-descriptions-item>
                <a-descriptions-item label="版本">{{ overview?.database?.productVersion || '-' }}</a-descriptions-item>
                <a-descriptions-item label="驱动">{{ overview?.database?.driverName || '-' }}</a-descriptions-item>
                <a-descriptions-item label="连接地址">{{ overview?.database?.url || '-' }}</a-descriptions-item>
                <a-descriptions-item label="账号">{{ overview?.database?.username || '-' }}</a-descriptions-item>
                <a-descriptions-item label="当前库">{{ overview?.database?.databaseName || '-' }}</a-descriptions-item>
                <a-descriptions-item label="连接池">
                  {{ overview?.database?.activeConnections ?? '-' }} / {{ overview?.database?.totalConnections ?? '-' }} / {{ overview?.database?.maxPoolSize ?? '-' }}
                  <span class="sub-inline-text">(活跃 / 总数 / 上限)</span>
                </a-descriptions-item>
              </a-descriptions>
            </a-card>

            <a-card title="Redis 信息">
              <a-alert
                :type="overview?.redis?.connected ? 'success' : 'warning'"
                :content="overview?.redis?.message || (overview?.redis?.connected ? '连接正常' : '连接异常')"
                show-icon
              />
              <a-descriptions :column="1" bordered class="metric-descriptions">
                <a-descriptions-item label="版本">{{ overview?.redis?.version || '-' }}</a-descriptions-item>
                <a-descriptions-item label="模式">{{ overview?.redis?.mode || '-' }}</a-descriptions-item>
                <a-descriptions-item label="已用内存">{{ overview?.redis?.usedMemoryHuman || '-' }}</a-descriptions-item>
                <a-descriptions-item label="连接客户端">{{ overview?.redis?.connectedClients ?? '-' }}</a-descriptions-item>
                <a-descriptions-item label="当前 DB Key 数">{{ overview?.redis?.dbSize ?? '-' }}</a-descriptions-item>
                <a-descriptions-item label="运行时长">{{ formatDurationSeconds(overview?.redis?.uptimeSeconds) }}</a-descriptions-item>
              </a-descriptions>
            </a-card>
          </a-space>
        </a-col>
      </a-row>

      <a-card title="磁盘信息">
        <a-table :columns="diskColumns" :data="overview?.disks || []" :pagination="false" row-key="mount">
          <template #usagePercent="{ record }">
            <a-tag :color="resolveUsageColor(record.usagePercent)">
              {{ formatPercent(record.usagePercent) }}
            </a-tag>
          </template>
          <template #space="{ record }">
            {{ formatBytes(record.used) }} / {{ formatBytes(record.total) }}
          </template>
          <template #usable="{ record }">
            {{ formatBytes(record.usable) }}
          </template>
        </a-table>
      </a-card>
    </a-space>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import { getMonitorOverview, type MonitorOverview } from '@/api/monitor'

const loading = ref(false)
const autoRefresh = ref(true)
const overview = ref<MonitorOverview | null>(null)
let refreshTimer: ReturnType<typeof setInterval> | null = null

const diskColumns = [
  { title: '挂载点', dataIndex: 'mount', width: 220, ellipsis: true, tooltip: true },
  { title: '名称', dataIndex: 'name', width: 220, ellipsis: true, tooltip: true },
  { title: '类型', dataIndex: 'type', width: 120 },
  { title: '说明', dataIndex: 'description', width: 140 },
  { title: '空间占用', slotName: 'space', width: 180 },
  { title: '可用空间', slotName: 'usable', width: 140 },
  { title: '使用率', slotName: 'usagePercent', width: 110 }
]

const formatDateTime = (value?: string) => {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

const formatBytes = (value?: number) => {
  if (value === undefined || value === null || value < 0) {
    return '-'
  }
  const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB']
  let currentValue = value
  let unitIndex = 0
  while (currentValue >= 1024 && unitIndex < units.length - 1) {
    currentValue /= 1024
    unitIndex += 1
  }
  return `${currentValue.toFixed(currentValue >= 10 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`
}

const formatPercent = (value?: number) => {
  if (value === undefined || value === null) {
    return '-'
  }
  return `${value.toFixed(2)}%`
}

const formatLoadAverage = (...values: Array<number | undefined>) => {
  const formatted = values
    .map((value) => (value === undefined || value === null ? '-' : value.toFixed(2)))
    .join(' / ')
  return formatted || '-'
}

const formatDurationSeconds = (value?: number) => {
  if (value === undefined || value === null || value < 0) {
    return '-'
  }
  const days = Math.floor(value / 86400)
  const hours = Math.floor((value % 86400) / 3600)
  const minutes = Math.floor((value % 3600) / 60)
  const seconds = value % 60
  const parts = []
  if (days > 0) {
    parts.push(`${days}天`)
  }
  if (hours > 0 || days > 0) {
    parts.push(`${hours}小时`)
  }
  if (minutes > 0 || hours > 0 || days > 0) {
    parts.push(`${minutes}分钟`)
  }
  parts.push(`${seconds}秒`)
  return parts.join(' ')
}

const formatDurationMs = (value?: number) => {
  if (value === undefined || value === null || value < 0) {
    return '-'
  }
  return formatDurationSeconds(Math.floor(value / 1000))
}

const resolveUsageColor = (value?: number) => {
  if (value === undefined || value === null) {
    return 'gray'
  }
  if (value >= 90) {
    return 'red'
  }
  if (value >= 75) {
    return 'orange'
  }
  return 'green'
}

const startAutoRefresh = () => {
  stopAutoRefresh()
  if (!autoRefresh.value) {
    return
  }
  refreshTimer = setInterval(() => {
    loadData(false).catch(() => undefined)
  }, 10000)
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

const handleRefresh = () => {
  loadData().catch(() => undefined)
}

const loadData = async (showError = true) => {
  loading.value = true
  try {
    overview.value = await getMonitorOverview()
  } catch (error) {
    if (showError) {
      Message.error('监控数据加载失败')
    }
    throw error
  } finally {
    loading.value = false
  }
}

watch(autoRefresh, () => {
  startAutoRefresh()
})

onMounted(async () => {
  await loadData()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.page-container {
  min-height: 100%;
}

.stat-card {
  min-height: 132px;
}

.stat-sub-text {
  color: var(--app-muted-text, #86909c);
  font-size: 12px;
  margin-top: 8px;
}

.snapshot-text {
  color: var(--app-muted-text, #86909c);
  font-size: 12px;
}

.metric-descriptions {
  margin-top: 12px;
}

.sub-inline-text {
  color: var(--app-muted-text, #86909c);
  margin-left: 6px;
}

.full-height {
  height: 100%;
}
</style>
