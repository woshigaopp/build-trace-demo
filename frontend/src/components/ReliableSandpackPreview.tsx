import { SandpackPreview, useSandpack } from '@codesandbox/sandpack-react'
import type { SandpackPreviewRef } from '@codesandbox/sandpack-react'
import { LoaderCircle } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'

export function ReliableSandpackPreview({ publicMode = false }: { publicMode?: boolean }) {
  const { sandpack, dispatch, listen } = useSandpack()
  const refreshed = useRef(false)
  const [ready, setReady] = useState(false)
  const [clientId, setClientId] = useState<string>()
  const capturePreview = useCallback((preview: SandpackPreviewRef | null) => setClientId(preview?.clientId), [])

  useEffect(() => {
    if (publicMode || sandpack.status !== 'running' || refreshed.current) return
    const timer = window.setTimeout(() => {
      refreshed.current = true
      dispatch({ type: 'refresh' })
    }, 300)
    return () => window.clearTimeout(timer)
  }, [dispatch, publicMode, sandpack.status])

  useEffect(() => {
    if (!publicMode || !clientId) return
    let refreshTimer: number | undefined
    const unsubscribe = listen((message) => {
      if (message.type === 'start' && message.firstLoad) {
        refreshed.current = false
        setReady(false)
      }
      if (message.type === 'stdout' && !refreshed.current && /(?:ready in|Local:)/i.test(message.payload.data ?? '')) {
        refreshed.current = true
        refreshTimer = window.setTimeout(() => dispatch({ type: 'refresh' }, clientId), 250)
      }
      if (message.type === 'done') {
        setReady(true)
      }
    }, clientId)
    return () => {
      unsubscribe()
      window.clearTimeout(refreshTimer)
    }
  }, [clientId, dispatch, listen, publicMode])

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
