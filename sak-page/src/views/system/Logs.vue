<template>
  <div class="page-container">
    <a-card title="操作日志">
      <template #extra>
        <a-space>
          <a-button
            v-if="authStore.hasPermission('system:log:export')"
            type="primary"
            status="success"
            :loading="exporting"
            @click="handleExport"
          >
            导出
          </a-button>
          <a-button @click="handleSearch" type="primary" :loading="loading">查询</a-button>
          <a-button @click="handleReset" :loading="loading">重置</a-button>
          <a-button :loading="loading" @click="loadData">刷新</a-button>
        </a-space>
      </template>

      <a-space class="toolbar">
        <a-input v-model="operatorKeyword" allow-clear placeholder="操作人" style="width: 180px" />
        <a-input v-model="logTypeKeyword" allow-clear placeholder="日志类型" style="width: 180px" />
        <a-input v-model="actionKeyword" allow-clear placeholder="操作描述" style="width: 240px" />
        <a-select v-model="successFilter" allow-clear placeholder="执行结果" style="width: 160px">
          <a-option v-for="item in successOptions" :key="item.value" :value="Number(item.value)">{{ item.label }}</a-option>
        </a-select>
      </a-space>

      <a-table
        :columns="columns"
        :data="logs"
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
        <template #success="{ record }">
          <a-tag :color="dictStore.getDictTagColor('sys_oper_success', record.success)">
            {{ dictStore.getDictLabel('sys_oper_success', record.success) }}
          </a-tag>
        </template>
        <template #action="{ record }">
          <div class="action-cell">{{ record.action || '-' }}</div>
        </template>
        <template #createTime="{ record }">
          {{ formatDateTime(record.createTime) }}
        </template>
        <template #operations="{ record }">
          <a-button type="text" size="small" @click="openDetail(record)">详情</a-button>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:visible="detailVisible" title="日志详情" :footer="false" width="720px">
      <a-descriptions v-if="currentLog" :column="2" bordered>
        <a-descriptions-item label="操作人">{{ currentLog.operator || '-' }}</a-descriptions-item>
        <a-descriptions-item label="结果">{{ dictStore.getDictLabel('sys_oper_success', currentLog.success) }}</a-descriptions-item>
        <a-descriptions-item label="日志类型">{{ currentLog.logType || '-' }}</a-descriptions-item>
        <a-descriptions-item label="子类型">{{ currentLog.subType || '-' }}</a-descriptions-item>
        <a-descriptions-item label="业务标识">{{ currentLog.bizNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="IP">{{ currentLog.ip || '-' }}</a-descriptions-item>
        <a-descriptions-item label="请求方式">{{ currentLog.requestMethod || '-' }}</a-descriptions-item>
        <a-descriptions-item label="请求地址">{{ currentLog.requestUrl || '-' }}</a-descriptions-item>
        <a-descriptions-item label="记录时间">{{ formatDateTime(currentLog.createTime) }}</a-descriptions-item>
        <a-descriptions-item label="方法标识">{{ currentLog.method || '-' }}</a-descriptions-item>
        <a-descriptions-item label="操作描述" :span="2">{{ currentLog.action || '-' }}</a-descriptions-item>
        <a-descriptions-item label="附加信息" :span="2">{{ currentLog.extra || '-' }}</a-descriptions-item>
        <a-descriptions-item label="错误信息" :span="2">{{ currentLog.errMsg || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import { useRouter } from 'vue-router'
import { exportOperLogs, getOperLogs, type OperLogItem } from '@/api/operLog.ts'
import { useAuthStore } from '@/stores/auth.ts'
import { useDictStore } from '@/stores/dict.ts'

const router = useRouter()
const authStore = useAuthStore()
const dictStore = useDictStore()
const loading = ref(false)
const exporting = ref(false)
const logs = ref<OperLogItem[]>([])
const operatorKeyword = ref('')
const logTypeKeyword = ref('')
const actionKeyword = ref('')
const successFilter = ref<number | undefined>()
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const currentLog = ref<OperLogItem | null>(null)
const successOptions = computed(() => dictStore.getDictItems('sys_oper_success'))

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '操作人', dataIndex: 'operator', width: 120 },
  { title: '日志类型', dataIndex: 'logType', width: 120 },
  { title: '子类型', dataIndex: 'subType', width: 120 },
  { title: '操作描述', slotName: 'action' },
  { title: 'IP地址', dataIndex: 'ip', width: 140 },
  { title: '结果', slotName: 'success', width: 90 },
  { title: '操作时间', slotName: 'createTime', width: 180 },
  { title: '操作', slotName: 'operations', width: 80 }
]

const formatDateTime = (value?: string) => {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

const loadData = async () => {
  loading.value = true
  try {
    const page = await getOperLogs({
      operator: operatorKeyword.value.trim() || undefined,
      logType: logTypeKeyword.value.trim() || undefined,
      action: actionKeyword.value.trim() || undefined,
      success: successFilter.value,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    logs.value = page.records
    total.value = page.total
    currentPage.value = page.pageNum
    pageSize.value = page.pageSize
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

const openDetail = (record: OperLogItem) => {
  currentLog.value = record
  detailVisible.value = true
}

const handleSearch = () => {
  currentPage.value = 1
  loadData()
}

const handleReset = () => {
  operatorKeyword.value = ''
  logTypeKeyword.value = ''
  actionKeyword.value = ''
  successFilter.value = undefined
  currentPage.value = 1
  loadData()
}

const handleExport = async () => {
  exporting.value = true
  try {
    const record = await exportOperLogs({
      operator: operatorKeyword.value.trim() || undefined,
      logType: logTypeKeyword.value.trim() || undefined,
      action: actionKeyword.value.trim() || undefined,
      success: successFilter.value
    })
    Message.success(`导出任务已创建，记录 ID：${record.id}`)
    await router.push('/layout/system/downloads')
  } finally {
    exporting.value = false
  }
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

.action-cell {
  color: #1d2129;
  line-height: 1.5;
}
</style>
