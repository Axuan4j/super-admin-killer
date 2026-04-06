import type { SiteMessage } from '@/api/siteMessage'

export const SITE_MESSAGE_READ_EVENT = 'site-messages-read'
export const SITE_MESSAGE_PUSH_EVENT = 'site-message-pushed'

export interface SiteMessageSocketPayload {
  type: 'UNREAD_SYNC' | 'NEW_MESSAGE' | 'FORCE_LOGOUT'
  unreadCount: number
  message?: SiteMessage | null
  text?: string | null
}
