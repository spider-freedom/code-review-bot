import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createSSEStream } from '@/utils/sse'

interface TestChunk {
  type: string
  text: string
}

function createMockResponse(
  chunks: string[],
  options?: { status?: number; statusText?: string; emptyBody?: boolean },
): Response {
  const status = options?.status ?? 200
  const statusText = options?.statusText ?? 'OK'

  if (options?.emptyBody) {
    return {
      ok: status >= 200 && status < 300,
      status,
      statusText,
      body: null,
    } as unknown as Response
  }

  const encoder = new TextEncoder()

  const readable = new ReadableStream({
    async start(controller) {
      for (const chunk of chunks) {
        controller.enqueue(encoder.encode(chunk))
      }
      controller.close()
    },
  })

  return {
    ok: status >= 200 && status < 300,
    status,
    statusText,
    body: readable,
  } as unknown as Response
}

describe('createSSEStream', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('正常接收单个 chunk 并调用 onChunk', async () => {
    const onChunk = vi.fn()
    const onDone = vi.fn()
    const onError = vi.fn()

    const response = createMockResponse([
      'data: {"type":"issue","text":"问题1"}\n\n',
    ])
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(response)

    createSSEStream<TestChunk>('/api/test', { code: 'test' }, { onChunk, onDone, onError })

    await vi.waitFor(() => expect(onChunk).toHaveBeenCalledTimes(1))
    expect(onChunk).toHaveBeenCalledWith({ type: 'issue', text: '问题1' })
    expect(onDone).toHaveBeenCalledTimes(1)
    expect(onError).not.toHaveBeenCalled()
  })

  it('连续接收多个 chunk', async () => {
    const onChunk = vi.fn()
    const onDone = vi.fn()
    const onError = vi.fn()

    const response = createMockResponse([
      'data: {"type":"issue","text":"1"}\n\n',
      'data: {"type":"issue","text":"2"}\n\n',
      'data: {"type":"done","text":"完成"}\n\n',
    ])
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(response)

    createSSEStream<TestChunk>('/api/test', { code: 'test' }, { onChunk, onDone, onError })

    await vi.waitFor(() => expect(onDone).toHaveBeenCalledTimes(1))
    expect(onChunk).toHaveBeenCalledTimes(3)
    expect(onChunk).toHaveBeenNthCalledWith(1, { type: 'issue', text: '1' })
    expect(onChunk).toHaveBeenNthCalledWith(2, { type: 'issue', text: '2' })
    expect(onChunk).toHaveBeenNthCalledWith(3, { type: 'done', text: '完成' })
  })

  it('跨 chunk 的 SSE 数据正确拼接', async () => {
    const onChunk = vi.fn()
    const onDone = vi.fn()
    const onError = vi.fn()

    const response = createMockResponse([
      'data: {"type":"issue","',
      'text":"split chunk"}\n\n',
    ])
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(response)

    createSSEStream<TestChunk>('/api/test', { code: 'test' }, { onChunk, onDone, onError })

    await vi.waitFor(() => expect(onDone).toHaveBeenCalledTimes(1))
    expect(onChunk).toHaveBeenCalledTimes(1)
    expect(onChunk).toHaveBeenCalledWith({ type: 'issue', text: 'split chunk' })
  })

  it('HTTP 错误状态码触发 onError', async () => {
    const onChunk = vi.fn()
    const onDone = vi.fn()
    const onError = vi.fn()

    const response = createMockResponse([], { status: 500, statusText: 'Internal Server Error' })
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(response)

    createSSEStream<TestChunk>('/api/test', { code: 'test' }, { onChunk, onDone, onError })

    await vi.waitFor(() => expect(onError).toHaveBeenCalledTimes(1))
    expect(onError).toHaveBeenCalledWith(expect.objectContaining({ message: expect.stringContaining('500') }))
    expect(onChunk).not.toHaveBeenCalled()
    expect(onDone).not.toHaveBeenCalled()
  })

  it('空响应体触发 onError', async () => {
    const onChunk = vi.fn()
    const onDone = vi.fn()
    const onError = vi.fn()

    const response = createMockResponse([], { emptyBody: true })
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(response)

    createSSEStream<TestChunk>('/api/test', { code: 'test' }, { onChunk, onDone, onError })

    await vi.waitFor(() => expect(onError).toHaveBeenCalledTimes(1))
    expect(onError).toHaveBeenCalledWith(expect.objectContaining({ message: 'Response body is empty' }))
    expect(onDone).not.toHaveBeenCalled()
  })

  it('网络错误触发 onError', async () => {
    const onChunk = vi.fn()
    const onDone = vi.fn()
    const onError = vi.fn()

    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('Network error'))

    createSSEStream<TestChunk>('/api/test', { code: 'test' }, { onChunk, onDone, onError })

    await vi.waitFor(() => expect(onError).toHaveBeenCalledTimes(1))
    expect(onError).toHaveBeenCalledWith(expect.objectContaining({ message: 'Network error' }))
  })

  it('abort 取消请求后不触发 onError', async () => {
    const onChunk = vi.fn()
    const onDone = vi.fn()
    const onError = vi.fn()

    vi.spyOn(globalThis, 'fetch').mockRejectedValue(
      Object.assign(new Error('Aborted'), { name: 'AbortError' }),
    )

    const { abort } = createSSEStream<TestChunk>('/api/test', { code: 'test' }, { onChunk, onDone, onError })
    abort()

    // AbortError should be silently ignored
    await new Promise((r) => setTimeout(r, 50))
    expect(onError).not.toHaveBeenCalled()
    expect(onDone).not.toHaveBeenCalled()
  })

  it('JSON 解析异常跳过该条数据继续处理', async () => {
    const onChunk = vi.fn()
    const onDone = vi.fn()
    const onError = vi.fn()

    const response = createMockResponse([
      'data: not valid json\n\n',
      'data: {"type":"issue","text":"valid"}\n\n',
    ])
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(response)

    createSSEStream<TestChunk>('/api/test', { code: 'test' }, { onChunk, onDone, onError })

    await vi.waitFor(() => expect(onDone).toHaveBeenCalledTimes(1))
    // 第一条 JSON 解析失败被跳过，第二条正常
    expect(onChunk).toHaveBeenCalledTimes(1)
    expect(onChunk).toHaveBeenCalledWith({ type: 'issue', text: 'valid' })
  })

  it('空 data 行被忽略', async () => {
    const onChunk = vi.fn()
    const onDone = vi.fn()
    const onError = vi.fn()

    const response = createMockResponse([
      '\n\n',
      'data: {"type":"issue","text":"only"}\n\n',
      '   \n\n',
    ])
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(response)

    createSSEStream<TestChunk>('/api/test', { code: 'test' }, { onChunk, onDone, onError })

    await vi.waitFor(() => expect(onDone).toHaveBeenCalledTimes(1))
    expect(onChunk).toHaveBeenCalledTimes(1)
    expect(onChunk).toHaveBeenCalledWith({ type: 'issue', text: 'only' })
  })
})
