<template>
  <div class="settings-page">
    <a-card class="settings-shell" title="个人设置" :bordered="false">
      <a-space direction="vertical" fill :size="24">
        <section class="settings-section">
          <div class="section-head">
            <div class="section-title">主题配色</div>
            <div class="section-desc">主题色会立即应用到当前后台，并自动保存在当前浏览器。</div>
          </div>

          <div class="theme-preview">
            <div class="preview-panel">
              <div class="preview-top">
                <span class="preview-pill" :style="{ backgroundColor: preferenceStore.themeColor }"></span>
                <span class="preview-title">当前主题色</span>
              </div>
              <div class="preview-value">{{ preferenceStore.themeColor }}</div>
            </div>
            <div class="preview-actions">
              <label class="color-picker">
                <span>自定义颜色</span>
                <input :value="preferenceStore.themeColor" type="color" @change="handleColorChange" />
              </label>
              <a-button @click="handleResetTheme">恢复默认</a-button>
            </div>
          </div>

          <div class="preset-grid">
            <button
              v-for="color in themeColorPresets"
              :key="color"
              type="button"
              class="preset-swatch"
              :class="{ active: preferenceStore.themeColor === color }"
              :style="{ backgroundColor: color }"
              @click="handlePresetSelect(color)"
            >
              <font-awesome-icon v-if="preferenceStore.themeColor === color" icon="fa-solid fa-check" />
            </button>
          </div>
        </section>

        <section class="settings-section">
          <div class="section-head">
            <div class="section-title">使用说明</div>
            <div class="section-desc">主题色是个人偏好设置，不会影响其他账号。</div>
          </div>

          <a-space wrap>
            <a-tag color="arcoblue">按钮与链接</a-tag>
            <a-tag color="arcoblue" bordered>菜单高亮</a-tag>
            <a-tag color="arcoblue">标签与状态强调</a-tag>
          </a-space>
        </section>
      </a-space>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { Message } from '@arco-design/web-vue'
import { usePreferenceStore } from '@/stores/preference'
import { THEME_COLOR_PRESETS } from '@/utils/theme'

const preferenceStore = usePreferenceStore()
const themeColorPresets = THEME_COLOR_PRESETS

const handlePresetSelect = (color: string) => {
  preferenceStore.setThemeColor(color)
  Message.success('主题配色已保存')
}

const handleColorChange = (event: Event) => {
  const value = (event.target as HTMLInputElement).value
  preferenceStore.setThemeColor(value)
  Message.success('主题配色已保存')
}

const handleResetTheme = () => {
  preferenceStore.resetThemeColor()
  Message.success('已恢复默认主题色')
}
</script>

<style scoped>
.settings-page {
  min-height: 100%;
}

.settings-shell {
  max-width: 920px;
}

.settings-section {
  padding: 4px 0;
}

.section-head {
  margin-bottom: 16px;
}

.section-title {
  color: #1d2129;
  font-size: 18px;
  font-weight: 600;
}

.section-desc {
  margin-top: 6px;
  color: #86909c;
  font-size: 14px;
}

.theme-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border: 1px solid #e5e6eb;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(var(--primary-6), 0.08), rgba(var(--primary-6), 0.16));
}

.preview-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preview-top {
  display: flex;
  align-items: center;
  gap: 10px;
}

.preview-pill {
  width: 14px;
  height: 14px;
  border-radius: 999px;
  box-shadow: 0 0 0 4px rgba(var(--primary-6), 0.12);
}

.preview-title {
  color: #4e5969;
  font-size: 14px;
}

.preview-value {
  color: #1d2129;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.preview-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.color-picker {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  background: #fff;
  color: #4e5969;
  cursor: pointer;
}

.color-picker input {
  width: 44px;
  height: 32px;
  border: none;
  padding: 0;
  background: transparent;
  cursor: pointer;
}

.preset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(56px, 1fr));
  gap: 12px;
}

.preset-swatch {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border: none;
  border-radius: 14px;
  color: #fff;
  font-size: 18px;
  cursor: pointer;
  box-shadow: 0 10px 18px rgba(15, 23, 42, 0.12);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.preset-swatch:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 24px rgba(15, 23, 42, 0.16);
}

.preset-swatch.active {
  box-shadow: 0 0 0 3px rgba(var(--primary-6), 0.22), 0 14px 24px rgba(15, 23, 42, 0.16);
}
</style>
