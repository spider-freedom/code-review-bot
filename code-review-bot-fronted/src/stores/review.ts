import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ReviewIssue, ReviewResult, ReviewStatus, HistoryRecord } from '@/types/review'
import { reviewCodeStream } from '@/api/review'

const HISTORY_KEY = 'code-review-history'
const MAX_HISTORY = 50

function loadFromStorage(): HistoryRecord[] {
  try {
    const raw = localStorage.getItem(HISTORY_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveToStorage(records: HistoryRecord[]) {
  localStorage.setItem(HISTORY_KEY, JSON.stringify(records))
}

export const useReviewStore = defineStore('review', () => {
  // ---- state ----
  const currentResult = ref<ReviewResult | null>(null)
  const status = ref<ReviewStatus>('idle')
  const streamIssues = ref<ReviewIssue[]>([])
  const streamSummary = ref('')
  const diffContent = ref('')
  const currentCode = ref('')
  const currentMode = ref<'code' | 'diff'>('code')
  const history = ref<HistoryRecord[]>(loadFromStorage())

  // ---- getters ----
  const issueCount = computed(() => {
    if (currentResult.value) return currentResult.value.issues.length
    return streamIssues.value.length
  })

  const errorCount = computed(() => {
    const issues = currentResult.value?.issues ?? streamIssues.value
    return issues.filter((i) => i.severity === 'error').length
  })

  const historyById = computed(() => {
    return (id: string) => history.value.find((r) => r.id === id) ?? null
  })

  // ---- actions ----
  function startReview(code: string, mode: 'code' | 'diff'): { abort: () => void } {
    diffContent.value = code
    currentCode.value = code
    currentMode.value = mode
    status.value = 'loading'
    currentResult.value = null
    streamIssues.value = []
    streamSummary.value = ''

    const ctrl = reviewCodeStream(code, mode, {
      onIssue(issue) {
        if (status.value === 'loading') status.value = 'streaming'
        streamIssues.value = [...streamIssues.value, issue]
      },
      onDone(summary) {
        currentResult.value = {
          issues: [...streamIssues.value],
          summary,
        }
        status.value = 'done'
        saveToHistory(
          currentResult.value,
          currentCode.value,
          currentMode.value,
        )
      },
      onError(err) {
        console.error('审查失败:', err)
        status.value = 'error'
      },
    })

    return ctrl
  }

  function addIssue(issue: ReviewIssue) {
    streamIssues.value.push(issue)
  }

  function finishReview(summary: string) {
    currentResult.value = { issues: [...streamIssues.value], summary }
    status.value = 'done'
    saveToHistory(currentResult.value, currentCode.value, currentMode.value)
  }

  function saveToHistory(result: ReviewResult, code: string, mode: 'code' | 'diff') {
    const record: HistoryRecord = {
      id: Date.now().toString(36) + Math.random().toString(36).slice(2, 8),
      createdAt: new Date().toISOString(),
      code,
      mode,
      result,
    }
    history.value = [record, ...history.value].slice(0, MAX_HISTORY)
    saveToStorage(history.value)
  }

  function deleteHistory(id: string) {
    history.value = history.value.filter((r) => r.id !== id)
    saveToStorage(history.value)
  }

  function clearCurrentReview() {
    currentResult.value = null
    status.value = 'idle'
    streamIssues.value = []
    streamSummary.value = ''
    diffContent.value = ''
    currentCode.value = ''
    currentMode.value = 'code'
  }

  return {
    // state
    currentResult,
    status,
    streamIssues,
    streamSummary,
    diffContent,
    currentCode,
    currentMode,
    history,
    // getters
    issueCount,
    errorCount,
    historyById,
    // actions
    startReview,
    addIssue,
    finishReview,
    saveToHistory,
    deleteHistory,
    clearCurrentReview,
  }
})
