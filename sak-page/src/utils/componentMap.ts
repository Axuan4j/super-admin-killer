// @ts-ignore
import type { Component } from 'vue'

// 自动扫描 src/views 下的所有 .vue 文件
// @ts-ignore
const modules = import.meta.glob('@/views/**/*.vue')

// 从文件路径提取路由路径
// @/views/Dashboard.vue -> /dashboard
// @/views/system/Users.vue -> /system/users
const extractPath = (path: string): string => {
  const match = path.match(/\/views\/(.+)\.vue$/)
  return match ? `/${match[1].toLowerCase()}` : ''
}

// 构建 componentMap
const componentMap: Record<string, () => Promise<Component>> = {}

Object.keys(modules).forEach(path => {
  const routePath = extractPath(path)
  if (routePath) {
    componentMap[routePath] = modules[path] as () => Promise<Component>
  }
})

export const getComponent = (path: string) => {
  // @ts-ignore
  return componentMap[path] || (() => import('@/views/NotFound.vue'))
}

export { componentMap }

