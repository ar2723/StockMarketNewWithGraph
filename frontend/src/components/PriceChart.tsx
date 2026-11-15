import {
  CandlestickSeries,
  ColorType,
  CrosshairMode,
  LineSeries,
  createChart,
  createSeriesMarkers,
  type IChartApi,
  type ISeriesApi,
  type ISeriesMarkersPluginApi,
  type MouseEventParams,
  type Time,
} from 'lightweight-charts'
import { useEffect, useMemo, useRef, useState } from 'react'
import type { DailyPrice } from '../lib/api'
import { direction, formatChange, formatDate, formatPrice } from '../lib/format'
import { spikeMarkers } from '../lib/markers'

export type ChartType = 'candle' | 'line'

interface Props {
  data: DailyPrice[]
  chartType: ChartType
  currency: string
  /** 급변 기준(%) — 범례 문구용 */
  spikeThresholdPct: number
}

interface Palette {
  surface: string
  text: string
  muted: string
  grid: string
  border: string
  up: string
  down: string
  line: string
}

function readPalette(el: HTMLElement): Palette {
  const css = getComputedStyle(el)
  const v = (name: string) => css.getPropertyValue(name).trim()
  return {
    surface: v('--surface-1'),
    text: v('--text-secondary'),
    muted: v('--text-muted'),
    grid: v('--grid'),
    border: v('--border'),
    up: v('--up'),
    down: v('--down'),
    line: v('--line'),
  }
}

function chartOptions(p: Palette, currency: string) {
  return {
    layout: {
      background: { type: ColorType.Solid, color: p.surface },
      textColor: p.text,
      fontFamily: 'system-ui, -apple-system, "Segoe UI", "Apple SD Gothic Neo", "Malgun Gothic", sans-serif',
      attributionLogo: true, // TradingView Lightweight Charts 라이선스 표기
    },
    grid: { vertLines: { visible: false }, horzLines: { color: p.grid } },
    rightPriceScale: { borderColor: p.border },
    timeScale: { borderColor: p.border, fixLeftEdge: true, fixRightEdge: true },
    crosshair: { mode: CrosshairMode.Magnet },
    localization: { locale: 'ko-KR', priceFormatter: (price: number) => formatPrice(price, currency) },
  }
}

export default function PriceChart({ data, chartType, currency, spikeThresholdPct }: Props) {
  const containerRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<IChartApi | null>(null)
  const seriesRef = useRef<ISeriesApi<'Candlestick'> | ISeriesApi<'Line'> | null>(null)
  const markersRef = useRef<ISeriesMarkersPluginApi<Time> | null>(null)
  const [hovered, setHovered] = useState<DailyPrice | null>(null)

  const byDate = useMemo(() => new Map(data.map((d) => [d.tradeDate, d])), [data])
  const latest = data.at(-1) ?? null
  const shown = hovered ?? latest

  // 차트 생성/정리. 지표·형태·데이터가 바뀌면 다시 그린다(데이터가 최대 1,100개라 비용이 작다).
  useEffect(() => {
    const el = containerRef.current
    if (!el) return
    let palette = readPalette(el)
    const chart = createChart(el, { autoSize: true, ...chartOptions(palette, currency) })
    chartRef.current = chart

    const series =
      chartType === 'candle'
        ? chart.addSeries(CandlestickSeries, {
            upColor: palette.up,
            downColor: palette.down,
            wickUpColor: palette.up,
            wickDownColor: palette.down,
            borderVisible: false,
            priceLineVisible: false,
          })
        : chart.addSeries(LineSeries, { color: palette.line, lineWidth: 2, priceLineVisible: false })
    seriesRef.current = series

    if (chartType === 'candle') {
      ;(series as ISeriesApi<'Candlestick'>).setData(
        data.map((d) => ({ time: d.tradeDate, open: d.open, high: d.high, low: d.low, close: d.close })),
      )
    } else {
      ;(series as ISeriesApi<'Line'>).setData(data.map((d) => ({ time: d.tradeDate, value: d.close })))
    }
    markersRef.current = createSeriesMarkers(series, spikeMarkers(data, palette))
    chart.timeScale().fitContent()

    const onMove = (param: MouseEventParams<Time>) => {
      const key = typeof param.time === 'string' ? param.time : null
      setHovered(key ? (byDate.get(key) ?? null) : null)
    }
    chart.subscribeCrosshairMove(onMove)

    // OS 다크 모드 전환 시 색을 다시 읽는다
    const media = window.matchMedia?.('(prefers-color-scheme: dark)')
    const onScheme = () => {
      palette = readPalette(el)
      chart.applyOptions(chartOptions(palette, currency))
      if (chartType === 'candle') {
        series.applyOptions({
          upColor: palette.up,
          downColor: palette.down,
          wickUpColor: palette.up,
          wickDownColor: palette.down,
        })
      } else {
        series.applyOptions({ color: palette.line })
      }
      markersRef.current?.setMarkers(spikeMarkers(data, palette))
    }
    media?.addEventListener('change', onScheme)

    return () => {
      media?.removeEventListener('change', onScheme)
      chart.unsubscribeCrosshairMove(onMove)
      chart.remove()
      chartRef.current = null
      seriesRef.current = null
      markersRef.current = null
    }
  }, [data, chartType, currency, byDate])

  const d = direction(shown?.changeRate)
  const changeClass = d === 'up' ? 'text-up' : d === 'down' ? 'text-down' : 'text-ink-2'

  return (
    <div>
      {/* 크로스헤어 값 표시줄: 마우스/터치한 날, 아니면 가장 최근 날 */}
      <div className="tabular flex flex-wrap items-baseline gap-x-4 gap-y-1 px-1 pb-2 text-sm" aria-live="polite">
        <span className="font-medium text-ink">{formatDate(shown?.tradeDate)}</span>
        {shown && (
          <>
            <span className="text-ink-2">
              시 <span className="text-ink">{formatPrice(shown.open, currency)}</span>
            </span>
            <span className="text-ink-2">
              고 <span className="text-ink">{formatPrice(shown.high, currency)}</span>
            </span>
            <span className="text-ink-2">
              저 <span className="text-ink">{formatPrice(shown.low, currency)}</span>
            </span>
            <span className="text-ink-2">
              종 <span className="text-ink">{formatPrice(shown.close, currency)}</span>
            </span>
            <span className={changeClass}>{formatChange(shown.changeRate)}</span>
            {shown.spike && (
              <span className="rounded border border-border px-1.5 py-0.5 text-xs text-ink-2">
                급변일 (±{spikeThresholdPct}% 이상)
              </span>
            )}
            {!shown.complete && <span className="text-xs text-ink-3">진행 중</span>}
          </>
        )}
      </div>
      <div ref={containerRef} className="h-[300px] w-full md:h-[420px]" data-testid="price-chart" />
      <p className="mt-2 flex flex-wrap gap-x-4 px-1 text-xs text-ink-3">
        <span>
          <span className="text-up">▲</span> 급변 상승 &nbsp;<span className="text-down">▼</span> 급변 하락 (전일 대비 ±
          {spikeThresholdPct}% 이상)
        </span>
      </p>
    </div>
  )
}
