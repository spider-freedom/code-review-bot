import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ReviewIssue, ReviewResult, ReviewStatus } from '@/types/review'
import { reviewCodeStream, submitReview, getTaskStatus, getTaskIssues } from '@/api/review'

export const useReviewStore = defineStore('review', () => {
  // ---- state ----
  const currentResult = ref<ReviewResult | null>(null)
  const status = ref<ReviewStatus>('idle')
  const streamIssues = ref<ReviewIssue[]>([])
  const streamSummary = ref('')
  const diffContent = ref('')
  const currentCode = ref('')
  const currentMode = ref<'code' | 'diff'>('code')

  // ---- getters ----
  const issueCount = computed(() => {
    if (currentResult.value) return currentResult.value.issues.length
    return streamIssues.value.length
  })

  const errorCount = computed(() => {
    const issues = currentResult.value?.issues ?? streamIssues.value
    return issues.filter((i) => i.severity === 'error').length
  })

  // ---- actions ----
  let generation = 0
  let abortCurrent: (() => void) | null = null

  function startReview(code: string, mode: 'code' | 'diff'): { abort: () => void } {
    // Abort any in-flight stream
    abortCurrent?.()
    generation++

    diffContent.value = code
    currentCode.value = code
    currentMode.value = mode
    status.value = 'loading'
    currentResult.value = null
    streamIssues.value = []
    streamSummary.value = ''

    const gen = generation

    const ctrl = reviewCodeStream(code, mode, {
      onIssue(issue) {
        if (generation !== gen) return // Stale callback from previous review
        if (status.value === 'loading') status.value = 'streaming'
        streamIssues.value.push(issue)
      },
      onDone(summary) {
        if (generation !== gen) return
        currentResult.value = {
          issues: [...streamIssues.value],
          summary,
        }
        status.value = 'done'
        // History is now stored server-side via MySQL, fetched from /api/review/history
      },
      onError(err) {
        if (generation !== gen) return
        console.error('审查失败:', err)
        status.value = 'error'
      },
    })

    abortCurrent = ctrl.abort
    return ctrl
  }

  function stopReview() {
    abortCurrent?.()
    abortCurrent = null
    status.value = 'idle'
  }

  function addIssue(issue: ReviewIssue) {
    streamIssues.value.push(issue)
  }

  function finishReview(summary: string) {
    currentResult.value = { issues: [...streamIssues.value], summary }
    status.value = 'done'
    // History stored server-side via MySQL
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

  // ── Async submit + poll mode ───────────────────────────────────────────

  let asyncPollTimer: ReturnType<typeof setInterval> | null = null

  async function startAsyncReview(code: string, mode: 'code' | 'diff') {
    // Clean up previous poll
    if (asyncPollTimer) { clearInterval(asyncPollTimer); asyncPollTimer = null }

    currentCode.value = code
    currentMode.value = mode
    diffContent.value = code
    status.value = 'async_pending'
    currentResult.value = null
    streamIssues.value = []

    try {
      const { taskId } = await submitReview(code, mode)
      status.value = 'async_processing'

      // Poll every 2s until COMPLETED or FAILED
      asyncPollTimer = setInterval(async () => {
        try {
          const task = await getTaskStatus(taskId)
          if (task.status === 'COMPLETED') {
            clearInterval(asyncPollTimer!)
            asyncPollTimer = null
            const issues = await getTaskIssues(taskId)
            currentResult.value = {
              issues,
              summary: `审查完成，共发现 ${issues.length} 个问题`,
            }
            status.value = 'done'
            // History persisted to MySQL via ReviewAsyncService
          } else if (task.status === 'FAILED') {
            clearInterval(asyncPollTimer!)
            asyncPollTimer = null
            status.value = 'error'
            console.error('Async review failed:', task.errorMessage)
          }
        } catch (err) {
          // Retry on next poll
          console.warn('Poll error:', err)
        }
      }, 2000)
    } catch (err) {
      status.value = 'error'
      console.error('Async review submit failed:', err)
    }
  }

  function stopAsyncPolling() {
    if (asyncPollTimer) {
      clearInterval(asyncPollTimer)
      asyncPollTimer = null
    }
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
    // getters
    issueCount,
    errorCount,
    // actions
    startReview,
    stopReview,
    addIssue,
    finishReview,
    clearCurrentReview,
    // async
    startAsyncReview,
    stopAsyncPolling,
  }
})
