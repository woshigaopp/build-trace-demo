import type {
  GenerationCompleted,
  ProjectDetail,
  ProjectSummary,
  StreamHandlers,
} from './types'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? `http://${window.location.hostname}:8080`
const GUEST_KEY = 'buildtrace.guest-id'

export function guestId(): string {
  const existing = localStorage.getItem(GUEST_KEY)
  if (existing) return existing
  const created = crypto.randomUUID()
  localStorage.setItem(GUEST_KEY, created)
  return created
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      'X-Guest-Id': guestId(),
      ...init?.headers,
    },
  })
  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    throw new Error(payload?.message ?? `Request failed (${response.status})`)
  }
  return response.json() as Promise<T>
}

export const api = {
  listProjects: () => request<ProjectSummary[]>('/api/projects'),
  getProject: (id: string) => request<ProjectDetail>(`/api/projects/${id}`),
  createProject: (name: string) =>
    request<ProjectDetail>('/api/projects', {
      method: 'POST',
      body: JSON.stringify({ name }),
    }),
  restoreVersion: (projectId: string, versionId: string) =>
    request<ProjectDetail>(`/api/projects/${projectId}/versions/${versionId}/restore`, {
      method: 'POST',
    }),
  generate: async (projectId: string, prompt: string, handlers: StreamHandlers) => {
    const response = await fetch(`${API_BASE}/api/projects/${projectId}/generate`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        'X-Guest-Id': guestId(),
      },
      body: JSON.stringify({ prompt }),
    })
    if (!response.ok || !response.body) {
      const payload = await response.json().catch(() => null)
      throw new Error(payload?.message ?? `Generation failed (${response.status})`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let completed = false
    let streamError: string | null = null
    const streamHandlers: StreamHandlers = {
      ...handlers,
      onCompleted: (event) => {
        completed = true
        handlers.onCompleted(event)
      },
      onError: (message) => {
        streamError = message
        handlers.onError(message)
      },
    }

    while (true) {
      const { value, done } = await reader.read()
      buffer += decoder.decode(value, { stream: !done }).replace(/\r\n/g, '\n')
      const frames = buffer.split('\n\n')
      buffer = frames.pop() ?? ''
      frames.forEach((frame) => dispatchFrame(frame, streamHandlers))
      if (done) break
    }
    if (buffer.trim()) dispatchFrame(buffer, streamHandlers)
    if (streamError) throw new Error(streamError)
    if (!completed) throw new Error('生成流在完成前中断，请重试')
  },
}

function dispatchFrame(frame: string, handlers: StreamHandlers) {
  let event = 'message'
  const dataLines: string[] = []
  frame.split('\n').forEach((line) => {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart())
  })
  if (dataLines.length === 0) return

  try {
    const data = JSON.parse(dataLines.join('\n'))
    if (event === 'phase') handlers.onPhase(data)
    if (event === 'token') handlers.onToken(data)
    if (event === 'completed') handlers.onCompleted(data as GenerationCompleted)
    if (event === 'generation-error') handlers.onError(data.message)
  } catch {
    handlers.onError('服务端返回了无法解析的流式事件')
  }
}
