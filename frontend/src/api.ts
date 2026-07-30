import type {
  AuthResponse,
  GenerationCompleted,
  ProjectDetail,
  ProjectSummary,
  PublishedProject,
  StreamHandlers,
  User,
  VersionDetail,
} from './types'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? `http://${window.location.hostname}:8080`
const TOKEN_KEY = 'buildtrace.auth-token'

export class AuthenticationError extends Error {}

export function hasToken() {
  return Boolean(localStorage.getItem(TOKEN_KEY))
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

function authHeaders(): Record<string, string> {
  const token = localStorage.getItem(TOKEN_KEY)
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(),
      ...init?.headers,
    },
  })
  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    if (response.status === 401) throw new AuthenticationError(payload?.message ?? '登录已失效')
    throw new Error(payload?.message ?? `Request failed (${response.status})`)
  }
  return response.json() as Promise<T>
}

async function publicRequest<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { Accept: 'application/json' },
  })
  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    throw new Error(payload?.message ?? `Request failed (${response.status})`)
  }
  return response.json() as Promise<T>
}

export const api = {
  register: (email: string, password: string) =>
    request<AuthResponse>('/api/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) }),
  login: (email: string, password: string) =>
    request<AuthResponse>('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  me: () => request<User>('/api/auth/me'),
  listProjects: () => request<ProjectSummary[]>('/api/projects'),
  getProject: (id: string) => request<ProjectDetail>(`/api/projects/${id}`),
  createProject: (name: string) =>
    request<ProjectDetail>('/api/projects', { method: 'POST', body: JSON.stringify({ name }) }),
  saveVersion: (projectId: string, files: Record<string, string>, summary: string) =>
    request<ProjectDetail>(`/api/projects/${projectId}/versions`, {
      method: 'POST',
      body: JSON.stringify({ files, summary }),
    }),
  getVersion: (projectId: string, versionId: string) =>
    request<VersionDetail>(`/api/projects/${projectId}/versions/${versionId}`),
  restoreVersion: (projectId: string, versionId: string) =>
    request<ProjectDetail>(`/api/projects/${projectId}/versions/${versionId}/restore`, { method: 'POST' }),
  publishProject: (projectId: string) =>
    request<ProjectDetail>(`/api/projects/${projectId}/publish`, { method: 'POST' }),
  getPublishedProject: (token: string) =>
    publicRequest<PublishedProject>(`/api/public/projects/${encodeURIComponent(token)}`),
  generate: async (projectId: string, prompt: string, handlers: StreamHandlers) => {
    const response = await fetch(`${API_BASE}/api/projects/${projectId}/generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream', ...authHeaders() },
      body: JSON.stringify({ prompt }),
    })
    if (!response.ok || !response.body) {
      const payload = await response.json().catch(() => null)
      if (response.status === 401) throw new AuthenticationError(payload?.message ?? '登录已失效')
      throw new Error(payload?.message ?? `Generation failed (${response.status})`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let completed = false
    let streamError: string | null = null
    const streamHandlers: StreamHandlers = {
      ...handlers,
      onCompleted: (event) => { completed = true; handlers.onCompleted(event) },
      onError: (message, project) => { streamError = message; handlers.onError(message, project) },
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
    if (!completed) throw new Error('生成流在完成前中断，任务仍会在服务端继续，可刷新查看状态')
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
    if (event === 'generation-error') handlers.onError(data.message, data.project)
  } catch {
    handlers.onError('服务端返回了无法解析的流式事件')
  }
}
