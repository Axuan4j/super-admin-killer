import { defineStore } from 'pinia'
import { getAllDicts, type DictItem, type DictMap } from '@/api/dict'

interface DictState {
  dictMap: DictMap
  loaded: boolean
  loading: boolean
}

let loadDictPromise: Promise<DictMap> | null = null

export const useDictStore = defineStore('dict', {
  state: (): DictState => ({
    dictMap: {},
    loaded: false,
    loading: false
  }),

  persist: {
    storage: localStorage
  },

  getters: {
    getDictItems: (state) => (dictType: string): DictItem[] => state.dictMap[dictType] || [],
    getDictLabel: (state) => (dictType: string, value?: string | number | null, fallback = '-') => {
      if (value === undefined || value === null || value === '') {
        return fallback
      }
      const item = (state.dictMap[dictType] || []).find(dict => dict.value === String(value))
      return item?.label || fallback
    },
    getDictTagColor: (state) => (dictType: string, value?: string | number | null, fallback = 'gray') => {
      if (value === undefined || value === null || value === '') {
        return fallback
      }
      const item = (state.dictMap[dictType] || []).find(dict => dict.value === String(value))
      return item?.tagColor || fallback
    }
  },

  actions: {
    async loadDictionaries(force = false) {
      if (this.loaded && !force) {
        return this.dictMap
      }
      if (loadDictPromise && !force) {
        return loadDictPromise
      }

      this.loading = true
      loadDictPromise = getAllDicts()
        .then((data) => {
          this.dictMap = data || {}
          this.loaded = true
          return this.dictMap
        })
        .finally(() => {
          this.loading = false
          loadDictPromise = null
        })

      return loadDictPromise
    },

    clearDictionaries() {
      this.dictMap = {}
      this.loaded = false
      this.loading = false
      loadDictPromise = null
    }
  }
})
