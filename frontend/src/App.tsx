import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import IndicatorCard from './components/IndicatorCard'
import PriceChart, { type ChartType } from './components/PriceChart'
import SpikeTable from './components/SpikeTable'
import { fetchDaily, fetchIndicators } from './lib/api'
import { RANGES, rangeDates, type RangeKey } from './lib/range'

const DATA_SOURCES: Record<string, string> = { BTC: '업비트' }

function SegmentButton({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      aria-pressed={active}
      onClick={onClick}
      className={[
        'min-h-9 rounded-md px-3 text-sm transition-colors',
        'focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-focus',
        active ? 'bg-surface-2 font-medium text-ink' : 'text-ink-2 hover:text-ink',
      ].join(' ')}
    >
      {children}
    </button>
  )
}

export default function App() {
  const [selectedCode, setSelectedCode] = useState<string | null>(null)
  const [range, setRange] = useState<RangeKey>('1Y')
  const [chartType, setChartType] = useState<ChartType>('candle')

  const indicators = useQuery({ queryKey: ['indicators'], queryFn: ({ signal }) => fetchIndicators(signal) })

  // 선택 전에는 데이터가 있는 첫 지표를 보여준다
  const list = indicators.data ?? []
  const fallback = list.find((i) => i.enabled && i.latestClose != null)
  const selected = list.find((i) => i.code === selectedCode) ?? fallback

  const { from, to } = rangeDates(range)
  const daily = useQuery({
    queryKey: ['daily', selected?.code, from, to],
    queryFn: ({ signal }) => fetchDaily(selected!.code, from, to, signal),
    enabled: selected != null,
    placeholderData: keepPreviousData,
  })

  return (
    <div className="mx-auto min-h-screen max-w-6xl px-4 py-6 md:px-6">
      <header className="mb-6">
        <h1 className="text-xl font-semibold text-ink md:text-2xl">StockGraph</h1>
        <p className="mt-1 text-sm text-ink-2">주요 금융 지표와 급변일을 한눈에 (연관 뉴스는 준비 중)</p>
      </header>

      {indicators.isPending && <p className="text-sm text-ink-3">지표를 불러오는 중…</p>}
      {indicators.isError && (
        <p role="alert" className="rounded-lg border border-border bg-surface-1 p-4 text-sm text-ink-2">
          지표를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.
        </p>
      )}

      {list.length > 0 && (
        <section aria-label="지표 목록" className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
          {list.map((i) => (
            <IndicatorCard
              key={i.code}
              indicator={i}
              selected={i.code === selected?.code}
              onSelect={setSelectedCode}
            />
          ))}
        </section>
      )}

      {selected && (
        <section aria-label={`${selected.name} 차트`} className="mt-6 rounded-lg border border-border bg-surface-1 p-3 md:p-4">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-base font-semibold text-ink">{selected.name}</h2>
            <div className="flex flex-wrap gap-2">
              <div role="group" aria-label="기간" className="flex rounded-lg border border-border p-0.5">
                {RANGES.map((r) => (
                  <SegmentButton key={r.key} active={range === r.key} onClick={() => setRange(r.key)}>
                    {r.label}
                  </SegmentButton>
                ))}
              </div>
              <div role="group" aria-label="차트 형태" className="flex rounded-lg border border-border p-0.5">
                <SegmentButton active={chartType === 'candle'} onClick={() => setChartType('candle')}>
                  캔들
                </SegmentButton>
                <SegmentButton active={chartType === 'line'} onClick={() => setChartType('line')}>
                  라인
                </SegmentButton>
              </div>
            </div>
          </div>

          {daily.isError ? (
            <p role="alert" className="py-16 text-center text-sm text-ink-2">
              차트 데이터를 불러오지 못했습니다.
            </p>
          ) : daily.data ? (
            <>
              <PriceChart
                data={daily.data}
                chartType={chartType}
                currency={selected.currency}
                spikeThresholdPct={selected.spikeThresholdPct}
              />
              <SpikeTable data={daily.data} currency={selected.currency} />
            </>
          ) : (
            <p className="py-16 text-center text-sm text-ink-3">차트를 불러오는 중…</p>
          )}
        </section>
      )}

      <footer className="mt-10 space-y-1 border-t border-border pt-4 text-xs text-ink-3">
        <p>이 서비스의 정보는 투자 권유가 아니며, 투자 판단의 책임은 이용자에게 있습니다.</p>
        <p>
          데이터: {Object.values(DATA_SOURCES).join(', ')} · 하루 기준: 한국 시간(KST) 00시 · 차트:{' '}
          <a className="underline" href="https://www.tradingview.com/" target="_blank" rel="noreferrer">
            TradingView Lightweight Charts
          </a>
        </p>
      </footer>
    </div>
  )
}
