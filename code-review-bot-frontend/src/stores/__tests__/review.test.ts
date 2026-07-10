import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useReviewStore } from '@/stores/review'
import { reviewCodeStream } from '@/api/review'
import type { ReviewIssue } from '@/types/review'

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
    vi.clearAllMocks()
  })

  it('初始状态为 idle，无结果', () => {
    const store = createStore()
    expect(store.status).toBe('idle')
    expect(store.currentResult).toBeNull()
    expect(store.streamIssues).toEqual([])
  })

  it('addIssue 正确追加问题到流式列表', () => {
    const store = createStore()
    store.addIssue(sampleIssue)
    store.addIssue(sampleIssue2)
    expect(store.streamIssues).toHaveLength(2)
    expect(store.streamIssues[0].severity).toBe('error')
    expect(store.streamIssues[1].severity).toBe('warning')
  })

  it('finishReview 完成审查并设置 currentResult', () => {
    const store = createStore()
    store.addIssue(sampleIssue)
    store.finishReview('总结文本')

    expect(store.status).toBe('done')
    expect(store.currentResult).not.toBeNull()
    expect(store.currentResult!.summary).toBe('总结文本')
    expect(store.currentResult!.issues).toHaveLength(1)
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

  it('startReview 正确重置状态并变为 loading', () => {
    const mockAbort = vi.fn()
    vi.mocked(reviewCodeStream).mockReturnValue({ abort: mockAbort })

    const store = createStore()
    // Set some prior state
    store.addIssue(sampleIssue)
    store.finishReview('prior')
    expect(store.status).toBe('done')

    const { abort } = store.startReview('test code', 'code')
    expect(store.status).toBe('loading')
    expect(store.currentResult).toBeNull()
    expect(store.streamIssues).toEqual([])
    expect(store.currentCode).toBe('test code')
    expect(store.currentMode).toBe('code')
    expect(typeof abort).toBe('function')
  })

  it('stopReview 调用 abort 并重置状态', () => {
    const mockAbort = vi.fn()
    vi.mocked(reviewCodeStream).mockReturnValue({ abort: mockAbort })

    const store = createStore()
    store.startReview('test', 'code')

    store.stopReview()
    expect(mockAbort).toHaveBeenCalledTimes(1)
    expect(store.status).toBe('idle')
  })
})