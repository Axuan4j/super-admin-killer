<template>
  <div class="blog-editor-page">
    <a-card
      class="editor-shell"
      :bordered="false"
      :body-style="{ padding: 0 }"
      :header-style="{ minHeight: '92px', padding: '20px 24px' }"
    >
      <template #title>
        <div class="editor-title-wrap">
          <div class="editor-header">
            <div>
              <div class="page-title">{{ postId ? '编辑文章' : '写文章' }}</div>
              <div class="page-subtitle">
                <span>{{ statusLabelMap[form.status] || '草稿' }}</span>
                <span>{{ dirty ? '有未保存变更' : '内容已同步' }}</span>
                <span>{{ lastSavedText }}</span>
              </div>
            </div>
            <a-space wrap>
              <a-button @click="goBack">返回列表</a-button>
              <a-button :loading="saving" @click="saveDraft(false)">{{ postId ? '保存修改' : '保存草稿' }}</a-button>
              <a-button
                v-if="postId && form.status === 'PUBLISHED' && authStore.hasPermission('blog:post:publish')"
                :loading="offlining"
                status="warning"
                @click="handleOffline"
              >
                下线
              </a-button>
              <a-button
                v-if="authStore.hasPermission('blog:post:publish')"
                type="primary"
                :loading="publishing"
                @click="handlePublish"
              >
                {{ form.status === 'PUBLISHED' ? '重新发布' : '发布文章' }}
              </a-button>
            </a-space>
          </div>
        </div>
      </template>

      <div class="editor-layout">
        <section class="main-panel">
          <a-input v-model="form.title" class="title-input" placeholder="输入文章标题" allow-clear />
          <a-input v-model="form.slug" class="slug-input" placeholder="文章别名，可留空自动生成" allow-clear />

          <div class="editor-toolbar">
            <span class="toolbar-tip">支持工具栏、实时预览、粘贴图片和多图上传，正文图片会自动走文件中心。</span>
          </div>

          <MdEditor
            id="blog-post-editor"
            v-model="form.contentMarkdown"
            class="md-editor"
            language="zh-CN"
            previewTheme="github"
            codeTheme="github"
            :toolbars-exclude="['github']"
            @on-upload-img="handleEditorUpload"
            @on-save="handleEditorSave"
          />
        </section>

        <aside class="side-panel">
          <a-card title="发布设置" size="small" class="publish-settings-card">
            <a-form layout="vertical">
              <a-form-item label="摘要">
                <a-textarea v-model="form.summary" :auto-size="{ minRows: 3, maxRows: 5 }" show-word-limit :max-length="1000" />
              </a-form-item>
              <a-form-item label="封面图">
                <div class="cover-upload-box">
                  <img v-if="form.coverImage" :src="form.coverImage" alt="" class="cover-preview" />
                  <div v-else class="cover-placeholder">暂无封面</div>
                  <a-space>
                    <a-button size="small" :loading="coverUploading" @click="openCoverPicker">上传封面</a-button>
                    <a-button v-if="form.coverImage" size="small" status="danger" @click="form.coverImage = ''">移除</a-button>
                  </a-space>
                </div>
              </a-form-item>
              <a-form-item label="分类">
                <a-select v-model="form.categoryId" allow-clear placeholder="选择分类">
                  <a-option v-for="item in categoryOptions" :key="item.id" :value="item.id">{{ item.name }}</a-option>
                </a-select>
              </a-form-item>
              <a-form-item label="标签">
                <a-select v-model="form.tagIds" multiple allow-clear placeholder="选择标签">
                  <a-option v-for="item in tagOptions" :key="item.id" :value="item.id">{{ item.name }}</a-option>
                </a-select>
              </a-form-item>
              <a-form-item label="文章来源">
                <a-select v-model="form.sourceType">
                  <a-option value="ORIGINAL">原创</a-option>
                  <a-option value="REPOST">转载</a-option>
                  <a-option value="TRANSLATION">翻译</a-option>
                </a-select>
              </a-form-item>
              <a-form-item v-if="form.sourceType !== 'ORIGINAL'" label="原文地址">
                <a-input v-model="form.sourceUrl" placeholder="https://example.com/original-post" />
              </a-form-item>
            </a-form>

            <div class="switch-list">
              <div class="switch-item">
                <span>允许评论</span>
                <a-switch :model-value="form.allowComment === 1" @change="val => form.allowComment = val ? 1 : 0" />
              </div>
              <div class="switch-item">
                <span>置顶文章</span>
                <a-switch :model-value="form.isTop === 1" @change="val => form.isTop = val ? 1 : 0" />
              </div>
              <div class="switch-item">
                <span>推荐文章</span>
                <a-switch :model-value="form.isRecommend === 1" @change="val => form.isRecommend = val ? 1 : 0" />
              </div>
            </div>

            <a-form layout="vertical">
              <a-form-item label="SEO 标题">
                <a-input v-model="form.seoTitle" allow-clear />
              </a-form-item>
              <a-form-item label="SEO 关键词">
                <a-input v-model="form.seoKeywords" allow-clear placeholder="多个关键词可用逗号分隔" />
              </a-form-item>
              <a-form-item label="SEO 描述">
                <a-textarea v-model="form.seoDescription" :auto-size="{ minRows: 3, maxRows: 5 }" show-word-limit :max-length="500" />
              </a-form-item>
            </a-form>
          </a-card>
        </aside>
      </div>
    </a-card>

    <input ref="coverInputRef" class="hidden-input" type="file" accept="image/*" @change="handleCoverSelected" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import { useRoute, useRouter } from 'vue-router'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { createBlogPost, getBlogCategoryOptions, getBlogPostDetail, getBlogTagOptions, offlineBlogPost, publishBlogPost, updateBlogPost, type BlogOptionItem, type BlogPostSavePayload } from '@/api/blog'
import { uploadCenterFile } from '@/api/fileCenter'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const createDefaultForm = () => ({
  title: '',
  slug: '',
  summary: '',
  coverImage: '',
  categoryId: undefined as number | undefined,
  tagIds: [] as number[],
  contentMarkdown: '',
  seoTitle: '',
  seoKeywords: '',
  seoDescription: '',
  sourceType: 'ORIGINAL',
  sourceUrl: '',
  allowComment: 1,
  isTop: 0,
  isRecommend: 0,
  status: 'DRAFT'
})

const form = reactive(createDefaultForm())
const postId = ref<number | null>(null)
const saving = ref(false)
const publishing = ref(false)
const offlining = ref(false)
const coverUploading = ref(false)
const categoryOptions = ref<BlogOptionItem[]>([])
const tagOptions = ref<BlogOptionItem[]>([])
const lastSavedAt = ref('')
const dirty = ref(false)
const hydrating = ref(true)
const coverInputRef = ref<HTMLInputElement | null>(null)

let autoSaveTimer: ReturnType<typeof setTimeout> | null = null
let lastSnapshot = ''

const statusLabelMap: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下线' }
const lastSavedText = computed(() => (lastSavedAt.value ? `上次保存：${lastSavedAt.value}` : '尚未保存'))

const formatDateTime = (value?: string) => (value ? value.replace('T', ' ').slice(0, 19) : '')
const currentSnapshot = () => JSON.stringify({ ...form, tagIds: [...form.tagIds].sort((a, b) => a - b) })
const syncDirtyState = () => { dirty.value = currentSnapshot() !== lastSnapshot }
const updateSnapshot = () => { lastSnapshot = currentSnapshot(); dirty.value = false }

const buildPayload = (): BlogPostSavePayload => ({
  title: form.title.trim(),
  slug: form.slug.trim() || undefined,
  summary: form.summary.trim() || undefined,
  coverImage: form.coverImage || undefined,
  categoryId: form.categoryId,
  tagIds: form.tagIds.length ? [...form.tagIds] : undefined,
  contentMarkdown: form.contentMarkdown,
  seoTitle: form.seoTitle.trim() || undefined,
  seoKeywords: form.seoKeywords.trim() || undefined,
  seoDescription: form.seoDescription.trim() || undefined,
  sourceType: form.sourceType,
  sourceUrl: form.sourceUrl.trim() || undefined,
  allowComment: form.allowComment,
  isTop: form.isTop,
  isRecommend: form.isRecommend
})

const loadMeta = async () => {
  const [categories, tags] = await Promise.all([getBlogCategoryOptions(), getBlogTagOptions()])
  categoryOptions.value = categories
  tagOptions.value = tags
}

const applyDetail = async (id: number) => {
  const detail = await getBlogPostDetail(id)
  Object.assign(form, {
    title: detail.title || '',
    slug: detail.slug || '',
    summary: detail.summary || '',
    coverImage: detail.coverImage || '',
    categoryId: detail.categoryId,
    tagIds: detail.tagIds || [],
    contentMarkdown: detail.contentMarkdown || '',
    seoTitle: detail.seoTitle || '',
    seoKeywords: detail.seoKeywords || '',
    seoDescription: detail.seoDescription || '',
    sourceType: detail.sourceType || 'ORIGINAL',
    sourceUrl: detail.sourceUrl || '',
    allowComment: detail.allowComment ?? 1,
    isTop: detail.isTop ?? 0,
    isRecommend: detail.isRecommend ?? 0,
    status: detail.status || 'DRAFT'
  })
  lastSavedAt.value = formatDateTime(detail.updateTime || detail.createTime)
  updateSnapshot()
}

const initPage = async () => {
  hydrating.value = true
  await loadMeta()
  const id = Number(route.query.id)
  if (Number.isFinite(id) && id > 0) {
    postId.value = id
    await applyDetail(id)
  } else {
    postId.value = null
    Object.assign(form, createDefaultForm())
    lastSavedAt.value = ''
    updateSnapshot()
  }
  hydrating.value = false
}

const saveDraft = async (autoSave: boolean) => {
  if (!form.title.trim() || !form.contentMarkdown.trim()) {
    if (!autoSave) Message.warning('请先填写标题和正文')
    return
  }
  if (autoSave && !dirty.value) return
  saving.value = !autoSave
  try {
    const payload = buildPayload()
    const detail = postId.value ? await updateBlogPost(postId.value, payload) : await createBlogPost(payload)
    form.status = detail.status
    postId.value = detail.id
    lastSavedAt.value = formatDateTime(detail.updateTime || detail.createTime) || formatDateTime(new Date().toISOString())
    if (!route.query.id || Number(route.query.id) !== detail.id) {
      await router.replace({ path: '/layout/blog/editor', query: { id: String(detail.id) } })
    }
    updateSnapshot()
    if (!autoSave) Message.success(detail.status === 'DRAFT' ? '草稿已保存' : '文章已保存')
  } finally {
    saving.value = false
  }
}

const handlePublish = async () => {
  if (!form.title.trim() || !form.contentMarkdown.trim()) {
    Message.warning('请先填写标题和正文')
    return
  }
  if (!postId.value || dirty.value) {
    await saveDraft(false)
  }
  if (!postId.value) return
  publishing.value = true
  try {
    const detail = await publishBlogPost(postId.value)
    form.status = detail.status
    lastSavedAt.value = formatDateTime(detail.updateTime || detail.publishTime)
    updateSnapshot()
    Message.success('文章已发布')
  } finally {
    publishing.value = false
  }
}

const handleOffline = async () => {
  if (!postId.value) return
  offlining.value = true
  try {
    const detail = await offlineBlogPost(postId.value)
    form.status = detail.status
    lastSavedAt.value = formatDateTime(detail.updateTime)
    updateSnapshot()
    Message.success('文章已下线')
  } finally {
    offlining.value = false
  }
}

const goBack = () => {
  router.push('/layout/blog/posts')
}

const openCoverPicker = () => coverInputRef.value?.click()

const resolveFileUrl = (downloadUrl?: string, filePath?: string) => {
  const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  const path = downloadUrl || filePath || ''
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  return `${baseURL}${path.startsWith('/') ? path : `/${path}`}`
}

const handleCoverSelected = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  coverUploading.value = true
  try {
    const record = await uploadCenterFile({ file, bizType: 'BLOG_POST_COVER', remark: form.title.trim() || '博客封面图' })
    form.coverImage = resolveFileUrl(record.downloadUrl, record.filePath)
    syncDirtyState()
    Message.success('封面上传成功')
  } finally {
    coverUploading.value = false
    target.value = ''
  }
}

const handleEditorUpload = async (files: File[], callback: (urls: string[]) => void) => {
  const uploaded = await Promise.all(
    files.map(file =>
      uploadCenterFile({
        file,
        bizType: 'BLOG_POST_IMAGE',
        remark: form.title.trim() || '博客正文图片'
      })
    )
  )
  callback(uploaded.map(item => resolveFileUrl(item.downloadUrl, item.filePath)))
  syncDirtyState()
  Message.success(`已上传 ${uploaded.length} 张图片`)
}

const handleEditorSave = async () => {
  await saveDraft(false)
}

const scheduleAutoSave = () => {
  if (hydrating.value) return
  syncDirtyState()
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  autoSaveTimer = setTimeout(() => { saveDraft(true) }, 15000)
}

const handleBeforeUnload = (event: BeforeUnloadEvent) => {
  if (!dirty.value) return
  event.preventDefault()
  event.returnValue = ''
}

watch(() => ({ ...form, tagIds: [...form.tagIds] }), scheduleAutoSave, { deep: true })
watch(() => route.query.id, async () => { await initPage() })

onMounted(async () => {
  await initPage()
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
})
</script>

<style scoped>
.blog-editor-page { min-height: 100%; }
.editor-title-wrap {
  width: 100%;
}

.editor-header {
  align-items: flex-start;
  display: flex;
  gap: 16px;
  justify-content: space-between;
  min-width: 0;
  width: 100%;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.35;
  padding-right: 12px;
}

.page-subtitle {
  color: var(--color-text-3);
  display: flex;
  flex-wrap: wrap;
  font-size: 12px;
  gap: 12px;
  line-height: 1.6;
  margin-top: 8px;
}
.editor-layout { align-items: start; display: grid; gap: 20px; grid-template-columns: minmax(0, 1fr) 360px; padding: 20px; }
.main-panel { min-width: 0; }
.title-input :deep(.arco-input) { font-size: 28px; font-weight: 700; }
.slug-input { margin-top: 12px; }
.editor-toolbar { align-items: center; display: flex; flex-wrap: wrap; gap: 12px; justify-content: flex-end; margin: 16px 0 12px; }
.toolbar-tip { color: var(--color-text-3); font-size: 12px; }
.md-editor {
  border-radius: 20px;
  height: 60vh;
  overflow: hidden;
}
.side-panel { display: flex; flex-direction: column; gap: 16px; }
.publish-settings-card :deep(.arco-card-body) {
  max-height: 65vh;
  overflow-y: auto;
  padding-right: 12px;
}

.publish-settings-card :deep(.arco-card-body)::-webkit-scrollbar {
  width: 6px;
}

.publish-settings-card :deep(.arco-card-body)::-webkit-scrollbar-thumb {
  background: var(--color-fill-4);
  border-radius: 999px;
}
.side-panel :deep(.arco-card-body) { padding: 16px 18px 18px; }
.side-panel :deep(.arco-form-item-label-col) { padding-left: 2px; padding-right: 2px; }
.side-panel :deep(.arco-form-item) { margin-bottom: 18px; }
.cover-upload-box { display: flex; flex-direction: column; gap: 12px; width: 100% }
.cover-preview, .cover-placeholder { background: linear-gradient(135deg, #f5f7fa, #eef2ff); border: 1px dashed var(--color-border-2); border-radius: 16px; height: 180px; object-fit: cover; width: 100%; }
.cover-placeholder { align-items: center; color: var(--color-text-3); display: flex; justify-content: center; }
.switch-list { display: flex; flex-direction: column; gap: 14px; }
.switch-item { align-items: center; display: flex; justify-content: space-between; }
.hidden-input { display: none; }
@media (max-width: 1440px) {
  .editor-layout { grid-template-columns: minmax(0, 1fr); }
  .side-panel {
    display: grid;
    gap: 16px;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .publish-settings-card :deep(.arco-card-body) {
    max-height: none;
    overflow: visible;
    padding-right: 18px;
  }
}

@media (max-width: 960px) {
  .editor-header {
    flex-direction: column;
  }

  .page-title {
    padding-right: 0;
  }
}
</style>
