import { defineStore } from 'pinia'
import {
  applyColorMode,
  applyThemeColor,
  DEFAULT_COLOR_MODE,
  DEFAULT_THEME_COLOR,
  normalizeColorMode,
  normalizeHexColor
} from '@/utils/theme'

interface PreferenceState {
  themeColor: string
  showTabs: boolean
  colorMode: 'light' | 'dark' | 'system'
}

export const usePreferenceStore = defineStore('preference', {
  state: (): PreferenceState => ({
    themeColor: DEFAULT_THEME_COLOR,
    showTabs: true,
    colorMode: DEFAULT_COLOR_MODE
  }),

  persist: {
    storage: localStorage
  },

  actions: {
    applyTheme() {
      this.themeColor = normalizeHexColor(this.themeColor)
      this.colorMode = normalizeColorMode(this.colorMode)
      applyThemeColor(this.themeColor)
      applyColorMode(this.colorMode)
    },

    setThemeColor(color: string) {
      this.themeColor = normalizeHexColor(color)
      applyThemeColor(this.themeColor)
    },

    resetThemeColor() {
      this.setThemeColor(DEFAULT_THEME_COLOR)
    },

    setShowTabs(value: boolean) {
      this.showTabs = value
    },

    setColorMode(value: 'light' | 'dark' | 'system') {
      this.colorMode = normalizeColorMode(value)
      applyColorMode(this.colorMode)
    }
  }
})
