import request from './request'

export interface NotificationRecipient {
  id: number
  username: string
  nickName: string
  email?: string
  status: string
}

export interface SendNotificationPayload {
  sendAll: boolean
  userIds?: number[]
  title: string
  content: string
}

export interface SendNotificationResult {
  successCount: number
}

export const getNotificationRecipients = () =>
  request.get<unknown, NotificationRecipient[]>('/system/notifications/recipient-options')

export const sendNotification = (data: SendNotificationPayload) =>
  request.post<unknown, SendNotificationResult>('/system/notifications/send', data)
