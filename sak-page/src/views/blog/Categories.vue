<template>
  <div class="blog-categories-page">
    <a-card title="分类管理">
      <template #extra>
        <a-space>
          <a-button :loading="loading" @click="loadData">刷新</a-button>
          <a-button v-if="authStore.hasPermission('blog:category:add')" type="primary" @click="openCreate">
            <template #icon><font-awesome-icon icon="fa-solid fa-plus" /></template>
            新增分类
          </a-button>
        </a-space>
      </template>

      <a-space class="toolbar" wrap>
        <a-input v-model="keyword" allow-clear placeholder="搜索名称/别名/描述" style="width: 280px" />
        <a-select v-model="statusFilter" allow-clear placeholder="筛选状态" style="width: 160px">
          <a-option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</a-option>
        </a-select>
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
        <template #coverImage="{ record }">
          <img v-if="record.coverImage" :src="record.coverImage" alt="" class="cover-thumb" />
          <span v-else>-</span>
        </template>
        <template #status="{ record }">
          <a-tag :color="statusColorMap[record.status] || 'gray'">
            {{ statusLabelMap[record.status] || record.status }}
          </a-tag>
        </template>
        <template #operations="{ record }">
          <a-space v-if="authStore.hasAnyPermission(['blog:category:edit', 'blog:category:remove'])">
            <a-button v-if="authStore.hasPermission('blog:category:edit')" type="text" size="small" @click="openEdit(record)">编辑</a-button>
            <a-popconfirm v-if="authStore.hasPermission('blog:category:remove')" content="确认删除该分类？" @ok="handleDelete(record.id)">
              <a-button type="text" size="small" status="danger">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:visible="modalVisible" :title="editingId ? '编辑分类' : '新增分类'" @ok="handleSubmit" @cancel="resetForm">
      <a-form :model="form" layout="vertical">
        <a-form-item field="categoryName" label="分类名称" :rules="[{ required: true, message: '请输入分类名称' }]">
          <a-input v-model="form.categoryName" placeholder="请输入分类名称" />
        </a-form-item>
        <a-form-item field="slug" label="分类别名" :rules="[{ required: true, message: '请输入分类别名' }]">
          <a-input v-model="form.slug" placeholder="用于URL，建议使用英文或拼音" />
        </a-form-item>
        <a-form-item field="description" label="分类描述">
          <a-textarea v-model="form.description" placeholder="可选" />
        </a-form-item>
        <a-form-item field="coverImage" label="封面图片">
          <a-input v-model="form.coverImage" placeholder="图片URL，可选" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import { useAuthStore } from '@/stores/auth'
import {
  createBlogCategory,
  deleteBlogCategory,
  getBlogCategories,
  updateBlogCategory,
  type BlogCategoryItem,
  type BlogCategorySavePayload
} from '@/api/blog'

const authStore = useAuthStore()
const loading = ref(false)
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const records = ref<BlogCategoryItem[]>([])
const keyword = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusOptions = [
  { value: '0', label: '正常' },
  { value: '1', label: '停用' }
]

const statusLabelMap: Record<string, string> = { '0': '正常', '1': '停用' }
const statusColorMap: Record<string, string> = { '0': 'green', '1': 'orange' }

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '分类名称', dataIndex: 'categoryName', width: 160 },
  { title: '别名', dataIndex: 'slug', width: 160 },
  { title: '封面', slotName: 'coverImage', width: 100 },
  { title: '排序', dataIndex: 'orderNum', width: 90 },
  { title: '状态', slotName: 'status', width: 100 },
  { title: '备注', dataIndex: 'remark', ellipsis: true, tooltip: true },
  { title: '操作', slotName: 'operations', width: 160 }
]

const createDefaultForm = () => ({
  categoryName: '',
  slug: '',
  description: '',
  coverImage: '',
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
    const page = await getBlogCategories({
      keyword: keyword.value || undefined,
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

const handleSearch = () => {
  currentPage.value = 1
  loadData()
}

const handleReset = () => {
  keyword.value = ''
  statusFilter.value = ''
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

const openCreate = () => {
  resetForm()
  modalVisible.value = true
}

const openEdit = (record: BlogCategoryItem) => {
  editingId.value = record.id
  Object.assign(form, {
    categoryName: record.categoryName,
    slug: record.slug,
    description: record.description || '',
    coverImage: record.coverImage || '',
    orderNum: record.orderNum ?? 0,
    status: record.status,
    remark: record.remark || ''
  })
  modalVisible.value = true
}

const buildPayload = (): BlogCategorySavePayload => ({
  categoryName: form.categoryName.trim(),
  slug: form.slug.trim(),
  description: form.description.trim() || undefined,
  coverImage: form.coverImage.trim() || undefined,
  orderNum: form.orderNum ?? 0,
  status: form.status,
  remark: form.remark.trim() || undefined
})

const handleSubmit = async () => {
  if (!form.categoryName.trim() || !form.slug.trim()) {
    Message.warning('请填写分类名称和别名')
    return
  }

  const payload = buildPayload()
  if (editingId.value) {
    await updateBlogCategory(editingId.value, payload)
    Message.success('分类更新成功')
  } else {
    await createBlogCategory(payload)
    Message.success('分类创建成功')
  }

  modalVisible.value = false
  resetForm()
  await loadData()
}

const handleDelete = async (id: number) => {
  await deleteBlogCategory(id)
  Message.success('分类删除成功')
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
.blog-categories-page {
  min-height: 100%;
}

.toolbar {
  margin-bottom: 16px;
}

.cover-thumb {
  border-radius: 6px;
  height: 40px;
  object-fit: cover;
  width: 60px;
}
</style>
