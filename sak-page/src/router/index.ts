import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import { getComponent } from '@/utils/componentMap'
import { getIcon } from '@/utils/menuIconMap'

// 静态路由
const staticRoutes: RouteRecordRaw[] = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/Login.vue'),
        meta: {requiresAuth: false}
    },
    {
        path: '/layout',
        name: 'Layout',
        component: () => import('@/views/layout/DefaultLayout.vue'),
        children: [
            {
                path: 'site-messages',
                name: 'SiteMessages',
                component: () => import('@/views/SiteMessages.vue'),
                meta: {
                    title: '站内消息'
                }
            }
        ],
        redirect: '/layout/dashboard'
    },
    {
        path: '/blank',
        name: 'BlankLayout',
        component: () => import('@/views/layout/BlankLayout.vue'),
        children: []
    }
]

let isRoutesGenerated = false
const dynamicRouteNames = new Set<string>()

const router = createRouter({
    history: createWebHistory(),
    routes: staticRoutes
})

// 构建嵌套路由
const buildNestedRoutes = (menus: any[]): RouteRecordRaw[] => {
    return menus.map(menu => {
        const hasChildren = menu.children?.length > 0

        const route: RouteRecordRaw = {
            children: [],
            component: undefined,
            redirect: undefined,
            path: menu.path,
            name: menu.name,
            meta: {
                title: menu.name,
                icon: getIcon(menu.icon),
                permission: menu.perms
            }
        }

        if (menu.component || !hasChildren) {
            route.component = getComponent(menu.component || menu.path)
        }

        if (hasChildren) {
            route.children = buildNestedRoutes(menu.children)
        }

        return route
    })
}

// 动态添加菜单路由
export const generateRoutes = (menus: any[]) => {
    if (isRoutesGenerated) return

    const defaultRoutes = buildNestedRoutes(menus.filter(m => m.layout !== 'blank'))
    const blankRoutes = buildNestedRoutes(menus.filter(m => m.layout === 'blank'))

    // 添加到对应布局
    defaultRoutes.forEach(route => {
        router.addRoute('Layout', route)
        if (route.name) {
            dynamicRouteNames.add(String(route.name))
        }
    })

    blankRoutes.forEach(route => {
        router.addRoute('BlankLayout', route)
        if (route.name) {
            dynamicRouteNames.add(String(route.name))
        }
    })

    // 添加 404 路由
    router.addRoute('Layout', {
        path: '/:pathMatch(.*)*',
        name: 'NotFound',
        component: () => import('@/views/NotFound.vue'),
        meta: {requiresAuth: false}
    })

    isRoutesGenerated = true
}

export const resetDynamicRoutes = () => {
    dynamicRouteNames.forEach(name => {
        if (router.hasRoute(name)) {
            router.removeRoute(name)
        }
    })
    dynamicRouteNames.clear()
    isRoutesGenerated = false
}

// 路由守卫
router.beforeEach(async (to) => {
    const authStore = useAuthStore()
    const menuStore = useMenuStore()

    // 未登录，跳转登录
    if (to.meta.requiresAuth !== false && !authStore.isLoggedIn) {
        return '/login'
    }

    // 已登录访问登录页，先校验当前 token 是否仍然有效
    if (to.path === '/login' && authStore.isLoggedIn) {
        try {
            await authStore.fetchUserInfo()
        } catch {
            menuStore.resetMenus()
            resetDynamicRoutes()
            authStore.logout()
            return true
        }

        if (!authStore.isLoggedIn) {
            return true
        }

        return '/layout/dashboard'
    }

    // 根路径跳转
    if (to.path === '/') {
        return '/layout/dashboard'
    }

    // 加载动态菜单并生成路由
    if (authStore.isLoggedIn && !isRoutesGenerated) {
        try {
            await menuStore.loadMenus()
            if (menuStore.menus.length > 0) {
                generateRoutes(menuStore.menus)
                // 路由生成后，重新导航到当前路径
                return router.replace(to.fullPath)
            }
        } catch {
            menuStore.resetMenus()
            resetDynamicRoutes()
            authStore.logout()
            return '/login'
        }

        return '/login'
    }

    if (typeof to.meta.permission === 'string' && to.meta.permission && !authStore.hasPermission(to.meta.permission as string)) {
        return '/layout/dashboard'
    }
})

export default router
