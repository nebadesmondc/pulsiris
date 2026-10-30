package dev.pulsiris.incidents.adapter.persistence;

import java.time.Instant;
import java.util.UUID;

import dev.pulsiris.incidents.domain.IncidentStatus;
import dev.pulsiris.incidents.domain.Resolution;
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
@Table(schema = "incidents", name = "incident")
public class IncidentEntity {

	@Id
	private UUID id;

	private UUID targetId;

	private String targetName;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(columnDefinition = "incidents.incident_status")
	private IncidentStatus status;

	private Instant startedAt;

	private Instant openedAt;

	private Instant acknowledgedAt;

	private Instant resolvedAt;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(columnDefinition = "incidents.incident_resolution")
	private Resolution resolution;

	private String lastError;

	@Version
	private Long version;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getTargetId() {
		return targetId;
	}

	public void setTargetId(UUID targetId) {
		this.targetId = targetId;
	}

	public String getTargetName() {
		return targetName;
	}

	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	public IncidentStatus getStatus() {
		return status;
	}

	public void setStatus(IncidentStatus status) {
		this.status = status;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(Instant startedAt) {
		this.startedAt = startedAt;
	}

	public Instant getOpenedAt() {
		return openedAt;
	}

	public void setOpenedAt(Instant openedAt) {
		this.openedAt = openedAt;
	}

	public Instant getAcknowledgedAt() {
		return acknowledgedAt;
	}

	public void setAcknowledgedAt(Instant acknowledgedAt) {
		this.acknowledgedAt = acknowledgedAt;
	}

	public Instant getResolvedAt() {
		return resolvedAt;
	}

	public void setResolvedAt(Instant resolvedAt) {
		this.resolvedAt = resolvedAt;
	}

	public Resolution getResolution() {
		return resolution;
	}

	public void setResolution(Resolution resolution) {
		this.resolution = resolution;
	}

	public String getLastError() {
		return lastError;
	}

	public void setLastError(String lastError) {
		this.lastError = lastError;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

}
