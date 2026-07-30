import { SandpackPreview, useSandpack } from '@codesandbox/sandpack-react'
import { useEffect, useRef } from 'react'

export function ReliableSandpackPreview() {
  const { sandpack, dispatch } = useSandpack()
  const refreshed = useRef(false)

  useEffect(() => {
    if (sandpack.status !== 'running' || refreshed.current) return
    const timer = window.setTimeout(() => {
      refreshed.current = true
      dispatch({ type: 'refresh' })
    }, 300)
    return () => window.clearTimeout(timer)
  }, [dispatch, sandpack.status])

  return <SandpackPreview showOpenInCodeSandbox={false} showRefreshButton />
}
