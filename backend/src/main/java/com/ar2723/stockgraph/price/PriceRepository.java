package com.ar2723.stockgraph.price;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * 가격 테이블 접근. 대량 upsert(ON CONFLICT) 가 필요해서 JPA 대신 JDBC 를 쓴다.
 * 같은 봉/날짜를 다시 저장하면 덮어쓰므로 수집을 다시 돌려도 안전하다.
 */
@Repository
public class PriceRepository {

	private static final String UPSERT_HOURLY = """
			INSERT INTO indicator_price_hourly
			    (indicator_id, candle_start, open_price, high_price, low_price, close_price, volume, collected_at)
			VALUES (?, ?, ?, ?, ?, ?, ?, now())
			ON CONFLICT (indicator_id, candle_start) DO UPDATE SET
			    open_price = EXCLUDED.open_price, high_price = EXCLUDED.high_price,
			    low_price = EXCLUDED.low_price, close_price = EXCLUDED.close_price,
			    volume = EXCLUDED.volume, collected_at = now()
			""";

	private static final String UPSERT_DAILY = """
			INSERT INTO indicator_price_daily
			    (indicator_id, trade_date, open_price, high_price, low_price, close_price, volume, change_rate,
			     complete, collected_at)
			VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())
			ON CONFLICT (indicator_id, trade_date) DO UPDATE SET
			    open_price = EXCLUDED.open_price, high_price = EXCLUDED.high_price,
			    low_price = EXCLUDED.low_price, close_price = EXCLUDED.close_price,
			    volume = EXCLUDED.volume, change_rate = EXCLUDED.change_rate,
			    complete = EXCLUDED.complete, collected_at = now()
			""";

	private static final String SELECT_DAILY = """
			SELECT trade_date, open_price, high_price, low_price, close_price, volume, change_rate, complete
			FROM indicator_price_daily
			""";

	private static final int BATCH_SIZE = 500;

	private final JdbcTemplate jdbcTemplate;

	private final JdbcClient jdbcClient;

	public PriceRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.jdbcClient = JdbcClient.create(jdbcTemplate);
	}

	public void upsertHourly(long indicatorId, List<HourlyCandle> candles) {
		jdbcTemplate.batchUpdate(UPSERT_HOURLY, candles, BATCH_SIZE, (ps, c) -> {
			ps.setLong(1, indicatorId);
			ps.setObject(2, utc(c.start()));
			ps.setBigDecimal(3, c.open());
			ps.setBigDecimal(4, c.high());
			ps.setBigDecimal(5, c.low());
			ps.setBigDecimal(6, c.close());
			ps.setBigDecimal(7, c.volume());
		});
	}

	public void upsertDaily(long indicatorId, List<DailyPrice> prices) {
		jdbcTemplate.batchUpdate(UPSERT_DAILY, prices, BATCH_SIZE, (ps, p) -> {
			ps.setLong(1, indicatorId);
			ps.setDate(2, Date.valueOf(p.tradeDate()));
			ps.setBigDecimal(3, p.open());
			ps.setBigDecimal(4, p.high());
			ps.setBigDecimal(5, p.low());
			ps.setBigDecimal(6, p.close());
			ps.setBigDecimal(7, p.volume());
			ps.setBigDecimal(8, p.changeRate());
			ps.setBoolean(9, p.complete());
		});
	}

	public Optional<Instant> findLatestHourlyStart(long indicatorId) {
		return jdbcClient.sql("SELECT max(candle_start) FROM indicator_price_hourly WHERE indicator_id = ?")
			.param(indicatorId)
			.query((rs, i) -> rs.getObject(1, OffsetDateTime.class))
			.optional()
			.map(OffsetDateTime::toInstant);
	}

	/** [from, to) 구간의 시간봉을 시각 순으로. */
	public List<HourlyCandle> findHourly(long indicatorId, Instant from, Instant to) {
		return jdbcClient.sql("""
				SELECT candle_start, open_price, high_price, low_price, close_price, volume
				FROM indicator_price_hourly
				WHERE indicator_id = ? AND candle_start >= ? AND candle_start < ?
				ORDER BY candle_start
				""")
			.params(indicatorId, utc(from), utc(to))
			.query((rs, i) -> new HourlyCandle(rs.getObject("candle_start", OffsetDateTime.class).toInstant(),
					rs.getBigDecimal("open_price"), rs.getBigDecimal("high_price"), rs.getBigDecimal("low_price"),
					rs.getBigDecimal("close_price"), rs.getBigDecimal("volume")))
			.list();
	}

	public Optional<BigDecimal> findDailyClose(long indicatorId, LocalDate date) {
		return jdbcClient
			.sql("SELECT close_price FROM indicator_price_daily WHERE indicator_id = ? AND trade_date = ?")
			.params(indicatorId, Date.valueOf(date))
			.query(BigDecimal.class)
			.optional();
	}

	/** [from, to] 구간의 일봉을 날짜 순으로. */
	public List<DailyPrice> findDaily(long indicatorId, LocalDate from, LocalDate to) {
		return jdbcClient.sql(SELECT_DAILY + " WHERE indicator_id = ? AND trade_date BETWEEN ? AND ? ORDER BY trade_date")
			.params(indicatorId, Date.valueOf(from), Date.valueOf(to))
			.query((rs, i) -> daily(rs))
			.list();
	}

	public Optional<DailyPrice> findLatestDaily(long indicatorId) {
		return jdbcClient.sql(SELECT_DAILY + " WHERE indicator_id = ? ORDER BY trade_date DESC LIMIT 1")
			.param(indicatorId)
			.query((rs, i) -> daily(rs))
			.optional();
	}

	private static DailyPrice daily(ResultSet rs) throws SQLException {
		return new DailyPrice(rs.getDate("trade_date").toLocalDate(), rs.getBigDecimal("open_price"),
				rs.getBigDecimal("high_price"), rs.getBigDecimal("low_price"), rs.getBigDecimal("close_price"),
				rs.getBigDecimal("volume"), rs.getBigDecimal("change_rate"), rs.getBoolean("complete"));
	}

	private static OffsetDateTime utc(Instant instant) {
		return instant.atOffset(ZoneOffset.UTC);
	}

}
