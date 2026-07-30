import { AlertTriangle, ArrowUp, Bot, CheckCircle2, LoaderCircle, RotateCw, Sparkles, UserRound } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import type { Message, ProjectDetail } from '../types'

interface Props {
  project: ProjectDetail
  generating: boolean
  phase: string
  streamedChars: number
  onGenerate: (prompt: string) => Promise<void>
}

const suggestions = ['增加深色模式和主题切换', '优化移动端布局和空状态', '加入筛选、搜索和数据统计']

export function ChatPanel({ project, generating, phase, streamedChars, onGenerate }: Props) {
  const [prompt, setPrompt] = useState('')
  const endRef = useRef<HTMLDivElement>(null)
  useEffect(() => { endRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [project.messages, phase])

  const submit = async () => {
    const value = prompt.trim()
    if (!value || generating) return
    setPrompt('')
    await onGenerate(value)
  }

  return (
    <section className="chat-panel">
      <header className="panel-header">
        <div><span className="panel-kicker">Engineer Agent</span><h1>{project.name}</h1></div>
        <span className="mode-badge"><Sparkles size={13} />React</span>
      </header>
      <div className="message-list">
        {project.messages.length === 0 && <div className="empty-conversation"><Bot size={24} /><strong>描述你想构建的应用</strong><p>我会生成多文件 React 代码、运行预览，并记录每次成功或失败。</p></div>}
        {project.messages.map((message) => <MessageRow key={message.id} message={message} generating={generating} retryPrompt={project.runs.find((run) => run.id === message.runId)?.prompt} onRetry={onGenerate} />)}
        {generating && <div className="build-event"><LoaderCircle className="spin" size={16} /><div><strong>{phase || '任务已入队'}</strong><span>{streamedChars > 0 ? `已接收 ${streamedChars.toLocaleString()} 个字符` : '状态已持久化，可安全刷新'}</span></div></div>}
        <div ref={endRef} />
      </div>
      <div className="suggestion-row">{suggestions.map((item) => <button key={item} onClick={() => setPrompt(item)} disabled={generating}>{item}</button>)}</div>
      <div className="composer">
        <textarea value={prompt} onChange={(event) => setPrompt(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); void submit() } }} placeholder={project.currentVersionId ? '描述下一次修改，未提及的功能会保留...' : '例如：做一个支持优先级、筛选和统计的待办应用'} disabled={generating} rows={3} />
        <button className="send-button" onClick={() => void submit()} disabled={!prompt.trim() || generating} title="发送需求">{generating ? <LoaderCircle className="spin" size={18} /> : <ArrowUp size={18} />}</button>
        <span className="composer-hint">Enter 发送 · Shift + Enter 换行</span>
      </div>
    </section>
  )
}

function MessageRow({ message, generating, retryPrompt, onRetry }: { message: Message; generating: boolean; retryPrompt?: string; onRetry: (prompt: string) => Promise<void> }) {
  const assistant = message.role === 'assistant'
  const failed = message.status === 'failed'
  return (
    <article className={`message-row ${assistant ? 'assistant' : 'user'} ${failed ? 'failed' : ''}`}>
      <span className="message-avatar">{failed ? <AlertTriangle size={15} /> : assistant ? <CheckCircle2 size={15} /> : <UserRound size={15} />}</span>
      <div><strong>{assistant ? 'BuildTrace' : '你'}</strong><p>{message.content}</p>
        {failed && <button className="retry-button" disabled={generating || !retryPrompt} onClick={() => retryPrompt && void onRetry(retryPrompt)}><RotateCw size={13} />重试本次需求</button>}
      </div>
    </article>
  )
}
