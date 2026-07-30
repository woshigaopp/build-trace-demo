import { Clock3, FileCode2, LogOut, Plus, UserRound } from 'lucide-react'
import type { ProjectSummary, User } from '../types'

interface Props {
  projects: ProjectSummary[]
  selectedId: string | null
  loading: boolean
  user: User
  onSelect: (id: string) => void
  onNew: () => void
  onLogout: () => void
}

export function ProjectSidebar({ projects, selectedId, loading, user, onSelect, onNew, onLogout }: Props) {
  return (
    <aside className="project-sidebar">
      <div className="brand-row">
        <div className="brand-mark"><FileCode2 size={18} /></div>
        <div><strong>BuildTrace</strong><span>AI App Builder</span></div>
      </div>
      <button className="new-project-button" onClick={onNew}><Plus size={16} />新建应用</button>
      <div className="sidebar-label">最近项目</div>
      <nav className="project-list" aria-label="项目列表">
        {loading && <div className="sidebar-empty">正在读取项目...</div>}
        {!loading && projects.length === 0 && <div className="sidebar-empty">还没有项目，从一个想法开始。</div>}
        {projects.map((project) => (
          <button key={project.id} className={`project-item ${project.id === selectedId ? 'active' : ''}`} onClick={() => onSelect(project.id)}>
            <span className="project-icon"><FileCode2 size={15} /></span>
            <span className="project-meta"><strong>{project.name}</strong><small><Clock3 size={11} />{formatRelative(project.updatedAt)}</small></span>
            {project.hasPreview && <span className="project-ready" title="已有可运行版本" />}
          </button>
        ))}
      </nav>
      <div className="account-footer">
        <span className="account-avatar"><UserRound size={14} /></span>
        <span className="account-meta"><strong>{user.email.split('@')[0]}</strong><small>{user.email}</small></span>
        <button onClick={onLogout} title="退出登录"><LogOut size={15} /></button>
      </div>
    </aside>
  )
}

function formatRelative(value: string) {
  const minutes = Math.max(0, Math.round((Date.now() - new Date(value).getTime()) / 60_000))
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  if (minutes < 1_440) return `${Math.floor(minutes / 60)} 小时前`
  return new Date(value).toLocaleDateString('zh-CN')
}
