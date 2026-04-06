import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import './style.css'
import ArcoVue from '@arco-design/web-vue'
import '@arco-design/web-vue/dist/arco.css'

// Font Awesome
import { library, dom } from '@fortawesome/fontawesome-svg-core'
import { fas } from '@fortawesome/free-solid-svg-icons'
import { fab } from '@fortawesome/free-brands-svg-icons'
import { far } from '@fortawesome/free-regular-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'

import persistedState from 'pinia-plugin-persistedstate'
import { usePreferenceStore } from '@/stores/preference'
import { useAuthStore } from '@/stores/auth'
import { useDictStore } from '@/stores/dict'

// 自动导入所有图标到库
library.add(fas, fab, far)
dom.watch()

const app = createApp(App)

const piniaApp = createPinia();
piniaApp.use(persistedState)

app.use(piniaApp)
usePreferenceStore(piniaApp).applyTheme()
const authStore = useAuthStore(piniaApp)
authStore.syncSession()
if (authStore.isLoggedIn) {
  useDictStore(piniaApp).loadDictionaries().catch((error) => {
    console.error('Failed to preload dictionaries:', error)
  })
}
app.use(router)
app.use(ArcoVue)

// 注册 FontAwesomeIcon 组件
app.component('FontAwesomeIcon', FontAwesomeIcon)

app.mount('#app')
