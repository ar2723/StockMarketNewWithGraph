import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import type { DailyPrice, IndicatorSummary } from './lib/api'

// jsdom 에는 canvas 가 없어서 차트 라이브러리 대신 받은 props 만 그리는 가짜 컴포넌트를 쓴다
vi.mock('./components/PriceChart', () => ({
  default: (props: { data: DailyPrice[]; chartType: string }) => (
    <div data-testid="price-chart">
      {props.chartType}:{props.data.length}
    </div>
  ),
}))

const indicators: IndicatorSummary[] = [
  {
    code: 'KOSPI', name: 'KOSPI', category: 'INDEX', currency: 'KRW', enabled: false, spikeThresholdPct: 2,
    latestDate: null, latestClose: null, changeRate: null, spike: false,
  },
  {
    code: 'BTC', name: '비트코인', category: 'CRYPTO', currency: 'KRW', enabled: true, spikeThresholdPct: 4,
    latestDate: '2026-11-15', latestClose: 89894000, changeRate: -0.008044, spike: false,
  },
]

const daily: DailyPrice[] = [
  { tradeDate: '2026-11-13', open: 1, high: 1, low: 1, close: 97201000, volume: 1, changeRate: -0.045, complete: true, spike: true },
  { tradeDate: '2026-11-14', open: 1, high: 1, low: 1, close: 90623000, volume: 1, changeRate: -0.0328, complete: true, spike: false },
]

function respond(body: unknown, ok = true, status = 200) {
  return Promise.resolve({ ok, status, json: async () => body } as Response)
}

function renderApp() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <App />
    </QueryClientProvider>,
  )
}

describe('App', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    fetchMock = vi.fn((url: string) => (url.startsWith('/api/indicators/') ? respond(daily) : respond(indicators)))
    vi.stubGlobal('fetch', fetchMock)
  })
  afterEach(() => vi.unstubAllGlobals())

  it('지표 카드를 그리고, 데이터가 없는 지표는 준비 중으로 비활성화한다', async () => {
    renderApp()
    const kospi = await screen.findByRole('button', { name: /KOSPI/ })
    expect(kospi).toBeDisabled()
    expect(within(kospi).getByText('데이터 준비 중')).toBeInTheDocument()

    const btc = screen.getByRole('button', { name: /비트코인/ })
    expect(within(btc).getByText('89,894,000')).toBeInTheDocument()
    expect(within(btc).getByText('▼ 0.80%')).toBeInTheDocument()
  })

  it('데이터가 있는 첫 지표(BTC)의 1년 차트를 기본으로 보여준다', async () => {
    renderApp()
    expect(await screen.findByTestId('price-chart')).toHaveTextContent('candle:2')
    expect(screen.getByRole('button', { name: /비트코인/ })).toHaveAttribute('aria-pressed', 'true')
    const dailyUrl = fetchMock.mock.calls.map((c) => c[0] as string).find((u) => u.includes('/BTC/daily'))
    expect(dailyUrl).toMatch(/from=\d{4}-\d{2}-\d{2}&to=\d{4}-\d{2}-\d{2}/)
  })

  it('라인으로 바꾸면 차트 형태가 바뀐다', async () => {
    renderApp()
    await screen.findByTestId('price-chart')
    await userEvent.click(screen.getByRole('button', { name: '라인' }))
    expect(screen.getByTestId('price-chart')).toHaveTextContent('line:2')
  })

  it('급변일을 표로도 볼 수 있다', async () => {
    renderApp()
    await screen.findByTestId('price-chart')
    expect(screen.getByText('이 기간의 급변일 1일 표로 보기')).toBeInTheDocument()
    expect(screen.getByText('2026.11.13')).toBeInTheDocument()
  })

  it('지표 API 가 실패하면 안내 문구를 보여준다', async () => {
    fetchMock.mockImplementation(() => respond({}, false, 503))
    renderApp()
    expect(await screen.findByRole('alert')).toHaveTextContent('지표를 불러오지 못했습니다')
  })

  it('면책 문구와 출처를 항상 보여준다', async () => {
    renderApp()
    expect(screen.getByText(/투자 권유가 아니며/)).toBeInTheDocument()
    expect(screen.getByText(/업비트/)).toBeInTheDocument()
  })
})
