package dev.pulsiris.registry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Query API the registry offers other modules.
 */
public interface TargetLookup {

	List<TargetSnapshot> findEnabled();

	Optional<TargetSnapshot> findById(UUID id);

}
