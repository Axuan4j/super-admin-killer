import { defineStore } from 'pinia'
import {
  getUserInfo,
  login as loginApi,
  verifyMfaLogin,
  type LoginRequest,
  type LoginResponse,
  type UserInfoResponse
} from '@/api/auth'
import type { ApiResult } from '@/types/api'

export interface UserInfo {
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

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  userInfo: UserInfo | null
  pendingMfaChallengeToken: string | null
  pendingMfaUsername: string | null
}

let fetchUserInfoPromise: Promise<UserInfo | null> | null = null

export const useAuthStore = defineStore('mobile-auth', {
  state: (): AuthState => ({
    accessToken: localStorage.getItem('accessToken'),
    refreshToken: localStorage.getItem('refreshToken'),
    userInfo: null,
    pendingMfaChallengeToken: null,
    pendingMfaUsername: null
  }),
  getters: {
    isLoggedIn: (state) => !!state.accessToken && !!localStorage.getItem('accessToken'),
    requiresMfa: (state) => !!state.pendingMfaChallengeToken
  },
  actions: {
    syncSession() {
      this.accessToken = localStorage.getItem('accessToken')
      this.refreshToken = localStorage.getItem('refreshToken')
      if (!this.accessToken) {
        this.userInfo = null
        fetchUserInfoPromise = null
      }
    },
    async login(credentials: LoginRequest) {
      const response = await loginApi(credentials)
      const payload = response.data as ApiResult<LoginResponse>
      if (payload.code !== 200 || !payload.data) {
        throw new Error(payload.message || '登录失败')
      }

      const data = payload.data
      this.clearMfaChallenge()

      if (data.mfaRequired && data.challengeToken) {
        this.accessToken = null
        this.refreshToken = null
        this.userInfo = null
        fetchUserInfoPromise = null
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        this.pendingMfaChallengeToken = data.challengeToken
        this.pendingMfaUsername = data.username
        return data
      }

      if (!data.accessToken || !data.refreshToken) {
        throw new Error(payload.message || '登录失败')
      }

      await this.applyLoginTokens(data)
      return payload.data
    },
    async verifyMfaLogin(code: string) {
      if (!this.pendingMfaChallengeToken) {
        throw new Error('二级验证状态已失效，请重新登录')
      }

      const response = await verifyMfaLogin({
        challengeToken: this.pendingMfaChallengeToken,
        code
      })
      const payload = response.data as ApiResult<LoginResponse>
      if (payload.code !== 200 || !payload.data?.accessToken || !payload.data?.refreshToken) {
        throw new Error(payload.message || '二级验证失败')
      }

      await this.applyLoginTokens(payload.data)
      this.clearMfaChallenge()
      return payload.data
    },
    async applyLoginTokens(data: LoginResponse) {
      this.accessToken = data.accessToken ?? null
      this.refreshToken = data.refreshToken ?? null
      this.userInfo = null
      this.clearMfaChallenge()
      fetchUserInfoPromise = null
      if (data.accessToken) {
        localStorage.setItem('accessToken', data.accessToken)
      }
      if (data.refreshToken) {
        localStorage.setItem('refreshToken', data.refreshToken)
      }
      await this.fetchUserInfo(true)
    },
    async fetchUserInfo(force = false) {
      if (!this.accessToken) {
        return null
      }

      if (this.userInfo && !force) {
        return this.userInfo
      }

      if (fetchUserInfoPromise) {
        return fetchUserInfoPromise
      }

      fetchUserInfoPromise = getUserInfo()
        .then((data: UserInfoResponse) => {
          this.userInfo = {
            id: data.id,
            username: data.username || '',
            nickName: data.nickName || data.username || '',
            email: data.email,
            wxPusherUid: data.wxPusherUid,
            phone: data.phone,
            avatar: data.avatar,
            mfaEnabled: data.mfaEnabled,
            roles: data.roles || [],
            authorities: data.authorities || []
          }
          return this.userInfo
        })
        .finally(() => {
          fetchUserInfoPromise = null
        })

      return fetchUserInfoPromise
    },
    logout() {
      this.accessToken = null
      this.refreshToken = null
      this.userInfo = null
      this.clearMfaChallenge()
      fetchUserInfoPromise = null
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
    },
    clearMfaChallenge() {
      this.pendingMfaChallengeToken = null
      this.pendingMfaUsername = null
    }
  }
})
