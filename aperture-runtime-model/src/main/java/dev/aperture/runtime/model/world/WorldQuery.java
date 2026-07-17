package dev.aperture.runtime.model.world;

import java.util.Map;
import java.util.Optional;

/**
 * Compatibility adapter for the original string-based query boundary.
 * New code should execute typed {@link WorldQueryRequest} values through
 * {@link WorldQueryExecutor}.
 */
@Deprecated(forRemoval = true)
public interface WorldQuery extends WorldQueryExecutor {
	Optional<Object> query(String queryType, Map<String, Object> arguments);

	@Override
	default <R> Optional<R> execute(WorldQueryRequest<R> request) {
		return query(request.queryType(), Map.of()).map(request.resultType()::cast);
	}

	static WorldQuery unavailable() { return (type, arguments) -> Optional.empty(); }
}
