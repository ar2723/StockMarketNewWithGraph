import type { DailyPrice } from '../lib/api'
import { direction, formatChange, formatDate, formatPrice } from '../lib/format'

interface Props {
  data: DailyPrice[]
  currency: string
}

/**
 * 차트의 급변일 마커를 표로도 보여준다(색·그림을 못 보는 경우 대비, 접근성).
 * Sprint 3 에서 각 행에 그날의 연관 뉴스가 붙는다.
 */
export default function SpikeTable({ data, currency }: Props) {
  const spikes = data.filter((d) => d.spike).reverse()

  return (
    <details className="mt-4 rounded-lg border border-border bg-surface-1">
      <summary className="cursor-pointer px-4 py-3 text-sm text-ink-2 select-none">
        이 기간의 급변일 {spikes.length}일 표로 보기
      </summary>
      {spikes.length === 0 ? (
        <p className="px-4 pb-4 text-sm text-ink-3">이 기간에는 급변일이 없습니다.</p>
      ) : (
        <div className="max-h-80 overflow-y-auto px-4 pb-4">
          <table className="tabular w-full text-sm">
            <thead className="sticky top-0 bg-surface-1 text-left text-xs text-ink-3">
              <tr>
                <th className="py-2 font-normal">날짜</th>
                <th className="py-2 text-right font-normal">종가</th>
                <th className="py-2 text-right font-normal">전일 대비</th>
              </tr>
            </thead>
            <tbody>
              {spikes.map((d) => {
                const dir = direction(d.changeRate)
                return (
                  <tr key={d.tradeDate} className="border-t border-border">
                    <td className="py-2 text-ink">{formatDate(d.tradeDate)}</td>
                    <td className="py-2 text-right text-ink">{formatPrice(d.close, currency)}</td>
                    <td className={`py-2 text-right ${dir === 'up' ? 'text-up' : 'text-down'}`}>
                      {formatChange(d.changeRate)}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </details>
  )
}
