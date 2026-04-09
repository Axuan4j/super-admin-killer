<template>
  <div class="login-container">
    <div class="login-bg">
      <div class="bg-circle circle-1"></div>
      <div class="bg-circle circle-2"></div>
      <div class="bg-circle circle-3"></div>
    </div>
    <div class="login-card">
      <div class="login-header">
        <div class="logo">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/>
            <path d="M2 17l10 5 10-5"/>
            <path d="M2 12l10 5 10-5"/>
          </svg>
        </div>
        <h1>{{ env.APP_TITLE }}</h1>
      </div>
        <a-form ref="formRef" :model="formData" class="login-form" auto-label-width>
        <template v-if="!authStore.requiresMfa">
        <a-form-item field="username" validate-trigger="blur" :rules="[{ required: true, message: '请输入用户名' }]">
          <a-input
              v-model="formData.username"
              placeholder="请输入用户名"
              size="large"
          >
            <template #prefix>
              <font-awesome-icon icon="fa-solid fa-user"/>
            </template>
          </a-input>
        </a-form-item>
        <a-form-item field="password" validate-trigger="blur" :rules="[{ required: true, message: '请输入密码' }]">
          <a-input-password
              v-model="formData.password"
              placeholder="请输入密码"
              size="large"
              @keyup.enter="handleLogin"
          >
            <template #prefix>
              <font-awesome-icon icon="fa-solid fa-lock"/>
            </template>
          </a-input-password>
        </a-form-item>
        <a-form-item field="captcha" validate-trigger="blur" :rules="[{ required: true, message: '请输入验证码' }]">
          <div class="captcha-row">
            <a-input
                v-model="formData.captcha"
                placeholder="请输入图形验证码"
                size="large"
                @keyup.enter="handleLogin"
            />
            <div class="captcha-box" @click="fetchCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="图形验证码" class="captcha-image" />
              <span v-else class="captcha-placeholder">加载中</span>
            </div>
          </div>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">
            登 录
          </a-button>
        </a-form-item>
        </template>
        <template v-else>
          <div class="mfa-tip">
            <div class="mfa-title">二级验证</div>
            <div class="mfa-sub">账号 {{ authStore.pendingMfaUsername || formData.username }} 已启用验证器，请输入 6 位动态码。</div>
          </div>
          <a-form-item field="mfaCode" validate-trigger="blur" :rules="[{ required: true, message: '请输入6位动态码' }]">
            <a-input
              v-model="formData.mfaCode"
              placeholder="请输入验证器动态码"
              size="large"
              :max-length="6"
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <font-awesome-icon icon="fa-solid fa-shield-halved"/>
              </template>
            </a-input>
          </a-form-item>
          <a-form-item>
            <a-space fill direction="vertical">
              <a-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">
                验 证 并 登 录
              </a-button>
              <a-button size="large" class="secondary-btn" @click="handleBackToLogin">返回账号密码登录</a-button>
            </a-space>
          </a-form-item>
        </template>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, onUnmounted, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useAuthStore} from '@/stores'
import { usePreferenceStore } from '@/stores/preference'
import {env} from "@/utils/envMap.ts";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import {getCaptcha} from "@/api/auth.ts";
import {Message} from '@arco-design/web-vue'
import axios from 'axios'
import { applyColorMode } from '@/utils/theme'

const router = useRouter()
const authStore = useAuthStore()
const preferenceStore = usePreferenceStore()

const formRef = ref()
const loading = ref(false)
const captchaImage = ref('')

const formData = reactive({
  username: '',
  password: '',
  captcha: '',
  captchaId: '',
  mfaCode: ''
})

const fetchCaptcha = async () => {
  const captcha = await getCaptcha()
  formData.captcha = ''
  formData.captchaId = captcha.captchaId
  captchaImage.value = `data:image/png;base64,${captcha.imageBase64}`
}

const handleLogin = async () => {
  loading.value = true
  try {
    if (authStore.requiresMfa) {
      if (!/^\d{6}$/.test(formData.mfaCode)) {
        Message.warning('请输入 6 位动态码')
        return
      }
      await authStore.verifyMfaLogin(formData.mfaCode)
      formData.mfaCode = ''
      await router.push('/layout')
      return
    }

    try {
      await formRef.value?.validate()
    } catch {
      return
    }

    const result = await authStore.login(formData)
    if (result.mfaRequired) {
      formData.mfaCode = ''
      Message.success('请输入验证器中的 6 位动态码')
      return
    }
    await router.push('/layout')
  } catch (e) {
    console.error('Login failed:', e)
    if (axios.isAxiosError(e)) {
      Message.error((e.response?.data as { message?: string } | undefined)?.message || '登录失败')
    } else if (e instanceof Error) {
      Message.error(e.message)
    } else {
      Message.error('登录失败')
    }
    await fetchCaptcha()
  } finally {
    loading.value = false
  }
}

const handleBackToLogin = async () => {
  authStore.clearMfaChallenge()
  formData.mfaCode = ''
  await fetchCaptcha()
}

onMounted(() => {
  applyColorMode('light')
  fetchCaptcha()
})

onUnmounted(() => {
  preferenceStore.applyTheme()
})
</script>

<style scoped>
.login-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  overflow: hidden;
  color-scheme: light;
}

.login-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.3), rgba(118, 75, 162, 0.3));
  animation: float 20s infinite ease-in-out;
}

.circle-1 {
  width: 400px;
  height: 400px;
  top: -100px;
  left: -100px;
  animation-delay: 0s;
}

.circle-2 {
  width: 300px;
  height: 300px;
  bottom: -50px;
  right: -50px;
  animation-delay: -5s;
}

.circle-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  left: 60%;
  animation-delay: -10s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(30px, -30px) scale(1.05);
  }
  50% {
    transform: translate(0, -50px) scale(1);
  }
  75% {
    transform: translate(-30px, -30px) scale(0.95);
  }
}

.login-card {
  position: relative;
  z-index: 1;
  width: 400px;
  padding: 48px 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(10px);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  margin-bottom: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  color: white;
}

.logo svg {
  width: 40px;
  height: 40px;
}

.login-header h1 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a2e;
  letter-spacing: 2px;
}

.login-header p {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.login-form {
  margin-top: 24px;
}

.login-form :deep(.arco-input-wrapper) {
  border-radius: 8px;
}

.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-row :deep(.arco-input-wrapper) {
  width: 100%;
}

.captcha-box {
  flex-shrink: 0;
  width: 120px;
  height: 40px;
  border: 1px solid #d9dee8;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  background: #f7f8fa;
}

.captcha-image {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.captcha-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 12px;
  color: #86909c;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 8px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
}

.secondary-btn {
  width: 100%;
  height: 44px;
  border-radius: 8px;
}

.mfa-tip {
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #f2f3f5;
}

.mfa-title {
  margin-bottom: 4px;
  color: #1d2129;
  font-size: 15px;
  font-weight: 600;
}

.mfa-sub {
  color: #4e5969;
  font-size: 13px;
  line-height: 1.6;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px -5px rgba(102, 126, 234, 0.5);
}
</style>
