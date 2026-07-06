<template>
  <div class="review-detail">
    <div class="detail-header">
      <el-button :icon="ArrowLeft" @click="goBack">返回历史记录</el-button>
      <h2 v-if="record">审查详情</h2>
    </div>

    <div v-if="!record" class="not-found">
      <p>未找到该审查记录</p>
    </div>

    <template v-else>
      <div class="meta-bar">
        <span>审查时间：{{ formatTime(record.createdAt) }}</span>
        <el-tag size="small" :type="record.mode === 'diff' ? 'success' : ''">
          {{ record.mode === 'diff' ? 'Git Diff' : '代码片段' }}
        </el-tag>
      </div>

      <DiffViewer
        v-if="record.mode === 'diff'"
        :diff-text="record.code"
        class="section"
      />
      <CodeHighlight
        v-else
        :code="record.code"
        class="section"
      />

      <ReviewReport :result="record.result" class="section" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useReviewStore } from '@/stores/review'
import { formatTime } from '@/utils/format'
import DiffViewer from '@/components/DiffViewer.vue'
import CodeHighlight from '@/components/CodeHighlight.vue'
import ReviewReport from '@/components/ReviewReport.vue'

const route = useRoute()
const router = useRouter()
const store = useReviewStore()

const record = computed(() => store.historyById(route.params.id as string))

function goBack() {
  router.push({ name: 'history' })
}
</script>

<style scoped>
.review-detail {
  padding: 0;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.detail-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
}

.not-found {
  text-align: center;
  padding: 80px 0;
  color: #909399;
}

.meta-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #909399;
}

.section {
  margin-bottom: 24px;
}
</style>
