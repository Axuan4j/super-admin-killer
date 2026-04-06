import { defineStore } from 'pinia'
import type { MenuItem } from '../api/menu'
import { getMenus } from '../api/menu'
import { getIcon } from '../utils/menuIconMap'

interface MenuWithMeta extends MenuItem {
  meta?: {
    title: string
    icon: { prefix: string; iconName: string }
  }
  children?: MenuWithMeta[]
}

interface MenuState {
  menus: MenuWithMeta[]
  loaded: boolean
  loading: boolean
}

// 处理菜单，添加 meta 信息
const processMenus = (menus: MenuItem[]): MenuWithMeta[] => {
  return menus.map(menu => {
    const processed: MenuWithMeta = {
      ...menu,
      meta: {
        title: menu.name,
        icon: getIcon(menu.icon || '')
      }
    }
    if (menu.children?.length) {
      processed.children = processMenus(menu.children)
    }
    return processed
  })
}

export const useMenuStore = defineStore('menu', {
  state: (): MenuState => ({
    menus: [],
    loaded: false,
    loading: false
  }),

  actions: {
    async loadMenus() {
      if (this.loaded || this.loading) return
      this.loading = true
      try {
        const rawMenus = await getMenus()
        this.menus = processMenus(rawMenus)
        this.loaded = true
      } catch (error) {
        console.error('Failed to load menus:', error)
        this.menus = []
        this.loaded = false
        throw error
      } finally {
        this.loading = false
      }
    },

    resetMenus() {
      this.menus = []
      this.loaded = false
      this.loading = false
    }
  }
})
