<template>
  <div class="login-page">
    <section class="login-panel">
      <div class="login-panel__eyebrow">Superkiller Mobile</div>
      <h1 class="login-panel__title">移动处理台</h1>
      <p class="login-panel__desc">打通现有后台账号体系，优先处理登录、工作台和消息中心。</p>

      <form class="login-form" @submit.prevent="handleSubmit">
        <template v-if="!authStore.requiresMfa">
        <label class="field">
          <span class="field__label">账号</span>
          <input v-model.trim="form.username" class="field__input" autocomplete="username" placeholder="请输入用户名" />
        </label>

        <label class="field">
          <span class="field__label">密码</span>
          <input
            v-model="form.password"
            class="field__input"
            type="password"
            autocomplete="current-password"
            placeholder="请输入密码"
          />
        </label>

        <div class="field">
          <span class="field__label">验证码</span>
          <div class="captcha-row">
            <input v-model.trim="form.captcha" class="field__input" placeholder="请输入验证码" />
            <button class="captcha-box" type="button" @click="refreshCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="captcha" class="captcha-box__image" />
              <span v-else class="captcha-box__text">加载中</span>
            </button>
          </div>
        </div>
        </template>

        <template v-else>
          <section class="mfa-panel">
            <div class="mfa-panel__title">二级验证</div>
            <div class="mfa-panel__desc">
              账号 {{ authStore.pendingMfaUsername || form.username }} 已开启验证器，请输入 6 位动态码完成登录。
            </div>
          </section>

          <label class="field">
            <span class="field__label">动态验证码</span>
            <input
              v-model.trim="form.mfaCode"
              class="field__input"
              inputmode="numeric"
              maxlength="6"
              placeholder="请输入 6 位动态码"
            />
          </label>
        </template>

        <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>

        <button class="submit-button" type="submit" :disabled="submitting">
          {{ submitting ? '处理中...' : authStore.requiresMfa ? '验证并登录' : '登录并进入工作台' }}
        </button>

        <button v-if="authStore.requiresMfa" class="secondary-button" type="button" @click="handleBackToLogin">
          返回账号密码登录
        </button>
      </form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getCaptcha } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const submitting = ref(false)
const errorMessage = ref('')
const captchaImage = ref('')
const form = reactive({
  username: 'admin',
  password: '',
  captcha: '',
  captchaId: '',
  mfaCode: ''
})

watch(
  () => authStore.pendingMfaUsername,
  (value) => {
    if (value) {
      form.username = value
    }
  },
  { immediate: true }
)

const normalizeCaptchaImage = (value?: string) => {
  if (!value) {
    return ''
  }
  return value.startsWith('data:image') ? value : `data:image/png;base64,${value}`
}

const refreshCaptcha = async () => {
  const data = await getCaptcha()
  form.captchaId = data.captchaId
  form.captcha = ''
  captchaImage.value = normalizeCaptchaImage(data.imageBase64)
}

const handleSubmit = async () => {
  errorMessage.value = ''
  submitting.value = true
  try {
    if (authStore.requiresMfa) {
      if (!/^\d{6}$/.test(form.mfaCode)) {
        errorMessage.value = '请输入 6 位动态码'
        return
      }
      await authStore.verifyMfaLogin(form.mfaCode)
      form.mfaCode = ''
      await router.replace('/')
      return
    }

    if (!form.username || !form.password || !form.captcha || !form.captchaId) {
      errorMessage.value = '请完整填写登录信息'
      return
    }

    const result = await authStore.login({
      username: form.username,
      password: form.password,
      captcha: form.captcha,
      captchaId: form.captchaId
    })
    if (result.mfaRequired) {
      form.mfaCode = ''
      errorMessage.value = ''
      return
    }
    await router.replace('/')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请重试'
    if (!authStore.requiresMfa) {
      await refreshCaptcha()
    }
  } finally {
    submitting.value = false
  }
}

const handleBackToLogin = async () => {
  authStore.clearMfaChallenge()
  form.mfaCode = ''
  await refreshCaptcha()
}

onMounted(() => {
  authStore.syncSession()
  if (!authStore.requiresMfa) {
    refreshCaptcha()
  }
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px 16px;
}

.login-panel {
  width: 100%;
  max-width: 420px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--sak-shadow);
  padding: 24px 20px;
}

.login-panel__eyebrow {
  color: var(--sak-brand);
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.login-panel__title {
  margin: 10px 0 8px;
  font-size: 28px;
}

.login-panel__desc {
  margin: 0;
  color: var(--sak-muted);
  line-height: 1.6;
}

.login-form {
  display: grid;
  gap: 14px;
  margin-top: 20px;
}

.field {
  display: grid;
  gap: 8px;
}

.field__label {
  font-size: 13px;
  font-weight: 600;
}

.field__input {
  width: 100%;
  border: 1px solid var(--sak-border);
  border-radius: 16px;
  background: #fff;
  color: var(--sak-text);
  font-size: 15px;
  padding: 14px 16px;
}

.captcha-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 112px;
  gap: 10px;
}

.captcha-box {
  border: 1px solid var(--sak-border);
  border-radius: 16px;
  background: #f8fafc;
  padding: 6px;
  min-height: 50px;
}

.captcha-box__image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.captcha-box__text {
  color: var(--sak-muted);
  font-size: 12px;
}

.error-message {
  color: var(--sak-danger);
  font-size: 13px;
}

.mfa-panel {
  border-radius: 18px;
  background: var(--sak-brand-soft);
  padding: 14px 16px;
}

.mfa-panel__title {
  color: var(--sak-brand);
  font-size: 15px;
  font-weight: 700;
}

.mfa-panel__desc {
  margin-top: 6px;
  color: var(--sak-muted);
  font-size: 13px;
  line-height: 1.6;
}

.submit-button {
  border: 0;
  border-radius: 18px;
  background: var(--sak-brand);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  padding: 15px 18px;
}

.submit-button:disabled {
  opacity: 0.6;
}

.secondary-button {
  border: 1px solid var(--sak-border);
  border-radius: 18px;
  background: #fff;
  color: var(--sak-text);
  font-size: 14px;
  font-weight: 600;
  padding: 14px 18px;
}
</style>
