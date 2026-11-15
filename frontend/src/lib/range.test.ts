import { describe, expect, it } from 'vitest'
import { kstToday, minusMonths, rangeDates } from './range'

describe('kstToday', () => {
  it('UTC 15시 이후는 KST 다음 날이다', () => {
    expect(kstToday(new Date('2026-11-14T14:59:59Z'))).toBe('2026-11-14')
    expect(kstToday(new Date('2026-11-14T15:00:00Z'))).toBe('2026-11-15')
  })
})

describe('minusMonths', () => {
  it('개월 수만큼 뺀다', () => {
    expect(minusMonths('2026-11-15', 1)).toBe('2026-10-15')
    expect(minusMonths('2026-11-15', 36)).toBe('2023-11-15')
  })
  it('해가 바뀌는 경우', () => {
    expect(minusMonths('2026-02-10', 3)).toBe('2025-11-10')
  })
  it('없는 날짜는 그 달 말일로 맞춘다', () => {
    expect(minusMonths('2026-03-31', 1)).toBe('2026-02-28')
  })
})

describe('rangeDates', () => {
  it('선택한 기간의 시작일과 오늘을 돌려준다', () => {
    expect(rangeDates('3M', '2026-11-15')).toEqual({ from: '2026-08-15', to: '2026-11-15' })
  })
})
