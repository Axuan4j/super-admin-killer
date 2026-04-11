import axios from 'axios'
import type { AxiosInstance, AxiosError, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { Message } from '@arco-design/web-vue'
import router from '../router'

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
    _retry?: boolean
    skipAuthRefresh?: boolean
}

const baseUri: string = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const clientDevice = 'web'
const request: AxiosInstance = axios.create({
    baseURL: baseUri,
    timeout: 10000
})

let refreshPromise: Promise<string | null> | null = null
let redirectingToLogin = false
let sessionExpiredMessageShown = false

type RefreshSubscriber = {
    resolve: (token: string) => void
    reject: (error: unknown) => void
}

let refreshSubscribers: RefreshSubscriber[] = []

type RefreshTokenResult = {
    accessToken: string
    refreshToken: string
}

const subscribeTokenRefresh = (resolve: (token: string) => void, reject: (error: unknown) => void) => {
    refreshSubscribers.push({resolve, reject})
}

const onTokenRefreshed = (token: string) => {
    refreshSubscribers.forEach(subscriber => subscriber.resolve(token))
    refreshSubscribers = []
}

const onTokenRefreshFailed = (error: unknown) => {
    refreshSubscribers.forEach(subscriber => subscriber.reject(error))
    refreshSubscribers = []
}

const clearAuthStorage = () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
}

const redirectToLogin = async (showMessage = false) => {
    clearAuthStorage()

    const { useAuthStore } = await import('../stores/auth')
    useAuthStore().logout()

    if (showMessage && !sessionExpiredMessageShown) {
        Message.error('会话已过期，请重新登录')
        sessionExpiredMessageShown = true
    }

    if (redirectingToLogin || router.currentRoute.value.path === '/login') {
        return
    }

    redirectingToLogin = true
    try {
        await router.push('/login')
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
        const res = await axios.post(
            `${baseUri}/user/refresh`,
            {refreshToken},
            {
                headers: {
                    'Content-Type': 'application/json',
                    'X-Client-Device': clientDevice
                },
                timeout: 10000
            }
        )
        const {code, data} = res.data as { code: number; data?: RefreshTokenResult }
        if (code === 200 && data?.accessToken && data?.refreshToken) {
            const newAccessToken = data.accessToken
            localStorage.setItem('accessToken', newAccessToken)
            localStorage.setItem('refreshToken', data.refreshToken)
            const { useAuthStore } = await import('../stores/auth')
            useAuthStore().syncSession()
            sessionExpiredMessageShown = false
            return newAccessToken
        }
        return null
    } catch {
        return null
    }
}

// 请求拦截器
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
    (error: AxiosError) => {
        return Promise.reject(error)
    }
)

// 响应拦截器
request.interceptors.response.use(
    (response: AxiosResponse) => {
        sessionExpiredMessageShown = false
        const {code, message, data} = response.data

        if (code === 200) {
            return data
        }

        Message.error(message || '请求失败')
        return Promise.reject(response.data)
    },
    async (error: AxiosError<{ code: number; message: string }>) => {
        const {response} = error
        const config = error.config as RetryableRequestConfig | undefined

        if (response && response.status === 401) {
            const refreshToken = localStorage.getItem('refreshToken')
            const isRefreshRequest = config?.url?.includes('/user/refresh')

            if (!refreshToken || isRefreshRequest || config?.skipAuthRefresh || config?._retry) {
                await redirectToLogin(true)
                return Promise.reject(error)
            }

            if (!refreshPromise) {
                refreshPromise = refreshAccessToken()
                    .then(newAccessToken => {
                        if (newAccessToken) {
                            onTokenRefreshed(newAccessToken)
                            return newAccessToken
                        }
                        throw new Error('refresh failed')
                    })
                    .catch(async (refreshError) => {
                        onTokenRefreshFailed(refreshError)
                        await redirectToLogin(true)
                        return null
                    })
                    .finally(() => {
                        refreshPromise = null
                    })
            }

            try {
                const newAccessToken = await refreshPromise

                if (newAccessToken && config && config.headers) {
                    config._retry = true
                    config.headers.Authorization = `Bearer ${newAccessToken}`
                    return request(config)
                }

                return Promise.reject(error)
            } catch (refreshError) {
                return Promise.reject(refreshError)
            }
        }

        if (refreshPromise && config && !config.skipAuthRefresh && !config._retry) {
            return new Promise((resolve, reject) => {
                subscribeTokenRefresh(
                    (token: string) => {
                        if (config && config.headers) {
                            config._retry = true
                            config.headers.Authorization = `Bearer ${token}`
                            resolve(request(config))
                            return
                        }
                        reject(error)
                    },
                    reject
                )
            })
        }

        if (response) {
            const {code, message} = response.data || {}

            switch (code) {
                case 403:
                    Message.error(message || '权限不足')
                    break
                default:
                    if (code !== 200) {
                        Message.error(message || '请求失败')
                    }
            }
        } else {
            Message.error('网络错误，请检查连接')
        }

        return Promise.reject(error)
    }
)

export default request
