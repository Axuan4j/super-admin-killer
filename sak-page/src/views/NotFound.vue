<template>
  <div class="not-found-container">
    <div class="content">
      <div class="error-code">404</div>
      <div class="error-message">抱歉，您访问的页面不存在</div>
      <a-button type="primary" @click="goHome">返回首页</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/dashboard')
}
</script>

<style scoped>
.not-found-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 60px);
  background: #f0f2f5;
}

.content {
  text-align: center;
}

.error-code {
  font-size: 120px;
  font-weight: bold;
  color: #165dff;
  line-height: 1;
  margin-bottom: 16px;
}

.error-message {
  font-size: 18px;
  color: #666;
  margin-bottom: 32px;
}
</style>
