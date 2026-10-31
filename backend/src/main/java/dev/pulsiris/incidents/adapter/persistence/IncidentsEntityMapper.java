package dev.pulsiris.incidents.adapter.persistence;

import dev.pulsiris.incidents.domain.Incident;
import dev.pulsiris.incidents.domain.TimelineEntry;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface IncidentsEntityMapper {

	@Mapping(target = "version", ignore = true)
	IncidentEntity toEntity(Incident incident);

	@InheritConfiguration(name = "toEntity")
	void copyOnto(Incident incident, @MappingTarget IncidentEntity entity);

	Incident toDomain(IncidentEntity entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "entryType", source = "type")
	TimelineEntryEntity toEntity(TimelineEntry entry);

	@Mapping(target = "type", source = "entryType")
	TimelineEntry toDomain(TimelineEntryEntity entity);

}
