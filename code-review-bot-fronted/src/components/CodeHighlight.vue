<template>
  <div class="code-highlight">
    <div class="ch-toolbar">
      <span class="ch-lang">{{ detectedLang }}</span>
      <el-button size="small" text @click="copyCode">
        <el-icon><DocumentCopy /></el-icon>
        复制代码
      </el-button>
    </div>
    <pre class="ch-block"><code v-html="highlighted" /></pre>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { DocumentCopy } from '@element-plus/icons-vue'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'

const props = defineProps<{
  code: string
  language?: string
}>()

const detectedLang = computed(() => {
  if (props.language) return props.language
  const result = hljs.highlightAuto(props.code)
  return result.language ?? 'text'
})

const highlighted = computed(() => {
  if (props.language) {
    const lang = hljs.getLanguage(props.language)
    if (lang) return hljs.highlight(props.code, { language: props.language }).value
  }
  return hljs.highlightAuto(props.code).value
})

async function copyCode() {
  try {
    await navigator.clipboard.writeText(props.code)
  } catch {
    // fallback
  }
}
</script>

<style scoped>
.code-highlight {
  border: 1px solid #3c3c3c;
  border-radius: 8px;
  overflow: hidden;
}

.ch-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 14px;
  background: #252526;
  border-bottom: 1px solid #3c3c3c;
}

.ch-lang {
  font-size: 11px;
  color: #8b949e;
  font-weight: 500;
  text-transform: lowercase;
  letter-spacing: 0.3px;
}

.ch-block {
  margin: 0;
  padding: 14px 16px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.6;
  background: #1e1e1e;
}
</style>
