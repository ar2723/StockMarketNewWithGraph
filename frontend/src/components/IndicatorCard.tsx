import type { IndicatorSummary } from '../lib/api'
import { direction, formatChange, formatDate, formatPrice } from '../lib/format'

interface Props {
  indicator: IndicatorSummary
  selected: boolean
  onSelect: (code: string) => void
}

export default function IndicatorCard({ indicator, selected, onSelect }: Props) {
  const ready = indicator.enabled && indicator.latestClose != null
  const d = direction(indicator.changeRate)
  const changeClass = d === 'up' ? 'text-up' : d === 'down' ? 'text-down' : 'text-ink-2'

  return (
    <button
      type="button"
      onClick={() => onSelect(indicator.code)}
      disabled={!ready}
      aria-pressed={selected}
      className={[
        'flex min-h-[88px] flex-col items-start gap-1 rounded-lg border bg-surface-1 p-3 text-left transition-colors',
        'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus',
        selected ? 'border-ink' : 'border-border hover:border-ink-3',
        ready ? 'cursor-pointer' : 'cursor-not-allowed opacity-60',
      ].join(' ')}
    >
      <span className="text-sm text-ink-2">{indicator.name}</span>
      {ready ? (
        <>
          <span className="tabular text-lg font-semibold text-ink">
            {formatPrice(indicator.latestClose, indicator.currency)}
          </span>
          <span className="tabular flex w-full items-center justify-between text-xs">
            <span className={changeClass}>{formatChange(indicator.changeRate)}</span>
            <span className="text-ink-3">{formatDate(indicator.latestDate)}</span>
          </span>
        </>
      ) : (
        <span className="text-sm text-ink-3">데이터 준비 중</span>
      )}
    </button>
  )
}
