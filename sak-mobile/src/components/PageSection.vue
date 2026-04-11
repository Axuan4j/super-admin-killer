<template>
  <section class="page-section">
    <header class="page-section__header">
      <div>
        <div class="page-section__title">{{ title }}</div>
        <div v-if="description" class="page-section__desc">{{ description }}</div>
      </div>
      <slot name="extra" />
    </header>
    <div class="page-section__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<style scoped>
.page-section {
  border-radius: var(--sak-radius);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--sak-shadow);
  padding: 18px;
}

.page-section__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.page-section__title {
  font-size: 17px;
  font-weight: 700;
  line-height: 1.3;
}

.page-section__desc {
  margin-top: 6px;
  color: var(--sak-muted);
  font-size: 12px;
  line-height: 1.5;
}

.page-section__body {
  display: grid;
  gap: 12px;
}
</style>
