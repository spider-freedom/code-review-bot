export interface SSEStreamCallbacks<T> {
  onChunk: (data: T) => void
  onDone: () => void
  onError: (error: Error) => void
}

const DEFAULT_TIMEOUT_MS = 5 * 60 * 1000

export function createSSEStream<T>(
  url: string,
  body: Record<string, unknown>,
  callbacks: SSEStreamCallbacks<T>,
): { abort: () => void } {
  const controller = new AbortController()
  let timeoutId: ReturnType<typeof setTimeout> | null = null

  const clearTimer = () => {
    if (timeoutId !== null) {
      clearTimeout(timeoutId)
      timeoutId = null
    }
  }

  timeoutId = setTimeout(() => {
    controller.abort()
    callbacks.onError(new Error('请求超时'))
  }, DEFAULT_TIMEOUT_MS)

  fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
    signal: controller.signal,
  })
    .then(async (response) => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }
      if (!response.body) {
        throw new Error('Response body is empty')
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        // CRLF → LF (HTTP chunked encoding uses \r\n)
        buffer = buffer.replace(/\r\n/g, '\n')

        const parts = buffer.split('\n\n')
        // Keep last (incomplete) part in buffer
        buffer = parts.pop() ?? ''

        for (const part of parts) {
          const trimmed = part.trim()
          if (!trimmed) continue

          for (const line of trimmed.split('\n')) {
            const jsonStr = extractDataPayload(line)
            if (jsonStr) {
              tryParsePayload(jsonStr, callbacks.onChunk)
            }
          }
        }
      }

      // Final flush: remaining decoder buffer
      buffer += decoder.decode()
      buffer = buffer.replace(/\r\n/g, '\n')
      if (buffer.trim()) {
        for (const line of buffer.trim().split('\n')) {
          const jsonStr = extractDataPayload(line)
          if (jsonStr) {
            tryParsePayload(jsonStr, callbacks.onChunk)
          }
        }
      }

      clearTimer()
      callbacks.onDone()
    })
    .catch((err) => {
      clearTimer()
      if (err.name !== 'AbortError') {
        callbacks.onError(err instanceof Error ? err : new Error(String(err)))
      }
    })

  return {
    abort: () => {
      clearTimer()
      controller.abort()
    },
  }
}

/** Extract JSON payload from a "data:" SSE line. Handles both "data:{json}" (Spring) and "data: {json}" (spec). */
function extractDataPayload(line: string): string | null {
  if (!line.startsWith('data:')) return null
  let payload = line.substring(5) // after "data:"
  if (payload.startsWith(' ')) payload = payload.substring(1)
  if (!payload) return null
  return payload
}

function tryParsePayload<T>(jsonStr: string, onChunk: (data: T) => void) {
  try {
    const data = JSON.parse(jsonStr) as T
    onChunk(data)
  } catch {
    if (import.meta.env.DEV) {
      console.warn('SSE parse error:', jsonStr.slice(0, 200))
    }
  }
}
