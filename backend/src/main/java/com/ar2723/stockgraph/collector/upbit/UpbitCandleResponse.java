package com.ar2723.stockgraph.collector.upbit;

import com.ar2723.stockgraph.price.HourlyCandle;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** 업비트 분봉 응답 한 건. 필요한 필드만 받는다. */
@JsonIgnoreProperties(ignoreUnknown = true)
record UpbitCandleResponse(
		@JsonProperty("candle_date_time_utc") LocalDateTime candleDateTimeUtc,
		@JsonProperty("opening_price") BigDecimal openingPrice,
		@JsonProperty("high_price") BigDecimal highPrice,
		@JsonProperty("low_price") BigDecimal lowPrice,
		@JsonProperty("trade_price") BigDecimal tradePrice,
		@JsonProperty("candle_acc_trade_volume") BigDecimal candleAccTradeVolume) {

	HourlyCandle toHourlyCandle() {
		return new HourlyCandle(candleDateTimeUtc.toInstant(ZoneOffset.UTC), openingPrice, highPrice, lowPrice,
				tradePrice, candleAccTradeVolume);
	}

}
