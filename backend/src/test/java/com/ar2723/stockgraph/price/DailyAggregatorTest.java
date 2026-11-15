package com.ar2723.stockgraph.price;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class DailyAggregatorTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private static HourlyCandle candle(String utc, String open, String high, String low, String close, String volume) {
		return new HourlyCandle(Instant.parse(utc), new BigDecimal(open), new BigDecimal(high), new BigDecimal(low),
				new BigDecimal(close), new BigDecimal(volume));
	}

	@Test
	void KST_자정을_기준으로_하루를_나눈다() {
		List<HourlyCandle> candles = List.of(
				candle("2026-11-13T14:00:00Z", "100", "101", "99", "100", "1"), // 11-13 23:00 KST
				candle("2026-11-13T15:00:00Z", "100", "105", "98", "104", "2"), // 11-14 00:00 KST (새 날 시작)
				candle("2026-11-14T02:00:00Z", "104", "110", "103", "108", "3"), // 11-14 11:00 KST
				candle("2026-11-14T14:00:00Z", "108", "109", "95", "102", "4"), // 11-14 23:00 KST
				candle("2026-11-14T15:00:00Z", "102", "103", "101", "103", "5")); // 11-15 00:00 KST

		List<DailyPrice> daily = DailyAggregator.aggregate(candles, KST, LocalDate.of(2026, 11, 15), null);

		assertThat(daily).extracting(DailyPrice::tradeDate)
			.containsExactly(LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 15));

		DailyPrice nov14 = daily.get(1);
		assertThat(nov14.open()).isEqualByComparingTo("100"); // 00:00 KST 봉의 시가
		assertThat(nov14.close()).isEqualByComparingTo("102"); // 23:00 KST 봉의 종가
		assertThat(nov14.high()).isEqualByComparingTo("110");
		assertThat(nov14.low()).isEqualByComparingTo("95");
		assertThat(nov14.volume()).isEqualByComparingTo("9");
	}

	@Test
	void 오늘_일봉만_미완성으로_표시한다() {
		List<HourlyCandle> candles = List.of(candle("2026-11-13T15:00:00Z", "1", "1", "1", "1", "1"),
				candle("2026-11-14T15:00:00Z", "1", "1", "1", "1", "1"));

		List<DailyPrice> daily = DailyAggregator.aggregate(candles, KST, LocalDate.of(2026, 11, 15), null);

		assertThat(daily).extracting(DailyPrice::complete).containsExactly(true, false);
	}

	@Test
	void 전일_종가_대비_변동률을_계산한다() {
		List<HourlyCandle> candles = List.of(candle("2026-11-13T15:00:00Z", "100", "103", "99", "102.5", "1"),
				candle("2026-11-14T15:00:00Z", "102.5", "103", "100", "100.45", "1"));

		List<DailyPrice> daily = DailyAggregator.aggregate(candles, KST, LocalDate.of(2026, 11, 16),
				new BigDecimal("100"));

		assertThat(daily.get(0).changeRate()).isEqualByComparingTo("0.025"); // 100 -> 102.5
		assertThat(daily.get(1).changeRate()).isEqualByComparingTo("-0.02"); // 102.5 -> 100.45
	}

	@Test
	void 직전_종가가_없으면_첫날_변동률은_null() {
		List<HourlyCandle> candles = List.of(candle("2026-11-13T15:00:00Z", "100", "100", "100", "100", "1"));

		List<DailyPrice> daily = DailyAggregator.aggregate(candles, KST, LocalDate.of(2026, 11, 16), null);

		assertThat(daily.getFirst().changeRate()).isNull();
	}

	@Test
	void 입력_순서가_섞여도_시각_순으로_시가와_종가를_정한다() {
		List<HourlyCandle> candles = List.of(candle("2026-11-14T14:00:00Z", "107", "108", "106", "108", "1"),
				candle("2026-11-13T15:00:00Z", "100", "101", "99", "101", "1"));

		DailyPrice day = DailyAggregator.aggregate(candles, KST, LocalDate.of(2026, 11, 16), null).getFirst();

		assertThat(day.open()).isEqualByComparingTo("100");
		assertThat(day.close()).isEqualByComparingTo("108");
	}

}
