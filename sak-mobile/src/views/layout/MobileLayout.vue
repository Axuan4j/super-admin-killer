<template>
  <div class="mobile-shell">
    <main class="mobile-main">
      <RouterView />
    </main>

    <nav class="mobile-tabbar">
      <button
        v-for="item in tabs"
        :key="item.value"
        class="mobile-tabbar__item"
        :class="{ 'is-active': currentTab === item.value }"
        type="button"
        @click="navigate(item.to)"
      >
        <span class="mobile-tabbar__label">{{ item.label }}</span>
      </button>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const tabs = [
  { label: '工作台', value: 'workbench', to: '/' },
  { label: '告警', value: 'alerts', to: '/alerts' },
  { label: '消息', value: 'messages', to: '/messages' },
  { label: '我的', value: 'me', to: '/me' }
]

const currentTab = computed(() => String(route.meta.tab ?? 'workbench'))

const navigate = (to: string) => {
  if (route.path !== to) {
    router.push(to)
  }
}
</script>

<style scoped>
.mobile-shell {
  min-height: 100vh;
  padding: env(safe-area-inset-top) 0 calc(84px + env(safe-area-inset-bottom));
}

.mobile-main {
  min-height: calc(100vh - 84px);
}

.mobile-tabbar {
  position: fixed;
  left: 12px;
  right: 12px;
  bottom: calc(12px + env(safe-area-inset-bottom));
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(16px);
  box-shadow: var(--sak-shadow);
}

.mobile-tabbar__item {
  border: 0;
  border-radius: 16px;
  background: transparent;
  color: var(--sak-muted);
  font-size: 13px;
  font-weight: 600;
  padding: 10px 6px;
}

.mobile-tabbar__item.is-active {
  background: var(--sak-brand-soft);
  color: var(--sak-brand);
}

.mobile-tabbar__label {
  display: block;
  text-align: center;
}
</style>
