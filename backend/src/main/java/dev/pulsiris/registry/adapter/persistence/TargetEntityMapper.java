package dev.pulsiris.registry.adapter.persistence;

import java.net.URI;
import java.time.Duration;

import dev.pulsiris.registry.domain.Target;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface TargetEntityMapper {

	@Mapping(target = "intervalSeconds", source = "interval", qualifiedByName = "toSeconds")
	@Mapping(target = "timeoutMs", source = "timeout", qualifiedByName = "toMillis")
	@Mapping(target = "version", ignore = true)
	TargetEntity toEntity(Target target);

	// Copies onto an already loaded entity, keeping its version for optimistic locking.
	@InheritConfiguration(name = "toEntity")
	void copyOnto(Target target, @MappingTarget TargetEntity entity);

	@Mapping(target = "interval", source = "intervalSeconds", qualifiedByName = "ofSeconds")
	@Mapping(target = "timeout", source = "timeoutMs", qualifiedByName = "ofMillis")
	Target toDomain(TargetEntity entity);

	default String toString(URI uri) {
		return uri.toString();
	}

	default URI toUri(String value) {
		return URI.create(value);
	}

	@Named("toSeconds")
	default int toSeconds(Duration duration) {
		return Math.toIntExact(duration.toSeconds());
	}

	@Named("toMillis")
	default int toMillis(Duration duration) {
		return Math.toIntExact(duration.toMillis());
	}

	@Named("ofSeconds")
	default Duration ofSeconds(int seconds) {
		return Duration.ofSeconds(seconds);
	}

	@Named("ofMillis")
	default Duration ofMillis(int millis) {
		return Duration.ofMillis(millis);
	}

}
