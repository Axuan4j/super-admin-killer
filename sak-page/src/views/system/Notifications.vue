<template>
  <div class="page-container">
    <a-card title="通知管理">
      <template #extra>
        <a-space>
          <a-button :loading="loadingRecipients" @click="loadRecipients">刷新用户</a-button>
          <a-button
            v-if="authStore.hasPermission('system:notification:send')"
            type="primary"
            :loading="submitting"
            @click="handleSubmit"
          >
            <template #icon><font-awesome-icon icon="fa-solid fa-paper-plane" /></template>
            发送通知
          </a-button>
        </a-space>
      </template>

      <a-alert type="info" show-icon class="info-banner">
        在线用户会实时收到推送；离线用户会先写入站内消息，登录后可在通知中心查看。
      </a-alert>

      <a-form :model="form" layout="vertical" class="notification-form">
        <a-form-item label="发送范围">
          <a-radio-group v-model="form.sendAll">
            <a-radio :value="true">发送给全部启用用户</a-radio>
            <a-radio :value="false">发送给指定用户</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item v-if="!form.sendAll" label="接收用户">
          <a-select
            v-model="form.userIds"
            :loading="loadingRecipients"
            multiple
            allow-search
            allow-clear
            placeholder="请选择一个或多个接收用户"
          >
            <a-option v-for="user in recipients" :key="user.id" :value="user.id">
              {{ user.nickName || user.username }}（{{ user.username }}）
            </a-option>
          </a-select>
          <div class="recipient-meta">
            已选择 {{ form.userIds.length }} 人，共可选 {{ recipients.length }} 名启用用户
          </div>
        </a-form-item>

        <a-form-item label="通知标题">
          <a-input v-model="form.title" :max-length="120" show-word-limit placeholder="请输入通知标题" />
        </a-form-item>

        <a-form-item label="通知内容">
          <a-textarea
            v-model="form.content"
            :max-length="1000"
            show-word-limit
            :auto-size="{ minRows: 6, maxRows: 10 }"
            placeholder="请输入通知内容"
          />
        </a-form-item>
      </a-form>

      <a-card title="接收用户预览" size="small" class="recipient-card">
        <a-empty v-if="!form.sendAll && selectedRecipients.length === 0" description="还没有选择接收用户" />
        <a-space v-else wrap>
          <a-tag v-if="form.sendAll" color="arcoblue">全部启用用户</a-tag>
          <a-tag v-for="user in selectedRecipients" :key="user.id" color="green">
            {{ user.nickName || user.username }}
          </a-tag>
        </a-space>
      </a-card>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import { getNotificationRecipients, sendNotification, type NotificationRecipient } from '@/api/adminNotification.ts'
import { useAuthStore } from '@/stores/auth.ts'

const authStore = useAuthStore()
const loadingRecipients = ref(false)
const submitting = ref(false)
const recipients = ref<NotificationRecipient[]>([])

const createDefaultForm = () => ({
  sendAll: true,
  userIds: [] as number[],
  title: '',
  content: ''
})

const form = reactive(createDefaultForm())

const selectedRecipients = computed(() => {
  const selectedIds = new Set(form.userIds)
  return recipients.value.filter(user => selectedIds.has(user.id))
})

const resetForm = () => {
  Object.assign(form, createDefaultForm())
}

const loadRecipients = async () => {
  loadingRecipients.value = true
  try {
    recipients.value = await getNotificationRecipients()
  } finally {
    loadingRecipients.value = false
  }
}

const handleSubmit = async () => {
  if (!form.title.trim()) {
    Message.warning('请填写通知标题')
    return
  }
  if (!form.content.trim()) {
    Message.warning('请填写通知内容')
    return
  }
  if (!form.sendAll && form.userIds.length === 0) {
    Message.warning('请选择至少一个接收用户')
    return
  }

  submitting.value = true
  try {
    const result = await sendNotification({
      sendAll: form.sendAll,
      userIds: form.sendAll ? undefined : form.userIds,
      title: form.title.trim(),
      content: form.content.trim()
    })
    Message.success(`通知发送成功，已投递 ${result.successCount} 人`)
    resetForm()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadRecipients()
})
</script>

<style scoped>
.page-container {
  min-height: 100%;
}

.info-banner {
  margin-bottom: 20px;
}

.notification-form {
  margin-bottom: 20px;
}

.recipient-meta {
  margin-top: 8px;
  color: var(--app-muted-text, #86909c);
  font-size: 12px;
}

.recipient-card {
  background: var(--app-panel-bg, #fbfdff);
  border: 1px solid var(--app-border-color, #e5e6eb);
}

.recipient-card :deep(.arco-card-header) {
  border-bottom-color: var(--app-border-color, #e5e6eb);
}

.recipient-card :deep(.arco-card-header-title) {
  color: var(--app-header-text, #1d2129);
}

.recipient-card :deep(.arco-empty-description) {
  color: var(--app-muted-text, #86909c);
}
</style>
