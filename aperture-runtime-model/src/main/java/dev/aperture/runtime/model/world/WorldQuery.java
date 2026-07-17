package dev.aperture.runtime.model.world;

import java.util.Map;
import java.util.Optional;

/** Read-only platform boundary exposed to behavior evaluation. */
public interface WorldQuery {
	Optional<Object> query(String queryType, Map<String, Object> arguments);

	static WorldQuery unavailable() { return (type, arguments) -> Optional.empty(); }
}
