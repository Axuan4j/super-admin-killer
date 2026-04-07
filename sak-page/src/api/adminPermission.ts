import request from './request'

export const refreshPermissionCache = () => request.post('/system/menus/refresh-cache')
