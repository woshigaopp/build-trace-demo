import { SandpackPreview, useSandpack } from '@codesandbox/sandpack-react'
import type { SandpackPreviewRef } from '@codesandbox/sandpack-react'
import { LoaderCircle } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'

export function ReliableSandpackPreview({ publicMode = false }: { publicMode?: boolean }) {
  const { sandpack, dispatch } = useSandpack()
  const refreshed = useRef(false)
  const previewHandle = useRef<SandpackPreviewRef | null>(null)
  const [ready, setReady] = useState(false)
  const [clientId, setClientId] = useState<string>()
  const capturePreview = useCallback((preview: SandpackPreviewRef | null) => {
    previewHandle.current = preview
    setClientId(preview?.clientId)
  }, [])

  useEffect(() => {
    if (sandpack.status !== 'running' || refreshed.current) return
    const timer = window.setTimeout(() => {
      refreshed.current = true
      dispatch({ type: 'refresh' })
    }, 300)
    return () => window.clearTimeout(timer)
  }, [dispatch, sandpack.status])

  useEffect(() => {
    if (!publicMode || !clientId) return
    let doneAt: number | null = null
    const interval = window.setInterval(() => {
      if (previewHandle.current?.getClient()?.status !== 'done') {
        doneAt = null
        return
      }
      doneAt ??= Date.now()
      if (Date.now() - doneAt >= 600) {
        setReady(true)
        window.clearInterval(interval)
      }
    }, 150)
    return () => window.clearInterval(interval)
  }, [clientId, publicMode])

  const failed = sandpack.status === 'timeout' || Boolean(sandpack.error)

  return <div className={`reliable-preview${publicMode ? ' public' : ''}`} aria-busy={publicMode && !ready && !failed}>
    <SandpackPreview
      ref={capturePreview}
      showOpenInCodeSandbox={false}
      showRefreshButton={!publicMode}
      showRestartButton={!publicMode}
    />
    {publicMode && !ready && !failed && <div className="public-runtime-loading" role="status">
      <LoaderCircle className="spin" size={21} />
      <strong>正在启动应用</strong>
      <span>首次打开需要准备运行环境</span>
    </div>}
  </div>
}
