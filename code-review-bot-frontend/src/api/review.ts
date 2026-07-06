import type { ReviewIssue, ReviewTaskResponse } from '@/types/review'
import { createSSEStream } from '@/utils/sse'

interface SSEChunk {
  type: 'issue' | 'done' | 'error'
  severity?: string
  line?: number
  title?: string
  description?: string
  suggestion?: string
  codeExample?: string
  summary?: string
  message?: string
}

function toReviewIssue(chunk: SSEChunk): ReviewIssue {
  return {
    severity: (chunk.severity as ReviewIssue['severity']) || 'info',
    line: chunk.line ?? 0,
    title: chunk.title ?? '',
    description: chunk.description ?? '',
    suggestion: chunk.suggestion ?? '',
    codeExample: chunk.codeExample,
  }
}

// ── SSE streaming mode (legacy, kept for real-time preview) ────────────────

export function reviewCodeStream(
  code: string,
  mode: 'code' | 'diff',
  callbacks: {
    onIssue: (issue: ReviewIssue) => void
    onDone: (summary: string) => void
    onError: (error: Error) => void
  },
): { abort: () => void } {
  return createSSEStream<SSEChunk>(
    '/api/review/stream',
    { code, mode },
    {
      onChunk(chunk) {
        switch (chunk.type) {
          case 'issue':
            callbacks.onIssue(toReviewIssue(chunk))
            break
          case 'done':
            callbacks.onDone(chunk.summary ?? '审查完成')
            break
          case 'error':
            callbacks.onError(new Error(chunk.message ?? '未知错误'))
            break
        }
      },
      onDone() {
        // stream ended cleanly — no additional action needed
      },
      onError(err) {
        callbacks.onError(err)
      },
    },
  )
}

// ── Async submit + poll mode ───────────────────────────────────────────────

const API_BASE = '/api/review'

/**
 * Submit code review asynchronously. Returns immediately with taskId.
 */
export async function submitReview(code: string, mode: 'code' | 'diff'): Promise<ReviewTaskResponse> {
  const response = await fetch(`${API_BASE}/submit`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code, mode }),
  })
  if (!response.ok) {
    if (response.status === 429) throw new Error('请求过于频繁，每分钟最多 5 次审查')
    throw new Error(`提交失败 (HTTP ${response.status})`)
  }
  const data = await response.json()
  return { taskId: data.taskId, status: data.status, createTime: '', updateTime: '' }
}

/**
 * Poll task status until COMPLETED or FAILED.
 */
export async function getTaskStatus(taskId: string): Promise<ReviewTaskResponse> {
  const response = await fetch(`${API_BASE}/tasks/${taskId}`)
  if (!response.ok) throw new Error(`查询任务状态失败 (HTTP ${response.status})`)
  return response.json()
}

/**
 * Fetch completed review issues for a task.
 */
export async function getTaskIssues(taskId: string): Promise<ReviewIssue[]> {
  const response = await fetch(`${API_BASE}/tasks/${taskId}/issues`)
  if (!response.ok) throw new Error(`查询审查结果失败 (HTTP ${response.status})`)
  return response.json()
}
