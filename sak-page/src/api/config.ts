import request from './request'

export interface SysConfig {
  [key: string]: string
}

export const getAllConfigs = () => {
  return request.get<unknown, SysConfig>('/system/config/all')
}
