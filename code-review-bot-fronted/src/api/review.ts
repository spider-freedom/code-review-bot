import type { ReviewIssue } from '@/types/review'
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
    '/api/review',
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
