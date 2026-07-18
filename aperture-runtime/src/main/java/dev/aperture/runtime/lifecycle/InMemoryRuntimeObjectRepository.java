package dev.aperture.runtime.lifecycle;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Thread-safe process-local repository used by the default server runtime. */
public final class InMemoryRuntimeObjectRepository implements RuntimeObjectRepository {
	private final Map<ArchitecturalObjectId, RuntimeObjectSession> sessions = new LinkedHashMap<>();

	@Override
	public synchronized RuntimeObjectSession add(RuntimeObjectSession session) {
		Objects.requireNonNull(session, "session");
		ArchitecturalObjectId id = session.instance().objectId();
		if (sessions.putIfAbsent(id, session) != null) {
			throw new IllegalStateException("Runtime object is already active: " + id);
		}
		return session;
	}

	@Override
	public synchronized Optional<RuntimeObjectSession> find(ArchitecturalObjectId objectId) {
		return Optional.ofNullable(sessions.get(Objects.requireNonNull(objectId, "objectId")));
	}

	@Override
	public synchronized RuntimeObjectSession require(ArchitecturalObjectId objectId) {
		return find(objectId).orElseThrow(() -> new IllegalArgumentException("Runtime object is not active: " + objectId));
	}

	@Override
	public synchronized boolean replace(RuntimeObjectSession expected, RuntimeObjectSession replacement) {
		Objects.requireNonNull(expected, "expected");
		Objects.requireNonNull(replacement, "replacement");
		ArchitecturalObjectId id = expected.instance().objectId();
		if (!id.equals(replacement.instance().objectId())) {
			throw new IllegalArgumentException("Replacement must retain object identity");
		}
		if (sessions.get(id) != expected) return false;
		sessions.put(id, replacement);
		return true;
	}

	@Override
	public synchronized Optional<RuntimeObjectSession> unload(ArchitecturalObjectId objectId) {
		return Optional.ofNullable(sessions.remove(Objects.requireNonNull(objectId, "objectId")));
	}

	@Override
	public synchronized Collection<RuntimeObjectSession> activeObjects() {
		return List.copyOf(sessions.values());
	}
}
