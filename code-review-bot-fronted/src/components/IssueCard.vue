<template>
  <div class="issue-card" :class="`issue--${issue.severity}`">
    <div class="issue-header" @click="expanded = !expanded">
      <span class="severity-dot" :class="`severity--${issue.severity}`" />
      <span class="issue-title">{{ issue.title }}</span>
      <el-tag :type="severityTagType" size="small" class="severity-tag">
        {{ severityLabel }}
      </el-tag>
      <span class="issue-line">第 {{ issue.line }} 行</span>
      <el-icon class="expand-icon" :class="{ rotated: expanded }">
        <ArrowDown />
      </el-icon>
    </div>

    <div v-show="expanded" class="issue-body">
      <div class="detail-block">
        <h4>问题描述</h4>
        <p>{{ issue.description }}</p>
      </div>

      <div class="detail-block">
        <h4>修改建议</h4>
        <p>{{ issue.suggestion }}</p>
      </div>

      <div v-if="issue.codeExample" class="detail-block">
        <h4>代码示例</h4>
        <CodeHighlight :code="issue.codeExample" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import type { ReviewIssue } from '@/types/review'
import CodeHighlight from './CodeHighlight.vue'

const props = defineProps<{
  issue: ReviewIssue
}>()

const expanded = ref(false)

const severityLabel = computed(() => {
  const map: Record<string, string> = {
    error: '严重',
    warning: '建议',
    info: '优化',
  }
  return map[props.issue.severity]
})

const severityTagType = computed(() => {
  const map: Record<string, string> = {
    error: 'danger',
    warning: 'warning',
    info: 'info',
  }
  return map[props.issue.severity]
})
</script>

<style scoped>
.issue-card {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  margin-bottom: 8px;
  overflow: hidden;
}

.issue-card.issue--error {
  border-left: 3px solid #f56c6c;
}
.issue-card.issue--warning {
  border-left: 3px solid #e6a23c;
}
.issue-card.issue--info {
  border-left: 3px solid #409eff;
}

.issue-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;
  background: #fafafa;
}
.issue-header:hover {
  background: #f0f2f5;
}

.severity-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.severity--error { background: #f56c6c; }
.severity--warning { background: #e6a23c; }
.severity--info { background: #409eff; }

.issue-title {
  font-weight: 500;
  font-size: 14px;
  flex: 1;
}

.severity-tag {
  flex-shrink: 0;
}

.issue-line {
  color: #909399;
  font-size: 12px;
  flex-shrink: 0;
}

.expand-icon {
  color: #909399;
  transition: transform 0.2s;
  flex-shrink: 0;
}
.expand-icon.rotated {
  transform: rotate(180deg);
}

.issue-body {
  padding: 14px;
  border-top: 1px solid #ebeef5;
}

.detail-block {
  margin-bottom: 14px;
}
.detail-block:last-child { margin-bottom: 0; }

.detail-block h4 {
  font-size: 13px;
  color: #606266;
  margin: 0 0 6px;
}

.detail-block p {
  font-size: 13px;
  color: #303133;
  line-height: 1.6;
  margin: 0;
}
</style>
