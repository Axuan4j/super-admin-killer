<template>
  <div class="users-page">
    <a-card title="用户管理">
      <template #extra>
        <a-space>
          <a-button @click="loadData" :loading="loading">刷新</a-button>
          <a-button v-if="authStore.hasPermission('system:user:add')" type="primary" @click="openCreate">
            <template #icon><font-awesome-icon icon="fa-solid fa-plus" /></template>
            新增用户
          </a-button>
        </a-space>
      </template>

      <a-space class="toolbar">
        <a-input v-model="keyword" allow-clear placeholder="搜索用户名/昵称/邮箱" style="width: 280px" />
        <a-select v-model="statusFilter" allow-clear placeholder="筛选状态" style="width: 160px">
          <a-option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</a-option>
        </a-select>
      </a-space>

      <a-table
        :columns="columns"
        :data="users"
        :loading="loading"
        :pagination="{
          total,
          current: currentPage,
          pageSize,
          showTotal: true,
          showPageSize: true
        }"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
        row-key="id"
      >
        <template #status="{ record }">
          <a-tag :color="dictStore.getDictTagColor('sys_normal_disable', record.status)">
            {{ dictStore.getDictLabel('sys_normal_disable', record.status) }}
          </a-tag>
        </template>
        <template #roles="{ record }">
          <a-space wrap>
            <a-tag v-for="roleName in record.roleNames" :key="roleName" color="arcoblue">{{ roleName }}</a-tag>
            <span v-if="record.roleNames.length === 0">-</span>
          </a-space>
        </template>
        <template #operations="{ record }">
          <a-space v-if="authStore.hasAnyPermission(['system:user:edit', 'system:user:remove'])">
            <a-button v-if="authStore.hasPermission('system:user:edit')" type="text" size="small" @click="openEdit(record)">编辑</a-button>
            <a-popconfirm v-if="authStore.hasPermission('system:user:remove')" content="确认删除该用户？" @ok="handleDelete(record.id)">
              <a-button type="text" size="small" status="danger">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:visible="modalVisible" :title="editingId ? '编辑用户' : '新增用户'" @ok="handleSubmit" @cancel="resetForm">
      <a-form :model="form" layout="vertical">
        <a-form-item field="username" label="用户名">
          <a-input v-model="form.username" :disabled="!!editingId" />
        </a-form-item>
        <a-form-item field="password" :label="editingId ? '密码（留空则不修改）' : '密码'">
          <a-input-password v-model="form.password" />
        </a-form-item>
        <a-form-item field="nickName" label="昵称">
          <a-input v-model="form.nickName" />
        </a-form-item>
        <a-form-item field="email" label="邮箱">
          <a-input v-model="form.email" />
        </a-form-item>
        <a-form-item field="wxPusherUid" label="WxPusher UID">
          <a-input v-model="form.wxPusherUid" placeholder="用于 WxPusher 单用户推送" />
        </a-form-item>
        <a-form-item field="phone" label="手机号">
          <a-input v-model="form.phone" />
        </a-form-item>
        <a-form-item field="roleIds" label="角色">
          <a-select v-model="form.roleIds" multiple allow-clear>
            <a-option v-for="role in roleOptions" :key="role.id" :value="role.id">{{ role.roleName }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="status" label="状态">
          <a-radio-group v-model="form.status">
            <a-radio v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item field="remark" label="备注">
          <a-textarea v-model="form.remark" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import { useAuthStore } from '@/stores/auth.ts'
import { useDictStore } from '@/stores/dict.ts'
import { createUser, deleteUser, getUserRoleOptions, getUsers, updateUser, type RoleOption, type UserItem } from '@/api/adminUser.ts'
import { refreshPermissionContext } from '@/utils/permissionSync.ts'

const authStore = useAuthStore()
const dictStore = useDictStore()
const loading = ref(false)
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const users = ref<UserItem[]>([])
const roleOptions = ref<RoleOption[]>([])
const keyword = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
let searchTimer: ReturnType<typeof setTimeout> | null = null
const statusOptions = computed(() => dictStore.getDictItems('sys_normal_disable'))

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '用户名', dataIndex: 'username' },
  { title: '昵称', dataIndex: 'nickName' },
  { title: '邮箱', dataIndex: 'email' },
  { title: 'WxPusher UID', dataIndex: 'wxPusherUid', ellipsis: true, tooltip: true },
  { title: '角色', slotName: 'roles' },
  { title: '状态', slotName: 'status', width: 100 },
  { title: '操作', slotName: 'operations', width: 160 }
]

const createDefaultForm = () => ({
  username: '',
  password: '',
  nickName: '',
  email: '',
  wxPusherUid: '',
  phone: '',
  status: '0',
  remark: '',
  roleIds: [] as number[]
})

const form = reactive(createDefaultForm())

const resetForm = () => {
  Object.assign(form, createDefaultForm())
  editingId.value = null
}

const loadData = async () => {
  loading.value = true
  try {
    const [userPage, roleData] = await Promise.all([
      getUsers({
        keyword: keyword.value || undefined,
        status: statusFilter.value || undefined,
        pageNum: currentPage.value,
        pageSize: pageSize.value
      }),
      getUserRoleOptions()
    ])
    users.value = userPage.records
    total.value = userPage.total
    currentPage.value = userPage.pageNum
    pageSize.value = userPage.pageSize
    roleOptions.value = roleData
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

const openCreate = () => {
  resetForm()
  modalVisible.value = true
}

const openEdit = (record: UserItem) => {
  editingId.value = record.id
  Object.assign(form, {
    username: record.username,
    password: '',
    nickName: record.nickName,
    email: record.email || '',
    wxPusherUid: record.wxPusherUid || '',
    phone: record.phone || '',
    status: record.status,
    remark: record.remark || '',
    roleIds: [...record.roleIds]
  })
  modalVisible.value = true
}

const handleSubmit = async () => {
  if (!form.username || !form.nickName) {
    Message.warning('请填写用户名和昵称')
    return
  }
  if (!/^[a-zA-Z0-9_]{3,20}$/.test(form.username)) {
    Message.warning('用户名需为 3-20 位字母、数字或下划线')
    return
  }
  if (!editingId.value && !form.password) {
    Message.warning('新增用户时请填写密码')
    return
  }
  if (form.password && form.password.length < 6) {
    Message.warning('密码至少 6 位')
    return
  }
  if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    Message.warning('邮箱格式不正确')
    return
  }

  const payload = {
    username: form.username,
    password: form.password || undefined,
    nickName: form.nickName,
    email: form.email || undefined,
    wxPusherUid: form.wxPusherUid || undefined,
    phone: form.phone || undefined,
    status: form.status,
    remark: form.remark || undefined,
    roleIds: form.roleIds
  }

  if (editingId.value) {
    await updateUser(editingId.value, payload)
    Message.success('用户更新成功')
    if (editingId.value === authStore.userInfo?.id) {
      await refreshPermissionContext()
      Message.info('当前账号角色已更新，权限已自动刷新')
    }
  } else {
    await createUser(payload)
    Message.success('用户创建成功')
  }

  modalVisible.value = false
  resetForm()
  await loadData()
}

const handleDelete = async (id: number) => {
  await deleteUser(id)
  Message.success('用户删除成功')
  await loadData()
}

onMounted(() => {
  loadData()
})

watch(statusFilter, () => {
  currentPage.value = 1
  loadData()
})

watch(keyword, () => {
  currentPage.value = 1
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  searchTimer = setTimeout(() => {
    loadData()
  }, 400)
})

onUnmounted(() => {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
})
</script>

<style scoped>
.users-page {
  min-height: 100%;
}

.toolbar {
  margin-bottom: 16px;
}
</style>
