package dev.aperture.runtime.model.world;

import java.util.Optional;

public interface WorldQueryExecutor {
	<R> Optional<R> execute(WorldQueryRequest<R> query);

	static WorldQueryExecutor unavailable() {
		return new WorldQueryExecutor() {
			@Override public <R> Optional<R> execute(WorldQueryRequest<R> query) { return Optional.empty(); }
		};
	}
}
