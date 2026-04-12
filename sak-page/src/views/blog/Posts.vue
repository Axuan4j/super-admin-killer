<template>
  <div class="blog-posts-page">
    <a-card title="文章管理">
      <template #extra>
        <a-space>
          <a-button :loading="loading" @click="loadData">刷新</a-button>
          <a-button v-if="authStore.hasPermission('blog:post:add')" type="primary" @click="goCreate">
            <template #icon><font-awesome-icon icon="fa-solid fa-plus" /></template>
            写文章
          </a-button>
        </a-space>
      </template>

      <a-space class="toolbar" wrap>
        <a-input v-model="titleKeyword" allow-clear placeholder="搜索标题" style="width: 260px" />
        <a-select v-model="statusFilter" allow-clear placeholder="状态" style="width: 160px">
          <a-option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</a-option>
        </a-select>
        <a-select v-model="categoryFilter" allow-clear placeholder="分类" style="width: 220px">
          <a-option v-for="item in categoryOptions" :key="item.id" :value="item.id">{{ item.name }}</a-option>
        </a-select>
        <a-button type="primary" :loading="loading" @click="handleSearch">查询</a-button>
        <a-button :loading="loading" @click="handleReset">重置</a-button>
      </a-space>

      <a-table
        :columns="columns"
        :data="records"
        :loading="loading"
        :pagination="{ total, current: currentPage, pageSize, showTotal: true, showPageSize: true }"
        row-key="id"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
      >
        <template #titleCell="{ record }">
          <div class="title-cell">
            <img v-if="record.coverImage" :src="record.coverImage" alt="" class="cover-thumb" />
            <div class="title-content">
              <div class="title-text">{{ record.title }}</div>
              <div class="title-meta">
                <span>{{ record.slug }}</span>
                <span v-if="record.wordCount">约 {{ record.wordCount }} 字</span>
                <span v-if="record.readingMinutes">阅读 {{ record.readingMinutes }} 分钟</span>
              </div>
            </div>
          </div>
        </template>
        <template #status="{ record }">
          <a-tag :color="statusColorMap[record.status] || 'gray'">
            {{ statusLabelMap[record.status] || record.status }}
          </a-tag>
        </template>
        <template #category="{ record }">{{ record.categoryName || '-' }}</template>
        <template #publishTime="{ record }">{{ formatDateTime(record.publishTime) }}</template>
        <template #updateTime="{ record }">{{ formatDateTime(record.updateTime) }}</template>
        <template #operations="{ record }">
          <a-space wrap>
            <a-button type="text" size="small" @click="goEdit(record.id)">编辑</a-button>
            <a-button
              v-if="authStore.hasPermission('blog:post:publish') && record.status !== 'PUBLISHED'"
              type="text"
              size="small"
              @click="handlePublish(record.id)"
            >
              发布
            </a-button>
            <a-button
              v-if="authStore.hasPermission('blog:post:publish') && record.status === 'PUBLISHED'"
              type="text"
              size="small"
              @click="handleOffline(record.id)"
            >
              下线
            </a-button>
            <a-popconfirm
              v-if="authStore.hasPermission('blog:post:remove')"
              content="确认删除这篇文章？删除后不可恢复。"
              @ok="handleDelete(record.id)"
            >
              <a-button type="text" size="small" status="danger">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import { useRouter } from 'vue-router'
import { deleteBlogPost, getBlogCategoryOptions, getBlogPosts, offlineBlogPost, publishBlogPost, type BlogOptionItem, type BlogPostListItem } from '@/api/blog'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const records = ref<BlogPostListItem[]>([])
const categoryOptions = ref<BlogOptionItem[]>([])
const titleKeyword = ref('')
const statusFilter = ref('')
const categoryFilter = ref<number | undefined>(undefined)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusOptions = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'OFFLINE', label: '已下线' }
]

const statusLabelMap: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下线' }
const statusColorMap: Record<string, string> = { DRAFT: 'gray', PUBLISHED: 'green', OFFLINE: 'orange' }

const columns = [
  { title: '文章', slotName: 'titleCell', width: 420 },
  { title: '分类', slotName: 'category', width: 140 },
  { title: '状态', slotName: 'status', width: 120 },
  { title: '发布时间', slotName: 'publishTime', width: 180 },
  { title: '更新时间', slotName: 'updateTime', width: 180 },
  { title: '操作', slotName: 'operations', width: 220, fixed: 'right' as const }
]

const formatDateTime = (value?: string) => (value ? value.replace('T', ' ').slice(0, 19) : '-')

const loadCategories = async () => {
  categoryOptions.value = await getBlogCategoryOptions()
}

const loadData = async () => {
  loading.value = true
  try {
    const page = await getBlogPosts({
      title: titleKeyword.value.trim() || undefined,
      status: statusFilter.value || undefined,
      categoryId: categoryFilter.value,
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
  titleKeyword.value = ''
  statusFilter.value = ''
  categoryFilter.value = undefined
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

const goCreate = () => {
  router.push('/layout/blog/editor')
}

const goEdit = (id: number) => {
  router.push({ path: '/layout/blog/editor', query: { id: String(id) } })
}

const handlePublish = async (id: number) => {
  await publishBlogPost(id)
  Message.success('文章已发布')
  await loadData()
}

const handleOffline = async (id: number) => {
  await offlineBlogPost(id)
  Message.success('文章已下线')
  await loadData()
}

const handleDelete = async (id: number) => {
  await deleteBlogPost(id)
  Message.success('文章已删除')
  if (records.value.length === 1 && currentPage.value > 1) {
    currentPage.value -= 1
  }
  await loadData()
}

onMounted(async () => {
  await loadCategories()
  await loadData()
})
</script>

<style scoped>
.blog-posts-page {
  min-height: 100%;
}

.toolbar {
  margin-bottom: 16px;
}

.title-cell {
  align-items: center;
  display: flex;
  gap: 12px;
}

.cover-thumb {
  border-radius: 10px;
  height: 56px;
  object-fit: cover;
  width: 84px;
}

.title-content {
  min-width: 0;
}

.title-text {
  color: var(--color-text-1);
  font-weight: 600;
}

.title-meta {
  color: var(--color-text-3);
  display: flex;
  flex-wrap: wrap;
  font-size: 12px;
  gap: 10px;
  margin-top: 4px;
}
</style>
