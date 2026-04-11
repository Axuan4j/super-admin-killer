<template>
  <div class="page">
    <PageSection title="待办告警" description="把手机端真正需要即时处理的项目收敛到这里。">
      <div v-for="item in alerts" :key="item.title" class="alert-card" :class="`alert-card--${item.level}`">
        <div class="alert-card__top">
          <span class="alert-card__level">{{ item.levelText }}</span>
          <span class="alert-card__time">{{ item.time }}</span>
        </div>
        <div class="alert-card__title">{{ item.title }}</div>
        <div class="alert-card__desc">{{ item.desc }}</div>
        <div class="alert-card__action">{{ item.action }}</div>
      </div>
    </PageSection>
  </div>
</template>

<script setup lang="ts">
import PageSection from '@/components/PageSection.vue'

const alerts = [
  {
    level: 'danger',
    levelText: '紧急',
    time: '2 分钟前',
    title: '订单回调任务连续失败',
    desc: '连续 3 次执行失败，已经达到移动端优先处理阈值。',
    action: '建议动作：先暂停任务，再进入详情页重试。'
  },
  {
    level: 'warning',
    levelText: '关注',
    time: '12 分钟前',
    title: 'Redis 连接池等待时间升高',
    desc: '当前只适合在手机端确认异常是否持续，不建议直接做复杂排障。',
    action: '建议动作：确认后通知值班同学。'
  },
  {
    level: 'info',
    levelText: '提醒',
    time: '25 分钟前',
    title: '5 条消息发送失败待重发',
    desc: '适合移动端快速筛一遍失败原因，并触发人工补发。',
    action: '建议动作：进入消息发送记录查看失败详情。'
  }
]
</script>

<style scoped>
.page {
  padding: 16px;
}

.alert-card {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--sak-border);
  background: #fff;
}

.alert-card--danger {
  border-color: rgba(207, 63, 76, 0.24);
  background: #fff7f7;
}

.alert-card--warning {
  border-color: rgba(217, 119, 6, 0.22);
  background: #fffaf3;
}

.alert-card--info {
  border-color: rgba(0, 82, 217, 0.18);
  background: #f7faff;
}

.alert-card__top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--sak-muted);
  font-size: 12px;
}

.alert-card__level {
  font-weight: 700;
}

.alert-card__title {
  margin-top: 8px;
  font-size: 16px;
  font-weight: 700;
}

.alert-card__desc,
.alert-card__action {
  margin-top: 8px;
  color: var(--sak-muted);
  font-size: 13px;
  line-height: 1.6;
}
</style>
