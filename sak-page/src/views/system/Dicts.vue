<template>
  <div class="dicts-page">
    <a-card title="字典配置">
      <template #extra>
        <a-space>
          <a-button :loading="loading" @click="loadData">刷新</a-button>
          <a-button v-if="authStore.hasPermission('system:dict:add')" type="primary" @click="openCreate">
            <template #icon><font-awesome-icon icon="fa-solid fa-plus" /></template>
            新增字典项
          </a-button>
        </a-space>
      </template>

      <a-space class="toolbar">
        <a-input v-model="keyword" allow-clear placeholder="搜索类型/标签/键值/备注" style="width: 280px" />
        <a-input v-model="dictTypeFilter" allow-clear placeholder="筛选字典类型" style="width: 220px" />
        <a-select v-model="statusFilter" allow-clear placeholder="筛选状态" style="width: 160px">
          <a-option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</a-option>
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
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
        row-key="id"
      >
        <template #status="{ record }">
          <a-tag :color="dictStore.getDictTagColor('sys_normal_disable', record.status)">
            {{ dictStore.getDictLabel('sys_normal_disable', record.status) }}
          </a-tag>
        </template>
        <template #tagPreview="{ record }">
          <a-tag :color="record.tagColor || 'gray'">
            {{ record.dictLabel }}
          </a-tag>
        </template>
        <template #operations="{ record }">
          <a-space v-if="authStore.hasAnyPermission(['system:dict:edit', 'system:dict:remove'])">
            <a-button v-if="authStore.hasPermission('system:dict:edit')" type="text" size="small" @click="openEdit(record)">编辑</a-button>
            <a-popconfirm v-if="authStore.hasPermission('system:dict:remove')" content="确认删除该字典项？" @ok="handleDelete(record.id)">
              <a-button type="text" size="small" status="danger">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:visible="modalVisible" :title="editingId ? '编辑字典项' : '新增字典项'" @ok="handleSubmit" @cancel="resetForm">
      <a-form :model="form" layout="vertical">
        <a-form-item field="dictType" label="字典类型">
          <a-input v-model="form.dictType" placeholder="例如：sys_normal_disable" />
        </a-form-item>
        <a-form-item field="dictLabel" label="字典标签">
          <a-input v-model="form.dictLabel" placeholder="例如：启用" />
        </a-form-item>
        <a-form-item field="dictValue" label="字典键值">
          <a-input v-model="form.dictValue" placeholder="例如：0" />
        </a-form-item>
        <a-form-item field="tagColor" label="标签颜色">
          <a-select v-model="form.tagColor" allow-clear placeholder="选择或留空">
            <a-option v-for="option in tagColorOptions" :key="option" :value="option">{{ option }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="tagType" label="标签类型">
          <a-input v-model="form.tagType" placeholder="可选" />
        </a-form-item>
        <a-form-item field="orderNum" label="排序">
          <a-input-number v-model="form.orderNum" :min="0" />
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
import { useAuthStore } from '@/stores/auth'
import { useDictStore } from '@/stores/dict'
import {
  createDictItem,
  deleteDictItem,
  getDictItems,
  updateDictItem,
  type DictManageItem,
  type DictSavePayload
} from '@/api/dict'

const authStore = useAuthStore()
const dictStore = useDictStore()
const loading = ref(false)
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const records = ref<DictManageItem[]>([])
const keyword = ref('')
const dictTypeFilter = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
let searchTimer: ReturnType<typeof setTimeout> | null = null

const statusOptions = computed(() => dictStore.getDictItems('sys_normal_disable'))
const tagColorOptions = ['green', 'red', 'arcoblue', 'orange', 'gray', 'gold', 'lime', 'pinkpurple', 'magenta', 'cyanorblue']

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '字典类型', dataIndex: 'dictType', width: 180 },
  { title: '标签', dataIndex: 'dictLabel', width: 140 },
  { title: '键值', dataIndex: 'dictValue', width: 140 },
  { title: '预览', slotName: 'tagPreview', width: 120 },
  { title: '排序', dataIndex: 'orderNum', width: 90 },
  { title: '状态', slotName: 'status', width: 100 },
  { title: '备注', dataIndex: 'remark', ellipsis: true, tooltip: true },
  { title: '操作', slotName: 'operations', width: 160 }
]

const createDefaultForm = () => ({
  dictType: '',
  dictLabel: '',
  dictValue: '',
  tagType: '',
  tagColor: '',
  orderNum: 0,
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
    const page = await getDictItems({
      keyword: keyword.value || undefined,
      dictType: dictTypeFilter.value || undefined,
      status: statusFilter.value || undefined,
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

const openEdit = (record: DictManageItem) => {
  editingId.value = record.id
  Object.assign(form, {
    dictType: record.dictType,
    dictLabel: record.dictLabel,
    dictValue: record.dictValue,
    tagType: record.tagType || '',
    tagColor: record.tagColor || '',
    orderNum: record.orderNum ?? 0,
    status: record.status,
    remark: record.remark || ''
  })
  modalVisible.value = true
}

const buildPayload = (): DictSavePayload => ({
  dictType: form.dictType.trim(),
  dictLabel: form.dictLabel.trim(),
  dictValue: form.dictValue.trim(),
  tagType: form.tagType.trim() || undefined,
  tagColor: form.tagColor || undefined,
  orderNum: form.orderNum ?? 0,
  status: form.status,
  remark: form.remark.trim() || undefined
})

const handleSubmit = async () => {
  if (!form.dictType.trim() || !form.dictLabel.trim() || !form.dictValue.trim()) {
    Message.warning('请填写字典类型、标签和键值')
    return
  }

  const payload = buildPayload()
  if (editingId.value) {
    await updateDictItem(editingId.value, payload)
    Message.success('字典项更新成功')
  } else {
    await createDictItem(payload)
    Message.success('字典项创建成功')
  }

  await dictStore.loadDictionaries(true)
  modalVisible.value = false
  resetForm()
  await loadData()
}

const handleDelete = async (id: number) => {
  await deleteDictItem(id)
  await dictStore.loadDictionaries(true)
  Message.success('字典项删除成功')
  await loadData()
}

onMounted(() => {
  loadData()
})

watch(statusFilter, () => {
  currentPage.value = 1
  loadData()
})

watch([keyword, dictTypeFilter], () => {
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
.dicts-page {
  min-height: 100%;
}

.toolbar {
  margin-bottom: 16px;
}
</style>
