<template>
  <div class="profile-page">
    <a-card class="profile-shell">
      <a-tabs default-active-key="info" lazy-load>
        <a-tab-pane key="info" title="我的信息">
          <a-space direction="vertical" :size="20" fill>
            <div class="profile-head">
              <a-avatar v-if="authStore.userInfo?.avatar" :size="72">
                <img :src="resolveAssetUrl(authStore.userInfo.avatar)" alt="avatar" class="avatar-image" />
              </a-avatar>
              <a-avatar v-else :size="72" :style="{ backgroundColor: 'rgb(var(--primary-6))' }">
                {{ authStore.userInfo?.nickName?.charAt(0)?.toUpperCase() || authStore.userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
              </a-avatar>
              <div>
                <div class="profile-name">{{ authStore.userInfo?.nickName || authStore.userInfo?.username }}</div>
                <div class="profile-sub">{{ authStore.userInfo?.username }}</div>
                <a-button type="text" size="small" class="avatar-upload-trigger" @click="openAvatarModal">
                  上传头像
                </a-button>
              </div>
            </div>

            <a-descriptions :column="2" bordered size="large">
              <a-descriptions-item label="邮箱">{{ authStore.userInfo?.email || '-' }}</a-descriptions-item>
              <a-descriptions-item label="WxPusher UID">{{ authStore.userInfo?.wxPusherUid || '-' }}</a-descriptions-item>
              <a-descriptions-item label="手机号">{{ authStore.userInfo?.phone || '-' }}</a-descriptions-item>
              <a-descriptions-item label="MFA">
                <a-tag :color="authStore.userInfo?.mfaEnabled ? 'green' : 'gray'">
                  {{ authStore.userInfo?.mfaEnabled ? '已启用' : '未启用' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="角色" :span="2">
                <a-space wrap>
                  <a-tag v-for="role in authStore.userInfo?.roles" :key="role" color="arcoblue">{{ role }}</a-tag>
                </a-space>
              </a-descriptions-item>
              <a-descriptions-item label="权限" :span="2">
                <a-space wrap>
                  <a-tag v-for="auth in authStore.userInfo?.authorities" :key="auth" color="green">{{ auth }}</a-tag>
                </a-space>
              </a-descriptions-item>
            </a-descriptions>
          </a-space>
        </a-tab-pane>

        <a-tab-pane key="profile" title="编辑资料">
          <a-form :model="profileForm" layout="vertical" class="tab-form">
            <a-form-item label="用户名">
              <a-input :model-value="authStore.userInfo?.username || ''" disabled />
            </a-form-item>
            <a-form-item label="昵称">
              <a-input v-model="profileForm.nickName" :max-length="30" show-word-limit />
            </a-form-item>
            <a-form-item label="邮箱">
              <a-input v-model="profileForm.email" />
            </a-form-item>
            <a-form-item label="WxPusher UID">
              <a-input v-model="profileForm.wxPusherUid" />
            </a-form-item>
            <a-form-item label="手机号">
              <a-input v-model="profileForm.phone" />
            </a-form-item>
            <a-form-item label="头像地址">
              <a-space fill>
                <a-input v-model="profileForm.avatar" />
                <a-button @click="openAvatarModal">上传头像</a-button>
              </a-space>
            </a-form-item>
            <a-form-item>
              <a-space>
                <a-button type="primary" :loading="savingProfile" @click="handleProfileSubmit">保存资料</a-button>
                <a-button @click="resetProfileForm">重置</a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <a-tab-pane key="password" title="修改密码">
          <a-form :model="passwordForm" layout="vertical" class="tab-form">
            <a-form-item label="原密码">
              <a-input-password v-model="passwordForm.oldPassword" />
            </a-form-item>
            <a-form-item label="新密码">
              <a-input-password v-model="passwordForm.newPassword" />
            </a-form-item>
            <a-form-item label="确认新密码">
              <a-input-password v-model="passwordForm.confirmPassword" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" status="warning" :loading="savingPassword" @click="handlePasswordSubmit">
                修改密码
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <a-tab-pane key="mfa" title="二级验证">
          <div class="mfa-panel">
            <a-alert type="info" show-icon>
              推荐使用 Microsoft Authenticator、Google Authenticator 等标准 TOTP 验证器。
            </a-alert>

            <a-card class="mfa-card" :bordered="false">
              <div class="mfa-status">
                <span>当前状态</span>
                <a-tag :color="authStore.userInfo?.mfaEnabled ? 'green' : 'gray'">
                  {{ authStore.userInfo?.mfaEnabled ? '已启用' : '未启用' }}
                </a-tag>
              </div>

              <template v-if="!authStore.userInfo?.mfaEnabled">
                <a-space direction="vertical" fill :size="16">
                  <a-button type="primary" :loading="loadingMfaSetup" @click="handleCreateMfaSetup">
                    生成绑定二维码
                  </a-button>

                  <div v-if="mfaSetup.secret" class="mfa-setup">
                    <div class="mfa-qr-box">
                      <img :src="`data:image/png;base64,${mfaSetup.qrCodeBase64}`" alt="mfa-qr" class="mfa-qr-image" />
                    </div>
                    <a-form :model="mfaForm" layout="vertical">
                      <a-form-item label="手动密钥">
                        <a-space fill>
                          <a-input :model-value="mfaSetup.secret" readonly />
                          <a-button @click="copyText(mfaSetup.secret)">复制</a-button>
                        </a-space>
                      </a-form-item>
                      <a-form-item label="绑定链接">
                        <a-space fill>
                          <a-input :model-value="mfaSetup.otpauthUri" readonly />
                          <a-button @click="copyText(mfaSetup.otpauthUri)">复制</a-button>
                        </a-space>
                      </a-form-item>
                      <a-form-item label="输入 6 位动态码完成绑定">
                        <a-space fill>
                          <a-input v-model="mfaForm.enableCode" :max-length="6" placeholder="请输入 6 位动态码" />
                          <a-button type="primary" :loading="enablingMfa" @click="handleEnableMfa">启用</a-button>
                        </a-space>
                      </a-form-item>
                    </a-form>
                  </div>
                </a-space>
              </template>

              <template v-else>
                <a-form :model="mfaForm" layout="vertical" class="tab-form">
                  <a-form-item label="关闭 MFA">
                    <a-space fill>
                      <a-input v-model="mfaForm.disableCode" :max-length="6" placeholder="请输入验证器中的 6 位动态码" />
                      <a-button status="danger" :loading="disablingMfa" @click="handleDisableMfa">关闭</a-button>
                    </a-space>
                  </a-form-item>
                </a-form>
              </template>
            </a-card>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal
      v-model:visible="avatarModalVisible"
      title="上传头像"
      :ok-loading="uploadingAvatar"
      @ok="handleAvatarUpload"
      @cancel="resetAvatarUpload"
    >
      <a-space direction="vertical" fill :size="16">
        <div class="avatar-preview-wrapper">
          <a-avatar v-if="avatarPreviewUrl" :size="96">
            <img :src="avatarPreviewUrl" alt="avatar-preview" class="avatar-image" />
          </a-avatar>
          <a-avatar v-else :size="96" :style="{ backgroundColor: 'rgb(var(--primary-6))' }">
            {{ authStore.userInfo?.nickName?.charAt(0)?.toUpperCase() || authStore.userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
          </a-avatar>
        </div>
        <input ref="avatarInputRef" type="file" accept="image/*" class="hidden-input" @change="handleAvatarFileChange" />
        <a-space>
          <a-button @click="triggerAvatarFileSelect">选择图片</a-button>
          <span class="profile-sub">{{ avatarFile?.name || '支持 jpg/png/gif/webp，大小不超过 2MB' }}</span>
        </a-space>
      </a-space>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import {
  disableMfa,
  enableMfa,
  setupMfa,
  updatePassword,
  updateProfile,
  uploadAvatar
} from '@/api/auth.ts'
import type { MfaSetupResponse } from '@/api/auth.ts'
import { useAuthStore } from '@/stores/auth.ts'

const authStore = useAuthStore()
const savingProfile = ref(false)
const savingPassword = ref(false)
const avatarModalVisible = ref(false)
const uploadingAvatar = ref(false)
const loadingMfaSetup = ref(false)
const enablingMfa = ref(false)
const disablingMfa = ref(false)
const avatarFile = ref<File | null>(null)
const avatarPreviewUrl = ref('')
const avatarInputRef = ref<HTMLInputElement | null>(null)

const profileForm = reactive({
  nickName: '',
  email: '',
  wxPusherUid: '',
  phone: '',
  avatar: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const mfaForm = reactive({
  enableCode: '',
  disableCode: ''
})

const mfaSetup = reactive<MfaSetupResponse>({
  issuer: '',
  accountName: '',
  secret: '',
  otpauthUri: '',
  qrCodeBase64: ''
})

const syncProfileForm = () => {
  profileForm.nickName = authStore.userInfo?.nickName || ''
  profileForm.email = authStore.userInfo?.email || ''
  profileForm.wxPusherUid = authStore.userInfo?.wxPusherUid || ''
  profileForm.phone = authStore.userInfo?.phone || ''
  profileForm.avatar = authStore.userInfo?.avatar || ''
}

const resetProfileForm = () => {
  syncProfileForm()
}

const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

const resetMfaSetup = () => {
  mfaSetup.issuer = ''
  mfaSetup.accountName = ''
  mfaSetup.secret = ''
  mfaSetup.otpauthUri = ''
  mfaSetup.qrCodeBase64 = ''
  mfaForm.enableCode = ''
  mfaForm.disableCode = ''
}

const resolveAssetUrl = (value?: string) => {
  if (!value) {
    return ''
  }
  if (/^(https?:)?\/\//.test(value) || value.startsWith('data:') || value.startsWith('blob:')) {
    return value
  }

  const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')
  const normalizedPath = value.startsWith('/') ? value : `/${value}`
  return `${apiBaseUrl}${normalizedPath}`
}

const resetAvatarUpload = () => {
  avatarFile.value = null
  avatarPreviewUrl.value = resolveAssetUrl(profileForm.avatar || authStore.userInfo?.avatar || '')
  if (avatarInputRef.value) {
    avatarInputRef.value.value = ''
  }
}

const openAvatarModal = () => {
  avatarModalVisible.value = true
  resetAvatarUpload()
}

const triggerAvatarFileSelect = () => {
  avatarInputRef.value?.click()
}

const handleAvatarFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) {
    return
  }
  if (!file.type.startsWith('image/')) {
    Message.warning('请选择图片文件')
    target.value = ''
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    Message.warning('头像文件大小不能超过 2MB')
    target.value = ''
    return
  }
  avatarFile.value = file
  avatarPreviewUrl.value = URL.createObjectURL(file)
}

const handleProfileSubmit = async () => {
  if (!profileForm.nickName.trim()) {
    Message.warning('请填写昵称')
    return
  }
  if (profileForm.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(profileForm.email)) {
    Message.warning('邮箱格式不正确')
    return
  }
  if (profileForm.phone && !/^[0-9\-+]{6,20}$/.test(profileForm.phone)) {
    Message.warning('手机号格式不正确')
    return
  }

  savingProfile.value = true
  try {
    await updateProfile({
      nickName: profileForm.nickName.trim(),
      email: profileForm.email.trim() || undefined,
      wxPusherUid: profileForm.wxPusherUid.trim() || undefined,
      phone: profileForm.phone.trim() || undefined,
      avatar: profileForm.avatar.trim() || undefined
    })
    await authStore.fetchUserInfo(true)
    syncProfileForm()
    Message.success('个人资料已更新')
  } finally {
    savingProfile.value = false
  }
}

const handleAvatarUpload = async () => {
  if (!avatarFile.value) {
    Message.warning('请先选择头像文件')
    return
  }

  uploadingAvatar.value = true
  try {
    const result = await uploadAvatar(avatarFile.value)
    profileForm.avatar = result.avatarUrl
    await updateProfile({
      nickName: profileForm.nickName.trim() || authStore.userInfo?.nickName || authStore.userInfo?.username || '',
      email: profileForm.email.trim() || undefined,
      wxPusherUid: profileForm.wxPusherUid.trim() || undefined,
      phone: profileForm.phone.trim() || undefined,
      avatar: result.avatarUrl
    })
    await authStore.fetchUserInfo(true)
    syncProfileForm()
    avatarModalVisible.value = false
    resetAvatarUpload()
    Message.success('头像上传成功')
  } finally {
    uploadingAvatar.value = false
  }
}

const handlePasswordSubmit = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    Message.warning('请填写完整密码信息')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    Message.warning('新密码至少 6 位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    Message.warning('两次输入的新密码不一致')
    return
  }

  savingPassword.value = true
  try {
    await updatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    resetPasswordForm()
    Message.success('密码修改成功')
  } finally {
    savingPassword.value = false
  }
}

const handleCreateMfaSetup = async () => {
  loadingMfaSetup.value = true
  try {
    const data = await setupMfa()
    mfaSetup.issuer = data.issuer
    mfaSetup.accountName = data.accountName
    mfaSetup.secret = data.secret
    mfaSetup.otpauthUri = data.otpauthUri
    mfaSetup.qrCodeBase64 = data.qrCodeBase64
    mfaForm.enableCode = ''
    Message.success('二维码已生成，请使用验证器扫码后输入 6 位动态码')
  } finally {
    loadingMfaSetup.value = false
  }
}

const handleEnableMfa = async () => {
  if (!/^\d{6}$/.test(mfaForm.enableCode)) {
    Message.warning('请输入 6 位动态码')
    return
  }
  enablingMfa.value = true
  try {
    await enableMfa({ code: mfaForm.enableCode })
    await authStore.fetchUserInfo(true)
    resetMfaSetup()
    Message.success('MFA 已启用')
  } finally {
    enablingMfa.value = false
  }
}

const handleDisableMfa = async () => {
  if (!/^\d{6}$/.test(mfaForm.disableCode)) {
    Message.warning('请输入 6 位动态码')
    return
  }
  disablingMfa.value = true
  try {
    await disableMfa({ code: mfaForm.disableCode })
    await authStore.fetchUserInfo(true)
    resetMfaSetup()
    Message.success('MFA 已关闭')
  } finally {
    disablingMfa.value = false
  }
}

const copyText = async (value: string) => {
  try {
    await navigator.clipboard.writeText(value)
    Message.success('已复制')
  } catch {
    Message.warning('复制失败，请手动复制')
  }
}

onMounted(async () => {
  if (!authStore.userInfo) {
    await authStore.fetchUserInfo()
  }
  syncProfileForm()
  resetAvatarUpload()
  resetMfaSetup()
})
</script>

<style scoped>
.profile-page {
  min-height: 100%;
}

.profile-shell {
  max-width: 960px;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-name {
  color: #1d2129;
  font-size: 20px;
  font-weight: 600;
}

.profile-sub {
  margin-top: 4px;
  color: #86909c;
  font-size: 13px;
}

.tab-form {
  max-width: 520px;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-upload-trigger {
  margin-top: 8px;
  padding-left: 0;
}

.avatar-preview-wrapper {
  display: flex;
  justify-content: center;
}

.hidden-input {
  display: none;
}

.mfa-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 760px;
}

.mfa-card {
  padding: 8px 0;
}

.mfa-status {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  color: #1d2129;
  font-weight: 600;
}

.mfa-setup {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mfa-qr-box {
  display: flex;
  justify-content: center;
  padding: 16px;
  border: 1px solid #e5e6eb;
  border-radius: 12px;
  background: #fff;
}

.mfa-qr-image {
  width: 240px;
  height: 240px;
  object-fit: contain;
}
</style>
