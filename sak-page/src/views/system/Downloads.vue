<template>
  <div class="page-container">
    <a-card title="下载中心">
      <template #extra>
        <a-space>
          <a-button :loading="loading" @click="loadData">刷新</a-button>
        </a-space>
      </template>

      <a-space class="toolbar">
        <a-select v-model="bizTypeFilter" allow-clear placeholder="业务类型" style="width: 180px">
          <a-option value="OPER_LOG">操作日志</a-option>
        </a-select>
        <a-select v-model="statusFilter" allow-clear placeholder="导出状态" style="width: 180px">
          <a-option value="PENDING">待执行</a-option>
          <a-option value="RUNNING">执行中</a-option>
          <a-option value="SUCCESS">已完成</a-option>
          <a-option value="FAILED">失败</a-option>
        </a-select>
      </a-space>

      <a-table
        :columns="columns"
        :data="records"
        :loading="loading"
        :pagination="{
          total,
          current: currentPage,
          pageSize,
          showTotal: true,
          showPageSize: true
        }"
        row-key="id"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
      >
        <template #status="{ record }">
          <a-tag :color="statusColorMap[record.status] || 'gray'">
            {{ statusLabelMap[record.status] || record.status }}
          </a-tag>
        </template>
        <template #fileSize="{ record }">
          {{ formatFileSize(record.fileSize) }}
        </template>
        <template #time="{ record }">
          <a-space direction="vertical" :size="2">
            <span>创建：{{ formatDateTime(record.createTime) }}</span>
            <span class="sub-text">完成：{{ formatDateTime(record.finishTime) }}</span>
          </a-space>
        </template>
        <template #operations="{ record }">
          <a-space>
            <a-button
              type="text"
              size="small"
              :disabled="record.status !== 'SUCCESS' || !record.downloadUrl"
              @click="handleDownload(record.downloadUrl)"
            >
              下载
            </a-button>
            <a-button v-if="record.errMsg" type="text" size="small" status="danger" @click="showError(record.errMsg)">
              失败原因
            </a-button>
          </a-space>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import { getExportRecords, type ExportRecordItem } from '@/api/exportRecord'

const loading = ref(false)
const records = ref<ExportRecordItem[]>([])
const bizTypeFilter = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '业务类型', dataIndex: 'bizType', width: 140 },
  { title: '文件名', dataIndex: 'fileName', ellipsis: true, tooltip: true },
  { title: '查询条件', dataIndex: 'queryCondition', ellipsis: true, tooltip: true },
  { title: '状态', slotName: 'status', width: 100 },
  { title: '文件路径', dataIndex: 'filePath', ellipsis: true, tooltip: true },
  { title: '导出条数', dataIndex: 'totalCount', width: 110 },
  { title: '文件大小', slotName: 'fileSize', width: 110 },
  { title: '操作人', dataIndex: 'operator', width: 120 },
  { title: '时间', slotName: 'time', width: 220 },
  { title: '操作', slotName: 'operations', width: 160 }
]

const statusLabelMap: Record<string, string> = {
  PENDING: '待执行',
  RUNNING: '执行中',
  SUCCESS: '已完成',
  FAILED: '失败'
}

const statusColorMap: Record<string, string> = {
  PENDING: 'gray',
  RUNNING: 'arcoblue',
  SUCCESS: 'green',
  FAILED: 'red'
}

const formatDateTime = (value?: string) => {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

const formatFileSize = (size?: number) => {
  if (!size || size <= 0) {
    return '-'
  }
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

const loadData = async () => {
  loading.value = true
  try {
    const page = await getExportRecords({
      bizType: bizTypeFilter.value || undefined,
      status: statusFilter.value || undefined,
      current: currentPage.value,
      size: pageSize.value
    })
    records.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadData()
}

const handlePageSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadData()
}

const handleDownload = (downloadUrl?: string) => {
  if (!downloadUrl) {
    Message.warning('文件尚未生成')
    return
  }
  const baseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')
  const normalizedUrl = /^(https?:)?\/\//.test(downloadUrl) ? downloadUrl : `${baseUrl}${downloadUrl.startsWith('/') ? '' : '/'}${downloadUrl}`
  window.open(normalizedUrl, '_blank')
}

const showError = (message: string) => {
  Modal.error({
    title: '导出失败',
    content: message
  })
}

watch([bizTypeFilter, statusFilter], () => {
  currentPage.value = 1
  loadData()
})

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page-container {
  min-height: 100%;
}

.toolbar {
  margin-bottom: 16px;
}

.sub-text {
  color: var(--app-muted-text, #86909c);
  font-size: 12px;
}
</style>
