package com.ar2723.stockgraph.collector.upbit;

import com.ar2723.stockgraph.price.HourlyCandle;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * 업비트 시세 API(키 불필요) 클라이언트.
 * 60분봉은 한 번에 최대 200개, 최신 봉부터 내려온다. {@code to}(미포함) 를 옮겨 가며 과거로 페이지를 넘긴다.
 */
public class UpbitClient {

	private static final Logger log = LoggerFactory.getLogger(UpbitClient.class);

	static final int PAGE_SIZE = 200;

	/** 3년치 60분봉(약 26,300개)이 132페이지라 넉넉하게 잡은 안전 한도. */
	private static final int MAX_PAGES = 300;

	private static final int MAX_ATTEMPTS = 3;

	private static final DateTimeFormatter TO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
		.withZone(ZoneOffset.UTC);

	private static final ParameterizedTypeReference<List<UpbitCandleResponse>> CANDLE_LIST = new ParameterizedTypeReference<>() {
	};

	private final RestClient restClient;

	private final Duration requestInterval;

	public UpbitClient(RestClient restClient, Duration requestInterval) {
		this.restClient = restClient;
		this.requestInterval = requestInterval;
	}

	/**
	 * {@code from} 이상 {@code to} 미만에 시작한 60분봉을 시각 순으로 돌려준다.
	 * 거래가 없던 시간은 업비트가 봉을 만들지 않으므로 빠질 수 있다.
	 */
	public List<HourlyCandle> fetchHourly(String market, Instant from, Instant to) {
		List<HourlyCandle> result = new ArrayList<>();
		Instant cursor = to;
		for (int page = 0; page < MAX_PAGES; page++) {
			List<UpbitCandleResponse> body = requestPage(market, cursor);
			if (body.isEmpty()) {
				break;
			}
			Instant oldest = null;
			for (UpbitCandleResponse r : body) {
				HourlyCandle candle = r.toHourlyCandle();
				if (!candle.start().isBefore(from) && candle.start().isBefore(to)) {
					result.add(candle);
				}
				if (oldest == null || candle.start().isBefore(oldest)) {
					oldest = candle.start();
				}
			}
			if (!oldest.isAfter(from) || body.size() < PAGE_SIZE) {
				break;
			}
			cursor = oldest;
			pause();
		}
		result.sort(Comparator.comparing(HourlyCandle::start));
		return result;
	}

	private List<UpbitCandleResponse> requestPage(String market, Instant to) {
		for (int attempt = 1;; attempt++) {
			try {
				List<UpbitCandleResponse> body = restClient.get()
					.uri(b -> b.path("/v1/candles/minutes/60")
						.queryParam("market", market)
						.queryParam("count", PAGE_SIZE)
						.queryParam("to", TO_FORMAT.format(to))
						.build())
					.retrieve()
					.body(CANDLE_LIST);
				return body == null ? List.of() : body;
			}
			catch (HttpClientErrorException.TooManyRequests | HttpServerErrorException | ResourceAccessException e) {
				if (attempt >= MAX_ATTEMPTS) {
					throw e;
				}
				log.warn("업비트 요청 재시도 {}/{}: {}", attempt, MAX_ATTEMPTS, e.getMessage());
				sleep(Duration.ofSeconds(attempt));
			}
		}
	}

	private void pause() {
		if (!requestInterval.isZero()) {
			sleep(requestInterval);
		}
	}

	private static void sleep(Duration duration) {
		try {
			Thread.sleep(duration);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("업비트 수집이 중단됐다", e);
		}
	}

}
