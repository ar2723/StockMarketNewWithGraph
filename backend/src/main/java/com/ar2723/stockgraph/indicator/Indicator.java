package com.ar2723.stockgraph.indicator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/** 금융 지표 메타 정보. 수집 on/off 와 급변 기준은 관리자가 바꾼다. */
@Entity
@Table(name = "indicator")
public class Indicator {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 20)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private IndicatorCategory category;

	@Column(nullable = false, length = 10)
	private String currency;

	@Column(length = 30)
	private String source;

	@Column(name = "source_symbol", length = 50)
	private String sourceSymbol;

	@Column(nullable = false)
	private boolean enabled;

	@Column(name = "spike_threshold_pct", nullable = false, precision = 5, scale = 2)
	private BigDecimal spikeThresholdPct;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false)
	private Instant updatedAt;

	protected Indicator() {
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public IndicatorCategory getCategory() {
		return category;
	}

	public String getCurrency() {
		return currency;
	}

	public String getSource() {
		return source;
	}

	public String getSourceSymbol() {
		return sourceSymbol;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public BigDecimal getSpikeThresholdPct() {
		return spikeThresholdPct;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	/** 전일 대비 변동률(비율)이 이 지표의 급변 기준(%) 이상인지. */
	public boolean isSpike(BigDecimal changeRate) {
		if (changeRate == null) {
			return false;
		}
		BigDecimal pct = changeRate.abs().multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP);
		return pct.compareTo(spikeThresholdPct) >= 0;
	}

}
