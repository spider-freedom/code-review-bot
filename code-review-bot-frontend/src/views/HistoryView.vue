<template>
  <div class="history-view">
    <h2 class="page-title">历史记录</h2>

    <template v-if="tasks.length > 0">
      <el-table :data="tasks" stripe class="history-table" v-loading="loading">
        <el-table-column label="审查时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="代码片段" min-width="280">
          <template #default="{ row }">
            <span class="code-snippet">{{ (row.code || '').slice(0, 60) }}{{ (row.code || '').length > 60 ? '...' : '' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="模式" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.mode === 'diff' ? 'success' : ''">
              {{ row.mode === 'diff' ? 'Git Diff' : '代码' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'COMPLETED'" size="small" type="success">已完成</el-tag>
            <el-tag v-else-if="row.status === 'PROCESSING'" size="small" type="warning">处理中</el-tag>
            <el-tag v-else-if="row.status === 'FAILED'" size="small" type="danger">失败</el-tag>
            <el-tag v-else size="small" type="info">等待中</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="viewDetail(row.taskId)">
              查看
            </el-button>
            <el-button size="small" type="danger" link @click="handleDelete(row.taskId)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <div v-else-if="!loading" class="empty-state">
      <el-icon class="empty-icon"><FolderOpened /></el-icon>
      <p>暂无历史记录</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { FolderOpened } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { fetchHistory } from '@/api/review'
import { formatTime } from '@/utils/format'
import type { ReviewTask } from '@/types/review'

const router = useRouter()
const tasks = ref<ReviewTask[]>([])
const loading = ref(false)

onMounted(() => loadHistory())

async function loadHistory() {
  loading.value = true
  try {
    tasks.value = await fetchHistory(1, 50)
  } catch {
    tasks.value = []
  } finally {
    loading.value = false
  }
}

function viewDetail(taskId: string) {
  router.push({ name: 'review-detail', params: { id: taskId } })
}

async function handleDelete(taskId: string) {
  try {
    await ElMessageBox.confirm('确定要删除这条审查记录吗？', '确认删除', {
      type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消',
    })
    tasks.value = tasks.value.filter(t => t.taskId !== taskId)
  } catch {
    // cancelled
  }
}
</script>

<style scoped>
.history-view { padding: 0 20px; }
.page-title { font-size: 20px; margin-bottom: 20px; }
.code-snippet { color: #999; font-family: monospace; }
.empty-state { text-align: center; padding: 60px 0; color: #999; }
.empty-icon { font-size: 48px; margin-bottom: 10px; }
</style>
