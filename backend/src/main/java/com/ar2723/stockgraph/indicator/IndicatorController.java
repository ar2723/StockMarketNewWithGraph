package com.ar2723.stockgraph.indicator;

import com.ar2723.stockgraph.price.DailyPrice;
import com.ar2723.stockgraph.price.PriceRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** 공개 조회 API. 캐싱(Redis)은 Sprint 2 에서 붙인다. */
@RestController
@RequestMapping("/api/indicators")
public class IndicatorController {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	/** 기간을 지정하지 않으면 최근 1년. 최대 조회 범위는 보관 기간(3년)보다 조금 넉넉하게. */
	private static final int DEFAULT_YEARS = 1;

	private static final int MAX_DAYS = 3 * 366 + 7;

	private final IndicatorRepository indicatorRepository;

	private final PriceRepository priceRepository;

	private final Clock clock;

	public IndicatorController(IndicatorRepository indicatorRepository, PriceRepository priceRepository, Clock clock) {
		this.indicatorRepository = indicatorRepository;
		this.priceRepository = priceRepository;
		this.clock = clock;
	}

	@GetMapping
	public List<IndicatorSummary> list() {
		return indicatorRepository.findAllByOrderByDisplayOrderAsc()
			.stream()
			.map(i -> IndicatorSummary.of(i, priceRepository.findLatestDaily(i.getId()).orElse(null)))
			.toList();
	}

	@GetMapping("/{code}/daily")
	public List<DailyPriceResponse> daily(@PathVariable String code,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		Indicator indicator = indicatorRepository.findByCode(code.toUpperCase())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "지표를 찾을 수 없다: " + code));
		LocalDate end = to != null ? to : LocalDate.now(clock.withZone(KST));
		LocalDate start = from != null ? from : end.minusYears(DEFAULT_YEARS);
		if (start.isAfter(end) || start.plusDays(MAX_DAYS).isBefore(end)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "조회 기간이 잘못됐다 (최대 3년)");
		}
		return priceRepository.findDaily(indicator.getId(), start, end)
			.stream()
			.map(p -> DailyPriceResponse.of(p, indicator.isSpike(p.changeRate())))
			.toList();
	}

	/**
	 * 지표 목록 한 줄.
	 *
	 * @param spikeThresholdPct 이 지표의 급변 기준(%). BTC 4, 나머지 기본 2
	 * @param spike 최근 일봉의 변동이 급변 기준 이상인지
	 */
	public record IndicatorSummary(String code, String name, IndicatorCategory category, String currency,
			boolean enabled, BigDecimal spikeThresholdPct, LocalDate latestDate, BigDecimal latestClose,
			BigDecimal changeRate, boolean spike) {

		static IndicatorSummary of(Indicator i, DailyPrice latest) {
			BigDecimal rate = latest == null ? null : latest.changeRate();
			return new IndicatorSummary(i.getCode(), i.getName(), i.getCategory(), i.getCurrency(), i.isEnabled(),
					i.getSpikeThresholdPct(), latest == null ? null : latest.tradeDate(),
					latest == null ? null : latest.close(), rate, i.isSpike(rate));
		}

	}

	/** 차트용 일봉 한 개. {@code spike} 는 지표별 급변 기준으로 서버가 판정한다(차트 마커). */
	public record DailyPriceResponse(LocalDate tradeDate, BigDecimal open, BigDecimal high, BigDecimal low,
			BigDecimal close, BigDecimal volume, BigDecimal changeRate, boolean complete, boolean spike) {

		static DailyPriceResponse of(DailyPrice p, boolean spike) {
			return new DailyPriceResponse(p.tradeDate(), p.open(), p.high(), p.low(), p.close(), p.volume(),
					p.changeRate(), p.complete(), spike);
		}

	}

}
