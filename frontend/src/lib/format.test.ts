import { describe, expect, it } from 'vitest'
import { direction, formatChange, formatDate, formatPrice } from './format'

describe('formatPrice', () => {
  it('원화는 천 단위 구분, 소수 없음', () => {
    expect(formatPrice(89894000, 'KRW')).toBe('89,894,000')
  })
  it('달러는 소수 둘째 자리까지', () => {
    expect(formatPrice(5432.1, 'USD')).toBe('5,432.10')
  })
  it('값이 없으면 -', () => {
    expect(formatPrice(null, 'KRW')).toBe('-')
  })
})

describe('formatChange', () => {
  it('상승은 ▲, 하락은 ▼ 기호를 붙인다(색만으로 구분하지 않기)', () => {
    expect(formatChange(0.0213)).toBe('▲ 2.13%')
    expect(formatChange(-0.008044)).toBe('▼ 0.80%')
  })
  it('보합은 기호 없이', () => {
    expect(formatChange(0)).toBe('0.00%')
  })
  it('값이 없으면 -', () => {
    expect(formatChange(null)).toBe('-')
  })
})

describe('direction / formatDate', () => {
  it('방향을 판정한다', () => {
    expect(direction(0.01)).toBe('up')
    expect(direction(-0.01)).toBe('down')
    expect(direction(null)).toBe('flat')
  })
  it('날짜를 점으로 구분한다', () => {
    expect(formatDate('2026-11-15')).toBe('2026.11.15')
  })
})
