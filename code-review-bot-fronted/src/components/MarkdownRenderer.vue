<template>
  <div class="markdown-body" v-html="rendered" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'

const props = defineProps<{
  content: string
}>()

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight(code: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch {
        // fall through
      }
    }
    return ''
  },
})

const rendered = computed(() => md.render(props.content))
</script>

<style>
.markdown-body {
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
}

.markdown-body h1,
.markdown-body h2,
.markdown-body h3,
.markdown-body h4 {
  margin: 16px 0 8px;
  font-weight: 600;
}

.markdown-body h1 { font-size: 1.5em; }
.markdown-body h2 { font-size: 1.3em; }
.markdown-body h3 { font-size: 1.1em; }

.markdown-body p {
  margin: 0 0 10px;
}

.markdown-body ul,
.markdown-body ol {
  padding-left: 24px;
  margin-bottom: 10px;
}

.markdown-body li {
  margin-bottom: 4px;
}

.markdown-body code {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 0.9em;
  font-family: Consolas, monospace;
}

.markdown-body pre {
  background: #1e1e1e;
  border-radius: 8px;
  padding: 14px 16px;
  overflow-x: auto;
  margin-bottom: 12px;
}

.markdown-body pre code {
  background: none;
  padding: 0;
  font-size: 13px;
  color: #d4d4d4;
}

.markdown-body blockquote {
  border-left: 3px solid #409eff;
  margin: 0 0 10px;
  padding: 4px 16px;
  color: #606266;
  background: #f0f5ff;
  border-radius: 0 4px 4px 0;
}

.markdown-body table {
  border-collapse: collapse;
  margin-bottom: 12px;
  width: 100%;
}

.markdown-body th,
.markdown-body td {
  border: 1px solid #e4e7ed;
  padding: 8px 12px;
  text-align: left;
}

.markdown-body th {
  background: #f6f8fa;
  font-weight: 600;
}

.markdown-body a {
  color: #409eff;
}

.markdown-body img {
  max-width: 100%;
}
</style>
