export type Direction = 'up' | 'down' | 'flat'

export function direction(rate: number | null | undefined): Direction {
  if (rate == null || rate === 0) return 'flat'
  return rate > 0 ? 'up' : 'down'
}

const krw = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 0 })
const usd = new Intl.NumberFormat('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

/** 원화는 정수, 그 외(달러 지수·금·환율)는 소수 둘째 자리까지. */
export function formatPrice(value: number | null | undefined, currency: string): string {
  if (value == null) return '-'
  return currency === 'KRW' ? krw.format(value) : usd.format(value)
}

/** 비율(0.0213)을 "▲ 2.13%" 처럼. 색만으로 방향을 구분하지 않도록 기호를 붙인다. */
export function formatChange(rate: number | null | undefined): string {
  if (rate == null) return '-'
  const pct = Math.abs(rate * 100).toFixed(2)
  const d = direction(rate)
  if (d === 'flat') return `${pct}%`
  return `${d === 'up' ? '▲' : '▼'} ${pct}%`
}

/** 화면 표시용 짧은 날짜: 2026-11-15 -> 2026.11.15 */
export function formatDate(isoDate: string | null | undefined): string {
  if (!isoDate) return '-'
  return isoDate.replaceAll('-', '.')
}
