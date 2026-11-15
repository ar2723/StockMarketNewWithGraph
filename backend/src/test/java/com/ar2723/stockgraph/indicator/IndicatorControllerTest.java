package com.ar2723.stockgraph.indicator;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ar2723.stockgraph.price.DailyPrice;
import com.ar2723.stockgraph.price.PriceRepository;
import com.ar2723.stockgraph.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class IndicatorControllerTest extends IntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	IndicatorRepository indicatorRepository;

	@Autowired
	PriceRepository priceRepository;

	@BeforeEach
	void setUp() {
		long btc = indicatorRepository.findByCode("BTC").orElseThrow().getId();
		// BTC 급변 기준은 4% (V3): 11-12 -4.3% 는 급변, 11-14 +2.5% 는 급변 아님
		priceRepository.upsertDaily(btc, List.of(daily(LocalDate.of(2026, 11, 12), "104.5", null, true),
				daily(LocalDate.of(2026, 11, 13), "100", "-0.043062", true),
				daily(LocalDate.of(2026, 11, 14), "102.5", "0.025", true),
				daily(LocalDate.of(2026, 11, 15), "102", "-0.004878", false)));
	}

	private static DailyPrice daily(LocalDate date, String close, String rate, boolean complete) {
		BigDecimal c = new BigDecimal(close);
		return new DailyPrice(date, c, c, c, c, BigDecimal.ONE, rate == null ? null : new BigDecimal(rate), complete);
	}

	@Test
	void 지표_목록은_표시_순서대로_최근_일봉과_함께_내려준다() throws Exception {
		mockMvc.perform(get("/api/indicators"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(5)))
			.andExpect(jsonPath("$[0].code").value("KOSPI"))
			.andExpect(jsonPath("$[0].latestClose").doesNotExist())
			.andExpect(jsonPath("$[4].code").value("BTC"))
			.andExpect(jsonPath("$[4].latestDate").value("2026-11-15"))
			.andExpect(jsonPath("$[4].latestClose").value(102.0))
			.andExpect(jsonPath("$[4].spikeThresholdPct").value(4.0))
			.andExpect(jsonPath("$[4].spike").value(false))
			.andExpect(jsonPath("$[0].spikeThresholdPct").value(2.0));
	}

	@Test
	void 일봉은_기간을_지정해_조회하고_지표별_기준으로_급변일을_표시한다() throws Exception {
		mockMvc.perform(get("/api/indicators/btc/daily").param("from", "2026-11-13").param("to", "2026-11-15"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(3)))
			.andExpect(jsonPath("$[0].tradeDate").value("2026-11-13"))
			.andExpect(jsonPath("$[0].spike").value(true))
			.andExpect(jsonPath("$[1].changeRate").value(0.025))
			.andExpect(jsonPath("$[1].spike").value(false))
			.andExpect(jsonPath("$[2].complete").value(false));
	}

	@Test
	void 없는_지표는_404() throws Exception {
		mockMvc.perform(get("/api/indicators/NOPE/daily")).andExpect(status().isNotFound());
	}

	@Test
	void 조회_기간이_3년을_넘거나_거꾸로면_400() throws Exception {
		mockMvc.perform(get("/api/indicators/BTC/daily").param("from", "2020-01-01").param("to", "2026-11-15"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/indicators/BTC/daily").param("from", "2026-11-15").param("to", "2026-11-01"))
			.andExpect(status().isBadRequest());
	}

}
