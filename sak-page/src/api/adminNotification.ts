import request from './request'

export interface NotificationRecipient {
  id: number
  username: string
  nickName: string
  email?: string
  wxPusherUid?: string
  status: string
}

export interface SendNotificationPayload {
  sendAll: boolean
  userIds?: number[]
  channels: string[]
  title: string
  content: string
}

export interface SendNotificationResult {
  recordId?: number
  recipientCount: number
  successUserCount: number
  channelSuccessCounts: Record<string, number>
  channelSkipCounts: Record<string, number>
}

export interface NotificationRecordItem {
  id: number
  senderName: string
  title: string
  content: string
  channels: string[]
  sendAll: boolean
  recipientCount: number
  successUserCount: number
  status: string
  channelSuccessCounts?: Record<string, number>
  channelSkipCounts?: Record<string, number>
  createTime?: string
}

export interface NotificationRecordDetail extends NotificationRecordItem {
  recipientDetails: Array<{
    userId?: number
    username?: string
    nickName?: string
    status?: string
    channels: Array<{
      channelCode: string
      status: string
      message?: string
    }>
  }>
}

export interface NotificationRecordQuery {
  title?: string
  senderName?: string
  channelCode?: string
  status?: string
  pageNum?: number
  pageSize?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

export const getNotificationRecipients = () =>
  request.get<unknown, NotificationRecipient[]>('/system/notifications/recipient-options')

export const sendNotification = (data: SendNotificationPayload) =>
  request.post<unknown, SendNotificationResult>('/system/notifications/send', data)

export const getNotificationRecords = (params: NotificationRecordQuery) =>
  request.get<unknown, PageResult<NotificationRecordItem>>('/system/notifications/records', { params })

export const getNotificationRecordDetail = (id: number) =>
  request.get<unknown, NotificationRecordDetail>(`/system/notifications/records/${id}`)
