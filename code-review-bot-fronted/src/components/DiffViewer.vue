<template>
  <div class="diff-viewer">
    <div v-if="!lines.length" class="diff-empty">
      暂无差异内容
    </div>
    <div v-else class="diff-table">
      <div
        v-for="(line, index) in lines"
        :key="index"
        class="diff-row"
        :class="lineClass(line.type)"
      >
        <span class="line-num old-num">{{ line.oldLine ?? '' }}</span>
        <span class="line-num new-num">{{ line.newLine ?? '' }}</span>
        <span class="line-sign">{{ line.sign }}</span>
        <pre class="line-content">{{ line.content }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export interface DiffLine {
  type: 'add' | 'delete' | 'normal' | 'header' | 'hunk'
  content: string
  oldLine?: number
  newLine?: number
  sign: string
}

const props = defineProps<{
  diffText: string
}>()

const lines = computed<DiffLine[]>(() => {
  if (!props.diffText) return []
  return parseDiff(props.diffText)
})

function parseDiff(text: string): DiffLine[] {
  const rawLines = text.split('\n')
  const result: DiffLine[] = []
  let oldLine = 0
  let newLine = 0

  for (const raw of rawLines) {
    if (raw.startsWith('diff --git') || raw.startsWith('index ') ||
        raw.startsWith('---') || raw.startsWith('+++')) {
      result.push({ type: 'header', content: raw, sign: '' })
      continue
    }

    if (raw.startsWith('@@')) {
      const match = raw.match(/@@ -(\d+),?\d* \+(\d+),?\d* @@/)
      if (match) {
        oldLine = parseInt(match[1], 10)
        newLine = parseInt(match[2], 10)
      }
      result.push({ type: 'hunk', content: raw, sign: '', oldLine: undefined, newLine: undefined })
      continue
    }

    if (raw.startsWith('+')) {
      result.push({ type: 'add', content: raw.slice(1), oldLine: undefined, newLine: newLine, sign: '+' })
      newLine++
    } else if (raw.startsWith('-')) {
      result.push({ type: 'delete', content: raw.slice(1), oldLine: oldLine, newLine: undefined, sign: '-' })
      oldLine++
    } else {
      result.push({ type: 'normal', content: raw.startsWith(' ') ? raw.slice(1) : raw, oldLine: oldLine, newLine: newLine, sign: ' ' })
      oldLine++
      newLine++
    }
  }

  return result
}

function lineClass(type: DiffLine['type']): string {
  return `diff-${type}`
}
</script>

<style scoped>
.diff-viewer {
  background: #1e1e1e;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
}

.diff-empty {
  text-align: center;
  color: #8b949e;
  padding: 40px 0;
  font-size: 14px;
}

.diff-table {
  font-family: 'Cascadia Code', 'Fira Code', 'JetBrains Mono', Consolas, monospace;
  font-size: 13px;
  line-height: 1.5;
  overflow-x: auto;
}

.diff-row {
  display: flex;
  min-height: 24px;
  align-items: stretch;
}

.diff-row.diff-add {
  background-color: #12261e;
}
.diff-row.diff-delete {
  background-color: #261212;
}
.diff-row.diff-header {
  background-color: #252526;
  color: #8b949e;
  font-weight: 600;
}
.diff-row.diff-hunk {
  background-color: #1a2333;
  color: #58a6ff;
}

.line-num {
  display: inline-block;
  width: 52px;
  min-width: 52px;
  text-align: right;
  padding-right: 10px;
  color: #858585;
  user-select: none;
  flex-shrink: 0;
  background: #1e1e1e;
}

.diff-add .line-num {
  background: #12261e;
}
.diff-delete .line-num {
  background: #261212;
}

.line-sign {
  display: inline-block;
  width: 22px;
  min-width: 22px;
  text-align: center;
  color: #858585;
  user-select: none;
  flex-shrink: 0;
}

.diff-add .line-sign { color: #3fb950; }
.diff-delete .line-sign { color: #f85149; }

.line-content {
  margin: 0;
  padding: 2px 8px;
  white-space: pre-wrap;
  word-break: break-all;
  flex: 1;
  color: #d4d4d4;
}
</style>
