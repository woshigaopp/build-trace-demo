import { Braces, LoaderCircle, LockKeyhole, Mail, ShieldCheck } from 'lucide-react'
import { useState } from 'react'

type Mode = 'login' | 'register'

interface Props {
  submitting: boolean
  error: string | null
  onSubmit: (mode: Mode, email: string, password: string) => Promise<void>
}

export function AuthScreen({ submitting, error, onSubmit }: Props) {
  const [mode, setMode] = useState<Mode>('register')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const submit = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!email.trim() || password.length < 8 || submitting) return
    await onSubmit(mode, email.trim(), password)
  }

  return (
    <main className="auth-page">
      <section className="auth-panel">
        <div className="auth-brand"><span><Braces size={21} /></span><div><strong>BuildTrace</strong><small>AI App Builder</small></div></div>
        <div className="auth-heading">
          <span className="auth-kicker"><ShieldCheck size={13} />持久化工作空间</span>
          <h1>{mode === 'register' ? '创建你的构建空间' : '继续上次的构建'}</h1>
          <p>{mode === 'register' ? '项目、对话、生成状态和所有版本都会绑定到这个账号。' : '登录后会恢复完整的项目、对话和代码版本。'}</p>
        </div>

        <div className="auth-mode" role="tablist" aria-label="登录方式">
          <button className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')} type="button">注册</button>
          <button className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')} type="button">登录</button>
        </div>

        <form onSubmit={(event) => void submit(event)} className="auth-form">
          <label><span>邮箱</span><div><Mail size={16} /><input type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" required /></div></label>
          <label><span>密码</span><div><LockKeyhole size={16} /><input type="password" autoComplete={mode === 'register' ? 'new-password' : 'current-password'} value={password} onChange={(event) => setPassword(event.target.value)} placeholder="至少 8 位" minLength={8} maxLength={72} required /></div></label>
          {error && <div className="auth-error" role="alert">{error}</div>}
          <button className="auth-submit" disabled={submitting || !email.trim() || password.length < 8}>
            {submitting && <LoaderCircle className="spin" size={17} />}
            {mode === 'register' ? '注册并进入' : '登录工作空间'}
          </button>
        </form>
      </section>
      <aside className="auth-proof">
        <span className="proof-index">01 / ACCOUNT</span>
        <h2>每次构建都有归属，也都能被恢复。</h2>
        <div className="proof-lines"><span>Authenticated identity</span><span>Durable build runs</span><span>Immutable file snapshots</span></div>
      </aside>
    </main>
  )
}
