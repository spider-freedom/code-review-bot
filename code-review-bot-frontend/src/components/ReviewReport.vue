<template>
  <div class="review-report">
    <div v-if="result.issues.length === 0 && !isStreaming" class="pass-state">
      <div class="pass-icon-wrap">
        <el-icon class="pass-icon"><CircleCheckFilled /></el-icon>
      </div>
      <p class="pass-text">审查通过</p>
      <p class="pass-sub">未发现任何问题</p>
    </div>

    <template v-if="result.issues.length > 0 || isStreaming">
      <div class="summary-box" :class="{ 'summary--pending': isStreaming }">
        <p v-if="result.summary">{{ result.summary }}</p>
        <p v-else class="summary-pending">
          <span class="typing-dots"><i /><i /><i /></span>
          正在逐项分析代码...
        </p>
      </div>

      <div v-if="result.issues.length > 0" class="issue-stats">
        <span class="stat stat--error">严重 {{ errorIssues.length }}</span>
        <span class="stat stat--warning">建议 {{ warningIssues.length }}</span>
        <span class="stat stat--info">优化 {{ infoIssues.length }}</span>
      </div>

      <section v-if="errorIssues.length" class="severity-section">
        <h3 class="section-title section-title--error">
          严重问题
        </h3>
        <IssueCard v-for="issue in errorIssues" :key="issue.line + issue.title" :issue="issue" />
      </section>

      <section v-if="warningIssues.length" class="severity-section">
        <h3 class="section-title section-title--warning">
          建议
        </h3>
        <IssueCard v-for="issue in warningIssues" :key="issue.line + issue.title" :issue="issue" />
      </section>

      <section v-if="infoIssues.length" class="severity-section">
        <h3 class="section-title section-title--info">
          优化建议
        </h3>
        <IssueCard v-for="issue in infoIssues" :key="issue.line + issue.title" :issue="issue" />
      </section>

      <div v-if="isStreaming" class="streaming-indicator">
        <span class="typing-dots"><i /><i /><i /></span>
        还有更多分析结果...
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import type { ReviewResult } from '@/types/review'
import IssueCard from './IssueCard.vue'

const props = defineProps<{
  result: ReviewResult
  isStreaming?: boolean
}>()

const errorIssues = computed(() => props.result.issues.filter((i) => i.severity === 'error'))
const warningIssues = computed(() => props.result.issues.filter((i) => i.severity === 'warning'))
const infoIssues = computed(() => props.result.issues.filter((i) => i.severity === 'info'))
</script>

<style scoped>
.review-report {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.pass-state {
  text-align: center;
  padding: 32px 0;
}

.pass-icon-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #ecfdf3;
  margin-bottom: 12px;
}

.pass-icon {
  font-size: 36px;
  color: #16a34a;
}

.pass-text {
  font-size: 18px;
  font-weight: 600;
  color: #16a34a;
  margin: 0 0 4px;
}

.pass-sub {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.summary-box {
  background: #f8faff;
  border: 1px solid #e8ecf4;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 14px;
}

.summary-box p {
  margin: 0;
  font-size: 13px;
  color: #303133;
  line-height: 1.65;
}

.summary--pending {
  background: #fffbeb;
  border-color: #fde68a;
}

.summary-pending {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #92400e !important;
  font-weight: 500;
}

.issue-stats {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.stat {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: 500;
}
.stat--error { background: #fef2f2; color: #dc2626; }
.stat--warning { background: #fffbeb; color: #d97706; }
.stat--info { background: #eff6ff; color: #2563eb; }

.severity-section {
  margin-bottom: 18px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 10px;
}
.section-title--error { color: #dc2626; }
.section-title--warning { color: #d97706; }
.section-title--info { color: #2563eb; }

.streaming-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 0;
  color: #909399;
  font-size: 13px;
}

/* typing dots animation */
.typing-dots {
  display: inline-flex;
  gap: 3px;
  align-items: center;
}
.typing-dots i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
  animation: dotPulse 1.4s infinite;
}
.typing-dots i:nth-child(2) { animation-delay: 0.2s; }
.typing-dots i:nth-child(3) { animation-delay: 0.4s; }

@keyframes dotPulse {
  0%, 60%, 100% { opacity: 0.25; transform: scale(0.8); }
  30% { opacity: 1; transform: scale(1.2); }
}
</style>
