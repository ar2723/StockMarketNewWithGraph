package com.ar2723.stockgraph.price;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 일봉 하나.
 *
 * @param changeRate 전일 종가 대비 비율(0.0213 = +2.13%). 전일 데이터가 없으면 null
 * @param complete 그날이 끝났는지. 오늘 일봉은 매시간 바뀌는 미완성 값이라 false
 */
public record DailyPrice(LocalDate tradeDate, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
		BigDecimal volume, BigDecimal changeRate, boolean complete) {
}
