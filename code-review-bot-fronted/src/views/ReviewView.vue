<template>
  <div class="review-view">
    <!-- Left Panel: Input + Diff -->
    <div class="panel panel--left">
      <CodeInput
        :loading="store.status === 'loading' || store.status === 'streaming'"
        @review="handleReview"
      />

      <DiffViewer
        v-if="store.diffContent"
        :diff-text="store.diffContent"
        class="diff-section"
      />
    </div>

    <!-- Right Panel: Results / States -->
    <div class="panel panel--right">
      <!-- Idle: Getting started guide -->
      <div v-if="store.status === 'idle'" class="guide-card">
        <div class="guide-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="#c4b5fd" stroke-width="1.5" />
            <path d="M9 12l2 2 4-4" stroke="#7c3aed" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
        <h3>开始代码审查</h3>
        <p>粘贴代码或 Git Diff 到左侧编辑器，然后点击「开始审查」按钮。</p>
        <p>AI 将逐行分析您的代码，发现潜在问题并提供修改建议。</p>
      </div>

      <!-- Loading: initial analysis (also covers streaming before first issue arrives) -->
      <div
        v-if="(store.status === 'loading' || store.status === 'streaming') && store.streamIssues.length === 0"
        class="state-card state-card--loading"
      >
        <el-icon class="loading-icon is-loading"><Loading /></el-icon>
        <p class="state-title">AI 正在分析代码</p>
        <p class="state-desc">正在扫描代码结构与逻辑...</p>
      </div>

      <!-- Error state -->
      <div v-if="store.status === 'error'" class="state-card state-card--error">
        <el-icon class="state-icon"><WarningFilled /></el-icon>
        <p class="state-title">审查失败</p>
        <p class="state-desc">发生了意外错误，请检查代码内容后重试</p>
        <el-button type="primary" size="small" @click="retryReview">
          重新审查
        </el-button>
      </div>

      <!-- Streaming / Done: Results -->
      <ReviewReport
        v-if="store.streamIssues.length > 0 || store.currentResult"
        :result="liveResult"
        :is-streaming="store.status === 'streaming'"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Loading, WarningFilled } from '@element-plus/icons-vue'
import CodeInput from '@/components/CodeInput.vue'
import DiffViewer from '@/components/DiffViewer.vue'
import ReviewReport from '@/components/ReviewReport.vue'
import { useReviewStore } from '@/stores/review'
import type { ReviewResult } from '@/types/review'

const store = useReviewStore()

const liveResult = computed<ReviewResult>(() => {
  if (store.currentResult) return store.currentResult
  return {
    issues: store.streamIssues,
    summary: '',
  }
})

const currentAbort = ref<(() => void) | null>(null)

function handleReview(content: string, mode: 'code' | 'diff') {
  currentAbort.value?.()
  const { abort } = store.startReview(content, mode)
  currentAbort.value = abort
}

function retryReview() {
  if (store.currentCode) {
    currentAbort.value?.()
    const { abort } = store.startReview(store.currentCode, store.currentMode)
    currentAbort.value = abort
  }
}
</script>

<style scoped>
.review-view {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel--left {
  flex: 6;
  min-width: 0;
}

.panel--right {
  flex: 4;
  min-width: 340px;
  position: sticky;
  top: 76px;
}

.diff-section {
  margin-top: 0;
}

/* Guide card */
.guide-card {
  background: #fff;
  border-radius: 10px;
  padding: 36px 24px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.guide-icon {
  margin-bottom: 16px;
}

.guide-card h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 10px;
  color: #303133;
}

.guide-card p {
  font-size: 13px;
  color: #909399;
  line-height: 1.7;
  margin: 0;
}

/* State cards */
.state-card {
  background: #fff;
  border-radius: 10px;
  padding: 36px 24px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.state-title {
  font-size: 16px;
  font-weight: 600;
  margin: 12px 0 6px;
  color: #303133;
}

.state-desc {
  font-size: 13px;
  color: #909399;
  margin: 0 0 16px;
}

.state-icon {
  font-size: 40px;
}

.loading-icon {
  font-size: 36px;
  color: #3451b2;
}

.state-card--loading .state-title { color: #3451b2; }
.state-card--error .state-icon { color: #f56c6c; }
.state-card--error .state-title { color: #dc2626; }

/* Responsive */
@media (max-width: 900px) {
  .review-view {
    flex-direction: column;
  }
  .panel--right {
    position: static;
    min-width: 0;
    width: 100%;
  }
}
</style>
