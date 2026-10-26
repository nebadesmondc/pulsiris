package dev.pulsiris.checks.adapter.persistence;

import java.time.Instant;
import java.util.UUID;

import dev.pulsiris.checks.domain.HealthStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "checks", name = "target_health_state")
public class HealthStateEntity {

	@Id
	private UUID targetId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(columnDefinition = "checks.health_status")
	private HealthStatus status;

	private int consecutiveFailures;

	private int consecutiveSuccesses;

	private Instant lastCheckedAt;

	private Instant failingSince;

	private Instant lastStatusChangeAt;

	@Version
	private Long version;

	public UUID getTargetId() {
		return targetId;
	}

	public void setTargetId(UUID targetId) {
		this.targetId = targetId;
	}

	public HealthStatus getStatus() {
		return status;
	}

	public void setStatus(HealthStatus status) {
		this.status = status;
	}

	public int getConsecutiveFailures() {
		return consecutiveFailures;
	}

	public void setConsecutiveFailures(int consecutiveFailures) {
		this.consecutiveFailures = consecutiveFailures;
	}

	public int getConsecutiveSuccesses() {
		return consecutiveSuccesses;
	}

	public void setConsecutiveSuccesses(int consecutiveSuccesses) {
		this.consecutiveSuccesses = consecutiveSuccesses;
	}

	public Instant getLastCheckedAt() {
		return lastCheckedAt;
	}

	public void setLastCheckedAt(Instant lastCheckedAt) {
		this.lastCheckedAt = lastCheckedAt;
	}

	public Instant getFailingSince() {
		return failingSince;
	}

	public void setFailingSince(Instant failingSince) {
		this.failingSince = failingSince;
	}

	public Instant getLastStatusChangeAt() {
		return lastStatusChangeAt;
	}

	public void setLastStatusChangeAt(Instant lastStatusChangeAt) {
		this.lastStatusChangeAt = lastStatusChangeAt;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

}
