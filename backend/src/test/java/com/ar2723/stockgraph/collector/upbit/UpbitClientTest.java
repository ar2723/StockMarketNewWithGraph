package com.ar2723.stockgraph.collector.upbit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;

import com.ar2723.stockgraph.price.HourlyCandle;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class UpbitClientTest {

	private static final Instant BASE = Instant.parse("2026-11-01T00:00:00Z");

	private static final DateTimeFormatter UPBIT_UTC = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
		.withZone(ZoneOffset.UTC);

	private MockRestServiceServer server;

	private UpbitClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.upbit.com");
		server = MockRestServiceServer.bindTo(builder).build();
		client = new UpbitClient(builder.build(), Duration.ZERO);
	}

	/** BASE 기준 hour 시간 뒤에 시작하는 봉들을 업비트처럼 최신순 JSON 배열로. */
	private static String candlesJson(int newestHour, int oldestHour) {
		return IntStream.iterate(newestHour, h -> h >= oldestHour, h -> h - 1)
			.mapToObj(h -> """
					{"market":"KRW-BTC","candle_date_time_utc":"%s","candle_date_time_kst":"x",
					 "opening_price":%d,"high_price":%d,"low_price":%d,"trade_price":%d,
					 "candle_acc_trade_volume":1.5,"unit":60}""".formatted(
					UPBIT_UTC.format(BASE.plus(h, ChronoUnit.HOURS)), h, h + 1, h, h))
			.collect(Collectors.joining(",", "[", "]"));
	}

	private static Instant hour(int h) {
		return BASE.plus(h, ChronoUnit.HOURS);
	}

	@Test
	void 과거로_페이지를_넘기며_from_이후_봉만_시각_순으로_돌려준다() {
		server.expect(requestTo(Matchers.startsWith("https://api.upbit.com/v1/candles/minutes/60")))
			.andExpect(queryParam("market", "KRW-BTC"))
			.andExpect(queryParam("to", "2026-11-11T10:00:00Z")) // hour(250)
			.andRespond(withSuccess(candlesJson(249, 50), MediaType.APPLICATION_JSON));
		server.expect(requestTo(Matchers.startsWith("https://api.upbit.com/v1/candles/minutes/60")))
			.andExpect(queryParam("to", "2026-11-03T02:00:00Z")) // hour(50): 앞 페이지의 가장 오래된 봉
			.andRespond(withSuccess(candlesJson(49, -150), MediaType.APPLICATION_JSON));

		List<HourlyCandle> result = client.fetchHourly("KRW-BTC", hour(0), hour(250));

		server.verify(); // 두 번째 페이지에서 from 을 지났으니 세 번째 요청은 없어야 한다
		assertThat(result).hasSize(250);
		assertThat(result.getFirst().start()).isEqualTo(hour(0));
		assertThat(result.getLast().start()).isEqualTo(hour(249));
		assertThat(result.getLast().high()).isEqualByComparingTo("250");
		assertThat(result.getLast().volume()).isEqualByComparingTo("1.5");
	}

	@Test
	void 한_페이지가_200개보다_적으면_더_요청하지_않는다() {
		server.expect(requestTo(Matchers.startsWith("https://api.upbit.com/v1/candles/minutes/60")))
			.andRespond(withSuccess(candlesJson(9, 0), MediaType.APPLICATION_JSON));

		List<HourlyCandle> result = client.fetchHourly("KRW-BTC", hour(-100), hour(10));

		server.verify();
		assertThat(result).hasSize(10);
	}

	@Test
	void 요청_수_제한_429_는_재시도한다() {
		server.expect(requestTo(Matchers.startsWith("https://api.upbit.com/v1/candles/minutes/60")))
			.andRespond(withTooManyRequests());
		server.expect(requestTo(Matchers.startsWith("https://api.upbit.com/v1/candles/minutes/60")))
			.andRespond(withSuccess(candlesJson(2, 0), MediaType.APPLICATION_JSON));

		List<HourlyCandle> result = client.fetchHourly("KRW-BTC", hour(0), hour(3));

		server.verify();
		assertThat(result).hasSize(3);
	}

}
