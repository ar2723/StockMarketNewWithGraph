// 백엔드 /api 응답 타입과 호출. 개발 서버에서는 Vite 가, 운영에서는 nginx 가 /api 를 백엔드로 넘긴다.

export type IndicatorCategory = 'INDEX' | 'FX' | 'COMMODITY' | 'CRYPTO'

export interface IndicatorSummary {
  code: string
  name: string
  category: IndicatorCategory
  currency: string
  enabled: boolean
  /** 급변 기준(%). BTC 4, 나머지 기본 2 */
  spikeThresholdPct: number
  latestDate: string | null
  latestClose: number | null
  /** 전일 대비 비율 (0.0213 = +2.13%) */
  changeRate: number | null
  spike: boolean
}

export interface DailyPrice {
  /** YYYY-MM-DD (BTC 는 KST 날짜) */
  tradeDate: string
  open: number
  high: number
  low: number
  close: number
  volume: number | null
  changeRate: number | null
  /** 그날이 끝났는지. 오늘 일봉은 false */
  complete: boolean
  spike: boolean
}

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function getJson<T>(url: string, signal?: AbortSignal): Promise<T> {
  const res = await fetch(url, { signal, headers: { Accept: 'application/json' } })
  if (!res.ok) {
    throw new ApiError(res.status, `요청 실패 (${res.status})`)
  }
  return (await res.json()) as T
}

export function fetchIndicators(signal?: AbortSignal) {
  return getJson<IndicatorSummary[]>('/api/indicators', signal)
}

export function fetchDaily(code: string, from: string, to: string, signal?: AbortSignal) {
  const qs = new URLSearchParams({ from, to })
  return getJson<DailyPrice[]>(`/api/indicators/${encodeURIComponent(code)}/daily?${qs}`, signal)
}
