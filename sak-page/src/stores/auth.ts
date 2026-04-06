import { defineStore } from 'pinia'
import { login as loginApi, getUserInfo } from '../api/auth'
import type { LoginRequest, LoginResponse, UserInfoResponse } from '../api/auth'

interface UserInfo {
  id: number
  username: string
  nickName: string
  email?: string
  phone?: string
  avatar?: string
  roles: string[]
  authorities: string[]
}

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  userInfo: UserInfo | null
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: localStorage.getItem('accessToken'),
    refreshToken: localStorage.getItem('refreshToken'),
    userInfo: null
  }),

  getters: {
    isLoggedIn: (state) => !!state.accessToken,
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
    async login(credentials: LoginRequest) {
      const res = await loginApi(credentials)
      const payload = res.data as ApiResult<LoginResponse>

      if (payload.code !== 200 || !payload.data?.accessToken || !payload.data?.refreshToken) {
        throw new Error(payload.message || '登录失败')
      }

      const data = payload.data
      this.accessToken = data.accessToken
      this.refreshToken = data.refreshToken
      this.userInfo = {
        id: 0,
        username: data.username,
        nickName: data.username,
        roles: [],
        authorities: []
      }
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      await this.fetchUserInfo()
    },

    async fetchUserInfo() {
      if (!this.accessToken) return
      try {
        const data: UserInfoResponse = await getUserInfo()
        this.userInfo = {
          id: data.id,
          username: data.username || '',
          nickName: data.nickName || data.username || '',
          email: data.email,
          phone: data.phone,
          avatar: data.avatar,
          roles: data.roles || [],
          authorities: data.authorities || []
        }
      } catch (error) {
        this.logout()
        throw error
      }
    },

    logout() {
      this.accessToken = null
      this.refreshToken = null
      this.userInfo = null
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
    }
  }
})
