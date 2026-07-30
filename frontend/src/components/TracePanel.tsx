import { BrainCircuit, Check, CheckCircle2, CircleDot, Clock3, FileDiff, ListChecks, LoaderCircle, RotateCw, XCircle } from 'lucide-react'
import { useMemo, useState } from 'react'
import type { ProjectDetail, RunStatus } from '../types'

interface Props {
  project: ProjectDetail
  generating: boolean
  phase: string
  streamedChars: number
}

const terminal = new Set<RunStatus>(['succeeded', 'failed', 'cancelled'])

export function TracePanel({ project, generating, phase, streamedChars }: Props) {
  const runs = useMemo(() => [...project.runs].reverse(), [project.runs])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const selected = runs.find((run) => run.id === selectedId) ?? runs[0] ?? null

  if (!selected) return <div className="trace-empty">{generating ? <LoaderCircle className="spin" size={27} /> : <BrainCircuit size={27} />}<strong>{generating ? phase || '正在建立第一条 Build Trace' : '第一条 Build Trace 将出现在这里'}</strong><p>{generating ? (streamedChars ? `已接收 ${streamedChars.toLocaleString()} 个模型输出字符，完成后会展示结构化轨迹。` : '任务入队后会依次记录生成、校验、修复和交付。') : '提交需求后，理解、计划、文件变化、校验和交付版本都会持久化。'}</p></div>

  const isLive = generating && !terminal.has(selected.status)
  return <div className="trace-view">
    <aside className="trace-runs" aria-label="构建记录">
      <header><span>BUILD HISTORY</span><strong>{runs.length} 次构建</strong></header>
      {runs.map((run) => <button key={run.id} className={run.id === selected.id ? 'active' : ''} onClick={() => setSelectedId(run.id)}>
        <StatusIcon status={run.status} />
        <span><strong>{compactPrompt(run.prompt)}</strong><small>{statusLabel(run.status)}{run.durationMs != null ? ` · ${(run.durationMs / 1000).toFixed(1)}s` : ''}</small></span>
        {run.deliveredVersionNumber && <em>v{run.deliveredVersionNumber}</em>}
      </button>)}
    </aside>

    <section className="trace-detail">
      <header className="trace-heading"><div><span>Build Trace</span><h2>{selected.understanding || selected.prompt}</h2></div><div className={`trace-status ${selected.status}`}><StatusIcon status={selected.status} />{statusLabel(selected.status)}</div></header>

      <div className="trace-meta"><span><BrainCircuit size={14}/>{selected.model}</span><span><Clock3 size={14}/>{selected.durationMs != null ? `${(selected.durationMs / 1000).toFixed(1)} 秒` : '进行中'}</span>{selected.attemptCount > 1 && <span><RotateCw size={14}/>自动修复 {selected.attemptCount - 1} 次</span>}</div>

      <div className="trace-grid">
        <TraceSection icon={<ListChecks size={16}/>} eyebrow="Implementation plan" title="实施计划">
          <ol className="plan-list">{(selected.plan.length ? selected.plan : ['分析当前文件与用户意图', '生成最小必要文件操作', '校验后交付不可变版本']).map((item, index) => <li key={`${item}-${index}`}><span>{index + 1}</span>{item}</li>)}</ol>
        </TraceSection>
        <TraceSection icon={<FileDiff size={16}/>} eyebrow="Changed files" title={selected.changedFiles.length ? `${selected.changedFiles.length} 个文件变化` : '等待候选文件'}>
          {selected.changedFiles.length ? <div className="changed-file-list">{selected.changedFiles.map(path => <code key={path}>{path}</code>)}</div> : <p className="trace-muted">通过校验并发布后，这里显示实际发生语义变化的路径。</p>}
        </TraceSection>
      </div>

      <TraceSection icon={<CircleDot size={16}/>} eyebrow="Execution timeline" title="执行轨迹" wide>
        <div className="timeline">{selected.trace.map((event, index) => <article key={`${event.createdAt}-${index}`} className={event.status}>
          <span className="timeline-dot"><StatusIcon status={event.status} /></span><div><strong>{event.title}</strong><p>{event.detail}</p><small>{formatTime(event.createdAt)}{event.attempt > 0 ? ` · attempt ${event.attempt}` : ''}</small></div>
        </article>)}
        {isLive && <article className="generating live-event"><span className="timeline-dot"><LoaderCircle className="spin" size={14}/></span><div><strong>{phase || '正在执行当前阶段'}</strong><p>{streamedChars ? `已接收 ${streamedChars.toLocaleString()} 个模型输出字符，原始 JSON 不展示给用户。` : '任务已持久化，可以安全刷新。'}</p><small>live</small></div></article>}</div>
      </TraceSection>

      <TraceSection icon={<CheckCircle2 size={16}/>} eyebrow="Server verification" title="交付校验" wide>
        {selected.checks.length ? <ul className="check-list">{selected.checks.map(check => <li key={check}><Check size={14}/>{check}</li>)}</ul> : <p className="trace-muted">只有服务端真正执行并通过的检查才会显示在这里。</p>}
      </TraceSection>
    </section>
  </div>
}

function TraceSection({ icon, eyebrow, title, children, wide = false }: { icon: React.ReactNode; eyebrow: string; title: string; children: React.ReactNode; wide?: boolean }) {
  return <section className={`trace-section ${wide ? 'wide' : ''}`}><header><span className="trace-section-icon">{icon}</span><div><small>{eyebrow}</small><strong>{title}</strong></div></header>{children}</section>
}

function StatusIcon({ status }: { status: RunStatus }) {
  if (status === 'succeeded') return <CheckCircle2 size={15}/>
  if (status === 'failed' || status === 'cancelled') return <XCircle size={15}/>
  return <LoaderCircle className="spin" size={15}/>
}

function statusLabel(status: RunStatus) {
  return ({ queued: '已入队', generating: '生成中', validating: '校验中', repairing: '自动修复', succeeded: '已交付', failed: '未交付', cancelled: '已取消' })[status]
}

function compactPrompt(value: string) { return value.length > 34 ? `${value.slice(0, 34)}...` : value }
function formatTime(value: string) { return new Date(value).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' }) }
