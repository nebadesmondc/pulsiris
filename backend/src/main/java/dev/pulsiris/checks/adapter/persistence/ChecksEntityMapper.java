package dev.pulsiris.checks.adapter.persistence;

import java.time.Duration;

import dev.pulsiris.checks.domain.CheckResult;
import dev.pulsiris.checks.domain.HealthState;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface ChecksEntityMapper {

	@Mapping(target = "version", ignore = true)
	HealthStateEntity toEntity(HealthState state);

	@InheritConfiguration(name = "toEntity")
	void copyOnto(HealthState state, @MappingTarget HealthStateEntity entity);

	HealthState toDomain(HealthStateEntity entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "latencyMs", source = "latency")
	CheckResultEntity toEntity(CheckResult result);

	@Mapping(target = "latency", source = "latencyMs")
	CheckResult toDomain(CheckResultEntity entity);

	default int toMillis(Duration duration) {
		return Math.toIntExact(duration.toMillis());
	}

	default Duration ofMillis(int millis) {
		return Duration.ofMillis(millis);
	}

}
