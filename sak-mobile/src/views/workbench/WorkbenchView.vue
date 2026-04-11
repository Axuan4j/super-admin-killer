<template>
  <div class="page">
    <section class="hero-card">
      <div class="hero-card__eyebrow">欢迎回来</div>
      <h1 class="hero-card__title">{{ displayName }}</h1>
      <p class="hero-card__desc">
        移动端优先承接即时处理类工作，这里只保留消息、告警和关键运行态摘要。
      </p>
      <div class="hero-metrics">
        <div v-for="item in metrics" :key="item.label" class="hero-metrics__item">
          <div class="hero-metrics__value">{{ item.value }}</div>
          <div class="hero-metrics__label">{{ item.label }}</div>
        </div>
      </div>
    </section>

    <PageSection title="运行概览" description="直接复用现有后端监控和消息接口，优先看关键摘要。">
      <div class="overview-grid">
        <div class="overview-item">
          <div class="overview-item__label">CPU 使用率</div>
          <div class="overview-item__value">{{ cpuUsage }}</div>
        </div>
        <div class="overview-item">
          <div class="overview-item__label">内存使用率</div>
          <div class="overview-item__value">{{ memoryUsage }}</div>
        </div>
        <div class="overview-item">
          <div class="overview-item__label">数据库</div>
          <div class="overview-item__value">{{ dbStatus }}</div>
        </div>
        <div class="overview-item">
          <div class="overview-item__label">Redis</div>
          <div class="overview-item__value">{{ redisStatus }}</div>
        </div>
      </div>
    </PageSection>

    <PageSection title="快捷入口" description="第一版建议优先落这 4 个轻操作入口。">
      <div class="quick-grid">
        <div v-for="item in quickLinks" :key="item.title" class="quick-card">
          <div class="quick-card__title">{{ item.title }}</div>
          <div class="quick-card__desc">{{ item.desc }}</div>
        </div>
      </div>
    </PageSection>

    <PageSection title="今日聚焦" description="把真正适合手机端即时处理的事情留在首页。">
      <div v-for="task in focusItems" :key="task.title" class="focus-item">
        <div class="focus-item__title">{{ task.title }}</div>
        <div class="focus-item__meta">{{ task.meta }}</div>
      </div>
    </PageSection>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getMonitorOverview, type MonitorOverview } from '@/api/monitor'
import { getCurrentSiteMessageUnreadCount } from '@/api/siteMessage'
import PageSection from '@/components/PageSection.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const monitor = ref<MonitorOverview | null>(null)
const unreadCount = ref(0)

const displayName = computed(() => authStore.userInfo?.nickName || authStore.userInfo?.username || '系统管理员')

const metrics = computed(() => [
  { label: '未读消息', value: String(unreadCount.value).padStart(2, '0') },
  { label: '在线会话', value: String(monitor.value?.security?.onlineSessionCount ?? 0).padStart(2, '0') },
  { label: '失败任务', value: String(monitor.value?.scheduledTask?.failureCount ?? 0).padStart(2, '0') }
])

const cpuUsage = computed(() => formatPercent(monitor.value?.cpu?.usagePercent))
const memoryUsage = computed(() => formatPercent(monitor.value?.memory?.usagePercent))
const dbStatus = computed(() => (monitor.value?.database?.connected ? '已连接' : '异常'))
const redisStatus = computed(() => (monitor.value?.redis?.connected ? '已连接' : '异常'))

const quickLinks = [
  { title: '失败任务重试', desc: '调度中心失败任务快速恢复' },
  { title: '告警确认', desc: '处理监控异常并回填结果' },
  { title: '消息重发', desc: '检查发送记录后直接补发' },
  { title: '现场上传', desc: '拍照、上传、生成文件分享外链' }
]

const focusItems = computed(() => {
  const items = []
  const failureCount = monitor.value?.scheduledTask?.failureCount ?? 0
  const latestFailureTaskName = monitor.value?.scheduledTask?.latestFailureTaskName
  const latestFailureTime = monitor.value?.scheduledTask?.latestFailureTime

  if (failureCount > 0) {
    items.push({
      title: `当前有 ${failureCount} 个失败任务待处理`,
      meta: latestFailureTaskName
        ? `最近失败任务：${latestFailureTaskName}${latestFailureTime ? `，时间 ${formatDateTime(latestFailureTime)}` : ''}`
        : '建议优先进入调度中心查看失败详情'
    })
  }

  if (unreadCount.value > 0) {
    items.push({
      title: `你有 ${unreadCount.value} 条未读站内消息`,
      meta: '适合在手机端快速查看、确认并批量标记已读'
    })
  }

  items.push({
    title: monitor.value?.database?.connected ? '数据库连接正常' : '数据库连接异常',
    meta: monitor.value?.database?.databaseName ? `当前数据库：${monitor.value.database.databaseName}` : '建议确认当前运行环境连通性'
  })

  return items
})

const formatPercent = (value?: number) => {
  if (value == null) {
    return '-'
  }
  return `${value.toFixed(1)}%`
}

const formatDateTime = (value?: string) => {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

const loadData = async () => {
  const [monitorData, unreadData] = await Promise.allSettled([
    getMonitorOverview(),
    getCurrentSiteMessageUnreadCount()
  ])

  if (monitorData.status === 'fulfilled') {
    monitor.value = monitorData.value
  }
  if (unreadData.status === 'fulfilled') {
    unreadCount.value = unreadData.value.unreadCount ?? 0
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
  padding: 16px;
}

.hero-card {
  padding: 22px 18px;
  border-radius: 28px;
  background: linear-gradient(160deg, #0b5bd3 0%, #2d79ff 52%, #81a8ff 100%);
  color: #fff;
  box-shadow: 0 18px 42px rgba(0, 82, 217, 0.25);
}

.hero-card__eyebrow {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  opacity: 0.78;
}

.hero-card__title {
  margin: 10px 0 8px;
  font-size: 28px;
  line-height: 1.2;
}

.hero-card__desc {
  margin: 0;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.88);
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 18px;
}

.hero-metrics__item {
  padding: 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.14);
}

.hero-metrics__value {
  font-size: 22px;
  font-weight: 700;
}

.hero-metrics__label {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.76);
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.quick-card,
.focus-item,
.overview-item {
  padding: 14px;
  border-radius: 18px;
  background: var(--sak-surface-soft);
  border: 1px solid var(--sak-border);
}

.quick-card__title,
.focus-item__title,
.overview-item__value {
  font-size: 15px;
  font-weight: 700;
}

.quick-card__desc,
.focus-item__meta,
.overview-item__label {
  margin-top: 6px;
  color: var(--sak-muted);
  font-size: 12px;
  line-height: 1.6;
}
</style>
