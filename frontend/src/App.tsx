import { useEffect, useState } from 'react'

type BackendStatus = 'checking' | 'up' | 'down'

// Phase 0 골격: 백엔드 연결만 확인한다. 지표 차트는 Sprint 1 에서 붙인다.
export default function App() {
  const [status, setStatus] = useState<BackendStatus>('checking')

  useEffect(() => {
    const controller = new AbortController()
    fetch('/actuator/health', { signal: controller.signal })
      .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
      .then((body: { status?: string }) => setStatus(body.status === 'UP' ? 'up' : 'down'))
      .catch((err: unknown) => {
        if (!(err instanceof DOMException && err.name === 'AbortError')) setStatus('down')
      })
    return () => controller.abort()
  }, [])

  return (
    <main style={{ maxWidth: 960, margin: '0 auto', padding: 16 }}>
      <h1>StockGraph</h1>
      <p>금융 지표 차트와 연관 뉴스를 함께 보여주는 서비스 (개발 중)</p>
      <p role="status">
        백엔드 상태:{' '}
        {status === 'checking' ? '확인 중' : status === 'up' ? '정상' : '연결 안 됨'}
      </p>
      <footer>
        <small>이 서비스의 정보는 투자 권유가 아니며, 투자 판단의 책임은 이용자에게 있습니다.</small>
      </footer>
    </main>
  )
}
