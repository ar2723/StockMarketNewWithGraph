package com.ar2723.stockgraph.collector;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 수집기 설정 ({@code stockgraph.collector.*}).
 *
 * @param enabled 스케줄 실행 여부. 테스트에서는 꺼 둔다
 * @param initialDelay 앱 기동 후 첫 실행까지 대기
 * @param interval 실행 간격 (이전 실행이 끝난 뒤부터)
 * @param backfillYears 데이터가 없을 때 처음 채울 기간(년)
 * @param upbitBaseUrl 업비트 API 주소
 * @param upbitRequestInterval 업비트 페이지 요청 사이 대기 (요청 수 제한 대비)
 */
@ConfigurationProperties("stockgraph.collector")
public record CollectorProperties(@DefaultValue("false") boolean enabled, @DefaultValue("10s") Duration initialDelay,
		@DefaultValue("1h") Duration interval, @DefaultValue("3") int backfillYears,
		@DefaultValue("https://api.upbit.com") String upbitBaseUrl,
		@DefaultValue("150ms") Duration upbitRequestInterval) {
}
