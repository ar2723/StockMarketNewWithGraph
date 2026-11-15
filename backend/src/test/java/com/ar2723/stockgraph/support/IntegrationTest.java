package com.ar2723.stockgraph.support;

import com.ar2723.stockgraph.TestcontainersConfiguration;
import com.ar2723.stockgraph.collector.upbit.UpbitClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 통합 테스트 공통 설정: 실제 Postgres/Redis(Testcontainers) + Flyway, 외부 API(업비트)는 목.
 * 설정을 한 곳에 모아서 테스트 클래스들이 같은 스프링 컨텍스트(컨테이너 한 벌)를 같이 쓴다.
 * 수집기는 자체 트랜잭션으로 커밋하므로 롤백 대신 매 테스트 전에 가격/이력 테이블을 비운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

	@MockitoBean
	protected UpbitClient upbitClient;

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanTables() {
		jdbcTemplate.execute("TRUNCATE indicator_price_hourly, indicator_price_daily, batch_job_run");
	}

}
