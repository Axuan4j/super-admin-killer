import request from './request'

export interface MenuItem {
    id: number
    path: string
    name: string
    component?: string
    icon?: string
    layout?: 'default' | 'blank'  // default=侧边栏菜单, blank=空白布局
    perms?: string
    children?: MenuItem[]
}

export const getMenus = () => {
    return request.get<unknown, MenuItem[]>('/menus/current')
}
