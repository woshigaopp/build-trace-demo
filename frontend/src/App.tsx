import { ArrowRight, Boxes, LoaderCircle, Plus, ShieldCheck, Sparkles } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { api, AuthenticationError, clearToken, hasToken, setToken } from './api'
import { AuthScreen } from './components/AuthScreen'
import { ChatPanel } from './components/ChatPanel'
import { ProjectSidebar } from './components/ProjectSidebar'
import { PublishedApp } from './components/PublishedApp'
import { WorkspacePanel } from './components/WorkspacePanel'
import type { GenerationCompleted, ProjectDetail, ProjectSummary, User } from './types'

const starters = [
  { label: '效率工具', prompt: '做一个支持添加、完成、删除、筛选和任务统计的待办清单' },
  { label: '数据产品', prompt: '做一个 SaaS 运营数据看板，包含 KPI、趋势图和最近活动列表' },
  { label: '业务应用', prompt: '做一个候选人跟进看板，支持新增、阶段流转、搜索和统计' },
]

export default function App() {
  const publishedToken = publishedTokenFromPath()
  return publishedToken ? <PublishedApp token={publishedToken} /> : <BuilderApp />
}

function BuilderApp() {
  const [user, setUser] = useState<User | null>(null)
  const [projects, setProjects] = useState<ProjectSummary[]>([])
  const [project, setProject] = useState<ProjectDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [authSubmitting, setAuthSubmitting] = useState(false)
  const [generating, setGenerating] = useState(false)
  const [phase, setPhase] = useState('')
  const [streamedChars, setStreamedChars] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [authError, setAuthError] = useState<string | null>(null)
  const [runMeta, setRunMeta] = useState<GenerationCompleted | null>(null)
  const [newProjectMode, setNewProjectMode] = useState(false)

  const signOut = useCallback(() => {
    clearToken()
    setUser(null)
    setProjects([])
    setProject(null)
    setGenerating(false)
    setError(null)
  }, [])

  const handleFailure = useCallback((cause: unknown) => {
    if (cause instanceof AuthenticationError) {
      signOut()
      setAuthError('登录已失效，请重新登录')
      return
    }
    setError(messageOf(cause))
  }, [signOut])

  const refreshProjects = useCallback(async () => {
    const list = await api.listProjects()
    setProjects(list)
    return list
  }, [])

  const loadWorkspace = useCallback(async () => {
    const currentUser = await api.me()
    setUser(currentUser)
    const list = await refreshProjects()
    if (list[0]) setProject(await api.getProject(list[0].id))
  }, [refreshProjects])

  useEffect(() => {
    void (async () => {
      if (!hasToken()) { setLoading(false); return }
      try { await loadWorkspace() } catch (cause) { handleFailure(cause) } finally { setLoading(false) }
    })()
  }, [handleFailure, loadWorkspace])

  useEffect(() => {
    if (!project || generating || !hasActiveRun(project)) return
    let cancelled = false
    const timeout = window.setTimeout(async () => {
      try {
        setPhase('正在恢复服务端生成任务状态')
        const latest = await api.getProject(project.id)
        if (cancelled) return
        setProject(latest)
        if (!hasActiveRun(latest)) { setPhase(''); void refreshProjects() }
      } catch (cause) {
        if (!cancelled) { setPhase(''); handleFailure(cause) }
      }
    }, 1200)
    return () => { cancelled = true; window.clearTimeout(timeout) }
  }, [generating, handleFailure, project, refreshProjects])

  const authenticate = async (mode: 'login' | 'register', email: string, password: string) => {
    setAuthSubmitting(true)
    setAuthError(null)
    try {
      const response = mode === 'register' ? await api.register(email, password) : await api.login(email, password)
      setToken(response.token)
      setUser(response.user)
      const list = await refreshProjects()
      setProject(list[0] ? await api.getProject(list[0].id) : null)
    } catch (cause) {
      clearToken()
      setAuthError(messageOf(cause))
    } finally {
      setAuthSubmitting(false)
      setLoading(false)
    }
  }

  const selectProject = async (id: string) => {
    if (generating || (project && hasActiveRun(project))) return
    setError(null)
    try { setProject(await api.getProject(id)); setNewProjectMode(false) } catch (cause) { handleFailure(cause) }
  }

  const build = async (prompt: string, targetProject = project) => {
    if (!targetProject || generating || hasActiveRun(targetProject)) return
    setGenerating(true)
    setPhase('任务正在入队')
    setStreamedChars(0)
    setError(null)
    setRunMeta(null)
    setProject({ ...targetProject, messages: [...targetProject.messages, { id: crypto.randomUUID(), role: 'user', content: prompt, status: 'accepted', createdAt: new Date().toISOString() }] })
    try {
      await api.generate(targetProject.id, prompt, {
        onPhase: (event) => setPhase(event.message),
        onToken: (event) => setStreamedChars((value) => value + event.content.length),
        onCompleted: (event) => { setProject(event.project); setRunMeta(event) },
        onError: (message, failedProject) => { setError(message); if (failedProject) setProject(failedProject) },
      })
      await refreshProjects()
    } catch (cause) {
      setError(messageOf(cause))
      try { setProject(await api.getProject(targetProject.id)) } catch (reloadError) { handleFailure(reloadError) }
    } finally {
      setGenerating(false)
      setPhase('')
    }
  }

  const createAndBuild = async (prompt: string) => {
    const normalized = prompt.trim()
    if (!normalized || generating) return
    setError(null)
    try {
      const created = await api.createProject(projectName(normalized))
      setProject(created)
      setNewProjectMode(false)
      await refreshProjects()
      await build(normalized, created)
    } catch (cause) { handleFailure(cause) }
  }

  const acceptProject = async (updated: ProjectDetail) => {
    setProject(updated)
    await refreshProjects()
  }

  if (loading) return <div className="workspace-loading full-page"><LoaderCircle className="spin" />正在恢复账号与工作空间</div>
  if (!user) return <AuthScreen submitting={authSubmitting} error={authError} onSubmit={authenticate} />

  const showStart = !project || newProjectMode
  const busy = generating || Boolean(project && hasActiveRun(project))
  return (
    <div className="app-shell">
      <ProjectSidebar projects={projects} selectedId={showStart ? null : project.id} loading={loading} user={user} onSelect={(id) => void selectProject(id)} onNew={() => setNewProjectMode(true)} onLogout={signOut} />
      <main className="workspace">
        {error && <div className="error-banner" role="alert"><ShieldCheck size={16} /><span>{error}</span><button onClick={() => setError(null)}>关闭</button></div>}
        {runMeta && !error && <div className="run-banner"><Sparkles size={15} /><span>{runMeta.fallback ? '本地 fallback' : runMeta.model} 完成 · {(runMeta.durationMs / 1000).toFixed(1)} 秒 · v{runMeta.project.versions[0]?.versionNumber}</span><button onClick={() => setRunMeta(null)}>关闭</button></div>}
        {showStart ? <StartWorkspace onBuild={createAndBuild} creating={generating} hasProjects={projects.length > 0} onCancel={() => setNewProjectMode(false)} /> : <div className="builder-layout"><ChatPanel project={project} generating={busy} phase={phase} streamedChars={streamedChars} onGenerate={build} /><WorkspacePanel project={project} generating={busy} phase={phase} streamedChars={streamedChars} onProjectChange={acceptProject} onError={setError} /></div>}
      </main>
    </div>
  )
}

function StartWorkspace({ onBuild, creating, hasProjects, onCancel }: { onBuild: (prompt: string) => Promise<void>; creating: boolean; hasProjects: boolean; onCancel: () => void }) {
  const [prompt, setPrompt] = useState('')
  return <section className="start-workspace"><div className="start-inner"><div className="start-icon"><Boxes size={24} /></div><span className="start-kicker">From idea to a running React app</span><h1>今天想构建什么？</h1><p>描述目标和关键交互。BuildTrace 会生成多文件代码、运行预览并保存每次可恢复版本。</p><div className="start-composer"><textarea autoFocus value={prompt} onChange={(event) => setPrompt(event.target.value)} placeholder="例如：做一个支持优先级、筛选和统计的待办应用" rows={4} /><button onClick={() => void onBuild(prompt)} disabled={!prompt.trim() || creating}>{creating ? <LoaderCircle className="spin" size={17} /> : <Sparkles size={17} />}创建并生成{!creating && <ArrowRight size={16} />}</button></div><div className="starter-grid">{starters.map((starter) => <button key={starter.label} onClick={() => setPrompt(starter.prompt)}><span>{starter.label}</span><p>{starter.prompt}</p><Plus size={15} /></button>)}</div>{hasProjects && <button className="cancel-new" onClick={onCancel}>返回当前项目</button>}</div></section>
}

function hasActiveRun(project: ProjectDetail) {
  return project.runs.some((run) => ['queued', 'generating', 'validating', 'repairing'].includes(run.status))
}

function projectName(prompt: string) { return prompt.replace(/[，。,.!?！？]/g, ' ').trim().slice(0, 28) || '未命名应用' }
function publishedTokenFromPath() {
  const match = window.location.pathname.match(/^\/p\/([A-Za-z0-9_-]+)\/?$/)
  return match?.[1] ?? null
}
function messageOf(cause: unknown) { return cause instanceof Error ? cause.message : '发生了未知错误' }
