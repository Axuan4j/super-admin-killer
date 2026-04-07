import router, { generateRoutes, resetDynamicRoutes } from '@/router'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'

export const refreshPermissionContext = async () => {
  const authStore = useAuthStore()
  const menuStore = useMenuStore()

  if (!authStore.isLoggedIn) {
    return
  }

  await authStore.fetchUserInfo(true)
  menuStore.resetMenus()
  resetDynamicRoutes()
  await menuStore.loadMenus()

  if (menuStore.menus.length > 0) {
    generateRoutes(menuStore.menus)
  }

  const currentPath = router.currentRoute.value.path
  if (currentPath !== '/login') {
    const hasCurrentRoute = router.getRoutes().some(route => route.path === currentPath)
    if (hasCurrentRoute) {
      await router.replace(currentPath)
    } else {
      await router.replace('/layout/dashboard')
    }
  }
}
