<template>
  <div class="page-container">
    <a-card title="消息推送中心">
      <a-tabs v-model:active-key="activeTab" lazy-load>
        <a-tab-pane key="send" title="发送通知">
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

          <a-space class="toolbar">
            <a-button :loading="loadingRecipients" @click="loadRecipients">刷新用户</a-button>
            <a-popconfirm
              v-if="authStore.hasPermission('system:notification:send')"
              content="确认发送这条通知？发送后将立即触达所选用户和渠道。"
              @ok="handleSubmit"
            >
              <a-button
                type="primary"
                :loading="submitting"
              >
                <template #icon><font-awesome-icon icon="fa-solid fa-paper-plane" /></template>
                发送通知
              </a-button>
            </a-popconfirm>
          </a-space>

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
        </a-tab-pane>

        <a-tab-pane key="records" title="发送记录">
          <a-space class="toolbar">
            <a-input v-model="recordQuery.title" allow-clear placeholder="通知标题" style="width: 200px" />
            <a-input v-model="recordQuery.senderName" allow-clear placeholder="发送人" style="width: 160px" />
            <a-select v-model="recordQuery.channelCode" allow-clear placeholder="发送渠道" style="width: 160px">
              <a-option value="SITE_MESSAGE">站内信</a-option>
              <a-option value="EMAIL">邮件</a-option>
              <a-option value="WXPUSHER">WxPusher</a-option>
            </a-select>
            <a-select v-model="recordQuery.status" allow-clear placeholder="状态" style="width: 140px">
              <a-option value="SUCCESS">成功</a-option>
              <a-option value="PARTIAL">部分成功</a-option>
              <a-option value="FAILED">失败</a-option>
            </a-select>
            <a-button type="primary" :loading="loadingRecords" @click="handleRecordSearch">查询</a-button>
            <a-button :loading="loadingRecords" @click="handleRecordReset">重置</a-button>
          </a-space>

          <a-table
            :columns="recordColumns"
            :data="records"
            :loading="loadingRecords"
            :pagination="{
              total: recordTotal,
              current: recordPageNum,
              pageSize: recordPageSize,
              showTotal: true,
              showPageSize: true
            }"
            row-key="id"
            @page-change="handleRecordPageChange"
            @page-size-change="handleRecordPageSizeChange"
          >
            <template #channels="{ record }">
              <a-space wrap>
                <a-tag v-for="channel in record.channels" :key="channel" color="arcoblue">{{ resolveChannelLabel(channel) }}</a-tag>
              </a-space>
            </template>
            <template #status="{ record }">
              <a-tag :color="resolveRecordStatusColor(record.status)">{{ resolveRecordStatusText(record.status) }}</a-tag>
            </template>
            <template #successRate="{ record }">
              {{ formatSuccessRate(record.successUserCount, record.recipientCount) }}
            </template>
            <template #createTime="{ record }">
              {{ formatDateTime(record.createTime) }}
            </template>
            <template #operations="{ record }">
              <a-button type="text" size="small" @click="openRecordDetail(record.id)">详情</a-button>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:visible="detailVisible" title="发送记录详情" :footer="false" width="960px">
      <a-space v-if="currentRecord" direction="vertical" fill :size="16">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="标题">{{ currentRecord.title }}</a-descriptions-item>
          <a-descriptions-item label="发送人">{{ currentRecord.senderName }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            {{ resolveRecordStatusText(currentRecord.status) }}
          </a-descriptions-item>
          <a-descriptions-item label="发送时间">{{ formatDateTime(currentRecord.createTime) }}</a-descriptions-item>
          <a-descriptions-item label="目标人数">{{ currentRecord.recipientCount }}</a-descriptions-item>
          <a-descriptions-item label="成功触达">{{ currentRecord.successUserCount }}</a-descriptions-item>
          <a-descriptions-item label="发送渠道" :span="2">
            <a-space wrap>
              <a-tag v-for="channel in currentRecord.channels" :key="channel" color="arcoblue">{{ resolveChannelLabel(channel) }}</a-tag>
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item label="通知内容" :span="2">{{ currentRecord.content }}</a-descriptions-item>
        </a-descriptions>

        <a-table :columns="detailColumns" :data="currentRecord.recipientDetails" :pagination="false" row-key="userId" size="small">
          <template #recipient="{ record }">
            {{ record.nickName || record.username || '-' }}
            <span class="sub-inline">({{ record.username || '-' }})</span>
          </template>
          <template #status="{ record }">
            <a-tag :color="record.status === 'SUCCESS' ? 'green' : 'red'">
              {{ record.status === 'SUCCESS' ? '成功' : '失败' }}
            </a-tag>
          </template>
          <template #channelResults="{ record }">
            <a-space direction="vertical" :size="4">
              <div v-for="channel in record.channels" :key="`${record.userId}-${channel.channelCode}`" class="channel-result-line">
                <a-tag :color="resolveChannelDetailColor(channel.status)">{{ resolveChannelLabel(channel.channelCode) }}</a-tag>
                <span>{{ resolveChannelStatusText(channel.status) }}</span>
                <span v-if="channel.message" class="sub-inline">- {{ channel.message }}</span>
              </div>
            </a-space>
          </template>
        </a-table>
      </a-space>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import {
  getNotificationRecipients,
  getNotificationRecordDetail,
  getNotificationRecords,
  sendNotification,
  type NotificationRecipient,
  type NotificationRecordDetail,
  type NotificationRecordItem
} from '@/api/adminNotification.ts'
import { useAuthStore } from '@/stores/auth.ts'

const authStore = useAuthStore()
const activeTab = ref('send')
const loadingRecipients = ref(false)
const submitting = ref(false)
const loadingRecords = ref(false)
const detailVisible = ref(false)
const recipients = ref<NotificationRecipient[]>([])
const records = ref<NotificationRecordItem[]>([])
const currentRecord = ref<NotificationRecordDetail | null>(null)
const recordPageNum = ref(1)
const recordPageSize = ref(10)
const recordTotal = ref(0)

const createDefaultForm = () => ({
  sendAll: true,
  userIds: [] as number[],
  channels: ['SITE_MESSAGE'] as string[],
  title: '',
  content: ''
})

const form = reactive(createDefaultForm())
const recordQuery = reactive({
  title: '',
  senderName: '',
  channelCode: undefined as string | undefined,
  status: undefined as string | undefined
})

const recordColumns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '标题', dataIndex: 'title', ellipsis: true, tooltip: true },
  { title: '发送人', dataIndex: 'senderName', width: 120 },
  { title: '渠道', slotName: 'channels', width: 240 },
  { title: '目标人数', dataIndex: 'recipientCount', width: 100 },
  { title: '成功率', slotName: 'successRate', width: 110 },
  { title: '状态', slotName: 'status', width: 110 },
  { title: '发送时间', slotName: 'createTime', width: 180 },
  { title: '操作', slotName: 'operations', width: 90 }
]

const detailColumns = [
  { title: '接收人', slotName: 'recipient', width: 180 },
  { title: '结果', slotName: 'status', width: 100 },
  { title: '渠道明细', slotName: 'channelResults' }
]

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

const loadRecords = async () => {
  loadingRecords.value = true
  try {
    const page = await getNotificationRecords({
      title: recordQuery.title.trim() || undefined,
      senderName: recordQuery.senderName.trim() || undefined,
      channelCode: recordQuery.channelCode,
      status: recordQuery.status,
      pageNum: recordPageNum.value,
      pageSize: recordPageSize.value
    })
    records.value = page.records
    recordTotal.value = page.total
    recordPageNum.value = page.pageNum
    recordPageSize.value = page.pageSize
  } finally {
    loadingRecords.value = false
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
    activeTab.value = 'records'
    recordPageNum.value = 1
    await loadRecords()
    if (result.recordId) {
      await openRecordDetail(result.recordId)
    }
  } finally {
    submitting.value = false
  }
}

const handleRecordSearch = () => {
  recordPageNum.value = 1
  loadRecords()
}

const handleRecordReset = () => {
  recordQuery.title = ''
  recordQuery.senderName = ''
  recordQuery.channelCode = undefined
  recordQuery.status = undefined
  recordPageNum.value = 1
  loadRecords()
}

const handleRecordPageChange = (page: number) => {
  recordPageNum.value = page
  loadRecords()
}

const handleRecordPageSizeChange = (size: number) => {
  recordPageSize.value = size
  recordPageNum.value = 1
  loadRecords()
}

const openRecordDetail = async (id: number) => {
  currentRecord.value = await getNotificationRecordDetail(id)
  detailVisible.value = true
}

const formatDateTime = (value?: string) => {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

const formatSuccessRate = (successUserCount?: number, recipientCount?: number) => {
  const total = recipientCount || 0
  if (!total) {
    return '0.00%'
  }
  return `${(((successUserCount || 0) / total) * 100).toFixed(2)}%`
}

const resolveChannelLabel = (channelCode?: string) => {
  switch (channelCode) {
    case 'SITE_MESSAGE':
      return '站内信'
    case 'EMAIL':
      return '邮件'
    case 'WXPUSHER':
      return 'WxPusher'
    default:
      return channelCode || '-'
  }
}

const resolveRecordStatusText = (status?: string) => {
  switch (status) {
    case 'SUCCESS':
      return '成功'
    case 'PARTIAL':
      return '部分成功'
    case 'FAILED':
      return '失败'
    default:
      return status || '-'
  }
}

const resolveRecordStatusColor = (status?: string) => {
  switch (status) {
    case 'SUCCESS':
      return 'green'
    case 'PARTIAL':
      return 'orange'
    case 'FAILED':
      return 'red'
    default:
      return 'gray'
  }
}

const resolveChannelStatusText = (status?: string) => {
  switch (status) {
    case 'SUCCESS':
      return '成功'
    case 'SKIPPED':
      return '跳过'
    case 'FAILED':
      return '失败'
    default:
      return status || '-'
  }
}

const resolveChannelDetailColor = (status?: string) => {
  switch (status) {
    case 'SUCCESS':
      return 'green'
    case 'SKIPPED':
      return 'orange'
    case 'FAILED':
      return 'red'
    default:
      return 'gray'
  }
}

watch(activeTab, (value) => {
  if (value === 'records' && records.value.length === 0) {
    loadRecords()
  }
})

onMounted(() => {
  loadRecipients()
  loadRecords()
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

.toolbar {
  margin-bottom: 16px;
}

.recipient-meta {
  margin-top: 8px;
  color: #86909c;
  font-size: 12px;
}

.channel-tip {
  margin-left: 8px;
  color: #86909c;
  font-size: 12px;
}

.recipient-card {
  background: #fbfdff;
  border: 1px solid #e5e6eb;
}

.channel-preview-card {
  margin-top: 16px;
}

.recipient-card :deep(.arco-card-header) {
  border-bottom-color: #e5e6eb;
}

.recipient-card :deep(.arco-card-header-title) {
  color: #1d2129;
}

.recipient-card :deep(.arco-empty-description) {
  color: #86909c;
}

.sub-inline {
  color: #86909c;
  margin-left: 4px;
}

.channel-result-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
