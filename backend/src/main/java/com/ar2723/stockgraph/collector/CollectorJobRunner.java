package com.ar2723.stockgraph.collector;

import com.ar2723.stockgraph.indicator.Indicator;
import com.ar2723.stockgraph.indicator.IndicatorRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 수집 작업 실행과 이력 기록. 지표 하나가 실패해도 나머지는 계속 수집한다.
 * 이력은 수집 트랜잭션과 따로 저장해서, 수집이 롤백돼도 FAILED 기록은 남는다.
 */
@Component
public class CollectorJobRunner {

	public static final String UPBIT_JOB = "upbit-hourly";

	private static final Logger log = LoggerFactory.getLogger(CollectorJobRunner.class);

	private final IndicatorRepository indicatorRepository;

	private final UpbitCollector upbitCollector;

	private final BatchJobRunRepository jobRunRepository;

	private final Clock clock;

	public CollectorJobRunner(IndicatorRepository indicatorRepository, UpbitCollector upbitCollector,
			BatchJobRunRepository jobRunRepository, Clock clock) {
		this.indicatorRepository = indicatorRepository;
		this.upbitCollector = upbitCollector;
		this.jobRunRepository = jobRunRepository;
		this.clock = clock;
	}

	public void runUpbit() {
		for (Indicator indicator : indicatorRepository.findByEnabledTrueAndSource(UpbitCollector.SOURCE)) {
			BatchJobRun run = jobRunRepository.save(BatchJobRun.start(UPBIT_JOB, indicator.getId(), clock.instant()));
			try {
				int items = upbitCollector.collect(indicator, clock.instant());
				run.succeed(items, clock.instant());
			}
			catch (RuntimeException e) {
				log.error("[{}] 수집 실패", indicator.getCode(), e);
				run.fail(e.getClass().getSimpleName() + ": " + e.getMessage(), clock.instant());
			}
			jobRunRepository.save(run);
		}
	}

}
