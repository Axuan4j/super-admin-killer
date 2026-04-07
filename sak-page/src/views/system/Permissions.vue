<template>
  <div class="page-container">
    <a-card title="权限管理">
      <template #extra>
        <a-space>
          <a-button @click="loadData" :loading="loading">刷新</a-button>
          <a-button type="outline" :loading="refreshingPermission" @click="handleRefreshPermission">刷新权限</a-button>
          <a-button v-if="authStore.hasPermission('system:permission:add')" type="primary" @click="openCreate">
            <template #icon>
              <font-awesome-icon icon="fa-solid fa-plus"/>
            </template>
            新增菜单/权限
          </a-button>
        </a-space>
      </template>
      <a-space class="toolbar">
        <a-input v-model="keyword" allow-clear placeholder="搜索名称/路由/权限标识" style="width: 320px"/>
        <a-select v-model="typeFilter" allow-clear placeholder="筛选类型" style="width: 160px">
          <a-option value="M">目录</a-option>
          <a-option value="C">菜单</a-option>
          <a-option value="F">按钮</a-option>
        </a-select>
      </a-space>
      <a-table
          :columns="columns"
          :data="treeMenus"
          :loading="loading"
          :pagination="false"
          row-key="id"
      >
        <template #menuType="{ record }">
          <a-tag :color="typeColorMap[record.menuType] || 'gray'">{{
              typeLabelMap[record.menuType] || record.menuType
            }}
          </a-tag>
        </template>
        <template #visible="{ record }">
          <a-tag :color="record.visible === '0' ? 'green' : 'red'">
            {{ record.visible === '0' ? '显示' : '隐藏' }}
          </a-tag>
        </template>
        <template #operations="{ record }">
          <a-space v-if="authStore.hasAnyPermission(['system:permission:edit', 'system:permission:remove'])">
            <a-button v-if="authStore.hasPermission('system:permission:edit')" type="text" size="small"
                      @click="openEdit(record)">编辑
            </a-button>
            <a-popconfirm v-if="authStore.hasPermission('system:permission:remove')" content="确认删除该菜单/权限？"
                          @ok="handleDelete(record.id)">
              <a-button type="text" size="small" status="danger">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:visible="modalVisible" :title="editingId ? '编辑菜单/权限' : '新增菜单/权限'" @ok="handleSubmit"
             @cancel="resetForm">
      <a-form :model="form" layout="vertical">
        <a-form-item field="menuName" label="名称">
          <a-input v-model="form.menuName"/>
        </a-form-item>
        <a-form-item field="menuType" label="类型">
          <a-select v-model="form.menuType">
            <a-option value="M">目录</a-option>
            <a-option value="C">菜单</a-option>
            <a-option value="F">按钮权限</a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="parentId" label="父级">
          <a-select v-model="form.parentId" allow-clear>
            <a-option :value="0">顶级</a-option>
            <a-option v-for="item in parentOptions" :key="item.id" :value="item.id">{{ item.menuName }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="path" label="路由路径">
          <a-input v-model="form.path"/>
        </a-form-item>
        <a-form-item field="component" label="组件路径">
          <a-input v-model="form.component"/>
        </a-form-item>
        <a-form-item field="perms" label="权限标识">
          <a-input v-model="form.perms"/>
        </a-form-item>
        <a-form-item field="icon" label="图标">
          <a-input v-model="form.icon"/>
        </a-form-item>
        <a-form-item field="orderNum" label="排序">
          <a-input-number v-model="form.orderNum" :min="0"/>
        </a-form-item>
        <a-form-item field="visible" label="显示状态">
          <a-radio-group v-model="form.visible">
            <a-radio value="0">显示</a-radio>
            <a-radio value="1">隐藏</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item field="remark" label="备注">
          <a-textarea v-model="form.remark"/>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, onUnmounted, reactive, ref, watch} from 'vue'
import {Message} from '@arco-design/web-vue'
import {useAuthStore} from '@/stores/auth.ts'
import {createMenu, deleteMenu, getManageMenus, updateMenu, type MenuManageItem} from '@/api/adminMenu.ts'
import { refreshPermissionCache } from '@/api/adminPermission'
import { refreshPermissionContext } from '@/utils/permissionSync'

const authStore = useAuthStore()
const loading = ref(false)
const refreshingPermission = ref(false)
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const keyword = ref('')
const typeFilter = ref('')
const allMenus = ref<MenuManageItem[]>([])
let searchTimer: ReturnType<typeof setTimeout> | null = null

interface TreeMenuItem extends MenuManageItem {
  children?: TreeMenuItem[]
}

const columns = [
  {title: 'ID', dataIndex: 'id', width: 120},
  {title: '名称', dataIndex: 'menuName'},
  {title: '类型', slotName: 'menuType', width: 120},
  {title: '路由', dataIndex: 'path', ellipsis: true, tooltip: true},
  {title: '权限标识', dataIndex: 'perms', ellipsis: true, tooltip: true},
  {title: '显示', slotName: 'visible', width: 100},
  {title: '操作', slotName: 'operations', width: 160}
]

const typeLabelMap: Record<string, string> = {M: '目录', C: '菜单', F: '按钮'}
const typeColorMap: Record<string, string> = {M: 'arcoblue', C: 'green', F: 'orange'}

const createDefaultForm = () => ({
  menuName: '',
  parentId: 0,
  orderNum: 0,
  path: '',
  component: '',
  menuType: 'C',
  visible: '0',
  perms: '',
  icon: '',
  remark: ''
})

const form = reactive(createDefaultForm())

const parentOptions = computed(() => allMenus.value.filter(item => item.menuType !== 'F' && item.id !== editingId.value))

const normalizedKeyword = computed(() => keyword.value.trim().toLowerCase())

const buildTree = (items: MenuManageItem[]) => {
  const nodeMap = new Map<number, TreeMenuItem>()
  const roots: TreeMenuItem[] = []

  items
      .slice()
      .sort((a, b) => {
        const orderCompare = (a.orderNum ?? 0) - (b.orderNum ?? 0)
        return orderCompare !== 0 ? orderCompare : a.id - b.id
      })
      .forEach(item => {
        nodeMap.set(item.id, {
          ...item
        })
      })

  nodeMap.forEach(node => {
    const parentId = node.parentId ?? 0
    if (!parentId || !nodeMap.has(parentId)) {
      roots.push(node)
      return
    }
    const parent = nodeMap.get(parentId)
    if (!parent) {
      roots.push(node)
      return
    }
    if (!parent.children) {
      parent.children = []
    }
    parent.children.push(node)
  })

  return roots
}

const filterTree = (nodes: TreeMenuItem[]): TreeMenuItem[] => nodes.reduce<TreeMenuItem[]>((acc, node) => {
  const filteredChildren = filterTree(node.children || [])
  const matchesKeyword = !normalizedKeyword.value || [node.menuName, node.path, node.perms]
      .filter(Boolean)
      .some(value => String(value).toLowerCase().includes(normalizedKeyword.value))
  const matchesType = !typeFilter.value || node.menuType === typeFilter.value

  if ((matchesKeyword && matchesType) || filteredChildren.length > 0) {
    acc.push(filteredChildren.length > 0
        ? {
          ...node,
          children: filteredChildren
        }
        : {
          ...node,
          children: undefined
        })
  }

  return acc
}, [])

const treeMenus = computed(() => filterTree(buildTree(allMenus.value)))

const resetForm = () => {
  Object.assign(form, createDefaultForm())
  editingId.value = null
}

const loadData = async () => {
  loading.value = true
  try {
    const menus = await getManageMenus({})
    allMenus.value = menus
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  resetForm()
  modalVisible.value = true
}

const openEdit = (record: MenuManageItem) => {
  editingId.value = record.id
  Object.assign(form, {
    menuName: record.menuName,
    parentId: record.parentId ?? 0,
    orderNum: record.orderNum ?? 0,
    path: record.path || '',
    component: record.component || '',
    menuType: record.menuType,
    visible: record.visible || '0',
    perms: record.perms || '',
    icon: record.icon || '',
    remark: record.remark || ''
  })
  modalVisible.value = true
}

const handleSubmit = async () => {
  if (!form.menuName) {
    Message.warning('请填写名称')
    return
  }
  if ((form.menuType === 'C' || form.menuType === 'M') && !form.path) {
    Message.warning('目录或菜单请填写路由路径')
    return
  }
  if ((form.menuType === 'C' || form.menuType === 'F') && !form.perms) {
    Message.warning('菜单或按钮请填写权限标识')
    return
  }

  const payload = {
    menuName: form.menuName,
    parentId: form.parentId,
    orderNum: form.orderNum,
    path: form.path || undefined,
    component: form.component || undefined,
    menuType: form.menuType,
    visible: form.visible,
    perms: form.perms || undefined,
    icon: form.icon || undefined,
    remark: form.remark || undefined
  }

  if (editingId.value) {
    await updateMenu(editingId.value, payload)
    Message.success('更新成功')
  } else {
    await createMenu(payload)
    Message.success('创建成功')
  }

  modalVisible.value = false
  resetForm()
  await loadData()
}

const handleDelete = async (id: number) => {
  await deleteMenu(id)
  Message.success('删除成功')
  await loadData()
}

const handleRefreshPermission = async () => {
  refreshingPermission.value = true
  try {
    await refreshPermissionCache()
    await refreshPermissionContext()
    await loadData()
    Message.success('权限缓存已刷新')
  } finally {
    refreshingPermission.value = false
  }
}

onMounted(() => {
  loadData()
})

watch(typeFilter, () => {
  // client-side tree filtering
})

watch(keyword, () => {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  searchTimer = setTimeout(() => {
    // client-side tree filtering
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

.page-container :deep(.arco-table-cell) {
  vertical-align: top;
}

.page-container :deep(.arco-table-td:first-child .arco-table-cell) {
  white-space: nowrap;
}
</style>
