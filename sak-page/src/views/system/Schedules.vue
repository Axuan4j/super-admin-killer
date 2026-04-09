<template>
  <div class="page-container">
    <a-card title="调度中心">
      <template #extra>
        <a-space>
          <a-button :loading="loading" @click="loadData">刷新</a-button>
          <a-button
            v-if="authStore.hasPermission('system:schedule:add')"
            type="primary"
            @click="openCreate"
          >
            <template #icon><font-awesome-icon icon="fa-solid fa-plus" /></template>
            新增任务
          </a-button>
        </a-space>
      </template>

      <a-alert type="info" show-icon class="info-banner">
        调度中心支持固定频率 / 固定延迟 / Cron 三种执行方式；新增任务类型后重启服务会自动同步到这里。
      </a-alert>

      <a-space class="toolbar">
        <a-input v-model="keyword" allow-clear placeholder="搜索任务名称/摘要/备注" style="width: 260px" />
        <a-select v-model="statusFilter" allow-clear placeholder="任务状态" style="width: 160px">
          <a-option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</a-option>
        </a-select>
        <a-select v-model="taskTypeFilter" allow-clear placeholder="任务类型" style="width: 200px">
          <a-option v-for="item in taskTypeOptions" :key="item.code" :value="item.code">{{ item.name }}</a-option>
        </a-select>
        <a-button type="primary" :loading="loading" @click="handleSearch">查询</a-button>
        <a-button :loading="loading" @click="handleReset">重置</a-button>
      </a-space>

      <a-table
        :columns="columns"
        :data="tasks"
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
          <a-tag :color="statusColorMap[record.status] || 'gray'">
            {{ statusLabelMap[record.status] || record.status }}
          </a-tag>
        </template>
        <template #taskType="{ record }">
          {{ taskTypeNameMap[record.taskType] || record.taskType }}
        </template>
        <template #execution="{ record }">
          <a-space direction="vertical" :size="2">
            <span>{{ formatExecution(record) }}</span>
            <span class="sub-text">下次：{{ formatDateTime(record.nextRunTime) }}</span>
          </a-space>
        </template>
        <template #time="{ record }">
          <a-space direction="vertical" :size="2">
            <span>最近：{{ formatDateTime(record.lastRunTime) }}</span>
            <span class="sub-text">
              {{ record.lastErrorMessage ? `错误：${record.lastErrorMessage}` : '最近执行正常' }}
            </span>
          </a-space>
        </template>
        <template #notify="{ record }">
          <a-space direction="vertical" :size="2">
            <span>成功：{{ record.successNotify ? formatNotify(record) : '关闭' }}</span>
            <span class="sub-text">失败：{{ record.failureNotify ? formatNotify(record) : '关闭' }}</span>
          </a-space>
        </template>
        <template #operations="{ record }">
          <a-space>
            <a-button
              v-if="authStore.hasPermission('system:schedule:edit')"
              type="text"
              size="small"
              :disabled="record.status === 'RUNNING'"
              @click="openEdit(record.id)"
            >
              编辑
            </a-button>
            <a-popconfirm
              v-if="authStore.hasPermission('system:schedule:edit') && ['PAUSED', 'CANCELED'].includes(record.status)"
              :content="`确认启动任务「${record.taskName}」？`"
              @ok="handleStart(record.id)"
            >
              <a-button
                type="text"
                size="small"
              >
                启动
              </a-button>
            </a-popconfirm>
            <a-popconfirm
              v-if="authStore.hasPermission('system:schedule:edit') && record.status === 'SCHEDULED'"
              :content="`确认暂停任务「${record.taskName}」？`"
              @ok="handlePause(record.id)"
            >
              <a-button
                type="text"
                size="small"
              >
                暂停
              </a-button>
            </a-popconfirm>
            <a-popconfirm
              v-if="authStore.hasPermission('system:schedule:edit') && ['SCHEDULED', 'PAUSED'].includes(record.status)"
              :content="`确认取消任务「${record.taskName}」？取消后不会继续调度。`"
              @ok="handleCancel(record.id)"
            >
              <a-button
                type="text"
                size="small"
                status="warning"
              >
                取消
              </a-button>
            </a-popconfirm>
            <a-popconfirm
              v-if="authStore.hasPermission('system:schedule:run')"
              :content="`确认立即执行一次任务「${record.taskName}」？`"
              @ok="handleRun(record.id)"
            >
              <a-button
                type="text"
                size="small"
                :disabled="record.status === 'RUNNING' || record.status === 'CANCELED'"
              >
                执行一次
              </a-button>
            </a-popconfirm>
            <a-popconfirm
              v-if="authStore.hasPermission('system:schedule:remove')"
              content="确认删除该调度任务？"
              @ok="handleDelete(record.id)"
            >
              <a-button type="text" size="small" status="danger" :disabled="record.status === 'RUNNING'">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:visible="modalVisible"
      :title="editingId ? '编辑任务' : '新增任务'"
      width="760px"
      :ok-loading="submitting"
      @ok="handleSubmit"
      @cancel="handleModalCancel"
    >
      <a-tabs v-model:active-key="activeTab" lazy-load>
        <a-tab-pane key="basicInfo" title="基础信息">
          <a-form :model="form" layout="vertical">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="任务名称">
                  <a-input v-model="form.taskName" :max-length="120" show-word-limit />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="任务类型">
                  <a-select v-model="form.taskType" @change="handleTaskTypeSelect">
                    <a-option v-for="item in taskTypeOptions" :key="item.code" :value="item.code">
                      {{ item.name }}
                    </a-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>

            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="初始状态">
                  <a-radio-group v-model="form.status">
                    <a-radio value="PAUSED">先保存为暂停</a-radio>
                    <a-radio value="SCHEDULED">保存后立即启用</a-radio>
                  </a-radio-group>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="执行类型">
                  <a-radio-group v-model="form.executionType">
                    <a-radio value="FIXED_RATE">每隔 N 个时间节点</a-radio>
                    <a-radio value="FIXED_DELAY">完成后延迟 N 个时间节点</a-radio>
                    <a-radio value="CRON">Cron 表达式</a-radio>
                  </a-radio-group>
                </a-form-item>
              </a-col>
            </a-row>

            <a-row v-if="form.executionType === 'CRON'" :gutter="16">
              <a-col :span="24">
                <a-form-item label="Cron 表达式">
                  <a-input v-model="form.cronExpression" placeholder="例如 0 0/10 * * * *" />
                </a-form-item>
              </a-col>
            </a-row>

            <a-row v-else :gutter="16">
              <a-col :span="12">
                <a-form-item label="间隔数值">
                  <a-input-number v-model="form.intervalValue" :min="1" :precision="0" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="时间单位">
                  <a-select v-model="form.intervalUnit">
                    <a-option v-for="item in intervalUnitOptions" :key="item.value" :value="item.value">
                      {{ item.label }}
                    </a-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>

            <a-form-item label="备注">
              <a-textarea v-model="form.remark" :max-length="500" show-word-limit />
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <a-tab-pane key="taskConfig" title="任务参数">
          <a-form :model="form" layout="vertical">
            <a-alert v-if="currentTaskTypeOption" type="info" show-icon class="section-banner">
              {{ currentTaskTypeOption.description }}
            </a-alert>

            <a-empty v-if="currentFormSchema.length === 0" description="当前任务类型暂无动态表单定义" />

            <template v-for="field in currentFormSchema" :key="field.field">
              <a-form-item v-if="isFieldVisible(field)" :label="field.label">
                <a-input
                  v-if="field.component === 'input'"
                  v-model="form.taskConfig[field.field]"
                  :placeholder="field.placeholder"
                  :max-length="field.maxLength"
                  show-word-limit
                />

                <a-textarea
                  v-else-if="field.component === 'textarea'"
                  v-model="form.taskConfig[field.field]"
                  :placeholder="field.placeholder"
                  :max-length="field.maxLength"
                  show-word-limit
                  :auto-size="{ minRows: field.minRows || 4, maxRows: field.maxRows || 8 }"
                />

                <a-radio-group v-else-if="field.component === 'radio-group'" v-model="form.taskConfig[field.field]">
                  <a-radio v-for="option in field.options || []" :key="String(option.value)" :value="option.value">
                    {{ option.label }}
                  </a-radio>
                </a-radio-group>

                <a-checkbox-group
                  v-else-if="field.component === 'checkbox-group'"
                  v-model="form.taskConfig[field.field]"
                >
                  <a-checkbox v-for="option in field.options || []" :key="String(option.value)" :value="option.value">
                    {{ option.label }}
                  </a-checkbox>
                </a-checkbox-group>

                <a-select
                  v-else-if="field.component === 'user-select'"
                  v-model="form.taskConfig[field.field]"
                  multiple
                  allow-search
                  allow-clear
                  :placeholder="field.placeholder"
                >
                  <a-option v-for="user in recipients" :key="user.id" :value="user.id">
                    {{ user.nickName || user.username }}（{{ user.username }}）
                  </a-option>
                </a-select>

                <div v-if="field.helpText" class="field-help">{{ field.helpText }}</div>
              </a-form-item>
            </template>
          </a-form>
        </a-tab-pane>

        <a-tab-pane key="notify" title="执行结果通知">
          <a-form :model="form" layout="vertical">
            <a-alert type="info" show-icon class="section-banner">
              这里的通知用于任务执行结果提醒，和任务自身的业务推送内容分开配置。
            </a-alert>

            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="成功通知">
                  <a-switch v-model="form.successNotify" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="失败通知">
                  <a-switch v-model="form.failureNotify" />
                </a-form-item>
              </a-col>
            </a-row>

            <a-form-item label="通知渠道">
              <a-checkbox-group v-model="form.notifyChannels">
                <a-checkbox v-for="item in notifyChannelOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </a-checkbox>
              </a-checkbox-group>
            </a-form-item>

            <a-form-item label="通知接收人">
              <a-select
                v-model="form.notifyUserIds"
                multiple
                allow-search
                allow-clear
                placeholder="执行结果通知要发给谁"
              >
                <a-option v-for="user in recipients" :key="user.id" :value="user.id">
                  {{ user.nickName || user.username }}（{{ user.username }}）
                </a-option>
              </a-select>
            </a-form-item>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import { useAuthStore } from '@/stores/auth'
import {
  cancelScheduledTask,
  createScheduledTask,
  deleteScheduledTask,
  getScheduledTask,
  getScheduledTasks,
  getScheduledTaskTypes,
  pauseScheduledTask,
  runScheduledTask,
  startScheduledTask,
  updateScheduledTask,
  type ScheduledExecutionType,
  type ScheduledIntervalUnit,
  type ScheduledTaskFormField,
  type ScheduledTaskItem,
  type ScheduledTaskPayload,
  type ScheduledTaskTypeOption
} from '@/api/scheduleCenter'
import { getNotificationRecipients, type NotificationRecipient } from '@/api/adminNotification'

interface TaskFormModel {
  taskName: string
  taskType: string
  executionType: ScheduledExecutionType
  intervalValue?: number
  intervalUnit?: ScheduledIntervalUnit
  cronExpression: string
  status: 'SCHEDULED' | 'PAUSED'
  taskConfig: Record<string, any>
  successNotify: boolean
  failureNotify: boolean
  notifyChannels: string[]
  notifyUserIds: number[]
  remark: string
}

const authStore = useAuthStore()
const loading = ref(false)
const submitting = ref(false)
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const activeTab = ref<'basicInfo' | 'taskConfig' | 'notify'>('basicInfo')
const tasks = ref<ScheduledTaskItem[]>([])
const recipients = ref<NotificationRecipient[]>([])
const taskTypeOptions = ref<ScheduledTaskTypeOption[]>([])
const keyword = ref('')
const statusFilter = ref('')
const taskTypeFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusLabelMap: Record<string, string> = {
  SCHEDULED: '已启用',
  PAUSED: '已暂停',
  RUNNING: '执行中',
  CANCELED: '已取消'
}

const statusColorMap: Record<string, string> = {
  SCHEDULED: 'green',
  PAUSED: 'gray',
  RUNNING: 'arcoblue',
  CANCELED: 'orange'
}

const statusOptions = [
  { label: '已启用', value: 'SCHEDULED' },
  { label: '已暂停', value: 'PAUSED' },
  { label: '执行中', value: 'RUNNING' },
  { label: '已取消', value: 'CANCELED' }
]

const intervalUnitOptions: { label: string; value: ScheduledIntervalUnit }[] = [
  { label: '秒', value: 'SECONDS' },
  { label: '分钟', value: 'MINUTES' },
  { label: '小时', value: 'HOURS' },
  { label: '天', value: 'DAYS' }
]

const notifyChannelOptions = [
  { label: '站内信', value: 'SITE_MESSAGE' },
  { label: '邮件', value: 'EMAIL' },
  { label: 'WxPusher', value: 'WXPUSHER' }
]

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '任务名称', dataIndex: 'taskName', width: 180, ellipsis: true, tooltip: true },
  { title: '任务类型', slotName: 'taskType', width: 140 },
  { title: '执行规则', slotName: 'execution', width: 220 },
  { title: '状态', slotName: 'status', width: 100 },
  { title: '任务摘要', dataIndex: 'taskSummary', ellipsis: true, tooltip: true },
  { title: '最近执行', slotName: 'time', width: 240 },
  { title: '执行次数', dataIndex: 'runCount', width: 100 },
  { title: '执行通知', slotName: 'notify', width: 220 },
  { title: '操作', slotName: 'operations', width: 320, fixed: 'right' }
]

const createDefaultForm = (): TaskFormModel => ({
  taskName: '',
  taskType: '',
  executionType: 'FIXED_RATE',
  intervalValue: 10,
  intervalUnit: 'MINUTES',
  cronExpression: '0 0/10 * * * *',
  status: 'PAUSED',
  taskConfig: {},
  successNotify: false,
  failureNotify: true,
  notifyChannels: ['SITE_MESSAGE'],
  notifyUserIds: [],
  remark: ''
})

const form = reactive<TaskFormModel>(createDefaultForm())

const taskTypeNameMap = computed(() =>
  Object.fromEntries(taskTypeOptions.value.map((item) => [item.code, item.name]))
)

const currentTaskTypeOption = computed(() =>
  taskTypeOptions.value.find((item) => item.code === form.taskType) ?? null
)

const currentFormSchema = computed(() => currentTaskTypeOption.value?.formSchema ?? [])

const cloneValue = <T>(value: T): T => {
  if (value === undefined || value === null) {
    return value
  }
  return JSON.parse(JSON.stringify(value)) as T
}

const defaultFieldValue = (field: ScheduledTaskFormField) => {
  if (field.defaultValue !== undefined) {
    return cloneValue(field.defaultValue)
  }
  if (field.component === 'checkbox-group' || field.component === 'user-select') {
    return []
  }
  if (field.component === 'radio-group') {
    return null
  }
  return ''
}

const buildTaskConfigBySchema = (
  schema: ScheduledTaskFormField[],
  source?: Record<string, any> | null
) => {
  const config: Record<string, any> = {}
  schema.forEach((field) => {
    if (source && source[field.field] !== undefined) {
      config[field.field] = cloneValue(source[field.field])
      return
    }
    config[field.field] = defaultFieldValue(field)
  })
  return config
}

const applyTaskType = (taskType: string, sourceConfig?: Record<string, any> | null) => {
  const option = taskTypeOptions.value.find((item) => item.code === taskType)
  form.taskType = option?.code ?? ''
  form.taskConfig = buildTaskConfigBySchema(option?.formSchema ?? [], sourceConfig)
}

const resetForm = () => {
  Object.assign(form, createDefaultForm())
  activeTab.value = 'basicInfo'
  editingId.value = null
  if (taskTypeOptions.value.length > 0) {
    applyTaskType(taskTypeOptions.value[0].code)
  }
}

const formatDateTime = (value?: string) => {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

const formatNotify = (record: ScheduledTaskItem) => {
  const channels = record.notifyChannels.length > 0 ? record.notifyChannels.join('/') : '无渠道'
  const users = record.notifyUserIds.length
  return `${channels} / ${users} 人`
}

const formatExecution = (record: ScheduledTaskItem) => {
  if (record.executionType === 'CRON') {
    return `Cron：${record.cronExpression || '-'}`
  }
  const unitLabel = intervalUnitOptions.find((item) => item.value === record.intervalUnit)?.label || record.intervalUnit || '-'
  if (record.executionType === 'FIXED_DELAY') {
    return `完成后延迟 ${record.intervalValue || '-'} ${unitLabel}`
  }
  return `每隔 ${record.intervalValue || '-'} ${unitLabel}`
}

const isFieldVisible = (field: ScheduledTaskFormField) => {
  if (!field.visibleWhenField) {
    return true
  }
  return form.taskConfig[field.visibleWhenField] === field.visibleWhenEquals
}

const isEmptyFieldValue = (field: ScheduledTaskFormField, value: unknown) => {
  if (field.component === 'checkbox-group' || field.component === 'user-select') {
    return !Array.isArray(value) || value.length === 0
  }
  if (typeof value === 'string') {
    return !value.trim()
  }
  return value === undefined || value === null || value === ''
}

const validateBasicInfo = () => {
  if (!form.taskName.trim()) {
    Message.warning('请填写任务名称')
    return false
  }
  if (!form.taskType) {
    Message.warning('请选择任务类型')
    return false
  }
  if (form.executionType === 'CRON') {
    if (!form.cronExpression.trim()) {
      Message.warning('请填写 Cron 表达式')
      return false
    }
  } else {
    if (!form.intervalValue || form.intervalValue <= 0) {
      Message.warning('请填写大于 0 的间隔数值')
      return false
    }
    if (!form.intervalUnit) {
      Message.warning('请选择时间单位')
      return false
    }
  }

  return true
}

const validateTaskConfig = () => {
  for (const field of currentFormSchema.value) {
    if (!isFieldVisible(field) || !field.required) {
      continue
    }
    const value = form.taskConfig[field.field]
    if (isEmptyFieldValue(field, value)) {
      Message.warning(`请填写${field.label}`)
      return false
    }
  }
  return true
}

const validateNotifyTab = () => {
  if ((form.successNotify || form.failureNotify) && form.notifyChannels.length === 0) {
    Message.warning('启用执行通知时，请至少选择一种通知渠道')
    return false
  }
  if ((form.successNotify || form.failureNotify) && form.notifyUserIds.length === 0) {
    Message.warning('启用执行通知时，请至少选择一个通知接收人')
    return false
  }
  return true
}

const buildTaskPayload = (): ScheduledTaskPayload => {
  const config: Record<string, any> = {}
  currentFormSchema.value.forEach((field) => {
    config[field.field] = cloneValue(form.taskConfig[field.field])
  })

  return {
    taskName: form.taskName.trim(),
    taskType: form.taskType,
    executionType: form.executionType,
    intervalValue: form.executionType === 'CRON' ? undefined : form.intervalValue,
    intervalUnit: form.executionType === 'CRON' ? undefined : form.intervalUnit,
    cronExpression: form.executionType === 'CRON' ? form.cronExpression.trim() : undefined,
    status: form.status,
    taskConfig: config,
    successNotify: form.successNotify,
    failureNotify: form.failureNotify,
    notifyChannels: form.notifyChannels,
    notifyUserIds: form.notifyUserIds,
    remark: form.remark.trim() || undefined
  }
}

const loadTaskOptions = async () => {
  const [users, types] = await Promise.all([getNotificationRecipients(), getScheduledTaskTypes()])
  recipients.value = users
  taskTypeOptions.value = types
  if (!form.taskType && taskTypeOptions.value.length > 0) {
    applyTaskType(taskTypeOptions.value[0].code)
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const taskPage = await getScheduledTasks({
      keyword: keyword.value.trim() || undefined,
      status: statusFilter.value || undefined,
      taskType: taskTypeFilter.value || undefined,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    tasks.value = taskPage.records
    total.value = taskPage.total
    currentPage.value = taskPage.pageNum
    pageSize.value = taskPage.pageSize
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
  taskTypeFilter.value = ''
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

const handleTaskTypeSelect = (value: string | number | Record<string, unknown> | undefined) => {
  if (typeof value === 'string') {
    applyTaskType(value)
  }
}

const openCreate = async () => {
  if (taskTypeOptions.value.length === 0) {
    await loadTaskOptions()
  }
  resetForm()
  modalVisible.value = true
}

const openEdit = async (id: number) => {
  if (taskTypeOptions.value.length === 0) {
    await loadTaskOptions()
  }
  const detail = await getScheduledTask(id)
  editingId.value = id
  Object.assign(form, {
    taskName: detail.taskName,
    taskType: detail.taskType,
    executionType: detail.executionType,
    intervalValue: detail.intervalValue ?? 10,
    intervalUnit: detail.intervalUnit ?? 'MINUTES',
    cronExpression: detail.cronExpression || '0 0/10 * * * *',
    status: ['SCHEDULED', 'PAUSED'].includes(detail.status) ? detail.status : 'PAUSED',
    successNotify: detail.successNotify,
    failureNotify: detail.failureNotify,
    notifyChannels: detail.notifyChannels || [],
    notifyUserIds: detail.notifyUserIds || [],
    remark: detail.remark || ''
  })
  applyTaskType(detail.taskType, (detail.taskConfig || {}) as Record<string, any>)
  activeTab.value = 'basicInfo'
  modalVisible.value = true
}

const handleSubmit = async () => {
  activeTab.value = 'basicInfo'
  if (!validateBasicInfo()) {
    return
  }
  activeTab.value = 'taskConfig'
  if (!validateTaskConfig()) {
    return
  }
  activeTab.value = 'notify'
  if (!validateNotifyTab()) {
    return
  }

  submitting.value = true
  try {
    const payload = buildTaskPayload()
    if (editingId.value) {
      await updateScheduledTask(editingId.value, payload)
      Message.success('任务更新成功')
    } else {
      await createScheduledTask(payload)
      Message.success('任务创建成功')
    }

    modalVisible.value = false
    resetForm()
    await loadData()
  } finally {
    submitting.value = false
  }
}

const handleStart = async (id: number) => {
  await startScheduledTask(id)
  Message.success('任务已启动')
  await loadData()
}

const handlePause = async (id: number) => {
  await pauseScheduledTask(id)
  Message.success('任务已暂停')
  await loadData()
}

const handleCancel = async (id: number) => {
  await cancelScheduledTask(id)
  Message.success('任务已取消')
  await loadData()
}

const handleRun = async (id: number) => {
  await runScheduledTask(id)
  Message.success('任务已提交执行')
  await loadData()
}

const handleDelete = async (id: number) => {
  await deleteScheduledTask(id)
  Message.success('任务已删除')
  if (tasks.value.length === 1 && currentPage.value > 1) {
    currentPage.value -= 1
  }
  await loadData()
}

const handleModalCancel = () => {
  modalVisible.value = false
  resetForm()
}

onMounted(async () => {
  await Promise.all([loadTaskOptions(), loadData()])
})
</script>

<style scoped>
.page-container {
  min-height: 100%;
}

.info-banner {
  margin-bottom: 20px;
}

.section-banner {
  margin-bottom: 16px;
}

.toolbar {
  margin-bottom: 16px;
}

.sub-text {
  color: var(--app-muted-text, #86909c);
  font-size: 12px;
}

.field-help {
  margin-top: 6px;
  color: var(--app-muted-text, #86909c);
  font-size: 12px;
}
</style>
