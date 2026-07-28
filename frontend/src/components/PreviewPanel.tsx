import {
  Check,
  Code2,
  ExternalLink,
  History,
  Laptop,
  MonitorPlay,
  RotateCcw,
  Smartphone,
} from 'lucide-react'
import { useState } from 'react'
import type { ProjectDetail } from '../types'

type Tab = 'preview' | 'code' | 'versions'
type Viewport = 'desktop' | 'mobile'

interface Props {
  project: ProjectDetail
  generating: boolean
  onRestore: (versionId: string) => Promise<void>
}

export function PreviewPanel({ project, generating, onRestore }: Props) {
  const [tab, setTab] = useState<Tab>('preview')
  const [viewport, setViewport] = useState<Viewport>('desktop')
  const [restoring, setRestoring] = useState<string | null>(null)

  const restore = async (versionId: string) => {
    setRestoring(versionId)
    try {
      await onRestore(versionId)
      setTab('preview')
    } finally {
      setRestoring(null)
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
          {tab === 'preview' && (
            <div className="viewport-switch" aria-label="预览尺寸">
              <button className={viewport === 'desktop' ? 'active' : ''} onClick={() => setViewport('desktop')} title="桌面预览"><Laptop size={15} /></button>
              <button className={viewport === 'mobile' ? 'active' : ''} onClick={() => setViewport('mobile')} title="移动端预览"><Smartphone size={15} /></button>
            </div>
          )}
          {project.currentHtml && (
            <button
              className="icon-button"
              title="在新窗口打开"
              onClick={() => openHtml(project.currentHtml!)}
            >
              <ExternalLink size={15} />
            </button>
          )}
        </div>
      </header>

      <div className={`preview-content ${tab}`}>
        {tab === 'preview' && (
          project.currentHtml ? (
            <div className={`iframe-shell ${viewport}`}>
              <iframe title="生成应用预览" srcDoc={project.currentHtml} sandbox="allow-scripts allow-forms" />
            </div>
          ) : (
            <EmptyPreview generating={generating} />
          )
        )}
        {tab === 'code' && (
          project.currentHtml ? (
            <pre className="code-view"><code>{project.currentHtml}</code></pre>
          ) : <EmptyPreview generating={generating} />
        )}
        {tab === 'versions' && (
          <div className="version-view">
            <div className="version-heading">
              <div>
                <span>Version history</span>
                <h2>可验证的每次交付</h2>
              </div>
              <p>生成和回滚都会创建新版本，旧版本始终保留。</p>
            </div>
            {project.versions.length === 0 && <div className="version-empty">生成第一个应用后，版本会出现在这里。</div>}
            <div className="version-list">
              {project.versions.map((version, index) => (
                <article className="version-item" key={version.id}>
                  <span className="version-number">v{version.versionNumber}</span>
                  <div>
                    <strong>{index === 0 ? '当前版本' : `版本 ${version.versionNumber}`}</strong>
                    <p>{version.prompt}</p>
                    <small>{new Date(version.createdAt).toLocaleString('zh-CN')}</small>
                  </div>
                  {index === 0 ? (
                    <span className="current-version"><Check size={14} />当前</span>
                  ) : (
                    <button onClick={() => void restore(version.id)} disabled={restoring !== null || generating}>
                      <RotateCcw size={14} />
                      {restoring === version.id ? '恢复中' : '恢复'}
                    </button>
                  )}
                </article>
              ))}
            </div>
          </div>
        )}
      </div>
    </section>
  )
}

function TabButton({ active, onClick, icon, label }: { active: boolean; onClick: () => void; icon: React.ReactNode; label: string }) {
  return <button role="tab" aria-selected={active} className={active ? 'active' : ''} onClick={onClick}>{icon}{label}</button>
}

function EmptyPreview({ generating }: { generating: boolean }) {
  return (
    <div className="empty-preview">
      <div className="empty-preview-icon"><MonitorPlay size={25} /></div>
      <strong>{generating ? '正在构建可运行版本' : '等待第一个应用'}</strong>
      <p>{generating ? '完成校验后会一次性更新预览，避免运行半份 HTML。' : '在左侧描述需求，生成结果将在安全 iframe 中运行。'}</p>
    </div>
  )
}

function openHtml(html: string) {
  const url = URL.createObjectURL(new Blob([html], { type: 'text/html' }))
  window.open(url, '_blank', 'noopener,noreferrer')
  window.setTimeout(() => URL.revokeObjectURL(url), 30_000)
}
