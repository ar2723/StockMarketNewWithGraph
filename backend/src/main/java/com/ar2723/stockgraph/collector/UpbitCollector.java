package com.ar2723.stockgraph.collector;

import com.ar2723.stockgraph.collector.upbit.UpbitClient;
import com.ar2723.stockgraph.indicator.Indicator;
import com.ar2723.stockgraph.price.DailyAggregator;
import com.ar2723.stockgraph.price.DailyPrice;
import com.ar2723.stockgraph.price.HourlyCandle;
import com.ar2723.stockgraph.price.PriceRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 업비트 지표(BTC) 수집: 60분봉을 받아 저장하고, 영향받은 날짜의 KST 일봉을 다시 집계한다.
 * <ul>
 * <li>처음 실행: KST 기준 {@code backfillYears}년 전 00시부터 지금까지 채운다.</li>
 * <li>이후 실행: 마지막으로 저장한 봉부터 다시 받는다(진행 중이던 봉을 확정값으로 덮어쓰기 위해).</li>
 * </ul>
 */
@Service
public class UpbitCollector {

	public static final String SOURCE = "UPBIT";

	/** BTC 하루 경계는 KST 00시 (project.md 3장). */
	public static final ZoneId DAY_ZONE = ZoneId.of("Asia/Seoul");

	private static final Logger log = LoggerFactory.getLogger(UpbitCollector.class);

	private final UpbitClient upbitClient;

	private final PriceRepository priceRepository;

	private final TransactionTemplate transactionTemplate;

	private final int backfillYears;

	public UpbitCollector(UpbitClient upbitClient, PriceRepository priceRepository, CollectorProperties properties,
			PlatformTransactionManager transactionManager) {
		this.upbitClient = upbitClient;
		this.priceRepository = priceRepository;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.backfillYears = properties.backfillYears();
	}

	/**
	 * 외부 API 호출(백필이면 수십 초)은 트랜잭션 밖에서 하고, 저장과 재집계만 한 트랜잭션으로 묶는다.
	 * @return 받은 시간봉 수
	 */
	public int collect(Indicator indicator, Instant now) {
		long id = indicator.getId();
		Instant from = priceRepository.findLatestHourlyStart(id).orElseGet(() -> backfillStart(now));

		List<HourlyCandle> candles = upbitClient.fetchHourly(indicator.getSourceSymbol(), from, now);
		if (candles.isEmpty()) {
			log.info("[{}] 새 시간봉 없음 (from={})", indicator.getCode(), from);
			return 0;
		}
		LocalDate firstDay = candles.getFirst().start().atZone(DAY_ZONE).toLocalDate();
		List<DailyPrice> daily = transactionTemplate.execute(status -> {
			priceRepository.upsertHourly(id, candles);
			List<DailyPrice> recomputed = reaggregate(id, firstDay, now);
			priceRepository.upsertDaily(id, recomputed);
			return recomputed;
		});
		log.info("[{}] 시간봉 {}개 저장, 일봉 {}개 재집계 ({} ~)", indicator.getCode(), candles.size(), daily.size(),
				firstDay);
		return candles.size();
	}

	/** firstDay 00시(KST)부터 지금까지 저장된 시간봉으로 일봉을 다시 만든다. */
	private List<DailyPrice> reaggregate(long id, LocalDate firstDay, Instant now) {
		Instant dayStart = firstDay.atStartOfDay(DAY_ZONE).toInstant();
		List<HourlyCandle> stored = priceRepository.findHourly(id, dayStart, now.plusSeconds(1));
		BigDecimal previousClose = priceRepository.findDailyClose(id, firstDay.minusDays(1)).orElse(null);
		LocalDate today = now.atZone(DAY_ZONE).toLocalDate();
		return DailyAggregator.aggregate(stored, DAY_ZONE, today, previousClose);
	}

	private Instant backfillStart(Instant now) {
		return now.atZone(DAY_ZONE).toLocalDate().minusYears(backfillYears).atStartOfDay(DAY_ZONE).toInstant();
	}

}
