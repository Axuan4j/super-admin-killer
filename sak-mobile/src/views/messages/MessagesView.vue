<template>
  <div class="page">
    <PageSection title="消息中心" description="适合手机端的不是完整通知后台，而是按处理优先级收消息。">
      <template #extra>
        <button class="text-button" type="button" @click="handleMarkAllRead">全部已读</button>
      </template>

      <div v-if="loading" class="empty-state">正在加载消息...</div>
      <div v-else-if="messages.length === 0" class="empty-state">暂无站内消息</div>
      <div v-for="message in messages" :key="message.id" class="message-card" :class="{ 'is-unread': message.readStatus === 0 }">
        <div class="message-card__title">{{ message.title }}</div>
        <div class="message-card__body">{{ message.content }}</div>
        <div class="message-card__meta">
          <span>{{ message.senderName || '系统' }}</span>
          <span>{{ formatDateTime(message.createTime) }}</span>
        </div>
      </div>
    </PageSection>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getCurrentSiteMessages, markCurrentSiteMessagesRead, type SiteMessage } from '@/api/siteMessage'
import PageSection from '@/components/PageSection.vue'

const loading = ref(false)
const messages = ref<SiteMessage[]>([])

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
  if (messages.value.length === 0) {
    return
  }
  await markCurrentSiteMessagesRead()
  await loadMessages()
}

onMounted(() => {
  loadMessages()
})
</script>

<style scoped>
.page {
  padding: 16px;
}

.text-button {
  border: 0;
  background: transparent;
  color: var(--sak-brand);
  font-size: 13px;
  font-weight: 700;
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
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
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
