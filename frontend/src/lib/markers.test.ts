import { describe, expect, it } from 'vitest'
import type { DailyPrice } from './api'
import { spikeMarkers } from './markers'

function day(tradeDate: string, changeRate: number | null, spike: boolean): DailyPrice {
  return { tradeDate, open: 1, high: 1, low: 1, close: 1, volume: 1, changeRate, complete: true, spike }
}

describe('spikeMarkers', () => {
  const palette = { up: '#e34948', down: '#2a78d6' }

  it('급변일만 마커로 만든다', () => {
    const markers = spikeMarkers([day('2026-11-12', 0.01, false), day('2026-11-13', 0.05, true)], palette)
    expect(markers).toHaveLength(1)
    expect(markers[0].time).toBe('2026-11-13')
  })

  it('상승은 봉 아래 위쪽 화살표(빨강), 하락은 봉 위 아래쪽 화살표(파랑)', () => {
    const [up, down] = spikeMarkers([day('2026-11-13', 0.05, true), day('2026-11-14', -0.06, true)], palette)
    expect(up).toMatchObject({ position: 'belowBar', shape: 'arrowUp', color: '#e34948' })
    expect(down).toMatchObject({ position: 'aboveBar', shape: 'arrowDown', color: '#2a78d6' })
  })
})
