import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useReviewStore } from '@/stores/review'
import type { ReviewIssue, HistoryRecord } from '@/types/review'

vi.mock('@/api/review', () => ({
  reviewCodeStream: vi.fn(),
}))

const sampleIssue: ReviewIssue = {
  severity: 'error',
  line: 10,
  title: '空值检查缺失',
  description: '变量可能为 null',
  suggestion: '添加空值检查',
}

const sampleIssue2: ReviewIssue = {
  severity: 'warning',
  line: 20,
  title: '硬编码值',
  description: '不应硬编码',
  suggestion: '使用常量',
}

function createStore() {
  const pinia = createPinia()
  setActivePinia(pinia)
  return useReviewStore()
}

describe('useReviewStore', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('初始状态为 idle，无结果无历史', () => {
    const store = createStore()
    expect(store.status).toBe('idle')
    expect(store.currentResult).toBeNull()
    expect(store.streamIssues).toEqual([])
    expect(store.history).toEqual([])
  })

  it('addIssue 正确追加问题到流式列表', () => {
    const store = createStore()
    store.addIssue(sampleIssue)
    store.addIssue(sampleIssue2)
    expect(store.streamIssues).toHaveLength(2)
    expect(store.streamIssues[0].severity).toBe('error')
    expect(store.streamIssues[1].severity).toBe('warning')
  })

  it('finishReview 完成审查并自动保存历史', () => {
    const store = createStore()
    store.addIssue(sampleIssue)
    store.finishReview('总结文本')

    expect(store.status).toBe('done')
    expect(store.currentResult).not.toBeNull()
    expect(store.currentResult!.summary).toBe('总结文本')
    expect(store.currentResult!.issues).toHaveLength(1)
    expect(store.history).toHaveLength(1)
    expect(store.history[0].result.summary).toBe('总结文本')
  })

  it('saveToHistory 正确持久化到 localStorage', () => {
    const store = createStore()
    const result = { issues: [sampleIssue], summary: '测试总结' }
    store.saveToHistory(result, 'const a = 1', 'code')

    expect(store.history).toHaveLength(1)
    expect(store.history[0].code).toBe('const a = 1')
    expect(store.history[0].mode).toBe('code')

    const raw = localStorage.getItem('code-review-history')
    expect(raw).not.toBeNull()
    const parsed: HistoryRecord[] = JSON.parse(raw!)
    expect(parsed).toHaveLength(1)
    expect(parsed[0].code).toBe('const a = 1')
  })

  it('deleteHistory 删除记录并同步 localStorage', () => {
    const store = createStore()
    const result = { issues: [sampleIssue], summary: 'S1' }
    store.saveToHistory(result, 'code1', 'code')
    store.saveToHistory(result, 'code2', 'diff')
    expect(store.history).toHaveLength(2)

    const idToDelete = store.history[0].id
    store.deleteHistory(idToDelete)
    expect(store.history).toHaveLength(1)
    // saveToHistory prepends: [code2, code1]; deleting [0] leaves code1
    expect(store.history[0].code).toBe('code1')

    const raw = localStorage.getItem('code-review-history')
    const parsed: HistoryRecord[] = JSON.parse(raw!)
    expect(parsed).toHaveLength(1)
  })

  it('历史记录上限为 50 条，超出自动删除最早的', () => {
    const store = createStore()
    const result = { issues: [sampleIssue], summary: 'S' }

    for (let i = 0; i < 55; i++) {
      store.saveToHistory(result, `code-${i}`, 'code')
    }

    expect(store.history).toHaveLength(50)
    // 最新一条应该最后保存的
    expect(store.history[0].code).toBe('code-54')
    // 最旧一条应该是 code-5（前5条被删除）
    expect(store.history[49].code).toBe('code-5')

    const raw = localStorage.getItem('code-review-history')
    const parsed: HistoryRecord[] = JSON.parse(raw!)
    expect(parsed).toHaveLength(50)
  })

  it('clearCurrentReview 重置所有当前审查状态', () => {
    const store = createStore()
    store.addIssue(sampleIssue)
    store.finishReview('总结')
    expect(store.status).toBe('done')
    expect(store.currentResult).not.toBeNull()

    store.clearCurrentReview()
    expect(store.status).toBe('idle')
    expect(store.currentResult).toBeNull()
    expect(store.streamIssues).toEqual([])
    expect(store.diffContent).toBe('')
    expect(store.currentCode).toBe('')
    // 历史记录不应被清除
    expect(store.history).toHaveLength(1)
  })

  it('issueCount getter 正确计算 currentResult 的 issues 数量', () => {
    const store = createStore()
    expect(store.issueCount).toBe(0)

    store.addIssue(sampleIssue)
    store.addIssue(sampleIssue2)
    expect(store.issueCount).toBe(2)

    store.finishReview('完成')
    expect(store.issueCount).toBe(2)
  })

  it('errorCount getter 只统计严重问题', () => {
    const store = createStore()
    store.addIssue(sampleIssue)       // error
    store.addIssue(sampleIssue2)      // warning
    store.addIssue({ severity: 'info', line: 30, title: 'T', description: 'D', suggestion: 'S' })
    expect(store.errorCount).toBe(1)
  })

  it('historyById getter 返回正确的历史记录', () => {
    const store = createStore()
    const result = { issues: [sampleIssue], summary: 'S' }
    store.saveToHistory(result, 'code', 'code')
    const id = store.history[0].id

    const found = store.historyById(id)
    expect(found).not.toBeNull()
    expect(found!.code).toBe('code')

    const notFound = store.historyById('nonexistent')
    expect(notFound).toBeNull()
  })

  it('页面加载时从 localStorage 恢复历史', () => {
    const existing: HistoryRecord[] = [
      {
        id: 'abc123',
        createdAt: '2025-01-01T00:00:00Z',
        code: 'old code',
        mode: 'code',
        result: { issues: [sampleIssue], summary: '旧审查' },
      },
    ]
    localStorage.setItem('code-review-history', JSON.stringify(existing))

    const store = createStore()
    expect(store.history).toHaveLength(1)
    expect(store.history[0].code).toBe('old code')
  })
})
