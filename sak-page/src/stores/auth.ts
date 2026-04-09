import { defineStore } from 'pinia'
import { login as loginApi, getUserInfo, verifyMfaLogin } from '../api/auth'
import type { LoginRequest, LoginResponse, UserInfoResponse } from '../api/auth'
import { useDictStore } from './dict'

interface UserInfo {
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

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

let fetchUserInfoPromise: Promise<UserInfo | null> | null = null

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: localStorage.getItem('accessToken'),
    refreshToken: localStorage.getItem('refreshToken'),
    userInfo: null,
    pendingMfaChallengeToken: null,
    pendingMfaUsername: null
  }),

  getters: {
    isLoggedIn: (state) => !!state.accessToken && !!localStorage.getItem('accessToken'),
    requiresMfa: (state) => !!state.pendingMfaChallengeToken,
    isAdmin: (state) => state.userInfo?.roles?.includes('admin') ?? false,
    hasPermission: (state) => (permission: string) => {
      if (state.userInfo?.roles?.includes('admin')) {
        return true
      }
      if (!permission) {
        return true
      }
      return state.userInfo?.authorities?.includes(permission) ?? false
    },
    hasAnyPermission: (state) => (permissions: string[]) => {
      if (state.userInfo?.roles?.includes('admin')) {
        return true
      }
      return permissions.some(permission => state.userInfo?.authorities?.includes(permission))
    }
  },

  persist: {
    storage: localStorage
  },
  actions: {
    syncSession() {
      const accessToken = localStorage.getItem('accessToken')
      const refreshToken = localStorage.getItem('refreshToken')

      this.accessToken = accessToken
      this.refreshToken = refreshToken

      if (!accessToken) {
        this.userInfo = null
        fetchUserInfoPromise = null
      }
    },

    async login(credentials: LoginRequest) {
      const res = await loginApi(credentials)
      const payload = res.data as ApiResult<LoginResponse>

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
      return data
    },

    async verifyMfaLogin(code: string) {
      if (!this.pendingMfaChallengeToken) {
        throw new Error('二级验证状态已失效，请重新登录')
      }

      const res = await verifyMfaLogin({
        challengeToken: this.pendingMfaChallengeToken,
        code
      })
      const payload = res.data as ApiResult<LoginResponse>
      if (payload.code !== 200 || !payload.data?.accessToken || !payload.data?.refreshToken) {
        throw new Error(payload.message || '二级验证失败')
      }

      await this.applyLoginTokens(payload.data)
      this.clearMfaChallenge()
      return payload.data
    },

    async applyLoginTokens(data: LoginResponse) {
      const dictStore = useDictStore()
      this.accessToken = data.accessToken ?? null
      this.refreshToken = data.refreshToken ?? null
      this.userInfo = null
      fetchUserInfoPromise = null
      localStorage.setItem('accessToken', data.accessToken!)
      localStorage.setItem('refreshToken', data.refreshToken!)
      await this.fetchUserInfo()
      await dictStore.loadDictionaries(true)
    },

    clearMfaChallenge() {
      this.pendingMfaChallengeToken = null
      this.pendingMfaUsername = null
    },

    async fetchUserInfo(force = false) {
      if (!this.accessToken) {
        return null
      }

      if (this.userInfo && !force) {
        return this.userInfo
      }

      if (force) {
        this.userInfo = null
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
    }
  }
})
