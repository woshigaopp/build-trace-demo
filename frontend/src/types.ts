export interface User {
  id: string
  email: string
  createdAt?: string
}

export interface AuthResponse {
  token: string
  user: User
}

export interface ProjectSummary {
  id: string
  name: string
  hasPreview: boolean
  createdAt: string
  updatedAt: string
}

export interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  runId?: string
  status?: 'accepted' | 'succeeded' | 'failed'
  createdAt: string
}

export type RunStatus = 'queued' | 'generating' | 'validating' | 'repairing' | 'succeeded' | 'failed' | 'cancelled'

export interface TraceEvent {
  status: RunStatus
  title: string
  detail: string
  attempt: number
  createdAt: string
}

export interface GenerationRun {
  id: string
  prompt: string
  status: RunStatus
  model: string
  attemptCount: number
  errorMessage?: string
  durationMs?: number
  understanding?: string
  plan: string[]
  changedFiles: string[]
  checks: string[]
  trace: TraceEvent[]
  deliveredVersionNumber?: number
  createdAt: string
  updatedAt: string
}

export interface Version {
  id: string
  versionNumber: number
  prompt: string
  source: 'ai' | 'manual' | 'restore' | 'legacy' | 'template'
  summary: string
  fileCount: number
  createdAt: string
}

export interface VersionDetail extends Version {
  files: Record<string, string>
}

export interface ProjectDetail {
  id: string
  name: string
  currentVersionId: string | null
  currentFiles: Record<string, string>
  publication: Publication | null
  createdAt: string
  updatedAt: string
  messages: Message[]
  versions: Version[]
  runs: GenerationRun[]
}

export interface Publication {
  token: string
  versionId: string
  versionNumber: number
  publishedAt: string
}

export interface PublishedProject {
  name: string
  versionNumber: number
  files: Record<string, string>
  publishedAt: string
}

export interface GenerationCompleted {
  project: ProjectDetail
  runId: string
  fallback: boolean
  model: string
  durationMs: number
}

export interface StreamHandlers {
  onPhase: (data: { step: string; status: RunStatus; message: string }) => void
  onToken: (data: { content: string }) => void
  onCompleted: (data: GenerationCompleted) => void
  onError: (message: string, project?: ProjectDetail) => void
}
