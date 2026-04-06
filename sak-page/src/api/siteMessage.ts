import request from './request'

export interface SiteMessage {
  id: number
  userId: number
  title: string
  content: string
  senderName: string
  readStatus: number
  readTime?: string | null
  createTime: string
  updateTime: string
}

export interface SiteMessageUnreadCount {
  unreadCount: number
}

export interface SiteMessageMarkReadResult {
  updatedCount: number
}

export const getCurrentSiteMessages = () => {
  return request.get<unknown, SiteMessage[]>('/site-messages/current')
}

export const getCurrentSiteMessageUnreadCount = () => {
  return request.get<unknown, SiteMessageUnreadCount>('/site-messages/current/unread-count')
}

export const markCurrentSiteMessagesRead = () => {
  return request.post<unknown, SiteMessageMarkReadResult>('/site-messages/current/mark-read')
}
