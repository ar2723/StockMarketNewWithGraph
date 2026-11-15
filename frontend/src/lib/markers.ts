import type { SeriesMarker, Time } from 'lightweight-charts'
import type { DailyPrice } from './api'
import { direction } from './format'

/** 급변일: 상승은 봉 아래 위쪽 화살표(빨강), 하락은 봉 위 아래쪽 화살표(파랑). 모양으로도 방향이 구분된다. */
export function spikeMarkers(data: DailyPrice[], p: { up: string; down: string }): SeriesMarker<Time>[] {
  return data
    .filter((d) => d.spike && d.changeRate != null)
    .map((d) =>
      direction(d.changeRate) === 'up'
        ? { time: d.tradeDate, position: 'belowBar', shape: 'arrowUp', color: p.up, size: 1 }
        : { time: d.tradeDate, position: 'aboveBar', shape: 'arrowDown', color: p.down, size: 1 },
    )
}
