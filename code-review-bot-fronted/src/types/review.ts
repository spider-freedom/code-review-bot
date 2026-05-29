export interface ReviewIssue {
  severity: 'error' | 'warning' | 'info'
  line: number
  title: string
  description: string
  suggestion: string
  codeExample?: string
}

export interface ReviewResult {
  issues: ReviewIssue[]
  summary: string
}

export type ReviewStatus = 'idle' | 'loading' | 'streaming' | 'done' | 'error'

export interface HistoryRecord {
  id: string
  createdAt: string
  code: string
  mode: 'code' | 'diff'
  result: ReviewResult
}
