<template>
  <div class="page-container">
    <a-card title="登录日志">
      <template #extra>
        <a-space>
          <a-button @click="handleSearch" type="primary" :loading="loading">查询</a-button>
          <a-button @click="handleReset" :loading="loading">重置</a-button>
          <a-button :loading="loading" @click="loadData">刷新</a-button>
        </a-space>
      </template>

      <a-space class="toolbar">
        <a-input v-model="usernameKeyword" allow-clear placeholder="登录账号" style="width: 180px" />
        <a-input v-model="loginIpKeyword" allow-clear placeholder="登录IP" style="width: 180px" />
        <a-select v-model="statusFilter" allow-clear placeholder="登录结果" style="width: 160px">
          <a-option v-for="item in statusOptions" :key="item.value" :value="Number(item.value)">{{ item.label }}</a-option>
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
        <template #status="{ record }">
          <a-tag :color="dictStore.getDictTagColor('sys_login_status', record.status)">
            {{ dictStore.getDictLabel('sys_login_status', record.status) }}
          </a-tag>
        </template>
        <template #loginLocation="{ record }">
          <span>{{ record.loginLocation || '-' }}</span>
        </template>
        <template #deviceType="{ record }">
          <a-tag :color="dictStore.getDictTagColor('sys_device_type', record.deviceType)">
            {{ dictStore.getDictLabel('sys_device_type', record.deviceType) || record.deviceType || '-' }}
          </a-tag>
        </template>
        <template #loginTime="{ record }">
          {{ formatDateTime(record.loginTime) }}
        </template>
        <template #operations="{ record }">
          <a-button type="text" size="small" @click="openDetail(record)">详情</a-button>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:visible="detailVisible" title="登录日志详情" :footer="false" width="760px">
      <a-descriptions v-if="currentLog" :column="2" bordered>
        <a-descriptions-item label="登录账号">{{ currentLog.username || '-' }}</a-descriptions-item>
        <a-descriptions-item label="登录结果">
          {{ dictStore.getDictLabel('sys_login_status', currentLog.status) }}
        </a-descriptions-item>
        <a-descriptions-item label="登录IP">{{ currentLog.loginIp || '-' }}</a-descriptions-item>
        <a-descriptions-item label="登录属地">{{ currentLog.loginLocation || '-' }}</a-descriptions-item>
        <a-descriptions-item label="设备类型">
          {{ dictStore.getDictLabel('sys_device_type', currentLog.deviceType) || currentLog.deviceType || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="浏览器">{{ currentLog.browser || '-' }}</a-descriptions-item>
        <a-descriptions-item label="操作系统">{{ currentLog.os || '-' }}</a-descriptions-item>
        <a-descriptions-item label="登录时间">{{ formatDateTime(currentLog.loginTime) }}</a-descriptions-item>
        <a-descriptions-item label="提示消息">{{ currentLog.message || '-' }}</a-descriptions-item>
        <a-descriptions-item label="User-Agent" :span="2">{{ currentLog.userAgent || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getLoginLogs, type LoginLogItem } from '@/api/loginLog'
import { useDictStore } from '@/stores/dict'

const dictStore = useDictStore()
const loading = ref(false)
const logs = ref<LoginLogItem[]>([])
const usernameKeyword = ref('')
const loginIpKeyword = ref('')
const statusFilter = ref<number | undefined>()
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const currentLog = ref<LoginLogItem | null>(null)
const statusOptions = computed(() => dictStore.getDictItems('sys_login_status'))

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '登录账号', dataIndex: 'username', width: 140 },
  { title: '登录IP', dataIndex: 'loginIp', width: 150 },
  { title: '登录属地', slotName: 'loginLocation' },
  { title: '设备', slotName: 'deviceType', width: 100 },
  { title: '浏览器', dataIndex: 'browser', width: 120 },
  { title: '操作系统', dataIndex: 'os', width: 120 },
  { title: '结果', slotName: 'status', width: 90 },
  { title: '登录时间', slotName: 'loginTime', width: 180 },
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
    const page = await getLoginLogs({
      username: usernameKeyword.value.trim() || undefined,
      loginIp: loginIpKeyword.value.trim() || undefined,
      status: statusFilter.value,
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

const handleSearch = () => {
  currentPage.value = 1
  loadData()
}

const handleReset = () => {
  usernameKeyword.value = ''
  loginIpKeyword.value = ''
  statusFilter.value = undefined
  currentPage.value = 1
  loadData()
}

const openDetail = (record: LoginLogItem) => {
  currentLog.value = record
  detailVisible.value = true
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
</style>
