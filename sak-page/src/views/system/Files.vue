<template>
  <div class="page-container">
    <a-card title="文件中心">
      <template #extra>
        <a-space>
          <a-button
            v-if="authStore.hasPermission('system:file:upload')"
            type="primary"
            @click="openUploadModal"
          >
            上传文件
          </a-button>
          <a-button :loading="loading" @click="loadData">刷新</a-button>
        </a-space>
      </template>

      <a-space class="toolbar">
        <a-input v-model="keyword" allow-clear placeholder="搜索文件名/路径/备注" style="width: 260px" />
        <a-input v-model="operatorKeyword" allow-clear placeholder="上传人" style="width: 180px" />
        <a-input v-model="bizTypeKeyword" allow-clear placeholder="业务类型" style="width: 180px" />
        <a-button type="primary" :loading="loading" @click="handleSearch">查询</a-button>
        <a-button :loading="loading" @click="handleReset">重置</a-button>
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
        <template #fileSize="{ record }">
          {{ formatFileSize(record.fileSize) }}
        </template>
        <template #createTime="{ record }">
          {{ formatDateTime(record.createTime) }}
        </template>
        <template #operations="{ record }">
          <a-space>
            <a-button type="text" size="small" @click="handleDownload(record.downloadUrl)">
              下载
            </a-button>
            <a-popconfirm
              v-if="authStore.hasPermission('system:file:remove')"
              content="确认删除这条文件记录并移除物理文件？"
              @ok="handleDelete(record.id)"
            >
              <a-button type="text" size="small" status="danger">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:visible="uploadVisible"
      title="上传文件"
      :ok-loading="uploading"
      @ok="handleUpload"
      @cancel="resetUploadState"
    >
      <a-form :model="uploadForm" layout="vertical">
        <a-form-item label="业务类型">
          <a-input v-model="uploadForm.bizType" placeholder="例如 COMMON、NOTICE、RESOURCE" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model="uploadForm.remark" :max-length="500" show-word-limit />
        </a-form-item>
        <a-form-item label="上传文件">
          <input ref="fileInputRef" type="file" class="hidden-input" @change="handleFileChange" />
          <a-space direction="vertical" fill>
            <a-button @click="openFilePicker">选择文件</a-button>
            <span class="sub-text">{{ selectedFile?.name || '支持任意文件，单个文件不超过 50MB' }}</span>
          </a-space>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import { deleteCenterFile, getFileRecords, uploadCenterFile, type FileRecordItem } from '@/api/fileCenter'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const uploading = ref(false)
const uploadVisible = ref(false)
const records = ref<FileRecordItem[]>([])
const keyword = ref('')
const operatorKeyword = ref('')
const bizTypeKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedFile = ref<File | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)

const uploadForm = reactive({
  bizType: 'COMMON',
  remark: ''
})

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '业务类型', dataIndex: 'bizType', width: 140 },
  { title: '文件名', dataIndex: 'fileName', ellipsis: true, tooltip: true },
  { title: '存储名', dataIndex: 'storageName', ellipsis: true, tooltip: true },
  { title: '文件类型', dataIndex: 'contentType', width: 180, ellipsis: true, tooltip: true },
  { title: '文件大小', slotName: 'fileSize', width: 110 },
  { title: '上传人', dataIndex: 'operator', width: 120 },
  { title: '备注', dataIndex: 'remark', ellipsis: true, tooltip: true },
  { title: '文件路径', dataIndex: 'filePath', ellipsis: true, tooltip: true },
  { title: '上传时间', slotName: 'createTime', width: 180 },
  { title: '操作', slotName: 'operations', width: 140 }
]

const resolveDownloadUrl = (downloadUrl?: string) => {
  if (!downloadUrl) {
    return ''
  }
  const baseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')
  return /^(https?:)?\/\//.test(downloadUrl) ? downloadUrl : `${baseUrl}${downloadUrl.startsWith('/') ? '' : '/'}${downloadUrl}`
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
    const page = await getFileRecords({
      keyword: keyword.value.trim() || undefined,
      operator: operatorKeyword.value.trim() || undefined,
      bizType: bizTypeKeyword.value.trim() || undefined,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    records.value = page.records
    total.value = page.total
    currentPage.value = page.pageNum
    pageSize.value = page.pageSize
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadData()
}

const handleReset = () => {
  keyword.value = ''
  operatorKeyword.value = ''
  bizTypeKeyword.value = ''
  currentPage.value = 1
  loadData()
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

const openUploadModal = () => {
  resetUploadState()
  uploadVisible.value = true
}

const resetUploadState = () => {
  uploadForm.bizType = 'COMMON'
  uploadForm.remark = ''
  selectedFile.value = null
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

const openFilePicker = () => {
  fileInputRef.value?.click()
}

const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  selectedFile.value = target.files?.[0] || null
}

const handleUpload = async () => {
  if (!selectedFile.value) {
    Message.warning('请先选择文件')
    return
  }
  uploading.value = true
  try {
    const record = await uploadCenterFile({
      file: selectedFile.value,
      bizType: uploadForm.bizType.trim() || 'COMMON',
      remark: uploadForm.remark.trim() || undefined
    })
    Message.success(`上传成功，文件记录 ID：${record.id}`)
    uploadVisible.value = false
    resetUploadState()
    currentPage.value = 1
    await loadData()
  } finally {
    uploading.value = false
  }
}

const handleDownload = (downloadUrl?: string) => {
  const resolvedUrl = resolveDownloadUrl(downloadUrl)
  if (!resolvedUrl) {
    Message.warning('文件下载地址不存在')
    return
  }
  window.open(resolvedUrl, '_blank')
}

const handleDelete = async (id: number) => {
  await deleteCenterFile(id)
  Message.success('文件已删除')
  if (records.value.length === 1 && currentPage.value > 1) {
    currentPage.value -= 1
  }
  await loadData()
}

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

.hidden-input {
  display: none;
}
</style>
