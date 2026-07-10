<template>
  <div class="review-detail">
    <div class="detail-header">
      <el-button :icon="ArrowLeft" @click="goBack">返回历史记录</el-button>
      <h2 v-if="task">审查详情</h2>
    </div>

    <div v-if="!task && !loading" class="not-found">
      <p>未找到该审查记录</p>
    </div>

    <template v-else-if="task">
      <div class="meta-bar">
        <span>审查时间：{{ task.createTime ? formatTime(task.createTime) : '' }}</span>
        <el-tag size="small" :type="task.mode === 'diff' ? 'success' : ''">
          {{ task.mode === 'diff' ? 'Git Diff' : '代码' }}
        </el-tag>
        <el-tag size="small" :type="task.status === 'COMPLETED' ? 'success' : 'warning'">
          {{ task.status === 'COMPLETED' ? '已完成' : task.status }}
        </el-tag>
      </div>

      <ReviewReport v-if="issues.length" :result="{ issues, summary: `共 ${issues.length} 个问题` }" class="section" />
      <div v-else-if="loading" style="text-align:center;padding:40px">加载中...</div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { formatTime } from '@/utils/format'
import { getTaskStatus, getTaskIssues } from '@/api/review'
import type { ReviewIssue, ReviewTask } from '@/types/review'
import ReviewReport from '@/components/ReviewReport.vue'

const route = useRoute()
const router = useRouter()

const task = ref<ReviewTask | null>(null)
const issues = ref<ReviewIssue[]>([])
const loading = ref(false)

onMounted(async () => {
  const taskId = route.params.id as string
  loading.value = true
  try {
    const t = await getTaskStatus(taskId)
    task.value = { taskId: t.taskId, code: t.code || '', mode: (t.mode as 'code' | 'diff') || 'code', status: t.status, createTime: t.createTime }
    if (t.status === 'COMPLETED') {
      issues.value = await getTaskIssues(taskId)
    }
  } catch {
    task.value = null
  } finally {
    loading.value = false
  }
})

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
