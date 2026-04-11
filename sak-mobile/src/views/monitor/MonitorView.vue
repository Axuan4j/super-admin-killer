<template>
  <div class="page">
    <section class="hero-card">
      <div class="hero-card__head">
        <div>
          <div class="hero-card__eyebrow">Runtime Overview</div>
          <h1 class="hero-card__title">移动监控中心</h1>
        </div>
        <button class="refresh-button" type="button" :disabled="loading" @click="loadData">
          {{ loading ? '刷新中' : '刷新' }}
        </button>
      </div>

      <div class="hero-card__meta">
        <span>快照 {{ formatDateTime(monitor?.snapshotTime) }}</span>
        <span>缓存 {{ monitor?.cacheTtlSeconds ?? 0 }}s</span>
      </div>

      <div class="hero-metrics">
        <div class="hero-metric">
          <div class="hero-metric__value">{{ formatPercent(monitor?.cpu?.usagePercent) }}</div>
          <div class="hero-metric__label">CPU</div>
        </div>
        <div class="hero-metric">
          <div class="hero-metric__value">{{ formatPercent(monitor?.memory?.usagePercent) }}</div>
          <div class="hero-metric__label">内存</div>
        </div>
        <div class="hero-metric">
          <div class="hero-metric__value">{{ monitor?.security?.onlineSessionCount ?? 0 }}</div>
          <div class="hero-metric__label">在线会话</div>
        </div>
      </div>
    </section>

    <PageSection title="趋势观察" description="把桌面监控里最适合手机快速判断的趋势摘出来，用轻量图形表达。">
      <div class="trend-grid">
        <section class="trend-card">
          <div class="trend-card__header">
            <span class="trend-card__title">CPU 负载</span>
            <span class="trend-card__value">{{ formatPercent(monitor?.cpu?.usagePercent) }}</span>
          </div>
          <div class="spark-bars">
            <div v-for="item in cpuTrendBars" :key="item.label" class="spark-bars__item">
              <div class="spark-bars__bar">
                <span class="spark-bars__fill" :style="{ height: `${item.percent}%` }"></span>
              </div>
              <span class="spark-bars__label">{{ item.label }}</span>
            </div>
          </div>
        </section>

        <section class="trend-card">
          <div class="trend-card__header">
            <span class="trend-card__title">安全态势</span>
            <span class="trend-card__value">{{ monitor?.security?.todayLoginSuccessCount ?? 0 }} 成功</span>
          </div>
          <div class="spark-bars spark-bars--wide">
            <div v-for="item in securityBars" :key="item.label" class="spark-bars__item spark-bars__item--wide">
              <div class="spark-bars__bar spark-bars__bar--wide">
                <span class="spark-bars__fill" :style="{ height: `${item.percent}%`, background: item.color }"></span>
              </div>
              <span class="spark-bars__label">{{ item.label }}</span>
              <span class="spark-bars__number">{{ item.value }}</span>
            </div>
          </div>
        </section>
      </div>
    </PageSection>

    <PageSection title="关键组件" description="把数据库、Redis、JVM 和调度摘要压成适合手机浏览的卡片。">
      <div class="component-grid">
        <article class="component-card">
          <div class="component-card__tag" :class="{ 'is-danger': !monitor?.database?.connected }">数据库</div>
          <div class="component-card__title">{{ monitor?.database?.productName || monitor?.database?.databaseName || '未知数据库' }}</div>
          <div class="component-card__desc">
            {{ monitor?.database?.connected ? '连接正常' : (monitor?.database?.message || '连接异常') }}
          </div>
          <div class="component-card__meta">连接池 {{ monitor?.database?.activeConnections ?? 0 }} / {{ monitor?.database?.maxPoolSize ?? 0 }}</div>
        </article>

        <article class="component-card">
          <div class="component-card__tag" :class="{ 'is-danger': !monitor?.redis?.connected }">Redis</div>
          <div class="component-card__title">{{ monitor?.redis?.mode || 'Standalone' }}</div>
          <div class="component-card__desc">
            {{ monitor?.redis?.connected ? '缓存连接正常' : (monitor?.redis?.message || '缓存连接异常') }}
          </div>
          <div class="component-card__meta">内存 {{ monitor?.redis?.usedMemoryHuman || '-' }}</div>
        </article>

        <article class="component-card">
          <div class="component-card__tag">JVM</div>
          <div class="component-card__title">{{ monitor?.jvm?.vendor || 'Java Runtime' }}</div>
          <div class="component-card__desc">
            Heap {{ formatBytes(monitor?.jvm?.heapUsed) }} / {{ formatBytes(monitor?.jvm?.heapMax) }}
          </div>
          <div class="component-card__meta">线程 {{ monitor?.jvm?.threadCount ?? 0 }}</div>
        </article>

        <article class="component-card">
          <div class="component-card__tag" :class="{ 'is-danger': (monitor?.scheduledTask?.failureCount ?? 0) > 0 }">调度</div>
          <div class="component-card__title">{{ monitor?.scheduledTask?.totalCount ?? 0 }} 个任务</div>
          <div class="component-card__desc">
            {{ (monitor?.scheduledTask?.failureCount ?? 0) > 0 ? `失败 ${monitor?.scheduledTask?.failureCount ?? 0} 个` : '当前无失败任务' }}
          </div>
          <div class="component-card__meta">
            {{ monitor?.scheduledTask?.latestFailureTaskName || monitor?.scheduledTask?.nearestNextRunTaskName || '暂无更多任务摘要' }}
          </div>
        </article>
      </div>
    </PageSection>

    <PageSection title="资源使用" description="把桌面端的系统资源表格改成手机更容易扫读的进度卡片。">
      <div class="usage-list">
        <article class="usage-card">
          <div class="usage-card__head">
            <span>内存</span>
            <strong>{{ formatPercent(monitor?.memory?.usagePercent) }}</strong>
          </div>
          <div class="usage-card__track">
            <span class="usage-card__fill" :style="{ width: `${toPercent(monitor?.memory?.usagePercent)}%` }"></span>
          </div>
          <div class="usage-card__meta">
            {{ formatBytes(monitor?.memory?.used) }} / {{ formatBytes(monitor?.memory?.total) }}
          </div>
        </article>

        <article class="usage-card">
          <div class="usage-card__head">
            <span>Swap</span>
            <strong>{{ formatRatio(monitor?.memory?.swapUsed, monitor?.memory?.swapTotal) }}</strong>
          </div>
          <div class="usage-card__track">
            <span class="usage-card__fill is-warning" :style="{ width: `${swapPercent}%` }"></span>
          </div>
          <div class="usage-card__meta">
            {{ formatBytes(monitor?.memory?.swapUsed) }} / {{ formatBytes(monitor?.memory?.swapTotal) }}
          </div>
        </article>

        <article v-for="disk in topDisks" :key="disk.mount || disk.name" class="usage-card">
          <div class="usage-card__head">
            <span>{{ disk.mount || disk.name || '磁盘' }}</span>
            <strong>{{ formatPercent(disk.usagePercent) }}</strong>
          </div>
          <div class="usage-card__track">
            <span
              class="usage-card__fill"
              :class="{ 'is-danger': disk.usagePercent >= 90, 'is-warning': disk.usagePercent >= 75 && disk.usagePercent < 90 }"
              :style="{ width: `${toPercent(disk.usagePercent)}%` }"
            ></span>
          </div>
          <div class="usage-card__meta">{{ formatBytes(disk.used) }} / {{ formatBytes(disk.total) }}</div>
        </article>
      </div>
    </PageSection>

    <PageSection title="调度与安全摘要" description="把桌面端长描述缩成手机可快速决策的摘要块。">
      <div class="summary-list">
        <article class="summary-card">
          <div class="summary-card__title">登录安全</div>
          <div class="summary-card__body">
            今日成功 {{ monitor?.security?.todayLoginSuccessCount ?? 0 }} 次，失败 {{ monitor?.security?.todayLoginFailureCount ?? 0 }} 次，MFA 用户 {{ monitor?.security?.mfaEnabledUserCount ?? 0 }} 个。
          </div>
        </article>
        <article class="summary-card">
          <div class="summary-card__title">调度执行</div>
          <div class="summary-card__body">
            {{ monitor?.scheduledTask?.latestFailureTaskName
              ? `最近失败任务：${monitor?.scheduledTask?.latestFailureTaskName}，时间 ${formatDateTime(monitor?.scheduledTask?.latestFailureTime)}`
              : `最近下次执行任务：${monitor?.scheduledTask?.nearestNextRunTaskName || '暂无'}，时间 ${formatDateTime(monitor?.scheduledTask?.nearestNextRunTime)}` }}
          </div>
        </article>
        <article class="summary-card">
          <div class="summary-card__title">主机信息</div>
          <div class="summary-card__body">
            {{ [monitor?.server?.hostName, monitor?.server?.osName, monitor?.server?.osVersion].filter(Boolean).join(' / ') || '暂无主机信息' }}
          </div>
        </article>
      </div>
    </PageSection>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getMonitorOverview, type MonitorOverview } from '@/api/monitor'
import PageSection from '@/components/PageSection.vue'

const loading = ref(false)
const monitor = ref<MonitorOverview | null>(null)

const toPercent = (value?: number | null) => {
  if (value == null || Number.isNaN(value)) {
    return 0
  }
  return Math.max(0, Math.min(100, value))
}

const formatDateTime = (value?: string) => {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

const formatPercent = (value?: number | null) => {
  if (value == null || Number.isNaN(value)) {
    return '-'
  }
  return `${value.toFixed(1)}%`
}

const formatRatio = (used?: number | null, total?: number | null) => {
  if (!total) {
    return '-'
  }
  return `${(((used ?? 0) / total) * 100).toFixed(1)}%`
}

const formatBytes = (value?: number | null) => {
  if (value == null || value < 0) {
    return '-'
  }
  const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB']
  let current = value
  let index = 0
  while (current >= 1024 && index < units.length - 1) {
    current /= 1024
    index += 1
  }
  return `${current.toFixed(current >= 10 || index === 0 ? 0 : 1)} ${units[index]}`
}

const cpuTrendBars = computed(() => [
  { label: '1m', percent: toPercent(monitor.value?.cpu?.loadAverage1m) },
  { label: '5m', percent: toPercent(monitor.value?.cpu?.loadAverage5m) },
  { label: '15m', percent: toPercent(monitor.value?.cpu?.loadAverage15m) }
])

const securityBars = computed(() => {
  const success = Number(monitor.value?.security?.todayLoginSuccessCount ?? 0)
  const failure = Number(monitor.value?.security?.todayLoginFailureCount ?? 0)
  const mfa = Number(monitor.value?.security?.mfaEnabledUserCount ?? 0)
  const max = Math.max(success, failure, mfa, 1)
  return [
    { label: '成功', value: success, percent: Math.max(10, (success / max) * 100), color: 'linear-gradient(180deg, #0f9d6c 0%, #3ac98c 100%)' },
    { label: '失败', value: failure, percent: Math.max(10, (failure / max) * 100), color: 'linear-gradient(180deg, #cf3f4c 0%, #ef6b77 100%)' },
    { label: 'MFA', value: mfa, percent: Math.max(10, (mfa / max) * 100), color: 'linear-gradient(180deg, #0052d9 0%, #4a8cff 100%)' }
  ]
})

const topDisks = computed(() =>
  [...(monitor.value?.disks ?? [])]
    .sort((left, right) => (right.usagePercent ?? 0) - (left.usagePercent ?? 0))
    .slice(0, 3)
)

const swapPercent = computed(() => {
  const total = monitor.value?.memory?.swapTotal ?? 0
  const used = monitor.value?.memory?.swapUsed ?? 0
  if (!total) {
    return 0
  }
  return Math.max(0, Math.min(100, (used / total) * 100))
})

const loadData = async () => {
  loading.value = true
  try {
    monitor.value = await getMonitorOverview()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page {
  display: grid;
  gap: 16px;
  padding: 6px 16px 16px;
}

.hero-card {
  padding: 22px 18px;
  border-radius: 28px;
  background: linear-gradient(160deg, #0b5bd3 0%, #2d79ff 52%, #81a8ff 100%);
  color: #fff;
  box-shadow: 0 18px 42px rgba(0, 82, 217, 0.25);
}

.hero-card__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.hero-card__eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  opacity: 0.8;
}

.hero-card__title {
  margin: 8px 0 0;
  font-size: 28px;
  line-height: 1.15;
}

.hero-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin-top: 12px;
  color: rgba(255, 255, 255, 0.84);
  font-size: 12px;
}

.refresh-button {
  border: 0;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  padding: 11px 14px;
}

.refresh-button:disabled {
  opacity: 0.6;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 18px;
}

.hero-metric {
  padding: 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.14);
}

.hero-metric__value {
  font-size: 22px;
  font-weight: 700;
}

.hero-metric__label {
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.76);
  font-size: 12px;
}

.trend-grid,
.component-grid,
.summary-list,
.usage-list {
  display: grid;
  gap: 12px;
}

.trend-card,
.component-card,
.usage-card,
.summary-card {
  padding: 16px;
  border-radius: 20px;
  border: 1px solid var(--sak-border);
  background: var(--sak-surface-soft);
}

.trend-card__header,
.usage-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.trend-card__title,
.component-card__title,
.summary-card__title {
  font-size: 15px;
  font-weight: 700;
}

.trend-card__value,
.usage-card__head strong {
  font-size: 14px;
  font-weight: 700;
}

.spark-bars {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  align-items: end;
  margin-top: 18px;
  min-height: 132px;
}

.spark-bars--wide {
  min-height: 152px;
}

.spark-bars__item {
  display: grid;
  justify-items: center;
  gap: 8px;
}

.spark-bars__item--wide {
  gap: 6px;
}

.spark-bars__bar {
  display: flex;
  align-items: flex-end;
  width: 100%;
  max-width: 52px;
  height: 96px;
  padding: 6px;
  border-radius: 18px;
  background: #eef3fb;
}

.spark-bars__bar--wide {
  height: 108px;
}

.spark-bars__fill {
  width: 100%;
  min-height: 12px;
  border-radius: 14px;
  background: linear-gradient(180deg, #0052d9 0%, #4a8cff 100%);
}

.spark-bars__label,
.spark-bars__number,
.component-card__desc,
.component-card__meta,
.usage-card__meta,
.summary-card__body {
  color: var(--sak-muted);
  font-size: 12px;
  line-height: 1.6;
}

.spark-bars__number {
  color: var(--sak-text);
  font-weight: 700;
}

.component-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.component-card__tag {
  display: inline-flex;
  width: fit-content;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(0, 82, 217, 0.08);
  color: var(--sak-brand);
  font-size: 11px;
  font-weight: 700;
}

.component-card__tag.is-danger {
  background: rgba(207, 63, 76, 0.1);
  color: var(--sak-danger);
}

.component-card__title {
  margin-top: 10px;
}

.component-card__desc {
  margin-top: 6px;
}

.component-card__meta {
  margin-top: 10px;
}

.usage-card__track {
  margin-top: 12px;
  height: 10px;
  border-radius: 999px;
  background: #e8eef8;
  overflow: hidden;
}

.usage-card__fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #0052d9 0%, #4a8cff 100%);
}

.usage-card__fill.is-warning {
  background: linear-gradient(90deg, #d97706 0%, #f59e0b 100%);
}

.usage-card__fill.is-danger {
  background: linear-gradient(90deg, #cf3f4c 0%, #ef6b77 100%);
}

.usage-card__meta,
.summary-card__body {
  margin-top: 8px;
}

@media (max-width: 420px) {
  .component-grid {
    grid-template-columns: 1fr;
  }
}
</style>
