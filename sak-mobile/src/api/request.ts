import axios from 'axios'
import type { AxiosError, AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import router from '@/router'

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
  skipAuthRefresh?: boolean
}

interface RefreshTokenResult {
  accessToken: string
  refreshToken: string
}

const baseUri = import.meta.env.VITE_API_BASE_URL || 'http://192.168.0.2:8080'
const clientDevice = 'mobile'

const request: AxiosInstance = axios.create({
  baseURL: baseUri,
  timeout: 10000
})

let refreshPromise: Promise<string | null> | null = null
let redirectingToLogin = false

const clearAuthStorage = () => {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
}

const redirectToLogin = async () => {
  clearAuthStorage()
  const { useAuthStore } = await import('@/stores/auth')
  useAuthStore().logout()

  if (redirectingToLogin || router.currentRoute.value.path === '/login') {
    return
  }

  redirectingToLogin = true
  try {
    await router.replace('/login')
  } finally {
    redirectingToLogin = false
  }
}

const refreshAccessToken = async (): Promise<string | null> => {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) {
    return null
  }

  try {
    const response = await axios.post(`${baseUri}/user/refresh`, { refreshToken }, {
      headers: {
        'Content-Type': 'application/json',
        'X-Client-Device': clientDevice
      },
      timeout: 10000
    })
    const payload = response.data as { code: number; data?: RefreshTokenResult }
    if (payload.code === 200 && payload.data?.accessToken && payload.data?.refreshToken) {
      localStorage.setItem('accessToken', payload.data.accessToken)
      localStorage.setItem('refreshToken', payload.data.refreshToken)
      const { useAuthStore } = await import('@/stores/auth')
      useAuthStore().syncSession()
      return payload.data.accessToken
    }
    return null
  } catch {
    return null
  }
}

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const accessToken = localStorage.getItem('accessToken')
    if (accessToken && config.headers) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }
    if (config.headers) {
      config.headers['X-Client-Device'] = clientDevice
    }
    return config
  },
  (error: AxiosError) => Promise.reject(error)
)

request.interceptors.response.use(
  (response: AxiosResponse) => {
    const payload = response.data
    if (payload?.code === 200) {
      return payload.data
    }
    return Promise.reject(payload ?? response)
  },
  async (error: AxiosError<{ code?: number; message?: string }>) => {
    const response = error.response
    const config = error.config as RetryableRequestConfig | undefined

    if (response?.status === 401) {
      const refreshToken = localStorage.getItem('refreshToken')
      const isRefreshRequest = config?.url?.includes('/user/refresh')

      if (!refreshToken || isRefreshRequest || config?.skipAuthRefresh || config?._retry) {
        await redirectToLogin()
        return Promise.reject(error)
      }

      if (!refreshPromise) {
        refreshPromise = refreshAccessToken()
          .catch(() => null)
          .finally(() => {
            refreshPromise = null
          })
      }

      const newAccessToken = await refreshPromise
      if (newAccessToken && config?.headers) {
        config._retry = true
        config.headers.Authorization = `Bearer ${newAccessToken}`
        return request(config)
      }

      await redirectToLogin()
    }

    return Promise.reject(error)
  }
)

export default request
