import request from './request'

export interface OnlineSessionItem {
  sessionId: string
  userId?: number
  username: string
  nickName: string
  ip?: string
  userAgent?: string
  loginTime?: string
  lastActiveTime?: string
  current: boolean
}

export const getOnlineUsers = () =>
  request.get<unknown, OnlineSessionItem[]>('/system/online-users')

export const forceLogoutOnlineUser = (sessionId: string) =>
  request.delete(`/system/online-users/${sessionId}`)
