package dev.pulsiris.checks.adapter.persistence;

import java.time.Instant;
import java.util.UUID;

import dev.pulsiris.checks.domain.CheckOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "checks", name = "check_result")
public class CheckResultEntity {

	// Hibernate reserves 50 ids per sequence call, so most inserts need no extra round trip.
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "check_result_seq")
	@SequenceGenerator(name = "check_result_seq", schema = "checks", sequenceName = "check_result_seq",
			allocationSize = 50)
	private Long id;

	private UUID targetId;

	private Instant checkedAt;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(columnDefinition = "checks.check_outcome")
	private CheckOutcome outcome;

	private Short httpStatus;

	private int latencyMs;

	private String errorMessage;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public UUID getTargetId() {
		return targetId;
	}

	public void setTargetId(UUID targetId) {
		this.targetId = targetId;
	}

	public Instant getCheckedAt() {
		return checkedAt;
	}

	public void setCheckedAt(Instant checkedAt) {
		this.checkedAt = checkedAt;
	}

	public CheckOutcome getOutcome() {
		return outcome;
	}

	public void setOutcome(CheckOutcome outcome) {
		this.outcome = outcome;
	}

	public Short getHttpStatus() {
		return httpStatus;
	}

	public void setHttpStatus(Short httpStatus) {
		this.httpStatus = httpStatus;
	}

	public int getLatencyMs() {
		return latencyMs;
	}

	public void setLatencyMs(int latencyMs) {
		this.latencyMs = latencyMs;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

}
