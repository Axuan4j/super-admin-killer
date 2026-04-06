<template>
  <a-layout class="layout-container">
    <!-- Header -->
    <a-layout-header class="layout-header">
      <div class="header-left">
        <span class="logo">{{ env.APP_TITLE }}</span>
      </div>
      <div class="header-right">
        <a-space :size="20">
          <a-badge :count="unreadCount" :max-count="99">
            <a-button type="outline" @click="handleNotificationClick">
              <template #icon>
                <font-awesome-icon icon="fa-solid fa-bell"/>
              </template>
              通知
            </a-button>
          </a-badge>
          <a-button type="outline" @click="handleSettingsClick">
            <template #icon>
              <font-awesome-icon icon="fa-solid fa-gear"/>
            </template>
            设置
          </a-button>
          <a-button type="outline" @click="handleLogout">
            <template #icon>
              <font-awesome-icon icon="fa-solid fa-right-from-bracket"/>
            </template>
            退出登录
          </a-button>
          <a-space>
            <a-avatar v-if="authStore.userInfo?.avatar" :size="32">
              <img :src="resolveAssetUrl(authStore.userInfo.avatar)" alt="avatar" class="header-avatar-image" />
            </a-avatar>
            <a-avatar v-else :size="32" :style="{ backgroundColor: 'rgb(var(--primary-6))' }">
              {{ authStore.userInfo?.nickName?.charAt(0)?.toUpperCase() || authStore.userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
            </a-avatar>
            <span class="username">{{ authStore.userInfo?.nickName || authStore.userInfo?.username }}</span>
          </a-space>
        </a-space>
      </div>
    </a-layout-header>

    <a-layout class="layout-main">
      <!-- 左侧菜单 -->
      <a-layout-sider
          v-model:collapsed="collapsed"
          :trigger="null"
          collapsible
          :width="220"
          class="layout-sider"
      >
        <a-spin :spinning="menuStore.loading" class="menu-scroll">
          <a-menu
              v-model:selected-keys="selectedKeys"
              v-model:open-keys="openKeys"
              :accordion="false"
              :style="{ width: '100%' }"
              breakpoint="xl"
          >
            <template v-for="menu in menuStore.menus" :key="menu.path">
              <a-menu-item
                v-if="!menu.children || menu.children.length === 0"
                :key="menu.path"
                @click="handleMenuClick(menu.path)"
              >
                <template #icon>
                  <font-awesome-icon v-if="menu.meta?.icon" :icon="[menu.meta.icon.prefix, menu.meta.icon.iconName]"/>
                  <font-awesome-icon v-else icon="fa-solid fa-gauge"/>
                </template>
                {{ menu.name }}
              </a-menu-item>
              <a-sub-menu v-else :key="menu.path">
                <template #icon>
                  <font-awesome-icon v-if="menu.meta?.icon" :icon="[menu.meta.icon.prefix, menu.meta.icon.iconName]"/>
                  <font-awesome-icon v-else icon="fa-solid fa-gears"/>
                </template>
                <template #title>{{ menu.name }}</template>
                <template v-for="child in menu.children" :key="child.path">
                  <a-menu-item
                    v-if="!child.children || child.children.length === 0"
                    :key="child.path"
                    @click="handleMenuClick(child.path)"
                  >
                    {{ child.name }}
                  </a-menu-item>
                </template>
              </a-sub-menu>
            </template>
          </a-menu>
        </a-spin>
      </a-layout-sider>

      <!-- 主内容区 -->
      <a-layout-content class="layout-content">
        <!-- Tab 标签页 -->
        <div v-if="preferenceStore.showTabs" class="tab-bar">
          <a-space :size="8">
            <a-tag
                v-for="tab in tabs"
                :key="tab.path"
                :color="activeTab === tab.path ? 'arcoblue' : 'gray'"
                closable
                checkable
                :checked="activeTab === tab.path"
                @click="handleTabClick(tab.path)"
                @close="handleTabClose(tab.path)"
            >
              {{ tab.title }}
            </a-tag>
          </a-space>
        </div>
        <!-- 页面内容 -->
        <div class="content-wrapper">
          <router-view/>
        </div>
      </a-layout-content>
    </a-layout>

    <a-drawer
      v-model:visible="settingsDrawerVisible"
      title="个人设置"
      :width="420"
      placement="right"
      unmount-on-close
    >
      <a-space direction="vertical" fill :size="24">
        <section class="settings-block">
          <div class="settings-title">显示模式</div>
          <div class="settings-desc">支持浅色、深色，以及自动跟随系统当前模式。</div>
          <a-radio-group
            type="button"
            :model-value="preferenceStore.colorMode"
            @change="handleColorModeChange"
          >
            <a-radio value="light">浅色</a-radio>
            <a-radio value="dark">深色</a-radio>
            <a-radio value="system">跟随系统</a-radio>
          </a-radio-group>
        </section>

        <section class="settings-block">
          <div class="settings-title">主题配色</div>
          <div class="settings-desc">主题色会立即生效，并自动保存在当前浏览器。</div>

          <div class="theme-preview-card">
            <div class="theme-preview-header">
              <span class="theme-preview-dot" :style="{ backgroundColor: preferenceStore.themeColor }"></span>
              <span>当前主题色</span>
            </div>
            <div class="theme-preview-value">{{ preferenceStore.themeColor }}</div>
          </div>

          <div class="theme-preset-list">
            <button
              v-for="color in themeColorPresets"
              :key="color"
              type="button"
              class="theme-swatch"
              :class="{ active: preferenceStore.themeColor === color }"
              :style="{ backgroundColor: color }"
              @click="handlePresetThemeSelect(color)"
            >
              <font-awesome-icon v-if="preferenceStore.themeColor === color" icon="fa-solid fa-check" />
            </button>
          </div>

          <div class="theme-custom-row">
            <label class="theme-color-picker">
              <span>自定义颜色</span>
              <input :value="preferenceStore.themeColor" type="color" @change="handleThemeColorChange" />
            </label>
            <a-button @click="handleResetTheme">恢复默认</a-button>
          </div>
        </section>

        <section class="settings-block">
          <div class="settings-title">界面偏好</div>
          <div class="settings-desc">这些设置只影响当前账号在当前浏览器里的使用体验。</div>
          <div class="preference-row">
            <div>
              <div class="preference-label">显示菜单标签页</div>
              <div class="preference-tip">关闭后顶部页签栏会隐藏，页面只保留主内容区。</div>
            </div>
            <a-switch :model-value="preferenceStore.showTabs" @change="handleShowTabsChange" />
          </div>
        </section>
      </a-space>
    </a-drawer>
  </a-layout>
</template>

<script setup lang="ts">
import {onMounted, onUnmounted, ref, watch} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import {useAuthStore} from '@/stores/auth.ts'
import {useMenuStore} from '@/stores/menu.ts'
import {Message} from '@arco-design/web-vue'
import request from '@/api/request.ts'
import {getCurrentSiteMessageUnreadCount} from '@/api/siteMessage.ts'
import {env} from '@/utils/envMap'
import { resetDynamicRoutes } from '@/router'
import {
  SITE_MESSAGE_PUSH_EVENT,
  SITE_MESSAGE_READ_EVENT,
  type SiteMessageSocketPayload
} from '@/utils/siteMessageEvents'
import { usePreferenceStore } from '@/stores/preference'
import { THEME_COLOR_PRESETS } from '@/utils/theme'

interface TabItem {
  path: string
  title: string
}

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const menuStore = useMenuStore()
const preferenceStore = usePreferenceStore()

const collapsed = ref(false)
const settingsDrawerVisible = ref(false)
const selectedKeys = ref<string[]>([route.path])
const openKeys = ref<string[]>([])
const tabs = ref<TabItem[]>([{path: '/layout/dashboard', title: '首页'}])
const activeTab = ref('/layout/dashboard')
const unreadCount = ref(0)
const siteMessageSocket = ref<WebSocket | null>(null)
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
let socketClosedManually = false
const themeColorPresets = THEME_COLOR_PRESETS

const findMenuItem = (menus: any[], path: string): any => {
  for (const menu of menus) {
    if (menu.path === path) return menu
    if (menu.children) {
      const found = findMenuItem(menu.children, path)
      if (found) return found
    }
  }
  return null
}

const findAncestorPaths = (menus: any[], path: string, ancestors: string[] = []): string[] => {
  for (const menu of menus) {
    if (menu.path === path) {
      return ancestors
    }
    if (menu.children?.length) {
      const found = findAncestorPaths(menu.children, path, [...ancestors, menu.path])
      if (found.length) {
        return found
      }
    }
  }
  return []
}

const syncMenuState = (path: string) => {
  selectedKeys.value = [path]
  const ancestorPaths = findAncestorPaths(menuStore.menus, path)
  if (ancestorPaths.length === 0) {
    return
  }
  openKeys.value = Array.from(new Set([...openKeys.value, ...ancestorPaths]))
}

const resolveTabTitle = (path: string) => {
  const menuItem = findMenuItem(menuStore.menus, path)
  if (menuItem?.name || menuItem?.meta?.title) {
    return menuItem?.name || menuItem?.meta?.title
  }

  const matchedRoute = router.getRoutes().find(item => item.path === path)
  if (typeof matchedRoute?.meta?.title === 'string') {
    return matchedRoute.meta.title
  }

  return path
}

const loadUnreadCount = async () => {
  try {
    const data = await getCurrentSiteMessageUnreadCount()
    unreadCount.value = data.unreadCount || 0
  } catch {
    unreadCount.value = 0
  }
}

const resolveAssetUrl = (value?: string) => {
  if (!value) {
    return ''
  }
  if (/^(https?:)?\/\//.test(value) || value.startsWith('data:') || value.startsWith('blob:')) {
    return value
  }

  const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')
  const normalizedPath = value.startsWith('/') ? value : `/${value}`
  return `${apiBaseUrl}${normalizedPath}`
}

const clearReconnectTimer = () => {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
}

const clearHeartbeatTimer = () => {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

const buildSiteMessageSocketUrl = () => {
  const token = localStorage.getItem('accessToken')
  if (!token) {
    return null
  }

  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  const wsBaseUrl = apiBaseUrl.replace(/^http/, 'ws')
  return `${wsBaseUrl}/ws/site-messages?token=${encodeURIComponent(token)}`
}

const handleSocketPayload = (payload: SiteMessageSocketPayload) => {
  if (payload.type === 'FORCE_LOGOUT') {
    Message.warning(payload.text || '当前会话已被管理员强制下线')
    menuStore.resetMenus()
    resetDynamicRoutes()
    authStore.logout()
    router.push('/login')
    return
  }

  unreadCount.value = payload.unreadCount || 0

  if (payload.type === 'NEW_MESSAGE' && payload.message) {
    Message.info(`收到新通知：${payload.message.title}`)
  }

  window.dispatchEvent(new CustomEvent(SITE_MESSAGE_PUSH_EVENT, { detail: payload }))
}

const closeSiteMessageSocket = () => {
  socketClosedManually = true
  clearReconnectTimer()
  clearHeartbeatTimer()
  if (siteMessageSocket.value) {
    siteMessageSocket.value.close()
    siteMessageSocket.value = null
  }
}

const scheduleReconnect = () => {
  clearReconnectTimer()
  reconnectTimer = setTimeout(() => {
    connectSiteMessageSocket()
  }, 3000)
}

const connectSiteMessageSocket = () => {
  if (!authStore.isLoggedIn) {
    return
  }
  if (
    siteMessageSocket.value &&
    (siteMessageSocket.value.readyState === WebSocket.OPEN || siteMessageSocket.value.readyState === WebSocket.CONNECTING)
  ) {
    return
  }

  const socketUrl = buildSiteMessageSocketUrl()
  if (!socketUrl) {
    return
  }

  socketClosedManually = false
  const socket = new WebSocket(socketUrl)
  siteMessageSocket.value = socket

  socket.onopen = () => {
    clearHeartbeatTimer()
    heartbeatTimer = setInterval(() => {
      if (socket.readyState === WebSocket.OPEN) {
        socket.send('PING')
      }
    }, 30000)
  }

  socket.onmessage = (event) => {
    try {
      handleSocketPayload(JSON.parse(event.data) as SiteMessageSocketPayload)
    } catch {
      // ignore invalid websocket payload
    }
  }

  socket.onclose = () => {
    siteMessageSocket.value = null
    clearHeartbeatTimer()
    if (!socketClosedManually && authStore.isLoggedIn) {
      scheduleReconnect()
    }
  }

  socket.onerror = () => {
    socket.close()
  }
}

watch(() => route.path, (newPath) => {
  syncMenuState(newPath)

  // 如果是新页面，添加到 tabs
  if (!tabs.value.find(t => t.path === newPath)) {
    tabs.value.push({
      path: newPath,
      title: resolveTabTitle(newPath)
    })
  }

  // 确保 tabs 更新后再设置 activeTab
  activeTab.value = newPath
}, {immediate: true})

watch(() => menuStore.menus, () => {
  syncMenuState(route.path)
}, { immediate: true, deep: true })

watch(() => authStore.accessToken, async (token) => {
  if (!token) {
    unreadCount.value = 0
    openKeys.value = []
    closeSiteMessageSocket()
    return
  }

  if (!authStore.userInfo) {
    await authStore.fetchUserInfo()
  }
  await loadUnreadCount()
  connectSiteMessageSocket()
}, { immediate: true })

const handleMenuClick = async (target: string) => {
  console.debug('[menu] click raw target:', target)

  if (!target) {
    console.warn('[menu] ignored empty target')
    return
  }

  const normalizedTarget = target.startsWith('/') ? target : `/${target}`
  console.debug('[menu] normalized target:', normalizedTarget)
  console.debug('[menu] current route before push:', route.path)
  console.debug(
    '[menu] matched routes:',
    router.getRoutes()
      .filter(item => item.path === normalizedTarget || item.name === normalizedTarget)
      .map(item => ({
        name: item.name,
        path: item.path,
        redirect: item.redirect
      }))
  )

  if (route.path === normalizedTarget) {
    console.debug('[menu] skip push because already on target route')
    return
  }

  try {
    await router.push(normalizedTarget)
    console.debug('[menu] push success, current route after push:', router.currentRoute.value.path)
  } catch (error) {
    console.error('[menu] push failed:', error)
    throw error
  }
}

const handleTabClick = (path: string) => {
  router.push(path)
}

const handleNotificationClick = () => {
  if (unreadCount.value === 0) {
    Message.info('当前没有未读消息')
  }
  router.push('/layout/site-messages')
}

const handleSettingsClick = () => {
  settingsDrawerVisible.value = true
}

const handlePresetThemeSelect = (color: string) => {
  preferenceStore.setThemeColor(color)
  Message.success('主题配色已保存')
}

const handleColorModeChange = (value: string | number | boolean) => {
  preferenceStore.setColorMode(String(value) as 'light' | 'dark' | 'system')
  Message.success('显示模式已保存')
}

const handleThemeColorChange = (event: Event) => {
  const value = (event.target as HTMLInputElement).value
  preferenceStore.setThemeColor(value)
  Message.success('主题配色已保存')
}

const handleResetTheme = () => {
  preferenceStore.resetThemeColor()
  Message.success('已恢复默认主题色')
}

const handleShowTabsChange = (value: string | number | boolean) => {
  preferenceStore.setShowTabs(Boolean(value))
  Message.success(Boolean(value) ? '已显示菜单标签页' : '已隐藏菜单标签页')
}

const handleTabClose = (path: string) => {
  const index = tabs.value.findIndex(t => t.path === path)
  if (index > -1 && tabs.value.length > 1) {
    tabs.value.splice(index, 1)
    if (activeTab.value === path) {
      const newIndex = Math.max(0, index - 1)
      router.push(tabs.value[newIndex].path)
    }
  }
}

const handleLogout = async () => {
  try {
    await request.post('/user/logout', {
      accessToken: authStore.accessToken,
      refreshToken: authStore.refreshToken
    })
  } catch {
    // 忽略退出接口错误
  }
  menuStore.resetMenus()
  resetDynamicRoutes()
  authStore.logout()
  Message.success('已退出登录')
  await router.push('/login')
}

onMounted(async () => {
  window.addEventListener(SITE_MESSAGE_READ_EVENT, loadUnreadCount)
  if (authStore.isLoggedIn && !authStore.userInfo) {
    await authStore.fetchUserInfo()
  }
  if (authStore.isLoggedIn) {
    await loadUnreadCount()
    connectSiteMessageSocket()
  }
})

onUnmounted(() => {
  window.removeEventListener(SITE_MESSAGE_READ_EVENT, loadUnreadCount)
  closeSiteMessageSocket()
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
  overflow: hidden;
}

.layout-main {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  padding: 0 20px;
  background: var(--app-header-bg);
  box-shadow: var(--app-shadow);
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  color: var(--app-header-text);
  font-size: 16px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;
}

.user-info:hover {
  background: var(--app-hover-bg);
}

.username {
  color: var(--app-header-text);
  font-size: 14px;
}

.header-avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.settings-block {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.settings-title {
  color: var(--app-header-text);
  font-size: 16px;
  font-weight: 600;
}

.settings-desc {
  color: var(--app-muted-text);
  font-size: 13px;
  line-height: 1.6;
}

.theme-preview-card {
  padding: 16px 18px;
  border: 1px solid var(--app-border-color);
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(var(--primary-6), 0.08), rgba(var(--primary-6), 0.16));
}

.theme-preview-header {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--app-muted-text);
  font-size: 14px;
}

.theme-preview-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  box-shadow: 0 0 0 4px rgba(var(--primary-6), 0.12);
}

.theme-preview-value {
  margin-top: 10px;
  color: var(--app-header-text);
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.theme-preset-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(52px, 1fr));
  gap: 12px;
}

.theme-swatch {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border: none;
  border-radius: 14px;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  box-shadow: 0 10px 18px rgba(15, 23, 42, 0.12);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.theme-swatch:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 24px rgba(15, 23, 42, 0.16);
}

.theme-swatch.active {
  box-shadow: 0 0 0 3px rgba(var(--primary-6), 0.22), 0 14px 24px rgba(15, 23, 42, 0.16);
}

.theme-custom-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.theme-color-picker {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border: 1px solid var(--app-border-color);
  border-radius: 10px;
  background: var(--app-panel-bg);
  color: var(--app-muted-text);
  cursor: pointer;
}

.theme-color-picker input {
  width: 44px;
  height: 32px;
  border: none;
  padding: 0;
  background: transparent;
  cursor: pointer;
}

.preference-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border: 1px solid var(--app-border-color);
  border-radius: 14px;
  background: var(--app-panel-bg);
}

.preference-label {
  color: var(--app-header-text);
  font-size: 14px;
  font-weight: 600;
}

.preference-tip {
  margin-top: 6px;
  color: var(--app-muted-text);
  font-size: 12px;
  line-height: 1.6;
}

.tab-bar {
  flex-shrink: 0;
  background: var(--app-panel-bg);
  padding: 12px 20px;
  border-bottom: 1px solid var(--app-border-color);
}

.layout-sider {
  background: var(--app-sider-bg);
  min-height: 0;
  border-right: 1px solid var(--app-border-color);
  overflow: hidden;
}

.layout-sider :deep(.arco-layout-sider-children) {
  height: 100%;
  overflow: hidden;
}

.menu-scroll {
  display: block;
  width: 100%;
  height: 100%;
  overflow: auto;
}

.menu-scroll :deep(.arco-spin-container) {
  min-height: 100%;
}

.layout-sider :deep(.arco-menu-item),
.layout-sider :deep(.arco-menu-trigger-title) {
  color: #666;
}

.layout-sider :deep(.arco-menu-item:hover) {
  color: rgb(var(--primary-6));
  background: rgba(var(--primary-6), 0.08);
}

.layout-sider :deep(.arco-menu-item.arco-menu-selected) {
  color: rgb(var(--primary-6));
  background: rgba(var(--primary-6), 0.1);
}

.layout-content {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: var(--app-content-bg);
}

.content-wrapper {
  flex: 1;
  min-height: 0;
  padding: 16px;
  overflow: auto;
}
</style>
