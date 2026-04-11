import { createRouter, createWebHistory } from 'vue-router'
import MobileLayout from '@/views/layout/MobileLayout.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { title: '登录', public: true }
    },
    {
      path: '/',
      component: MobileLayout,
      children: [
        {
          path: '',
          name: 'workbench',
          component: () => import('@/views/workbench/WorkbenchView.vue'),
          meta: { title: '工作台', tab: 'workbench' }
        },
        {
          path: 'alerts',
          name: 'alerts',
          component: () => import('@/views/alerts/AlertsView.vue'),
          meta: { title: '待办告警', tab: 'alerts' }
        },
        {
          path: 'monitor',
          name: 'monitor',
          component: () => import('@/views/monitor/MonitorView.vue'),
          meta: { title: '监控中心', tab: 'monitor' }
        },
        {
          path: 'me',
          name: 'me',
          component: () => import('@/views/me/MeView.vue'),
          meta: { title: '我的', tab: 'me' }
        }
      ]
    }
  ]
})

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : 'Superkiller Mobile'
  document.title = `${title} | Superkiller Mobile`
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  authStore.syncSession()

  if (to.meta.public) {
    if (to.path === '/login' && authStore.isLoggedIn) {
      return '/'
    }
    return true
  }

  if (!authStore.isLoggedIn) {
    return '/login'
  }

  if (!authStore.userInfo) {
    try {
      await authStore.fetchUserInfo()
    } catch {
      authStore.logout()
      return '/login'
    }
  }

  return true
})

export default router
