<template>
  <div class="page-container">
    <a-card title="站内消息">
      <template #extra>
        <a-button type="outline" :loading="loading" @click="handleRefreshClick">
          刷新
        </a-button>
      </template>

      <a-spin :loading="loading" style="width: 100%">
        <a-empty v-if="messages.length === 0" description="当前没有站内消息" />

        <div v-else class="message-list">
          <article
            v-for="message in messages"
            :key="message.id"
            class="message-item"
            :class="{ unread: message.readStatus === 0 }"
          >
            <div class="message-header">
              <div class="message-title-row">
                <h3 class="message-title">{{ message.title }}</h3>
                <a-tag :color="message.readStatus === 0 ? 'orangered' : 'green'">
                  {{ message.readStatus === 0 ? '未读' : '已读' }}
                </a-tag>
              </div>
              <div class="message-meta">
                <span>发送人：{{ message.senderName || '系统' }}</span>
                <span>发送时间：{{ formatDateTime(message.createTime) }}</span>
                <span v-if="message.readTime">阅读时间：{{ formatDateTime(message.readTime) }}</span>
              </div>
            </div>

            <div class="message-content">
              {{ message.content }}
            </div>
          </article>
        </div>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import {
  getCurrentSiteMessages,
  markCurrentSiteMessagesRead,
  type SiteMessage
} from '@/api/siteMessage'
import {
  SITE_MESSAGE_PUSH_EVENT,
  SITE_MESSAGE_READ_EVENT,
  type SiteMessageSocketPayload
} from '@/utils/siteMessageEvents'

const loading = ref(false)
const messages = ref<SiteMessage[]>([])

const formatDateTime = (value?: string | null) => {
  if (!value) {
    return '-'
  }

  return value.replace('T', ' ').slice(0, 19)
}

const loadMessages = async (markUnreadAsRead = true) => {
  loading.value = true
  try {
    const currentMessages = await getCurrentSiteMessages()
    const hasUnread = currentMessages.some(message => message.readStatus === 0)

    if (markUnreadAsRead && hasUnread) {
      await markCurrentSiteMessagesRead()
      window.dispatchEvent(new CustomEvent(SITE_MESSAGE_READ_EVENT))
      messages.value = await getCurrentSiteMessages()
      return
    }

    messages.value = currentMessages
  } finally {
    loading.value = false
  }
}

const handleRefreshClick = () => {
  loadMessages(false)
}

const handleMessagePushed = async (event: Event) => {
  const payload = (event as CustomEvent<SiteMessageSocketPayload>).detail
  if (payload?.type === 'NEW_MESSAGE') {
    await loadMessages(false)
  }
}

onMounted(() => {
  loadMessages()
  window.addEventListener(SITE_MESSAGE_PUSH_EVENT, handleMessagePushed as EventListener)
})

onUnmounted(() => {
  window.removeEventListener(SITE_MESSAGE_PUSH_EVENT, handleMessagePushed as EventListener)
})
</script>

<style scoped>
.page-container {
  min-height: 100%;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  border: 1px solid #e5e6eb;
  border-radius: 12px;
  padding: 18px 20px;
  background: #fff;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.message-item.unread {
  border-color: #ffb65d;
  box-shadow: 0 6px 18px rgba(255, 125, 0, 0.08);
}

.message-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.message-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.message-title {
  margin: 0;
  color: #1d2129;
  font-size: 16px;
  font-weight: 600;
}

.message-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 20px;
  color: #86909c;
  font-size: 13px;
}

.message-content {
  color: #4e5969;
  line-height: 1.7;
  white-space: pre-wrap;
}
</style>
