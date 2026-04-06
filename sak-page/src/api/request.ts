import axios from 'axios'
import type { AxiosInstance, AxiosError, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { Message } from '@arco-design/web-vue'
import router from '../router'

const baseUri: string = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const request: AxiosInstance = axios.create({
    baseURL: baseUri,
    timeout: 10000
})

// 标记是否正在刷新 token，避免多个请求同时触发刷新
let isRefreshing = false
// 存储等待刷新完成的请求队列
let refreshSubscribers: Array<(token: string) => void> = []

const subscribeTokenRefresh = (callback: (token: string) => void) => {
    refreshSubscribers.push(callback)
}

const onTokenRefreshed = (token: string) => {
    refreshSubscribers.forEach(callback => callback(token))
    refreshSubscribers = []
}

// 刷新 access_token
const refreshAccessToken = async (): Promise<string | null> => {
    const refreshToken = localStorage.getItem('refreshToken')
    if (!refreshToken) {
        return null
    }

    try {
        const res = await axios.post(
            `${baseUri}/user/refresh`,
            {refreshToken},
            {headers: {'Content-Type': 'application/json'}}
        )
        const {code, data} = res.data
        if (code === 200 && data.accessToken) {
            const newAccessToken = data.accessToken
            localStorage.setItem('accessToken', newAccessToken)
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
        return config
    },
    (error: AxiosError) => {
        return Promise.reject(error)
    }
)

// 响应拦截器
request.interceptors.response.use(
    (response: AxiosResponse) => {
        const {code, message, data} = response.data

        if (code === 200) {
            return data
        }

        Message.error(message || '请求失败')
        return Promise.reject(response.data)
    },
    async (error: AxiosError<{ code: number; message: string }>) => {
        const {response, config} = error

        if (response && response.status === 401) {
            const refreshToken = localStorage.getItem('refreshToken')

            if (!refreshToken) {
                // 没有 refreshToken，直接跳转登录
                localStorage.removeItem('accessToken')
                localStorage.removeItem('refreshToken')
                await router.push('/login')
                return Promise.reject(error)
            }

            if (!isRefreshing) {
                isRefreshing = true

                try {
                    const newAccessToken = await refreshAccessToken()
                    isRefreshing = false

                    if (newAccessToken) {
                        onTokenRefreshed(newAccessToken)

                        // 重试当前请求
                        if (config && config.headers) {
                            config.headers.Authorization = `Bearer ${newAccessToken}`
                            return request(config)
                        }
                        return Promise.reject(error)
                    } else {
                        throw new Error('refresh failed')
                    }
                } catch {
                    isRefreshing = false
                    localStorage.removeItem('accessToken')
                    localStorage.removeItem('refreshToken')
                    await router.push('/login')
                    Message.error('会话已过期，请重新登录')
                    return Promise.reject(error)
                }
            } else {
                // 正在刷新，将请求加入队列
                return new Promise(resolve => {
                    subscribeTokenRefresh((token: string) => {
                        if (config && config.headers) {
                            config.headers.Authorization = `Bearer ${token}`
                            resolve(request(config))
                            return
                        }
                        resolve(Promise.reject(error))
                    })
                })
            }
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
