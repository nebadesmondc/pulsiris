package dev.pulsiris.registry.adapter.persistence;

import java.time.Instant;
import java.util.UUID;

import dev.pulsiris.registry.domain.HttpMethod;
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
@Table(schema = "registry", name = "monitored_target")
public class TargetEntity {

	@Id
	private UUID id;

	private String name;

	private String endpointUrl;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(columnDefinition = "registry.http_method")
	private HttpMethod httpMethod;

	private Short expectedStatus;

	private int intervalSeconds;

	private int timeoutMs;

	private short failureThreshold;

	private short recoveryThreshold;

	private boolean enabled;

	private Instant createdAt;

	private Instant updatedAt;

	// Optimistic locking; null marks a new entity so Spring Data inserts instead of merging.
	@Version
	private Long version;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEndpointUrl() {
		return endpointUrl;
	}

	public void setEndpointUrl(String endpointUrl) {
		this.endpointUrl = endpointUrl;
	}

	public HttpMethod getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}

	public Short getExpectedStatus() {
		return expectedStatus;
	}

	public void setExpectedStatus(Short expectedStatus) {
		this.expectedStatus = expectedStatus;
	}

	public int getIntervalSeconds() {
		return intervalSeconds;
	}

	public void setIntervalSeconds(int intervalSeconds) {
		this.intervalSeconds = intervalSeconds;
	}

	public int getTimeoutMs() {
		return timeoutMs;
	}

	public void setTimeoutMs(int timeoutMs) {
		this.timeoutMs = timeoutMs;
	}

	public short getFailureThreshold() {
		return failureThreshold;
	}

	public void setFailureThreshold(short failureThreshold) {
		this.failureThreshold = failureThreshold;
	}

	public short getRecoveryThreshold() {
		return recoveryThreshold;
	}

	public void setRecoveryThreshold(short recoveryThreshold) {
		this.recoveryThreshold = recoveryThreshold;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

}
