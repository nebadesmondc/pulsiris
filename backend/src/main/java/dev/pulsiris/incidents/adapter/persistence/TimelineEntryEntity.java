package dev.pulsiris.incidents.adapter.persistence;

import java.time.Instant;
import java.util.UUID;

import dev.pulsiris.incidents.domain.TimelineEntryType;
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
@Table(schema = "incidents", name = "incident_timeline_entry")
public class TimelineEntryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "timeline_entry_seq")
	@SequenceGenerator(name = "timeline_entry_seq", schema = "incidents", sequenceName = "timeline_entry_seq",
			allocationSize = 50)
	private Long id;

	private UUID incidentId;

	private Instant occurredAt;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(columnDefinition = "incidents.timeline_entry_type")
	private TimelineEntryType entryType;

	private String actor;

	private String message;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public UUID getIncidentId() {
		return incidentId;
	}

	public void setIncidentId(UUID incidentId) {
		this.incidentId = incidentId;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(Instant occurredAt) {
		this.occurredAt = occurredAt;
	}

	public TimelineEntryType getEntryType() {
		return entryType;
	}

	public void setEntryType(TimelineEntryType entryType) {
		this.entryType = entryType;
	}

	public String getActor() {
		return actor;
	}

	public void setActor(String actor) {
		this.actor = actor;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
