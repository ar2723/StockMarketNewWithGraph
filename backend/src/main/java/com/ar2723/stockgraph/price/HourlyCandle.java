package com.ar2723.stockgraph.price;

import java.math.BigDecimal;
import java.time.Instant;

/** 시간봉 하나. {@code start} 는 봉이 시작하는 시각(UTC). */
public record HourlyCandle(Instant start, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
		BigDecimal volume) {
}
