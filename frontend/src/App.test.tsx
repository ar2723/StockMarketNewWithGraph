import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

function mockFetch(response: Partial<Response> | Error) {
  const fn = response instanceof Error ? vi.fn().mockRejectedValue(response) : vi.fn().mockResolvedValue(response)
  vi.stubGlobal('fetch', fn)
  return fn
}

describe('App', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('백엔드 health 가 UP 이면 정상으로 표시한다', async () => {
    const fetchMock = mockFetch({ ok: true, json: async () => ({ status: 'UP' }) })
    render(<App />)
    expect(await screen.findByText('백엔드 상태: 정상')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/actuator/health', expect.anything())
  })

  it('백엔드에 연결할 수 없으면 연결 안 됨으로 표시한다', async () => {
    mockFetch(new TypeError('Failed to fetch'))
    render(<App />)
    expect(await screen.findByText('백엔드 상태: 연결 안 됨')).toBeInTheDocument()
  })

  it('면책 문구를 항상 보여준다', () => {
    mockFetch({ ok: false, status: 503 })
    render(<App />)
    expect(screen.getByText(/투자 권유가 아니며/)).toBeInTheDocument()
  })
})
