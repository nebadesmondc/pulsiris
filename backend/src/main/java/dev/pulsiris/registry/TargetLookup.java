package dev.pulsiris.registry;

import java.util.List;

/**
 * Query API the registry offers other modules.
 */
public interface TargetLookup {

	List<TargetSnapshot> findEnabled();

}
