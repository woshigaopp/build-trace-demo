import { SandpackProvider } from '@codesandbox/sandpack-react'
import { AlertTriangle, ArrowLeft, LoaderCircle, Sparkles } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { api } from '../api'
import type { PublishedProject } from '../types'
import { ReliableSandpackPreview } from './ReliableSandpackPreview'

export function PublishedApp({ token }: { token: string }) {
  const [project, setProject] = useState<PublishedProject | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    api.getPublishedProject(token)
      .then((value) => { if (active) setProject(value) })
      .catch((cause) => { if (active) setError(messageOf(cause)) })
    return () => { active = false }
  }, [token])

  const files = useMemo(
    () => Object.fromEntries(Object.entries(project?.files ?? {}).map(([path, code]) => [path, { code }])),
    [project],
  )

  if (error) return <PublicState icon={<AlertTriangle size={24} />} title="这个发布版本不可用" detail={error} />
  if (!project) return <PublicState icon={<LoaderCircle className="spin" size={24} />} title="正在启动应用" detail="加载已发布的不可变版本" />

  return <main className="published-shell">
    <header className="published-header">
      <div className="published-brand"><span><Sparkles size={15} /></span><div><strong>{project.name}</strong><small>Built with BuildTrace · v{project.versionNumber}</small></div></div>
      <a href="/"><ArrowLeft size={14} />返回 BuildTrace</a>
    </header>
    <section className="published-runtime" aria-label="已发布应用">
      <SandpackProvider key={`${token}-${project.versionNumber}`} template="vite-react" files={files} options={{ activeFile: '/App.jsx', visibleFiles: Object.keys(files) }} theme="light">
        <ReliableSandpackPreview />
      </SandpackProvider>
    </section>
  </main>
}

function PublicState({ icon, title, detail }: { icon: React.ReactNode; title: string; detail: string }) {
  return <main className="public-state"><div>{icon}</div><strong>{title}</strong><p>{detail}</p><a href="/"><ArrowLeft size={14} />返回 BuildTrace</a></main>
}

function messageOf(cause: unknown) { return cause instanceof Error ? cause.message : '发布版本加载失败' }
