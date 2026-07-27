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
  createdAt: string
}

export interface Version {
  id: string
  versionNumber: number
  prompt: string
  createdAt: string
}

export interface ProjectDetail {
  id: string
  name: string
  currentHtml: string | null
  createdAt: string
  updatedAt: string
  messages: Message[]
  versions: Version[]
}

export interface GenerationCompleted {
  project: ProjectDetail
  fallback: boolean
  model: string
  durationMs: number
}

export interface StreamHandlers {
  onPhase: (data: { step: string; message: string }) => void
  onToken: (data: { content: string }) => void
  onCompleted: (data: GenerationCompleted) => void
  onError: (message: string) => void
}
