<template>
  <div class="code-input">
    <el-tabs v-model="inputMode" class="mode-tabs">
      <el-tab-pane label="粘贴代码片段" name="code" />
      <el-tab-pane label="粘贴 Git Diff" name="diff" />
    </el-tabs>

    <el-input
      v-model="content"
      type="textarea"
      :rows="16"
      :placeholder="placeholder"
      class="code-textarea"
    />

    <div class="input-footer">
      <span class="char-count">{{ content.length }} 字符</span>
      <el-button
        type="primary"
        size="large"
        :disabled="!content.trim()"
        :loading="loading"
        @click="handleReview"
      >
        {{ loading ? '正在审查...' : '开始审查' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const emit = defineEmits<{
  review: [content: string, mode: 'code' | 'diff']
}>()

defineProps<{
  loading?: boolean
}>()

const inputMode = ref<'code' | 'diff'>('code')
const content = ref('')

const placeholder = computed(() =>
  inputMode.value === 'diff'
    ? '请粘贴 git diff 内容...'
    : '请粘贴需要审查的代码...\n\n例如：\nfunction getUser(id) {\n  const user = fetchUser(id)\n  return user.name\n}',
)

function handleReview() {
  if (!content.value.trim()) return
  emit('review', content.value, inputMode.value)
}
</script>

<style scoped>
.code-input {
  background: #fff;
  border-radius: 10px;
  padding: 18px 20px 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.mode-tabs {
  margin-bottom: 10px;
}

.code-textarea :deep(.el-textarea__inner) {
  font-family: 'Cascadia Code', 'Fira Code', 'JetBrains Mono', Consolas, monospace;
  font-size: 13px;
  line-height: 1.65;
  background: #1e1e1e;
  color: #d4d4d4;
  border: 1px solid #3c3c3c;
  border-radius: 6px;
  resize: vertical;
  min-height: 280px;
}
.code-textarea :deep(.el-textarea__inner):focus {
  border-color: #3451b2;
  box-shadow: 0 0 0 2px rgba(52, 81, 178, 0.15);
}
.code-textarea :deep(.el-textarea__inner)::placeholder {
  color: #6a6a6a;
}

.input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}

.char-count {
  font-size: 12px;
  color: #909399;
}
</style>
