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
              <a-statistic title="CPU 逻辑核心" :value="overview?.cpu?.logicalCores || 0" />
              <div class="stat-sub-text">物理核心 {{ overview?.cpu?.physicalCores ?? '-' }}</div>
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
          <a-card title="系统信息" class="info-card">
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
          <a-card title="CPU / 内存" class="info-card">
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
        <a-col :span="24">
          <a-card title="运行组件">
            <a-tabs v-model:active-key="activeMetricTab" lazy-load>
              <a-tab-pane key="jvm" title="JVM">
                <a-descriptions :column="1" bordered class="metric-descriptions">
                  <a-descriptions-item label="JVM">{{ overview?.jvm?.name || '-' }}</a-descriptions-item>
                  <a-descriptions-item label="厂商 / 版本">
                    {{ formatJoin(overview?.jvm?.vendor, overview?.jvm?.version) }}
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
              </a-tab-pane>

              <a-tab-pane key="database" title="数据库">
                <a-space direction="vertical" fill :size="16">
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
                </a-space>
              </a-tab-pane>

              <a-tab-pane key="redis" title="Redis">
                <a-space direction="vertical" fill :size="16">
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
                </a-space>
              </a-tab-pane>

              <a-tab-pane key="threadPool" title="线程池">
                <a-table
                  :columns="threadPoolColumns"
                  :data="overview?.threadPools || []"
                  :pagination="false"
                  row-key="beanName"
                  size="small"
                >
                  <template #poolSize="{ record }">
                    {{ record.poolSize ?? '-' }} / {{ record.largestPoolSize ?? '-' }}
                    <span class="sub-inline-text">(当前 / 峰值)</span>
                  </template>
                  <template #queue="{ record }">
                    {{ record.queueSize ?? '-' }} / {{ record.queueRemainingCapacity ?? '-' }}
                    <span class="sub-inline-text">(已用 / 剩余)</span>
                  </template>
                  <template #tasks="{ record }">
                    {{ record.completedTaskCount ?? '-' }} / {{ record.taskCount ?? '-' }}
                    <span class="sub-inline-text">(已完成 / 总提交)</span>
                  </template>
                  <template #state="{ record }">
                    <a-tag :color="record.terminated ? 'red' : record.shutdown ? 'orange' : 'green'">
                      {{ record.terminated ? '已终止' : record.shutdown ? '已关闭' : '运行中' }}
                    </a-tag>
                  </template>
                </a-table>
              </a-tab-pane>

              <a-tab-pane key="cache" title="缓存">
                <a-space direction="vertical" fill :size="16">
                  <a-descriptions :column="4" bordered class="metric-descriptions">
                    <a-descriptions-item label="缓存数量">{{ overview?.cache?.cacheCount ?? 0 }}</a-descriptions-item>
                    <a-descriptions-item label="本地缓存">
                      {{ overview?.cache?.cacheCount ? '已启用' : '暂无数据' }}
                    </a-descriptions-item>
                    <a-descriptions-item label="缓存体系">Caffeine + Redis</a-descriptions-item>
                    <a-descriptions-item label="说明">命中率基于本地缓存统计</a-descriptions-item>
                  </a-descriptions>
                  <a-table
                    :columns="cacheColumns"
                    :data="overview?.cache?.caches || []"
                    :pagination="false"
                    row-key="name"
                    size="small"
                  >
                    <template #hitRate="{ record }">
                      <a-tag :color="resolveCacheHitRateColor(record.hitRate)">
                        {{ formatPercent(record.hitRate) }}
                      </a-tag>
                    </template>
                    <template #requestStats="{ record }">
                      {{ record.hitCount ?? 0 }} / {{ record.missCount ?? 0 }}
                      <span class="sub-inline-text">(命中 / 未命中)</span>
                    </template>
                  </a-table>
                </a-space>
              </a-tab-pane>

              <a-tab-pane key="scheduledTask" title="调度任务">
                <a-space direction="vertical" fill :size="16">
                  <a-descriptions :column="5" bordered class="metric-descriptions">
                    <a-descriptions-item label="任务总数">{{ overview?.scheduledTask?.totalCount ?? 0 }}</a-descriptions-item>
                    <a-descriptions-item label="已调度">{{ overview?.scheduledTask?.scheduledCount ?? 0 }}</a-descriptions-item>
                    <a-descriptions-item label="暂停">{{ overview?.scheduledTask?.pausedCount ?? 0 }}</a-descriptions-item>
                    <a-descriptions-item label="运行中">{{ overview?.scheduledTask?.runningCount ?? 0 }}</a-descriptions-item>
                    <a-descriptions-item label="曾失败">{{ overview?.scheduledTask?.failureCount ?? 0 }}</a-descriptions-item>
                  </a-descriptions>
                  <a-descriptions :column="1" bordered class="metric-descriptions">
                    <a-descriptions-item label="最近失败任务">
                      {{ overview?.scheduledTask?.latestFailureTaskName || '-' }}
                    </a-descriptions-item>
                    <a-descriptions-item label="最近失败时间">
                      {{ formatDateTime(overview?.scheduledTask?.latestFailureTime) }}
                    </a-descriptions-item>
                    <a-descriptions-item label="失败原因">
                      {{ overview?.scheduledTask?.latestFailureMessage || '-' }}
                    </a-descriptions-item>
                    <a-descriptions-item label="最近即将执行任务">
                      {{ overview?.scheduledTask?.nearestNextRunTaskName || '-' }}
                    </a-descriptions-item>
                    <a-descriptions-item label="最近下次执行时间">
                      {{ formatDateTime(overview?.scheduledTask?.nearestNextRunTime) }}
                    </a-descriptions-item>
                  </a-descriptions>
                </a-space>
              </a-tab-pane>

              <a-tab-pane key="security" title="登录安全">
                <a-descriptions :column="4" bordered class="metric-descriptions">
                  <a-descriptions-item label="今日登录成功">
                    {{ overview?.security?.todayLoginSuccessCount ?? 0 }}
                  </a-descriptions-item>
                  <a-descriptions-item label="今日登录失败">
                    {{ overview?.security?.todayLoginFailureCount ?? 0 }}
                  </a-descriptions-item>
                  <a-descriptions-item label="当前在线会话">
                    {{ overview?.security?.onlineSessionCount ?? 0 }}
                  </a-descriptions-item>
                  <a-descriptions-item label="已启用 MFA 用户">
                    {{ overview?.security?.mfaEnabledUserCount ?? 0 }}
                  </a-descriptions-item>
                </a-descriptions>
              </a-tab-pane>
            </a-tabs>
          </a-card>
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
import { onBeforeRouteLeave, useRoute } from 'vue-router'

const loading = ref(false)
const autoRefresh = ref(true)
const overview = ref<MonitorOverview | null>(null)
const activeMetricTab = ref('jvm')
const route = useRoute()
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

const threadPoolColumns = [
  { title: '线程池 Bean', dataIndex: 'beanName', width: 180 },
  { title: '线程名前缀', dataIndex: 'threadNamePrefix', width: 180 },
  { title: '核心线程', dataIndex: 'corePoolSize', width: 100 },
  { title: '最大线程', dataIndex: 'maxPoolSize', width: 100 },
  { title: '活跃线程', dataIndex: 'activeCount', width: 100 },
  { title: '线程规模', slotName: 'poolSize', width: 180 },
  { title: '队列', slotName: 'queue', width: 180 },
  { title: '任务数', slotName: 'tasks', width: 200 },
  { title: '状态', slotName: 'state', width: 100 }
]

const cacheColumns = [
  { title: '缓存名称', dataIndex: 'name', width: 180 },
  { title: '估算条数', dataIndex: 'estimatedSize', width: 100 },
  { title: '命中率', slotName: 'hitRate', width: 100 },
  { title: '请求统计', slotName: 'requestStats', width: 180 },
  { title: '驱逐次数', dataIndex: 'evictionCount', width: 100 }
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

const formatJoin = (...values: Array<string | undefined>) => {
  const result = values.filter((value): value is string => !!value && value.trim().length > 0).join(' / ')
  return result || '-'
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

const resolveCacheHitRateColor = (value?: number) => {
  if (value === undefined || value === null) {
    return 'gray'
  }
  if (value >= 90) {
    return 'green'
  }
  if (value >= 70) {
    return 'orange'
  }
  return 'red'
}

const startAutoRefresh = () => {
  stopAutoRefresh()
  if (!autoRefresh.value || document.hidden) {
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

const handleVisibilityChange = () => {
  const isMonitorPage = route.path.includes('/monitor')
  if (!isMonitorPage) {
    stopAutoRefresh()
    return
  }

  if (document.hidden) {
    stopAutoRefresh()
    return
  }

  loadData(false).catch(() => undefined)
  startAutoRefresh()
}

watch(autoRefresh, () => {
  startAutoRefresh()
})

onMounted(async () => {
  await loadData()
  document.addEventListener('visibilitychange', handleVisibilityChange)
  startAutoRefresh()
})

onBeforeRouteLeave(() => {
  stopAutoRefresh()
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
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
  color: #86909c;
  font-size: 12px;
  margin-top: 8px;
}

.snapshot-text {
  color: #86909c;
  font-size: 12px;
}

.metric-descriptions {
  margin-top: 12px;
}

.sub-inline-text {
  color: #86909c;
  margin-left: 6px;
}

.info-card {
  height: 100%;
  min-height: 420px;
}

.info-card :deep(.arco-card-body) {
  height: calc(100% - 57px);
}
</style>
