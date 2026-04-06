import { defineStore } from 'pinia'
import { getAllConfigs } from '../api/config'
import type { SysConfig } from '../api/config'

interface ConfigState {
  configs: SysConfig
  loaded: boolean
}

export const useConfigStore = defineStore('config', {
  state: (): ConfigState => ({
    configs: {},
    loaded: false
  }),

  getters: {
    getConfig: (state) => (key: string): string => {
      return state.configs[key] || ''
    }
  },

  actions: {
    async loadConfigs() {
      if (this.loaded) return
      try {
        const data = await getAllConfigs()
        this.configs = data
        this.loaded = true
      } catch (error) {
        console.error('Failed to load configs:', error)
      }
    }
  }
})
