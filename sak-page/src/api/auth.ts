import request from './request'
import axios from 'axios'

export interface LoginRequest {
  username: string
  password: string
  captcha: string
  captchaId: string
}

export interface LoginResponse {
  accessToken?: string
  refreshToken?: string
  username: string
  mfaRequired?: boolean
  challengeToken?: string
}

export interface CaptchaResponse {
  captchaId: string
  imageBase64: string
}

export interface UserInfoResponse {
  id: number
  username: string
  nickName: string
  email?: string
  wxPusherUid?: string
  phone?: string
  avatar?: string
  mfaEnabled?: boolean
  roles: string[]
  authorities: string[]
}

export interface MfaSetupResponse {
  issuer: string
  accountName: string
  secret: string
  otpauthUri: string
  qrCodeBase64: string
}

export interface UpdateProfilePayload {
  nickName: string
  email?: string
  wxPusherUid?: string
  phone?: string
  avatar?: string
}

export interface UpdatePasswordPayload {
  oldPassword: string
  newPassword: string
}

export interface AvatarUploadResponse {
  avatarUrl: string
  fileName: string
}

export interface MfaCodePayload {
  code: string
}

export interface MfaVerifyLoginPayload extends MfaCodePayload {
  challengeToken: string
}

export const login = (data: LoginRequest) => {
  const params = new URLSearchParams()
  params.append('username', data.username)
  params.append('password', data.password)
  params.append('captcha', data.captcha)
  params.append('captchaId', data.captchaId)
  return axios.post('/auth/login', params, {
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}

export const getCaptcha = () => {
  return request.get<unknown, CaptchaResponse>('/auth/captcha')
}

export const getUserInfo = () => {
  return request.get<unknown, UserInfoResponse>('/user/info')
}

export const verifyMfaLogin = (data: MfaVerifyLoginPayload) => {
  return axios.post('/auth/mfa/verify', data, {
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

export const setupMfa = () => {
  return request.post<unknown, MfaSetupResponse>('/user/mfa/setup')
}

export const enableMfa = (data: MfaCodePayload) => {
  return request.post('/user/mfa/enable', data)
}

export const disableMfa = (data: MfaCodePayload) => {
  return request.post('/user/mfa/disable', data)
}

export const updateProfile = (data: UpdateProfilePayload) => {
  return request.put<unknown, UserInfoResponse>('/user/profile', data)
}

export const updatePassword = (data: UpdatePasswordPayload) => {
  return request.put('/user/password', data)
}

export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<unknown, AvatarUploadResponse>('/user/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
