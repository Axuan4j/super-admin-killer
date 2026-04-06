<template>
  <div class="dashboard-page">
    <section class="hero-panel">
      <div>
        <div class="hero-eyebrow">工作台</div>
        <h1 class="hero-title">{{ greetingText }}，{{ displayName }}</h1>
        <p class="hero-desc">
          {{ heroDescription }}
        </p>
        <a-space wrap>
          <a-tag v-for="role in authStore.userInfo?.roles || []" :key="role" color="arcoblue">{{ role }}</a-tag>
          <a-tag bordered>{{ visibleMenuCount }} 个可见菜单</a-tag>
          <a-tag bordered>{{ permissionCount }} 项可用权限</a-tag>
        </a-space>
      </div>
    </section>

    <section class="stats-grid">
      <a-card v-for="card in statCards" :key="card.title" class="stat-card" :bordered="false">
        <div class="stat-top">
          <div>
            <div class="stat-label">{{ card.title }}</div>
            <div class="stat-value">{{ card.value }}</div>
          </div>
          <div class="stat-icon" :style="{ background: card.iconBg, color: card.iconColor }">
            <font-awesome-icon :icon="card.icon" />
          </div>
        </div>
        <div class="stat-desc">{{ card.description }}</div>
      </a-card>
    </section>

    <section class="content-grid">
      <a-card class="dashboard-card" title="最近通知" :bordered="false">
        <template #extra>
          <a-button type="text" @click="goToPath('/layout/site-messages')">全部消息</a-button>
        </template>

        <a-empty v-if="recentMessages.length === 0" description="暂时没有通知" />
        <div v-else class="list-block">
          <button
            v-for="message in recentMessages"
            :key="message.id"
            type="button"
            class="list-item"
            @click="goToPath('/layout/site-messages')"
          >
            <div class="list-item-main">
              <div class="list-item-title-row">
                <span class="list-item-title">{{ message.title }}</span>
                <a-tag :color="message.readStatus === 1 ? 'gray' : 'arcoblue'" size="small">
                  {{ message.readStatus === 1 ? '已读' : '未读' }}
                </a-tag>
              </div>
              <p class="list-item-desc">{{ message.content }}</p>
            </div>
            <div class="list-item-meta">
              <span>{{ message.senderName || '系统' }}</span>
              <span>{{ formatDateTime(message.createTime) }}</span>
            </div>
          </button>
        </div>
      </a-card>

      <a-card class="dashboard-card" title="最近操作" :bordered="false">
        <template #extra>
          <a-button
            v-if="canViewLogs"
            type="text"
            @click="goToMenuPermission('system:log:view', '/layout/logs')"
          >
            查看日志
          </a-button>
        </template>

        <a-empty v-if="!canViewLogs" description="当前账号没有查看操作日志权限" />
        <a-empty v-else-if="recentLogs.length === 0" description="暂时没有操作记录" />
        <div v-else class="list-block">
          <div v-for="log in recentLogs" :key="log.id" class="list-item plain">
            <div class="list-item-main">
              <div class="list-item-title-row">
                <span class="list-item-title">{{ log.action || '系统操作' }}</span>
                <a-tag :color="dictStore.getDictTagColor('sys_oper_success', log.success)" size="small">
                  {{ dictStore.getDictLabel('sys_oper_success', log.success) }}
                </a-tag>
              </div>
              <p class="list-item-desc">{{ log.extra || log.requestUrl || '无补充信息' }}</p>
            </div>
            <div class="list-item-meta">
              <span>{{ log.operator || '-' }}</span>
              <span>{{ formatDateTime(log.createTime) }}</span>
            </div>
          </div>
        </div>
      </a-card>
    </section>

    <section class="shortcut-section">
      <a-card class="dashboard-card" title="快捷操作" :bordered="false">
        <div class="shortcut-grid">
          <button
            v-for="item in quickActions"
            :key="item.title"
            type="button"
            class="shortcut-item"
            @click="goToPath(item.path)"
          >
            <div class="shortcut-icon" :style="{ background: item.iconBg, color: item.iconColor }">
              <font-awesome-icon :icon="item.icon" />
            </div>
            <div class="shortcut-title">{{ item.title }}</div>
            <div class="shortcut-desc">{{ item.description }}</div>
          </button>
        </div>
      </a-card>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useDictStore } from '@/stores/dict'
import { useMenuStore } from '@/stores/menu'
import { getCurrentSiteMessageUnreadCount, getCurrentSiteMessages, type SiteMessage } from '@/api/siteMessage'
import { getOnlineUsers } from '@/api/adminOnline'
import { getOperLogs, type OperLogItem } from '@/api/operLog'

interface MenuNode {
  path: string
  name: string
  perms?: string
  children?: MenuNode[]
}

interface StatCard {
  title: string
  value: string | number
  description: string
  icon: string
  iconBg: string
  iconColor: string
}

interface QuickAction {
  title: string
  description: string
  path: string
  icon: string
  iconBg: string
  iconColor: string
}

const router = useRouter()
const authStore = useAuthStore()
const dictStore = useDictStore()
const menuStore = useMenuStore()

const unreadCount = ref(0)
const onlineCount = ref<number | null>(null)
const logTotal = ref<number | null>(null)
const recentMessages = ref<SiteMessage[]>([])
const recentLogs = ref<OperLogItem[]>([])

const canViewLogs = computed(() => authStore.hasPermission('system:log:view'))
const canViewOnline = computed(() => authStore.hasPermission('system:online:view'))
const displayName = computed(() => authStore.userInfo?.nickName || authStore.userInfo?.username || '用户')
const permissionCount = computed(() => authStore.userInfo?.authorities?.length || 0)

const flattenMenus = (menus: MenuNode[]): MenuNode[] => menus.flatMap(menu => [menu, ...(menu.children ? flattenMenus(menu.children) : [])])

const visibleMenuCount = computed(() =>
  flattenMenus(menuStore.menus as MenuNode[]).filter(menu => !menu.children?.length).length
)

const greetingText = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const heroDescription = computed(() => {
  if (unreadCount.value > 0) {
    return `你当前有 ${unreadCount.value} 条未读站内消息，建议优先处理最新通知和待确认事项。`
  }
  if (authStore.isAdmin) {
    return '系统管理能力已经就绪，可以从下方快捷入口进入用户、角色、权限和通知相关工作。'
  }
  return '当前账号状态正常，可以从下方查看最近通知、个人资料和你有权限访问的系统入口。'
})

const statCards = computed<StatCard[]>(() => [
  {
    title: '未读通知',
    value: unreadCount.value,
    description: unreadCount.value > 0 ? '有新的站内消息待处理' : '当前没有未读消息',
    icon: 'fa-solid fa-envelope-open-text',
    iconBg: 'rgba(var(--primary-6), 0.14)',
    iconColor: 'rgb(var(--primary-6))'
  },
  {
    title: '当前角色',
    value: authStore.userInfo?.roles?.length || 0,
    description: (authStore.userInfo?.roles || []).join(' / ') || '尚未分配角色',
    icon: 'fa-solid fa-user-shield',
    iconBg: 'rgba(12, 183, 135, 0.14)',
    iconColor: '#0CB787'
  },
  {
    title: '可用权限',
    value: permissionCount.value,
    description: permissionCount.value > 0 ? '当前账号已加载权限标识' : '当前没有权限标识',
    icon: 'fa-solid fa-key',
    iconBg: 'rgba(255, 125, 0, 0.14)',
    iconColor: '#FF7D00'
  },
  {
    title: canViewOnline.value ? '在线会话' : '可见菜单',
    value: canViewOnline.value ? onlineCount.value ?? '--' : visibleMenuCount.value,
    description: canViewOnline.value ? '当前系统在线会话总数' : '当前账号可直接访问的菜单数',
    icon: canViewOnline.value ? 'fa-solid fa-signal' : 'fa-solid fa-compass',
    iconBg: 'rgba(114, 46, 209, 0.14)',
    iconColor: '#722ED1'
  }
])

const quickActions = computed<QuickAction[]>(() => {
  const definitions = [
    {
      title: '用户管理',
      description: '新增、编辑或查看用户',
      permission: 'system:user:view',
      fallbackPath: '/layout/users',
      icon: 'fa-solid fa-user',
      iconBg: 'rgba(var(--primary-6), 0.14)',
      iconColor: 'rgb(var(--primary-6))'
    },
    {
      title: '角色管理',
      description: '配置角色与授权范围',
      permission: 'system:role:view',
      fallbackPath: '/layout/roles',
      icon: 'fa-solid fa-user-shield',
      iconBg: 'rgba(12, 183, 135, 0.14)',
      iconColor: '#0CB787'
    },
    {
      title: '权限管理',
      description: '维护菜单和按钮权限',
      permission: 'system:permission:view',
      fallbackPath: '/layout/permissions',
      icon: 'fa-solid fa-key',
      iconBg: 'rgba(255, 125, 0, 0.14)',
      iconColor: '#FF7D00'
    },
    {
      title: '通知管理',
      description: '发送系统通知消息',
      permission: 'system:notification:view',
      fallbackPath: '/layout/notifications',
      icon: 'fa-solid fa-bullhorn',
      iconBg: 'rgba(245, 63, 63, 0.14)',
      iconColor: '#F53F3F'
    },
    {
      title: '在线用户',
      description: '查看在线会话和强退',
      permission: 'system:online:view',
      fallbackPath: '/layout/online',
      icon: 'fa-solid fa-user-lock',
      iconBg: 'rgba(114, 46, 209, 0.14)',
      iconColor: '#722ED1'
    },
    {
      title: '个人中心',
      description: '维护个人信息和密码',
      permission: '',
      fallbackPath: '/blank/profile',
      icon: 'fa-solid fa-id-card',
      iconBg: 'rgba(64, 128, 255, 0.14)',
      iconColor: '#4080FF'
    }
  ]

  return definitions
    .filter(item => !item.permission || authStore.hasPermission(item.permission))
    .map(item => ({
      title: item.title,
      description: item.description,
      path: item.permission ? resolveMenuPath(item.permission, item.fallbackPath) : item.fallbackPath,
      icon: item.icon,
      iconBg: item.iconBg,
      iconColor: item.iconColor
    }))
})

const resolveMenuPath = (permission: string, fallbackPath: string) => {
  const menu = flattenMenus(menuStore.menus as MenuNode[]).find(item => item.perms === permission)
  return menu?.path || fallbackPath
}

const goToPath = (path: string) => {
  router.push(path)
}

const goToMenuPermission = (permission: string, fallbackPath: string) => {
  goToPath(resolveMenuPath(permission, fallbackPath))
}

const formatDateTime = (value?: string) => {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 16)
}

const loadDashboardData = async () => {
  if (!authStore.userInfo) {
    await authStore.fetchUserInfo()
  }

  const tasks: Promise<unknown>[] = [
    getCurrentSiteMessageUnreadCount().then(result => {
      unreadCount.value = result.unreadCount || 0
    }),
    getCurrentSiteMessages().then(result => {
      recentMessages.value = result.slice(0, 5)
    })
  ]

  if (canViewOnline.value) {
    tasks.push(
      getOnlineUsers().then(result => {
        onlineCount.value = result.length
      })
    )
  }

  if (canViewLogs.value) {
    tasks.push(
      getOperLogs({ current: 1, size: 5 }).then(result => {
        logTotal.value = result.total
        recentLogs.value = result.records
      })
    )
  }

  await Promise.allSettled(tasks)
}

onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-panel {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 30px;
  border-radius: 20px;
  background:
    radial-gradient(circle at top right, rgba(var(--primary-6), 0.22), transparent 34%),
    linear-gradient(135deg, rgba(var(--primary-6), 0.08), var(--app-panel-bg, #ffffff));
  border: 1px solid var(--app-border-color, #e5e6eb);
  box-shadow: var(--app-shadow);
}

.hero-eyebrow {
  color: rgb(var(--primary-6));
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-title {
  margin: 10px 0 12px;
  color: var(--app-header-text, #1d2129);
  font-size: 32px;
  line-height: 1.2;
}

.hero-desc {
  max-width: 760px;
  margin: 0 0 18px;
  color: var(--app-muted-text, #86909c);
  font-size: 14px;
  line-height: 1.8;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card,
.dashboard-card {
  background: var(--app-panel-bg, #ffffff);
}

.stat-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.stat-label {
  color: var(--app-muted-text, #86909c);
  font-size: 13px;
}

.stat-value {
  margin-top: 8px;
  color: var(--app-header-text, #1d2129);
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
}

.stat-desc {
  margin-top: 16px;
  color: var(--app-muted-text, #86909c);
  font-size: 13px;
  line-height: 1.7;
  min-height: 44px;
}

.stat-icon,
.shortcut-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  font-size: 18px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1.25fr 1fr;
  gap: 16px;
}

.list-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.list-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border: 1px solid var(--app-border-color, #e5e6eb);
  border-radius: 14px;
  background: var(--app-panel-bg, #ffffff);
  text-align: left;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.list-item:not(.plain) {
  cursor: pointer;
}

.list-item:not(.plain):hover {
  transform: translateY(-1px);
  border-color: rgba(var(--primary-6), 0.4);
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.08);
}

.list-item-main {
  min-width: 0;
  flex: 1;
}

.list-item-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.list-item-title {
  color: var(--app-header-text, #1d2129);
  font-size: 15px;
  font-weight: 600;
}

.list-item-desc {
  display: -webkit-box;
  margin: 8px 0 0;
  color: var(--app-muted-text, #86909c);
  font-size: 13px;
  line-height: 1.7;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.list-item-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  color: var(--app-muted-text, #86909c);
  font-size: 12px;
  white-space: nowrap;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.shortcut-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  padding: 18px;
  border: 1px solid var(--app-border-color, #e5e6eb);
  border-radius: 16px;
  background: var(--app-panel-bg, #ffffff);
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.shortcut-item:hover {
  transform: translateY(-2px);
  border-color: rgba(var(--primary-6), 0.4);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.shortcut-icon {
  width: 44px;
  height: 44px;
  font-size: 16px;
}

.shortcut-title {
  color: var(--app-header-text, #1d2129);
  font-size: 15px;
  font-weight: 600;
}

.shortcut-desc {
  color: var(--app-muted-text, #86909c);
  font-size: 13px;
  line-height: 1.7;
}

.dashboard-card :deep(.arco-card-header-title) {
  color: var(--app-header-text, #1d2129);
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero-panel {
    flex-direction: column;
    padding: 22px 20px;
  }

  .hero-title {
    font-size: 26px;
  }

  .stats-grid,
  .shortcut-grid {
    grid-template-columns: 1fr;
  }

  .list-item,
  .list-item-meta {
    align-items: flex-start;
  }

  .list-item {
    flex-direction: column;
  }
}
</style>
