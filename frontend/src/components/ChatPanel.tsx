import { ArrowUp, Bot, CheckCircle2, LoaderCircle, Sparkles, UserRound } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import type { Message, ProjectDetail } from '../types'

interface Props {
  project: ProjectDetail
  generating: boolean
  phase: string
  streamedChars: number
  onGenerate: (prompt: string) => Promise<void>
}

const suggestions = [
  '增加深色模式和任务统计',
  '优化移动端布局和空状态',
  '加入筛选、搜索和撤销删除',
]

export function ChatPanel({ project, generating, phase, streamedChars, onGenerate }: Props) {
  const [prompt, setPrompt] = useState('')
  const endRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [project.messages, phase])

  const submit = async () => {
    const value = prompt.trim()
    if (!value || generating) return
    setPrompt('')
    await onGenerate(value)
  }

  return (
    <section className="chat-panel">
      <header className="panel-header">
        <div>
          <span className="panel-kicker">Engineer Agent</span>
          <h1>{project.name}</h1>
        </div>
        <span className="mode-badge"><Sparkles size={13} />Build</span>
      </header>

      <div className="message-list">
        {project.messages.length === 0 && (
          <div className="empty-conversation">
            <Bot size={24} />
            <strong>描述你想构建的应用</strong>
            <p>我会生成完整网页、运行预览，并把每次修改保存成可回滚版本。</p>
          </div>
        )}
        {project.messages.map((message) => <MessageRow key={message.id} message={message} />)}
        {generating && (
          <div className="build-event">
            <LoaderCircle className="spin" size={16} />
            <div>
              <strong>{phase || '正在准备生成上下文'}</strong>
              <span>{streamedChars > 0 ? `已接收 ${streamedChars.toLocaleString()} 个字符` : '等待模型响应'}</span>
            </div>
          </div>
        )}
        <div ref={endRef} />
      </div>

      <div className="suggestion-row">
        {suggestions.map((suggestion) => (
          <button key={suggestion} onClick={() => setPrompt(suggestion)} disabled={generating}>
            {suggestion}
          </button>
        ))}
      </div>

      <div className="composer">
        <textarea
          value={prompt}
          onChange={(event) => setPrompt(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter' && !event.shiftKey) {
              event.preventDefault()
              void submit()
            }
          }}
          placeholder={project.currentHtml ? '继续描述要修改的内容...' : '例如：做一个支持优先级和筛选的待办应用'}
          disabled={generating}
          rows={3}
        />
        <button className="send-button" onClick={() => void submit()} disabled={!prompt.trim() || generating} title="发送需求">
          {generating ? <LoaderCircle className="spin" size={18} /> : <ArrowUp size={18} />}
        </button>
        <span className="composer-hint">Enter 发送 · Shift + Enter 换行</span>
      </div>
    </section>
  )
}

function MessageRow({ message }: { message: Message }) {
  const assistant = message.role === 'assistant'
  return (
    <article className={`message-row ${assistant ? 'assistant' : 'user'}`}>
      <span className="message-avatar">
        {assistant ? <CheckCircle2 size={15} /> : <UserRound size={15} />}
      </span>
      <div>
        <strong>{assistant ? 'BuildTrace' : '你'}</strong>
        <p>{message.content}</p>
      </div>
    </article>
  )
}
