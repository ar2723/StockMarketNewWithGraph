package com.ar2723.stockgraph.collector;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 수집 스케줄. {@code stockgraph.collector.enabled=true} 일 때만 등록된다(테스트에서는 꺼짐).
 * fixedDelay 라 이전 실행(처음의 3년치 백필 포함)이 끝나야 다음 실행이 시작된다.
 */
@Component
@ConditionalOnBooleanProperty("stockgraph.collector.enabled")
public class CollectorScheduler {

	private final CollectorJobRunner jobRunner;

	public CollectorScheduler(CollectorJobRunner jobRunner) {
		this.jobRunner = jobRunner;
	}

	@Scheduled(initialDelayString = "${stockgraph.collector.initial-delay:10s}",
			fixedDelayString = "${stockgraph.collector.interval:1h}")
	public void collectUpbit() {
		jobRunner.runUpbit();
	}

}
