export interface SSEStreamCallbacks<T> {
  onChunk: (data: T) => void
  onDone: () => void
  onError: (error: Error) => void
}

export function createSSEStream<T>(
  url: string,
  body: Record<string, unknown>,
  callbacks: SSEStreamCallbacks<T>,
): { abort: () => void } {
  const controller = new AbortController()

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
        const parts = buffer.split('\n\n')
        buffer = parts.pop() ?? ''

        for (const part of parts) {
          const trimmed = part.trim()
          if (!trimmed) continue

          const lines = trimmed.split('\n')
          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const jsonStr = line.slice(6)
              try {
                const data = JSON.parse(jsonStr) as T
                callbacks.onChunk(data)
              } catch {
                // skip malformed JSON
              }
            }
          }
        }
      }
      callbacks.onDone()
    })
    .catch((err) => {
      if (err.name !== 'AbortError') {
        callbacks.onError(err instanceof Error ? err : new Error(String(err)))
      }
    })

  return {
    abort: () => controller.abort(),
  }
}
