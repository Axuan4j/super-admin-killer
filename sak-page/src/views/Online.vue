<template>
  <div class="page-container">
    <a-card title="在线用户">
      <template #extra>
        <a-space>
          <a-tag color="arcoblue">在线会话 {{ filteredData.length }}</a-tag>
          <a-button :loading="loading" @click="loadData">刷新</a-button>
        </a-space>
      </template>

      <a-space class="toolbar">
        <a-input
          v-model="keyword"
          allow-clear
          placeholder="搜索用户名/昵称/IP"
          style="width: 280px"
        />
      </a-space>

      <a-table
        :columns="columns"
        :data="filteredData"
        :loading="loading"
        :pagination="false"
        row-key="sessionId"
        :row-class="getRowClass"
      >
        <template #userInfo="{ record }">
          <a-space direction="vertical" :size="2">
            <a-space :size="6">
              <span>{{ record.nickName || record.username }}</span>
              <a-tag v-if="record.current" color="arcoblue" size="small">当前设备</a-tag>
            </a-space>
            <span class="sub-text">{{ record.username }}</span>
          </a-space>
        </template>
        <template #current="{ record }">
          <a-tag :color="record.current ? 'arcoblue' : 'gray'" bordered>
            {{ record.current ? '当前会话' : '其他会话' }}
          </a-tag>
        </template>
        <template #userAgent="{ record }">
          <span class="user-agent">{{ record.userAgent || '-' }}</span>
        </template>
        <template #time="{ record }">
          <a-space direction="vertical" :size="2">
            <span>登录：{{ formatDateTime(record.loginTime) }}</span>
            <span class="sub-text">活跃：{{ formatDateTime(record.lastActiveTime) }}</span>
          </a-space>
        </template>
        <template #operations="{ record }">
          <a-button
            v-if="authStore.hasPermission('system:online:forceLogout')"
            type="text"
            size="small"
            status="danger"
            :disabled="record.current"
            @click="handleForceLogout(record)"
          >
            强制下线
          </a-button>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import { useAuthStore } from '@/stores/auth'
import { forceLogoutOnlineUser, getOnlineUsers, type OnlineSessionItem } from '@/api/adminOnline'

const loading = ref(false)
const authStore = useAuthStore()
const data = ref<OnlineSessionItem[]>([])
const keyword = ref('')

const columns = [
  { title: '会话标识', dataIndex: 'sessionId', width: 200, ellipsis: true, tooltip: true },
  { title: '用户', slotName: 'userInfo', width: 180 },
  { title: '状态', slotName: 'current', width: 120 },
  { title: 'IP地址', dataIndex: 'ip', width: 160 },
  { title: '客户端', slotName: 'userAgent' },
  { title: '时间', slotName: 'time', width: 220 },
  { title: '操作', slotName: 'operations', width: 120 }
]

const formatDateTime = (value?: string) => {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

const filteredData = computed(() => {
  const searchText = keyword.value.trim().toLowerCase()
  if (!searchText) {
    return data.value
  }

  return data.value.filter(record =>
    [record.username, record.nickName, record.ip]
      .filter(Boolean)
      .some(value => String(value).toLowerCase().includes(searchText))
  )
})

const getRowClass = (record: OnlineSessionItem) => {
  return record.current ? 'current-session-row' : ''
}

const loadData = async () => {
  loading.value = true
  try {
    data.value = await getOnlineUsers()
  } finally {
    loading.value = false
  }
}

const handleForceLogout = (record: OnlineSessionItem) => {
  Modal.warning({
    title: '确认强制下线',
    content: `确定将 ${record.nickName || record.username} 的该会话强制下线吗？`,
    hideCancel: false,
    onOk: async () => {
      await forceLogoutOnlineUser(record.sessionId)
      Message.success(`已将 ${record.nickName || record.username} 强制下线`)
      await loadData()
    }
  })
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

.user-agent {
  color: var(--app-muted-text, #4e5969);
  word-break: break-all;
}

:deep(.arco-table-th) {
  background: var(--app-panel-bg, #ffffff);
  color: var(--app-header-text, #1d2129);
  border-bottom-color: var(--app-border-color, #e5e6eb);
}

:deep(.arco-table-td) {
  background: var(--app-panel-bg, #ffffff);
  color: var(--app-header-text, #1d2129);
  border-bottom-color: var(--app-border-color, #e5e6eb);
}

:deep(.arco-table-tr:hover .arco-table-td) {
  background: rgba(var(--primary-6), 0.08);
}

:deep(.current-session-row .arco-table-td) {
  background: rgba(var(--primary-6), 0.12);
}

:deep(.current-session-row:hover .arco-table-td) {
  background: rgba(var(--primary-6), 0.18);
}

:deep(.arco-table-container::before) {
  background: var(--app-border-color, #e5e6eb);
}
</style>
