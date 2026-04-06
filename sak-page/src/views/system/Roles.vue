<template>
  <div class="page-container">
    <a-card title="角色管理">
      <template #extra>
        <a-space>
          <a-button @click="loadData" :loading="loading">刷新</a-button>
          <a-button v-if="authStore.hasPermission('system:role:add')" type="primary" @click="openCreate">
            <template #icon><font-awesome-icon icon="fa-solid fa-plus" /></template>
            新增角色
          </a-button>
        </a-space>
      </template>
      <a-space class="toolbar">
        <a-input v-model="keyword" allow-clear placeholder="搜索角色名称/标识" style="width: 280px" />
        <a-select v-model="statusFilter" allow-clear placeholder="筛选状态" style="width: 160px">
          <a-option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</a-option>
        </a-select>
      </a-space>
      <a-table
        :columns="columns"
        :data="roles"
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
        <template #operations="{ record }">
          <a-space v-if="authStore.hasAnyPermission(['system:role:edit', 'system:role:remove'])">
            <a-button v-if="authStore.hasPermission('system:role:edit')" type="text" size="small" @click="openEdit(record)">编辑</a-button>
            <a-button v-if="authStore.hasPermission('system:role:edit')" type="text" size="small" @click="openPermission(record)">授权</a-button>
            <a-popconfirm v-if="authStore.hasPermission('system:role:remove')" content="确认删除该角色？" @ok="handleDelete(record.id)">
              <a-button type="text" size="small" status="danger">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:visible="modalVisible" :title="editingId ? '编辑角色' : '新增角色'" @ok="handleSubmit" @cancel="resetForm">
      <a-form :model="form" layout="vertical">
        <a-form-item field="roleName" label="角色名称">
          <a-input v-model="form.roleName" />
        </a-form-item>
        <a-form-item field="roleKey" label="角色标识">
          <a-input v-model="form.roleKey" />
        </a-form-item>
        <a-form-item field="roleSort" label="排序">
          <a-input-number v-model="form.roleSort" :min="0" />
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

    <a-modal v-model:visible="permissionVisible" title="配置角色权限" width="720px" @ok="handlePermissionSubmit" @cancel="resetPermission">
      <a-tree
        v-model:checked-keys="checkedMenuIds"
        checkable
        block-node
        :data="menuTree"
        :field-names="{ key: 'id', title: 'title', children: 'children' }"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import { useAuthStore } from '@/stores/auth.ts'
import { useDictStore } from '@/stores/dict.ts'
import { createRole, deleteRole, getRoleMenuIds, getRoles, updateRole, updateRoleMenuIds, type RoleItem } from '@/api/adminRole.ts'
import { getManageMenus, type MenuManageItem } from '@/api/adminMenu.ts'
import { refreshPermissionContext } from '@/utils/permissionSync.ts'

const authStore = useAuthStore()
const dictStore = useDictStore()
const loading = ref(false)
const roles = ref<(RoleItem & { roleSort?: number; status?: string; remark?: string })[]>([])
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const permissionVisible = ref(false)
const permissionRoleId = ref<number | null>(null)
const checkedMenuIds = ref<number[]>([])
const allMenus = ref<MenuManageItem[]>([])
const keyword = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
let searchTimer: ReturnType<typeof setTimeout> | null = null
const statusOptions = computed(() => dictStore.getDictItems('sys_normal_disable'))

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '角色名称', dataIndex: 'roleName' },
  { title: '角色标识', dataIndex: 'roleKey' },
  { title: '状态', slotName: 'status', width: 100 },
  { title: '操作', slotName: 'operations', width: 220 }
]

const createDefaultForm = () => ({
  roleName: '',
  roleKey: '',
  roleSort: 0,
  status: '0',
  remark: ''
})

const form = reactive(createDefaultForm())

const resetForm = () => {
  Object.assign(form, createDefaultForm())
  editingId.value = null
}

const loadData = async () => {
  loading.value = true
  try {
    const [rolePage, menuPage] = await Promise.all([
      getRoles({
        keyword: keyword.value || undefined,
        status: statusFilter.value || undefined,
        current: currentPage.value,
        size: pageSize.value
      }),
      getManageMenus({ current: 1, size: 500 })
    ])
    roles.value = rolePage.records
    total.value = rolePage.total
    allMenus.value = menuPage.records
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

const openEdit = (record: any) => {
  editingId.value = record.id
  Object.assign(form, {
    roleName: record.roleName,
    roleKey: record.roleKey,
    roleSort: record.roleSort || 0,
    status: record.status || '0',
    remark: record.remark || ''
  })
  modalVisible.value = true
}

const handleSubmit = async () => {
  if (!form.roleName || !form.roleKey) {
    Message.warning('请填写角色名称和角色标识')
    return
  }
  if (!/^[a-zA-Z][a-zA-Z0-9:_-]{1,30}$/.test(form.roleKey)) {
    Message.warning('角色标识格式不正确')
    return
  }

  const payload = {
    roleName: form.roleName,
    roleKey: form.roleKey,
    roleSort: form.roleSort,
    status: form.status,
    remark: form.remark || undefined
  }

  if (editingId.value) {
    await updateRole(editingId.value, payload)
    Message.success('角色更新成功')
  } else {
    await createRole(payload)
    Message.success('角色创建成功')
  }

  modalVisible.value = false
  resetForm()
  await loadData()
}

const handleDelete = async (id: number) => {
  await deleteRole(id)
  Message.success('角色删除成功')
  await loadData()
}

const buildMenuTree = (parentId: number): Array<{ id: number; title: string; children?: any[] }> => {
  return allMenus.value
    .filter(item => (item.parentId ?? 0) === parentId)
    .sort((a, b) => (a.orderNum ?? 0) - (b.orderNum ?? 0) || a.id - b.id)
    .map(item => ({
      id: item.id,
      title: `${item.menuName}${item.perms ? ` (${item.perms})` : ''}`,
      children: buildMenuTree(item.id)
    }))
}

const menuTree = computed(() => buildMenuTree(0))

const resetPermission = () => {
  permissionRoleId.value = null
  checkedMenuIds.value = []
}

const openPermission = async (record: RoleItem) => {
  permissionRoleId.value = record.id
  checkedMenuIds.value = await getRoleMenuIds(record.id)
  permissionVisible.value = true
}

const handlePermissionSubmit = async () => {
  if (!permissionRoleId.value) return
  const currentRole = roles.value.find(role => role.id === permissionRoleId.value)
  await updateRoleMenuIds(permissionRoleId.value, checkedMenuIds.value)
  Message.success('角色权限更新成功')
  if (currentRole && authStore.userInfo?.roles?.includes(currentRole.roleKey)) {
    await refreshPermissionContext()
    Message.info('当前账号所属角色权限已更新，菜单与权限已自动刷新')
  }
  permissionVisible.value = false
  resetPermission()
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
.page-container {
  min-height: 100%;
}

.toolbar {
  margin-bottom: 16px;
}
</style>
