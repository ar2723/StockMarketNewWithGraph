package com.ar2723.stockgraph.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ar2723.stockgraph.indicator.Indicator;
import com.ar2723.stockgraph.indicator.IndicatorRepository;
import com.ar2723.stockgraph.price.DailyPrice;
import com.ar2723.stockgraph.price.HourlyCandle;
import com.ar2723.stockgraph.price.PriceRepository;
import com.ar2723.stockgraph.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.ResourceAccessException;

class UpbitCollectorIntegrationTest extends IntegrationTest {

	/** 2026-11-13 00:00 KST */
	private static final Instant NOV13_KST = Instant.parse("2026-11-12T15:00:00Z");

	@Autowired
	UpbitCollector collector;

	@Autowired
	CollectorJobRunner jobRunner;

	@Autowired
	IndicatorRepository indicatorRepository;

	@Autowired
	PriceRepository priceRepository;

	@Autowired
	BatchJobRunRepository jobRunRepository;

	private Indicator btc;

	@BeforeEach
	void setUp() {
		btc = indicatorRepository.findByCode("BTC").orElseThrow(); // V2 시드 데이터
	}

	/** NOV13_KST 부터 hours 개의 시간봉. 날짜(KST)별 종가가 13일 100, 14일 103, 15일 101 이 되도록 만든다. */
	private static List<HourlyCandle> candles(int hours) {
		List<HourlyCandle> list = new ArrayList<>();
		for (int h = 0; h < hours; h++) {
			BigDecimal price = BigDecimal.valueOf(h < 24 ? 100 : h < 48 ? 103 : 101);
			list.add(new HourlyCandle(NOV13_KST.plus(h, ChronoUnit.HOURS), price, price, price, price, BigDecimal.ONE));
		}
		return list;
	}

	@Test
	void 첫_수집은_3년치를_요청하고_KST_일봉을_만든다() {
		Instant now = Instant.parse("2026-11-15T03:30:00Z"); // 11-15 12:30 KST
		given(upbitClient.fetchHourly(eq("KRW-BTC"), any(), any())).willReturn(candles(61)); // 15일 12시 봉까지

		int count = collector.collect(btc, now);

		assertThat(count).isEqualTo(61);
		verify(upbitClient).fetchHourly("KRW-BTC", Instant.parse("2023-11-14T15:00:00Z"), now); // 2023-11-15 00시 KST
		List<DailyPrice> daily = priceRepository.findDaily(btc.getId(), LocalDate.of(2026, 11, 1),
				LocalDate.of(2026, 11, 30));
		assertThat(daily).extracting(DailyPrice::tradeDate)
			.containsExactly(LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 15));
		assertThat(daily.get(0).changeRate()).isNull();
		assertThat(daily.get(1).changeRate()).isEqualByComparingTo("0.03");
		assertThat(daily.get(2).complete()).isFalse();
	}

	@Test
	void 다음_수집은_마지막_봉부터_받아_오늘_일봉만_갱신한다() {
		given(upbitClient.fetchHourly(eq("KRW-BTC"), any(), any())).willReturn(candles(61));
		collector.collect(btc, Instant.parse("2026-11-15T03:30:00Z"));

		Instant lastStored = NOV13_KST.plus(60, ChronoUnit.HOURS); // 15일 12:00 KST 봉
		Instant now = Instant.parse("2026-11-15T04:30:00Z");
		BigDecimal p = new BigDecimal("106.09");
		given(upbitClient.fetchHourly("KRW-BTC", lastStored, now)).willReturn(List.of(
				new HourlyCandle(lastStored, new BigDecimal("101"), p, new BigDecimal("101"), new BigDecimal("102"),
						BigDecimal.TEN),
				new HourlyCandle(lastStored.plus(1, ChronoUnit.HOURS), p, p, p, p, BigDecimal.ONE)));

		collector.collect(btc, now);

		verify(upbitClient).fetchHourly("KRW-BTC", lastStored, now);
		List<DailyPrice> daily = priceRepository.findDaily(btc.getId(), LocalDate.of(2026, 11, 1),
				LocalDate.of(2026, 11, 30));
		assertThat(daily).hasSize(3);
		DailyPrice today = daily.get(2);
		assertThat(today.close()).isEqualByComparingTo("106.09");
		assertThat(today.high()).isEqualByComparingTo("106.09");
		assertThat(today.changeRate()).isEqualByComparingTo("0.03"); // 103 -> 106.09
		assertThat(today.complete()).isFalse();
		assertThat(daily.get(1).close()).isEqualByComparingTo("103"); // 어제는 그대로
	}

	@Test
	void 같은_데이터를_다시_받아도_중복_저장하지_않는다() {
		given(upbitClient.fetchHourly(eq("KRW-BTC"), any(), any())).willReturn(candles(30));
		Instant now = Instant.parse("2026-11-15T03:30:00Z");

		collector.collect(btc, now);
		collector.collect(btc, now);

		Integer hourly = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM indicator_price_hourly WHERE indicator_id = ?", Integer.class, btc.getId());
		assertThat(hourly).isEqualTo(30);
	}

	@Test
	void 수집이_실패하면_실행_이력에_FAILED_와_원인을_남긴다() {
		given(upbitClient.fetchHourly(eq("KRW-BTC"), any(), any()))
			.willThrow(new ResourceAccessException("connect timed out"));

		jobRunner.runUpbit();

		List<BatchJobRun> runs = jobRunRepository.findTop20ByJobNameOrderByStartedAtDesc(CollectorJobRunner.UPBIT_JOB);
		assertThat(runs).hasSize(1);
		assertThat(runs.getFirst().getStatus()).isEqualTo(BatchJobRun.Status.FAILED);
		assertThat(runs.getFirst().getMessage()).contains("connect timed out");
		assertThat(runs.getFirst().getFinishedAt()).isNotNull();
	}

	@Test
	void 수집이_성공하면_처리_건수를_기록한다() {
		given(upbitClient.fetchHourly(eq("KRW-BTC"), any(), any())).willReturn(candles(5));

		jobRunner.runUpbit();

		BatchJobRun run = jobRunRepository.findTop20ByJobNameOrderByStartedAtDesc(CollectorJobRunner.UPBIT_JOB)
			.getFirst();
		assertThat(run.getStatus()).isEqualTo(BatchJobRun.Status.SUCCESS);
		assertThat(run.getItemsProcessed()).isEqualTo(5);
	}

}
