import {
  SandpackCodeEditor,
  SandpackLayout,
  SandpackPreview,
  SandpackProvider,
  useSandpack,
} from '@codesandbox/sandpack-react'
import { Check, Code2, FileCode2, History, Laptop, LoaderCircle, MonitorPlay, RotateCcw, Save, Smartphone } from 'lucide-react'
import { useMemo, useState } from 'react'
import { api } from '../api'
import type { ProjectDetail, VersionDetail } from '../types'

type Tab = 'preview' | 'code' | 'versions'
type Viewport = 'desktop' | 'mobile'

interface Props {
  project: ProjectDetail
  generating: boolean
  onProjectChange: (project: ProjectDetail) => Promise<void>
  onError: (message: string) => void
}

export function WorkspacePanel({ project, generating, onProjectChange, onError }: Props) {
  const files = useMemo(() => Object.fromEntries(Object.entries(project.currentFiles).map(([path, code]) => [path, { code }])), [project.currentFiles])
  if (!project.currentVersionId) return <EmptyWorkspace generating={generating} />
  return (
    <SandpackProvider key={project.currentVersionId} template="vite-react" files={files} options={{ activeFile: '/App.jsx', visibleFiles: Object.keys(files) }} theme="light">
      <WorkspaceContent project={project} generating={generating} onProjectChange={onProjectChange} onError={onError} />
    </SandpackProvider>
  )
}

function WorkspaceContent({ project, generating, onProjectChange, onError }: Props) {
  const { sandpack } = useSandpack()
  const [tab, setTab] = useState<Tab>('preview')
  const [viewport, setViewport] = useState<Viewport>('desktop')
  const [saving, setSaving] = useState(false)
  const currentFiles = Object.fromEntries(Object.entries(sandpack.files).map(([path, file]) => [path, file.code]))
  const editedPaths = changedPaths(currentFiles, project.currentFiles)
  const dirty = sandpack.editorState === 'dirty' && editedPaths.length > 0

  const save = async () => {
    setSaving(true)
    try {
      const editedFiles = { ...project.currentFiles }
      editedPaths.forEach((path) => {
        if (path in currentFiles) editedFiles[path] = currentFiles[path]
        else delete editedFiles[path]
      })
      await onProjectChange(await api.saveVersion(project.id, editedFiles, '手动保存代码修改'))
    } catch (cause) {
      onError(messageOf(cause))
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="preview-panel">
      <header className="preview-toolbar">
        <div className="tab-list" role="tablist">
          <TabButton active={tab === 'preview'} onClick={() => setTab('preview')} icon={<MonitorPlay size={15} />} label="预览" />
          <TabButton active={tab === 'code'} onClick={() => setTab('code')} icon={<Code2 size={15} />} label="代码" />
          <TabButton active={tab === 'versions'} onClick={() => setTab('versions')} icon={<History size={15} />} label={`版本 ${project.versions.length}`} />
        </div>
        <div className="toolbar-actions">
          <span className={`runtime-status ${sandpack.status}`}>{sandpack.status === 'running' ? 'Preview running' : sandpack.status === 'idle' ? '正在初始化' : sandpack.status}</span>
          {tab === 'preview' && <div className="viewport-switch"><button className={viewport === 'desktop' ? 'active' : ''} onClick={() => setViewport('desktop')} title="桌面预览"><Laptop size={15} /></button><button className={viewport === 'mobile' ? 'active' : ''} onClick={() => setViewport('mobile')} title="移动端预览"><Smartphone size={15} /></button></div>}
          {tab === 'code' && <button className="save-code-button" onClick={() => void save()} disabled={!dirty || saving || generating}>{saving ? <LoaderCircle className="spin" size={14} /> : <Save size={14} />}{dirty ? '保存版本' : '已保存'}</button>}
        </div>
      </header>
      <div className={`preview-content ${tab}`}>
        {tab === 'preview' && <div className={`sandpack-preview-shell ${viewport}`}><SandpackPreview showOpenInCodeSandbox={false} showRefreshButton /></div>}
        {tab === 'code' && <SandpackLayout className="code-workspace"><FileTree /><SandpackCodeEditor showTabs={false} showLineNumbers wrapContent closableTabs={false} /></SandpackLayout>}
        {tab === 'versions' && <VersionPanel project={project} generating={generating} onProjectChange={onProjectChange} onError={onError} onDone={() => setTab('preview')} />}
      </div>
    </section>
  )
}

function FileTree() {
  const { sandpack } = useSandpack()
  return <nav className="file-tree" aria-label="项目文件"><div className="file-tree-heading">PROJECT FILES</div>{Object.keys(sandpack.files).sort().map((path) => <button key={path} className={sandpack.activeFile === path ? 'active' : ''} onClick={() => sandpack.setActiveFile(path)} title={path}><FileCode2 size={13} /><span>{path.slice(1)}</span></button>)}</nav>
}

function VersionPanel({ project, generating, onProjectChange, onError, onDone }: Props & { onDone: () => void }) {
  const [selected, setSelected] = useState<VersionDetail | null>(null)
  const [busy, setBusy] = useState<string | null>(null)
  const changedFiles = selected ? changedPaths(selected.files, project.currentFiles) : []

  const compare = async (versionId: string) => {
    setBusy(versionId)
    try { setSelected(await api.getVersion(project.id, versionId)) } catch (cause) { onError(messageOf(cause)) } finally { setBusy(null) }
  }
  const restore = async (versionId: string) => {
    setBusy(versionId)
    try { await onProjectChange(await api.restoreVersion(project.id, versionId)); onDone() } catch (cause) { onError(messageOf(cause)) } finally { setBusy(null) }
  }

  return <div className="version-view"><div className="version-heading"><div><span>Immutable history</span><h2>每次交付都可比较、可恢复</h2></div><p>AI 生成、手动保存和恢复都会创建新快照，旧版本始终保持不变。</p></div>
    <div className="version-grid"><div className="version-list">{project.versions.map((version) => { const current = version.id === project.currentVersionId; return <article className="version-item" key={version.id}><span className="version-number">v{version.versionNumber}</span><div><strong>{version.summary}</strong><p>{version.source} · {version.fileCount} files</p><small>{new Date(version.createdAt).toLocaleString('zh-CN')}</small></div><div className="version-actions"><button onClick={() => void compare(version.id)} disabled={busy !== null}>{busy === version.id ? <LoaderCircle className="spin" size={13} /> : <Code2 size={13} />}比较</button>{current ? <span className="current-version"><Check size={13} />当前</span> : <button onClick={() => void restore(version.id)} disabled={busy !== null || generating}><RotateCcw size={13} />恢复</button>}</div></article>})}</div>
      <aside className="diff-panel">{selected ? <><span>与当前版本比较</span><h3>v{selected.versionNumber}</h3>{changedFiles.length ? <ul>{changedFiles.map((path) => <li key={path}>{path}</li>)}</ul> : <p>文件内容与当前版本一致。</p>}</> : <><span>Changed files</span><h3>选择一个版本</h3><p>这里会列出与当前快照内容不同的完整文件路径。</p></>}</aside></div>
  </div>
}

function changedPaths(left: Record<string, string>, right: Record<string, string>) {
  return [...new Set([...Object.keys(left), ...Object.keys(right)])]
    .filter((path) => !equivalentContent(path, left[path], right[path]))
    .sort()
}

function equivalentContent(path: string, left?: string, right?: string) {
  if (left === right) return true
  if (path !== '/package.json' || left === undefined || right === undefined) return false
  try {
    return JSON.stringify(JSON.parse(left)) === JSON.stringify(JSON.parse(right))
  } catch {
    return false
  }
}

function TabButton({ active, onClick, icon, label }: { active: boolean; onClick: () => void; icon: React.ReactNode; label: string }) {
  return <button role="tab" aria-selected={active} className={active ? 'active' : ''} onClick={onClick}>{icon}{label}</button>
}

function EmptyWorkspace({ generating }: { generating: boolean }) {
  return <section className="preview-panel empty-workspace"><div className="empty-preview"><div className="empty-preview-icon">{generating ? <LoaderCircle className="spin" size={25} /> : <MonitorPlay size={25} />}</div><strong>{generating ? '正在构建第一个多文件版本' : '等待第一个应用'}</strong><p>{generating ? '候选文件通过校验后才会更新这里。' : '在左侧描述需求，完整代码和预览会出现在这里。'}</p></div></section>
}

function messageOf(cause: unknown) { return cause instanceof Error ? cause.message : '发生了未知错误' }
