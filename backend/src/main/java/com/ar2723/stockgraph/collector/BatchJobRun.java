package com.ar2723.stockgraph.collector;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** 수집 배치 실행 이력. 관리자 화면(A-04)에서 최근 실행 결과와 실패 원인을 보여준다. */
@Entity
@Table(name = "batch_job_run")
public class BatchJobRun {

	public enum Status {
		RUNNING, SUCCESS, FAILED
	}

	private static final int MAX_MESSAGE = 2000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "job_name", nullable = false, length = 50)
	private String jobName;

	@Column(name = "indicator_id")
	private Long indicatorId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status;

	@Column(name = "items_processed", nullable = false)
	private int itemsProcessed;

	@Column(columnDefinition = "text")
	private String message;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	protected BatchJobRun() {
	}

	public static BatchJobRun start(String jobName, Long indicatorId, Instant now) {
		BatchJobRun run = new BatchJobRun();
		run.jobName = jobName;
		run.indicatorId = indicatorId;
		run.status = Status.RUNNING;
		run.startedAt = now;
		return run;
	}

	public void succeed(int items, Instant now) {
		this.status = Status.SUCCESS;
		this.itemsProcessed = items;
		this.finishedAt = now;
	}

	public void fail(String reason, Instant now) {
		this.status = Status.FAILED;
		this.message = reason != null && reason.length() > MAX_MESSAGE ? reason.substring(0, MAX_MESSAGE) : reason;
		this.finishedAt = now;
	}

	public Long getId() {
		return id;
	}

	public String getJobName() {
		return jobName;
	}

	public Long getIndicatorId() {
		return indicatorId;
	}

	public Status getStatus() {
		return status;
	}

	public int getItemsProcessed() {
		return itemsProcessed;
	}

	public String getMessage() {
		return message;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getFinishedAt() {
		return finishedAt;
	}

}
