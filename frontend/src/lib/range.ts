// 차트 기간 선택. 날짜 계산은 KST 기준(BTC 하루 경계가 KST 00시라서).

export type RangeKey = '1M' | '3M' | '1Y' | '3Y'

export const RANGES: { key: RangeKey; label: string; months: number }[] = [
  { key: '1M', label: '1개월', months: 1 },
  { key: '3M', label: '3개월', months: 3 },
  { key: '1Y', label: '1년', months: 12 },
  { key: '3Y', label: '3년', months: 36 },
]

/** KST 오늘 날짜 (YYYY-MM-DD). */
export function kstToday(now: Date = new Date()): string {
  // en-CA 로케일은 YYYY-MM-DD 형식으로 내려준다
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul' }).format(now)
}

/** YYYY-MM-DD 에서 n개월 뺀 날짜. 말일은 그 달의 마지막 날로 맞춘다(3/31 - 1개월 = 2/28). */
export function minusMonths(isoDate: string, months: number): string {
  const [y, m, d] = isoDate.split('-').map(Number)
  const targetMonthIndex = m - 1 - months
  const lastDay = new Date(Date.UTC(y, targetMonthIndex + 1, 0)).getUTCDate()
  const result = new Date(Date.UTC(y, targetMonthIndex, Math.min(d, lastDay)))
  return result.toISOString().slice(0, 10)
}

export function rangeDates(key: RangeKey, today: string = kstToday()): { from: string; to: string } {
  const months = RANGES.find((r) => r.key === key)!.months
  return { from: minusMonths(today, months), to: today }
}
