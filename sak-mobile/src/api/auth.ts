import axios from 'axios'
import request from '@/api/request'
import type { ApiResult } from '@/types/api'

const baseUri = import.meta.env.VITE_API_BASE_URL || 'http://192.168.0.2:8080'
const clientDevice = 'mobile'

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

export interface MfaVerifyLoginPayload {
  challengeToken: string
  code: string
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

export interface UpdateProfilePayload {
  nickName: string
  email?: string
  wxPusherUid?: string
  phone?: string
  avatar?: string
}

export interface AvatarUploadResponse {
  avatarUrl: string
  fileName: string
}

export interface UpdatePasswordPayload {
  oldPassword: string
  newPassword: string
}

export const login = (data: LoginRequest) => {
  const params = new URLSearchParams()
  params.append('username', data.username)
  params.append('password', data.password)
  params.append('captcha', data.captcha)
  params.append('captchaId', data.captchaId)
  return axios.post<ApiResult<LoginResponse>>('/auth/login', params, {
    baseURL: baseUri,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'X-Client-Device': clientDevice
    }
  })
}

export const getCaptcha = () => request.get<unknown, CaptchaResponse>('/auth/captcha')

export const getUserInfo = () => request.get<unknown, UserInfoResponse>('/user/info')

export const verifyMfaLogin = (data: MfaVerifyLoginPayload) =>
  axios.post<ApiResult<LoginResponse>>('/auth/mfa/verify', data, {
    baseURL: baseUri,
    headers: {
      'Content-Type': 'application/json',
      'X-Client-Device': clientDevice
    }
  })

export const updateProfile = (data: UpdateProfilePayload) =>
  request.put<unknown, UserInfoResponse>('/user/profile', data)

export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<unknown, AvatarUploadResponse>('/user/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const updatePassword = (data: UpdatePasswordPayload) =>
  request.put('/user/password', data)
