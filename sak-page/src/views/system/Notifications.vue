<template>
  <div class="page-container">
    <a-card title="消息推送中心">
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
        可同时勾选站内信、邮件、WxPusher 多种渠道。站内信会实时刷新消息中心；邮件依赖邮箱配置；WxPusher 依赖用户 UID 和服务端 AppToken。
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

        <a-form-item label="推送渠道">
          <a-checkbox-group v-model="form.channels" direction="vertical">
            <a-checkbox value="SITE_MESSAGE">
              站内信
              <span class="channel-tip">写入消息中心，并通过 websocket 实时提示</span>
            </a-checkbox>
            <a-checkbox value="EMAIL">
              邮件
              <span class="channel-tip">仅向已配置邮箱的用户发送</span>
            </a-checkbox>
            <a-checkbox value="WXPUSHER">
              WxPusher
              <span class="channel-tip">仅向已配置 WxPusher UID 的用户发送</span>
            </a-checkbox>
          </a-checkbox-group>
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

      <a-card title="渠道预检查" size="small" class="recipient-card channel-preview-card">
        <a-space wrap>
          <a-tag color="arcoblue">目标用户 {{ targetRecipients.length }} 人</a-tag>
          <a-tag color="green">邮箱已配置 {{ targetRecipientsWithEmailCount }} 人</a-tag>
          <a-tag color="orange">WxPusher UID 已配置 {{ targetRecipientsWithWxPusherCount }} 人</a-tag>
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
  channels: ['SITE_MESSAGE'] as string[],
  title: '',
  content: ''
})

const form = reactive(createDefaultForm())

const selectedRecipients = computed(() => {
  const selectedIds = new Set(form.userIds)
  return recipients.value.filter(user => selectedIds.has(user.id))
})

const targetRecipients = computed(() => (form.sendAll ? recipients.value : selectedRecipients.value))

const targetRecipientsWithEmailCount = computed(() =>
  targetRecipients.value.filter(user => !!user.email?.trim()).length
)

const targetRecipientsWithWxPusherCount = computed(() =>
  targetRecipients.value.filter(user => !!user.wxPusherUid?.trim()).length
)

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
  if (form.channels.length === 0) {
    Message.warning('请至少选择一种推送渠道')
    return
  }

  submitting.value = true
  try {
    const result = await sendNotification({
      sendAll: form.sendAll,
      userIds: form.sendAll ? undefined : form.userIds,
      channels: form.channels,
      title: form.title.trim(),
      content: form.content.trim()
    })
    Message.success(`推送完成：目标 ${result.recipientCount} 人，至少成功触达 ${result.successUserCount} 人`)
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

.channel-tip {
  margin-left: 8px;
  color: var(--app-muted-text, #86909c);
  font-size: 12px;
}

.recipient-card {
  background: var(--app-panel-bg, #fbfdff);
  border: 1px solid var(--app-border-color, #e5e6eb);
}

.channel-preview-card {
  margin-top: 16px;
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
