<template>
  <a-layout class="layout-container">
    <!-- 主内容区 -->
    <a-layout-content class="layout-content">
      <div class="content-wrapper">
        <router-view/>
      </div>
    </a-layout-content>
  </a-layout>
</template>

<script setup lang="ts">

</script>

<style scoped>
.layout-container {
  height: 100vh;
  overflow: hidden;
}

.layout-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 0;
  background: var(--app-content-bg, #f0f2f5);
}

.content-wrapper {
  flex: 1;
  min-height: 0;
  padding: 20px;
  overflow: auto;
}
</style>
