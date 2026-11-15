package com.ar2723.stockgraph.price;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 시간봉을 지정한 시간대의 자정 기준 일봉으로 묶는다.
 * 업비트 일봉은 UTC 00시(KST 09시) 경계라서, BTC 의 KST 00시 기준 일봉은 이 집계로 만든다.
 */
public final class DailyAggregator {

	private static final int RATE_SCALE = 6;

	private DailyAggregator() {
	}

	/**
	 * @param candles 집계할 시간봉. 첫 날은 그날 00시부터 들어 있어야 시가가 맞다
	 * @param zone 하루를 나누는 기준 시간대
	 * @param today 기준 시간대의 오늘 날짜. 이 날짜와 그 이후는 미완성(complete=false)
	 * @param previousClose 첫 날 직전 날의 종가. 없으면 null (첫 날 changeRate 도 null)
	 */
	public static List<DailyPrice> aggregate(List<HourlyCandle> candles, ZoneId zone, LocalDate today,
			BigDecimal previousClose) {
		Map<LocalDate, List<HourlyCandle>> byDate = new TreeMap<>();
		candles.stream()
			.sorted(Comparator.comparing(HourlyCandle::start))
			.forEach(c -> byDate.computeIfAbsent(c.start().atZone(zone).toLocalDate(), d -> new ArrayList<>()).add(c));

		List<DailyPrice> result = new ArrayList<>(byDate.size());
		BigDecimal prevClose = previousClose;
		for (Map.Entry<LocalDate, List<HourlyCandle>> entry : byDate.entrySet()) {
			List<HourlyCandle> day = entry.getValue();
			BigDecimal open = day.getFirst().open();
			BigDecimal close = day.getLast().close();
			BigDecimal high = day.stream().map(HourlyCandle::high).reduce(BigDecimal::max).orElseThrow();
			BigDecimal low = day.stream().map(HourlyCandle::low).reduce(BigDecimal::min).orElseThrow();
			BigDecimal volume = day.stream()
				.map(HourlyCandle::volume)
				.filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
			result.add(new DailyPrice(entry.getKey(), open, high, low, close, volume, changeRate(prevClose, close),
					entry.getKey().isBefore(today)));
			prevClose = close;
		}
		return result;
	}

	static BigDecimal changeRate(BigDecimal previousClose, BigDecimal close) {
		if (previousClose == null || previousClose.signum() == 0) {
			return null;
		}
		return close.subtract(previousClose).divide(previousClose, RATE_SCALE, RoundingMode.HALF_UP);
	}

}
