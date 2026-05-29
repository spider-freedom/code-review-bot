<template>
  <div class="history-view">
    <h2 class="page-title">历史记录</h2>

    <template v-if="store.history.length > 0">
      <el-table :data="store.history" stripe class="history-table">
        <el-table-column label="审查时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="代码片段" min-width="280">
          <template #default="{ row }">
            <span class="code-snippet">{{ row.code.slice(0, 50) }}{{ row.code.length > 50 ? '...' : '' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="输入模式" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.mode === 'diff' ? 'success' : ''">
              {{ row.mode === 'diff' ? 'Git Diff' : '代码片段' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="问题数" width="90">
          <template #default="{ row }">
            <span>{{ row.result.issues.length }}</span>
          </template>
        </el-table-column>
        <el-table-column label="严重" width="70">
          <template #default="{ row }">
            <span class="error-count">
              {{ errorCount(row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="viewDetail(row.id)">
              查看
            </el-button>
            <el-button size="small" type="danger" link @click="handleDelete(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <div v-else class="empty-state">
      <el-icon class="empty-icon"><FolderOpened /></el-icon>
      <p>暂无历史记录</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { FolderOpened } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useReviewStore } from '@/stores/review'
import type { HistoryRecord } from '@/types/review'

const store = useReviewStore()
const router = useRouter()

function errorCount(row: HistoryRecord): number {
  return row.result.issues.filter((i) => i.severity === 'error').length
}

function formatTime(iso: string): string {
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function viewDetail(id: string) {
  router.push({ name: 'review-detail', params: { id } })
}

function handleDelete(id: string) {
  store.deleteHistory(id)
}
</script>

<style scoped>
.history-view {
  padding: 0;
}

.page-title {
  margin-bottom: 20px;
  font-size: 22px;
  font-weight: 600;
}

.history-table {
  width: 100%;
}

.code-snippet {
  font-family: Consolas, monospace;
  font-size: 12px;
  color: #606266;
}

.error-count {
  color: #f56c6c;
  font-weight: 600;
}

.empty-state {
  text-align: center;
  padding: 80px 0;
  color: #909399;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
</style>
