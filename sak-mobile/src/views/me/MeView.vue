<template>
  <div class="page">
    <section class="profile-hero">
      <button class="avatar-button" type="button" @click="triggerAvatarSelect">
        <img v-if="avatarUrl" :src="avatarUrl" alt="avatar" class="avatar-image" />
        <span v-else class="avatar-fallback">{{ avatarFallback }}</span>
        <span class="avatar-badge">{{ uploadingAvatar ? '上传中' : '编辑' }}</span>
      </button>

      <div class="profile-hero__content">
        <div class="profile-hero__name">{{ userName }}</div>
        <div class="profile-hero__role">{{ roleText }}</div>
        <div class="profile-hero__hint">点击头像可上传新头像，资料编辑改成更像 App 的个人中心交互。</div>
        <div class="hero-tags">
          <span class="hero-tag">{{ authStore.userInfo?.mfaEnabled ? 'MFA 已开启' : 'MFA 未开启' }}</span>
          <span class="hero-tag">{{ roleShortText }}</span>
        </div>
      </div>
    </section>

    <PageSection title="个人信息" description="手机端保留最常用的个人资料查看和修改入口。">
      <button class="info-item" type="button" @click="openEditPanel">
        <span class="info-item__label">昵称</span>
        <span class="info-item__value">{{ profileForm.nickName || '-' }}</span>
      </button>
      <div class="info-item">
        <span class="info-item__label">用户名</span>
        <span class="info-item__value">{{ authStore.userInfo?.username || '-' }}</span>
      </div>
      <button class="info-item" type="button" @click="openEditPanel">
        <span class="info-item__label">邮箱</span>
        <span class="info-item__value">{{ profileForm.email || '-' }}</span>
      </button>
      <button class="info-item" type="button" @click="openEditPanel">
        <span class="info-item__label">手机号</span>
        <span class="info-item__value">{{ profileForm.phone || '-' }}</span>
      </button>
      <button class="info-item" type="button" @click="openEditPanel">
        <span class="info-item__label">WxPusher UID</span>
        <span class="info-item__value">{{ profileForm.wxPusherUid || '-' }}</span>
      </button>
    </PageSection>

    <PageSection title="账号状态" description="保留和个人操作直接相关的账号状态。">
      <div class="status-grid">
        <div class="status-card">
          <div class="status-card__label">MFA</div>
          <div class="status-card__value">{{ authStore.userInfo?.mfaEnabled ? '已启用' : '未启用' }}</div>
        </div>
        <div class="status-card">
          <div class="status-card__label">角色</div>
          <div class="status-card__value">{{ roleShortText }}</div>
        </div>
      </div>
    </PageSection>

    <PageSection title="账号与安全" description="让高频安全动作单独成区，更像正式商业产品。">
      <button class="info-item" type="button" @click="openPasswordPanel">
        <span class="info-item__label">修改密码</span>
        <span class="info-item__value">更新当前登录密码</span>
      </button>
      <div class="info-item">
        <span class="info-item__label">二级验证</span>
        <span class="info-item__value">{{ authStore.userInfo?.mfaEnabled ? '已启用，请在 PC 端管理' : '未启用，请在 PC 端开启' }}</span>
      </div>
      <div class="info-item">
        <span class="info-item__label">当前登录账号</span>
        <span class="info-item__value">{{ authStore.userInfo?.username || '-' }}</span>
      </div>
    </PageSection>

    <button class="logout-button" type="button" @click="handleLogout">退出登录</button>

    <input ref="avatarInputRef" class="hidden-input" type="file" accept="image/*" @change="handleAvatarChange" />

    <div v-if="editVisible" class="sheet-mask" @click.self="closeEditPanel">
      <section class="edit-sheet">
        <div class="edit-sheet__header">
          <div>
            <div class="edit-sheet__title">编辑个人资料</div>
            <div class="edit-sheet__desc">只保留昵称、邮箱、手机号和 WxPusher UID 这些手机端高频修改项。</div>
          </div>
          <button class="text-button" type="button" @click="closeEditPanel">关闭</button>
        </div>

        <div class="edit-form">
          <label class="field">
            <span class="field__label">昵称</span>
            <input v-model.trim="profileForm.nickName" class="field__input" maxlength="30" placeholder="请输入昵称" />
          </label>
          <label class="field">
            <span class="field__label">邮箱</span>
            <input v-model.trim="profileForm.email" class="field__input" placeholder="请输入邮箱" />
          </label>
          <label class="field">
            <span class="field__label">手机号</span>
            <input v-model.trim="profileForm.phone" class="field__input" placeholder="请输入手机号" />
          </label>
          <label class="field">
            <span class="field__label">WxPusher UID</span>
            <input v-model.trim="profileForm.wxPusherUid" class="field__input" placeholder="请输入 WxPusher UID" />
          </label>

          <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>

          <button class="save-button" type="button" :disabled="saving" @click="handleSaveProfile">
            {{ saving ? '保存中...' : '保存资料' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="passwordVisible" class="sheet-mask" @click.self="closePasswordPanel">
      <section class="edit-sheet">
        <div class="edit-sheet__header">
          <div>
            <div class="edit-sheet__title">修改密码</div>
            <div class="edit-sheet__desc">安全操作单独放在账号与安全区，避免和资料编辑混在一起。</div>
          </div>
          <button class="text-button" type="button" @click="closePasswordPanel">关闭</button>
        </div>

        <div class="edit-form">
          <label class="field">
            <span class="field__label">原密码</span>
            <input v-model="passwordForm.oldPassword" class="field__input" type="password" placeholder="请输入原密码" />
          </label>
          <label class="field">
            <span class="field__label">新密码</span>
            <input v-model="passwordForm.newPassword" class="field__input" type="password" placeholder="请输入新密码" />
          </label>
          <label class="field">
            <span class="field__label">确认新密码</span>
            <input
              v-model="passwordForm.confirmPassword"
              class="field__input"
              type="password"
              placeholder="请再次输入新密码"
            />
          </label>

          <div v-if="passwordErrorMessage" class="error-message">{{ passwordErrorMessage }}</div>

          <button class="save-button" type="button" :disabled="savingPassword" @click="handleSavePassword">
            {{ savingPassword ? '提交中...' : '确认修改密码' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { updatePassword, updateProfile, uploadAvatar } from '@/api/auth'
import PageSection from '@/components/PageSection.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const editVisible = ref(false)
const passwordVisible = ref(false)
const saving = ref(false)
const savingPassword = ref(false)
const uploadingAvatar = ref(false)
const errorMessage = ref('')
const passwordErrorMessage = ref('')
const avatarInputRef = ref<HTMLInputElement | null>(null)

const profileForm = reactive({
  nickName: '',
  email: '',
  phone: '',
  wxPusherUid: '',
  avatar: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const resolveAssetUrl = (value?: string | null) => {
  if (!value) {
    return ''
  }
  if (/^(https?:)?\/\//.test(value)) {
    return value
  }
  const baseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://192.168.0.2:8080').replace(/\/+$/, '')
  return `${baseUrl}${value.startsWith('/') ? '' : '/'}${value}`
}

const syncProfileForm = () => {
  profileForm.nickName = authStore.userInfo?.nickName || ''
  profileForm.email = authStore.userInfo?.email || ''
  profileForm.phone = authStore.userInfo?.phone || ''
  profileForm.wxPusherUid = authStore.userInfo?.wxPusherUid || ''
  profileForm.avatar = authStore.userInfo?.avatar || ''
}

watch(
  () => authStore.userInfo,
  () => {
    syncProfileForm()
  },
  { immediate: true }
)

const userName = computed(() => authStore.userInfo?.nickName || authStore.userInfo?.username || '未登录用户')
const roleText = computed(() => {
  const roles = authStore.userInfo?.roles ?? []
  if (roles.length === 0) {
    return '移动端账号'
  }
  return roles.join(' / ')
})
const roleShortText = computed(() => {
  const roles = authStore.userInfo?.roles ?? []
  return roles.length === 0 ? '普通账号' : roles[0]
})
const avatarUrl = computed(() => resolveAssetUrl(authStore.userInfo?.avatar))
const avatarFallback = computed(() => {
  const text = authStore.userInfo?.nickName || authStore.userInfo?.username || 'U'
  return text.charAt(0).toUpperCase()
})

const openEditPanel = () => {
  errorMessage.value = ''
  syncProfileForm()
  editVisible.value = true
}

const closeEditPanel = () => {
  editVisible.value = false
  errorMessage.value = ''
  syncProfileForm()
}

const openPasswordPanel = () => {
  passwordErrorMessage.value = ''
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordVisible.value = true
}

const closePasswordPanel = () => {
  passwordVisible.value = false
  passwordErrorMessage.value = ''
}

const handleSaveProfile = async () => {
  errorMessage.value = ''
  if (!profileForm.nickName) {
    errorMessage.value = '昵称不能为空'
    return
  }
  saving.value = true
  try {
    await updateProfile({
      nickName: profileForm.nickName,
      email: profileForm.email || undefined,
      phone: profileForm.phone || undefined,
      wxPusherUid: profileForm.wxPusherUid || undefined,
      avatar: profileForm.avatar || undefined
    })
    await authStore.fetchUserInfo(true)
    editVisible.value = false
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存失败，请重试'
  } finally {
    saving.value = false
  }
}

const handleSavePassword = async () => {
  passwordErrorMessage.value = ''
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    passwordErrorMessage.value = '请完整填写密码信息'
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordErrorMessage.value = '两次输入的新密码不一致'
    return
  }
  savingPassword.value = true
  try {
    await updatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    closePasswordPanel()
  } catch (error) {
    passwordErrorMessage.value = error instanceof Error ? error.message : '修改密码失败，请重试'
  } finally {
    savingPassword.value = false
  }
}

const triggerAvatarSelect = () => {
  avatarInputRef.value?.click()
}

const handleAvatarChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) {
    return
  }

  try {
    uploadingAvatar.value = true
    const result = await uploadAvatar(file)
    await updateProfile({
      nickName: authStore.userInfo?.nickName || authStore.userInfo?.username || '用户',
      email: authStore.userInfo?.email || undefined,
      phone: authStore.userInfo?.phone || undefined,
      wxPusherUid: authStore.userInfo?.wxPusherUid || undefined,
      avatar: result.avatarUrl
    })
    await authStore.fetchUserInfo(true)
  } finally {
    uploadingAvatar.value = false
    target.value = ''
  }
}

const handleLogout = async () => {
  authStore.logout()
  await router.replace('/login')
}
</script>

<style scoped>
.page {
  display: grid;
  gap: 16px;
  padding: 16px;
}

.profile-hero {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  padding: 20px 18px;
  border-radius: 28px;
  background: linear-gradient(145deg, #ffffff 0%, #eef5ff 100%);
  box-shadow: var(--sak-shadow);
}

.avatar-button {
  width: 88px;
  height: 88px;
  border: 0;
  border-radius: 28px;
  background: linear-gradient(180deg, #dbeafe 0%, #bfdbfe 100%);
  display: grid;
  place-items: center;
  overflow: hidden;
  position: relative;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  color: var(--sak-brand);
  font-size: 28px;
  font-weight: 800;
}

.avatar-badge {
  position: absolute;
  right: 6px;
  bottom: 6px;
  min-width: 36px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(23, 32, 42, 0.82);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
}

.profile-hero__name {
  font-size: 22px;
  font-weight: 700;
}

.profile-hero__role {
  margin-top: 6px;
  color: var(--sak-brand);
  font-size: 13px;
  font-weight: 600;
}

.profile-hero__hint {
  margin-top: 8px;
  color: var(--sak-muted);
  font-size: 13px;
  line-height: 1.6;
}

.hero-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.hero-tag {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(0, 82, 217, 0.08);
  color: var(--sak-brand);
  font-size: 12px;
  font-weight: 700;
}

.info-item {
  width: 100%;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 14px 0;
  border: 0;
  border-bottom: 1px solid var(--sak-border);
  background: transparent;
  text-align: left;
}

.info-item:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.info-item__label {
  color: var(--sak-muted);
  font-size: 13px;
}

.info-item__value {
  color: var(--sak-text);
  font-size: 14px;
  font-weight: 600;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.status-card {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--sak-border);
  background: var(--sak-surface-soft);
}

.status-card__label {
  color: var(--sak-muted);
  font-size: 12px;
}

.status-card__value {
  margin-top: 8px;
  font-size: 16px;
  font-weight: 700;
}

.logout-button {
  border: 0;
  border-radius: 18px;
  background: var(--sak-danger);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  padding: 14px 18px;
}

.hidden-input {
  display: none;
}

.sheet-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.36);
  display: flex;
  align-items: flex-end;
  z-index: 20;
}

.edit-sheet {
  width: 100%;
  border-radius: 28px 28px 0 0;
  background: #fff;
  padding: 20px 16px calc(20px + env(safe-area-inset-bottom));
  box-shadow: 0 -16px 40px rgba(15, 23, 42, 0.18);
}

.edit-sheet__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.edit-sheet__title {
  font-size: 18px;
  font-weight: 700;
}

.edit-sheet__desc {
  margin-top: 6px;
  color: var(--sak-muted);
  font-size: 12px;
  line-height: 1.6;
}

.text-button {
  border: 0;
  background: transparent;
  color: var(--sak-brand);
  font-size: 13px;
  font-weight: 700;
}

.edit-form {
  display: grid;
  gap: 14px;
  margin-top: 18px;
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

.error-message {
  color: var(--sak-danger);
  font-size: 13px;
}

.save-button {
  border: 0;
  border-radius: 18px;
  background: var(--sak-brand);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  padding: 15px 18px;
}

.save-button:disabled {
  opacity: 0.6;
}
</style>
