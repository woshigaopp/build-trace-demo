import { ArrowRight, Boxes, LoaderCircle, Plus, ShieldCheck, Sparkles } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { api } from './api'
import { ChatPanel } from './components/ChatPanel'
import { PreviewPanel } from './components/PreviewPanel'
import { ProjectSidebar } from './components/ProjectSidebar'
import type { GenerationCompleted, ProjectDetail, ProjectSummary } from './types'

const starters = [
  { label: '效率工具', prompt: '做一个支持添加、完成、删除、筛选和任务统计的待办清单' },
  { label: '数据产品', prompt: '做一个 SaaS 运营数据看板，包含 KPI、趋势图和最近活动列表' },
  { label: '商业页面', prompt: '做一个 AI 简历分析产品介绍页，包含功能、定价和试用表单' },
]

export default function App() {
  const [projects, setProjects] = useState<ProjectSummary[]>([])
  const [project, setProject] = useState<ProjectDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [generating, setGenerating] = useState(false)
  const [phase, setPhase] = useState('')
  const [streamedChars, setStreamedChars] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [runMeta, setRunMeta] = useState<GenerationCompleted | null>(null)
  const [newProjectMode, setNewProjectMode] = useState(false)

  const refreshProjects = useCallback(async () => {
    const list = await api.listProjects()
    setProjects(list)
    return list
  }, [])

  useEffect(() => {
    void (async () => {
      try {
        const list = await refreshProjects()
        if (list[0]) setProject(await api.getProject(list[0].id))
      } catch (cause) {
        setError(messageOf(cause))
      } finally {
        setLoading(false)
      }
    })()
  }, [refreshProjects])

  const selectProject = async (id: string) => {
    if (generating) return
    setError(null)
    try {
      setProject(await api.getProject(id))
      setNewProjectMode(false)
    } catch (cause) {
      setError(messageOf(cause))
    }
  }

  const build = async (prompt: string, targetProject = project) => {
    if (!targetProject || generating) return
    setGenerating(true)
    setPhase('正在提交需求')
    setStreamedChars(0)
    setError(null)
    setRunMeta(null)
    setProject({
      ...targetProject,
      messages: [
        ...targetProject.messages,
        { id: crypto.randomUUID(), role: 'user', content: prompt, createdAt: new Date().toISOString() },
      ],
    })
    try {
      await api.generate(targetProject.id, prompt, {
        onPhase: (event) => setPhase(event.message),
        onToken: (event) => setStreamedChars((value) => value + event.content.length),
        onCompleted: (event) => {
          setProject(event.project)
          setRunMeta(event)
        },
        onError: (message) => setError(message),
      })
      await refreshProjects()
    } catch (cause) {
      setError(messageOf(cause))
      setProject(await api.getProject(targetProject.id).catch(() => targetProject))
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
    } catch (cause) {
      setError(messageOf(cause))
    }
  }

  const restore = async (versionId: string) => {
    if (!project) return
    setError(null)
    try {
      const restored = await api.restoreVersion(project.id, versionId)
      setProject(restored)
      await refreshProjects()
    } catch (cause) {
      setError(messageOf(cause))
    }
  }

  const showStart = !project || newProjectMode

  return (
    <div className="app-shell">
      <ProjectSidebar
        projects={projects}
        selectedId={showStart ? null : project.id}
        loading={loading}
        onSelect={(id) => void selectProject(id)}
        onNew={() => setNewProjectMode(true)}
      />

      <main className="workspace">
        {error && (
          <div className="error-banner" role="alert">
            <ShieldCheck size={16} />
            <span>{error}</span>
            <button onClick={() => setError(null)}>关闭</button>
          </div>
        )}
        {runMeta && !error && (
          <div className="run-banner">
            <Sparkles size={15} />
            <span>
              {runMeta.fallback ? '本地 fallback' : runMeta.model} 完成 · {(runMeta.durationMs / 1000).toFixed(1)} 秒 · v{runMeta.project.versions[0]?.versionNumber}
            </span>
            <button onClick={() => setRunMeta(null)}>关闭</button>
          </div>
        )}

        {loading ? (
          <div className="workspace-loading"><LoaderCircle className="spin" />正在加载工作空间</div>
        ) : showStart ? (
          <StartWorkspace onBuild={createAndBuild} creating={generating} hasProjects={projects.length > 0} onCancel={() => setNewProjectMode(false)} />
        ) : (
          <div className="builder-layout">
            <ChatPanel
              project={project}
              generating={generating}
              phase={phase}
              streamedChars={streamedChars}
              onGenerate={build}
            />
            <PreviewPanel project={project} generating={generating} onRestore={restore} />
          </div>
        )}
      </main>
    </div>
  )
}

function StartWorkspace({
  onBuild,
  creating,
  hasProjects,
  onCancel,
}: {
  onBuild: (prompt: string) => Promise<void>
  creating: boolean
  hasProjects: boolean
  onCancel: () => void
}) {
  const [prompt, setPrompt] = useState('')
  return (
    <section className="start-workspace">
      <div className="start-inner">
        <div className="start-icon"><Boxes size={24} /></div>
        <span className="start-kicker">From idea to a running app</span>
        <h1>今天想构建什么？</h1>
        <p>描述目标和关键交互。BuildTrace 会生成完整代码、运行预览并保存每次可回滚版本。</p>

        <div className="start-composer">
          <textarea
            autoFocus
            value={prompt}
            onChange={(event) => setPrompt(event.target.value)}
            placeholder="例如：做一个支持优先级、筛选和统计的待办应用"
            rows={4}
          />
          <button onClick={() => void onBuild(prompt)} disabled={!prompt.trim() || creating}>
            {creating ? <LoaderCircle className="spin" size={17} /> : <Sparkles size={17} />}
            创建并生成
            {!creating && <ArrowRight size={16} />}
          </button>
        </div>

        <div className="starter-grid">
          {starters.map((starter) => (
            <button key={starter.label} onClick={() => setPrompt(starter.prompt)}>
              <span>{starter.label}</span>
              <p>{starter.prompt}</p>
              <Plus size={15} />
            </button>
          ))}
        </div>
        {hasProjects && <button className="cancel-new" onClick={onCancel}>返回当前项目</button>}
      </div>
    </section>
  )
}

function projectName(prompt: string) {
  return prompt.replace(/[，。,.!?！？]/g, ' ').trim().slice(0, 28) || '未命名应用'
}

function messageOf(cause: unknown) {
  return cause instanceof Error ? cause.message : '发生了未知错误'
}
