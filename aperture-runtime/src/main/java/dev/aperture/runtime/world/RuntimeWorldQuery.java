package dev.aperture.runtime.world;

import java.util.Optional;

/** Read-only platform boundary for behavior and simulation world queries. */
public interface RuntimeWorldQuery {
	<T> Optional<T> query(WorldQuery<T> query);

	static RuntimeWorldQuery empty() {
		return new RuntimeWorldQuery() {
			@Override
			public <T> Optional<T> query(WorldQuery<T> query) {
				return Optional.empty();
			}
		};
	}
}
