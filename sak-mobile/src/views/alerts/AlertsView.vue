<template>
  <div class="page">
    <PageSection title="告警与消息" description="把高频待办和站内消息合成一个主入口，适合手机端快速切换处理。">
      <div class="switch-tabs">
        <button
          v-for="item in switchTabs"
          :key="item.value"
          class="switch-tabs__item"
          :class="{ 'is-active': activeTab === item.value }"
          type="button"
          @click="activeTab = item.value"
        >
          <span>{{ item.label }}</span>
          <span v-if="item.badge > 0" class="switch-tabs__badge">{{ item.badge > 99 ? '99+' : item.badge }}</span>
        </button>
      </div>

      <template v-if="activeTab === 'alerts'">
        <div v-for="item in alerts" :key="item.title" class="alert-card" :class="`alert-card--${item.level}`">
          <div class="alert-card__top">
            <span class="alert-card__level">{{ item.levelText }}</span>
            <span class="alert-card__time">{{ item.time }}</span>
          </div>
          <div class="alert-card__title">{{ item.title }}</div>
          <div class="alert-card__desc">{{ item.desc }}</div>
          <div class="alert-card__action">{{ item.action }}</div>
        </div>
      </template>

      <template v-else>
        <div class="message-actions">
          <button class="text-button" type="button" :disabled="loading" @click="loadMessages">
            {{ loading ? '刷新中...' : '刷新消息' }}
          </button>
          <button class="primary-button" type="button" :disabled="loading || unreadCount === 0" @click="handleMarkAllRead">
            全部已读
          </button>
        </div>

        <div v-if="loading" class="empty-state">正在加载消息...</div>
        <div v-else-if="messages.length === 0" class="empty-state">暂无站内消息</div>
        <div v-else class="message-list">
          <article
            v-for="message in messages"
            :key="message.id"
            class="message-card"
            :class="{ 'is-unread': message.readStatus === 0 }"
          >
            <div class="message-card__title">{{ message.title }}</div>
            <div class="message-card__body">{{ message.content }}</div>
            <div class="message-card__meta">
              <span>{{ message.senderName || '系统' }}</span>
              <span>{{ formatDateTime(message.createTime) }}</span>
            </div>
          </article>
        </div>
      </template>
    </PageSection>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getCurrentSiteMessages, markCurrentSiteMessagesRead, type SiteMessage } from '@/api/siteMessage'
import PageSection from '@/components/PageSection.vue'

const activeTab = ref<'alerts' | 'messages'>('alerts')
const loading = ref(false)
const messages = ref<SiteMessage[]>([])

const alerts = [
  {
    level: 'danger',
    levelText: '紧急',
    time: '2 分钟前',
    title: '订单回调任务连续失败',
    desc: '连续 3 次执行失败，已经达到移动端优先处理阈值。',
    action: '建议动作：先暂停任务，再进入详情页重试。'
  },
  {
    level: 'warning',
    levelText: '关注',
    time: '12 分钟前',
    title: 'Redis 连接池等待时间升高',
    desc: '当前只适合在手机端确认异常是否持续，不建议直接做复杂排障。',
    action: '建议动作：确认后通知值班同学。'
  },
  {
    level: 'info',
    levelText: '提醒',
    time: '25 分钟前',
    title: '5 条消息发送失败待重发',
    desc: '适合移动端快速筛一遍失败原因，并触发人工补发。',
    action: '建议动作：进入消息发送记录查看失败详情。'
  }
]

const unreadCount = computed(() => messages.value.filter((item) => item.readStatus === 0).length)

const switchTabs = computed(() => [
  { label: '告警', value: 'alerts', badge: alerts.length },
  { label: '消息', value: 'messages', badge: unreadCount.value }
])

const formatDateTime = (value?: string) => {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

const loadMessages = async () => {
  loading.value = true
  try {
    messages.value = await getCurrentSiteMessages()
  } finally {
    loading.value = false
  }
}

const handleMarkAllRead = async () => {
  if (unreadCount.value === 0) {
    return
  }
  loading.value = true
  try {
    await markCurrentSiteMessagesRead()
    messages.value = messages.value.map((item) => ({
      ...item,
      readStatus: 1
    }))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadMessages()
})
</script>

<style scoped>
.page {
  padding: 16px;
}

.switch-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.switch-tabs__item {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border: 0;
  border-radius: 16px;
  background: var(--sak-surface-soft);
  color: var(--sak-muted);
  font-size: 14px;
  font-weight: 700;
}

.switch-tabs__item.is-active {
  background: var(--sak-brand-soft);
  color: var(--sak-brand);
}

.switch-tabs__badge {
  min-width: 20px;
  padding: 2px 6px;
  border-radius: 999px;
  background: rgba(0, 82, 217, 0.14);
  color: var(--sak-brand);
  font-size: 11px;
}

.alert-card {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--sak-border);
  background: #fff;
}

.alert-card--danger {
  border-color: rgba(207, 63, 76, 0.24);
  background: #fff7f7;
}

.alert-card--warning {
  border-color: rgba(217, 119, 6, 0.22);
  background: #fffaf3;
}

.alert-card--info {
  border-color: rgba(0, 82, 217, 0.18);
  background: #f7faff;
}

.alert-card__top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--sak-muted);
  font-size: 12px;
}

.alert-card__level {
  font-weight: 700;
}

.alert-card__title {
  margin-top: 8px;
  font-size: 16px;
  font-weight: 700;
}

.alert-card__desc,
.alert-card__action {
  margin-top: 8px;
  color: var(--sak-muted);
  font-size: 13px;
  line-height: 1.6;
}

.message-actions {
  display: flex;
  gap: 10px;
}

.text-button,
.primary-button {
  border: 0;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 700;
  padding: 11px 14px;
}

.text-button {
  flex: 1;
  background: var(--sak-surface-soft);
  color: var(--sak-text);
}

.primary-button {
  flex: 1;
  background: var(--sak-brand);
  color: #fff;
}

.primary-button:disabled,
.text-button:disabled {
  opacity: 0.6;
}

.message-list {
  display: grid;
  gap: 12px;
}

.message-card {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--sak-border);
  background: #fff;
}

.message-card.is-unread {
  border-color: rgba(0, 82, 217, 0.24);
  background: #f7faff;
}

.message-card__title {
  font-size: 15px;
  font-weight: 700;
}

.message-card__body {
  margin-top: 8px;
  color: var(--sak-muted);
  font-size: 13px;
  line-height: 1.6;
}

.message-card__meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 10px;
  color: var(--sak-muted);
  font-size: 12px;
}

.empty-state {
  padding: 20px 0;
  text-align: center;
  color: var(--sak-muted);
  font-size: 13px;
}
</style>
