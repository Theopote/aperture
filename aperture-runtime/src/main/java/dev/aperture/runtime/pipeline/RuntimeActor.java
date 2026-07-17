package dev.aperture.runtime.pipeline;

import java.util.Objects;
import java.util.Set;

/** Identity and roles of the player, automation, or simulation issuing an interaction. */
public record RuntimeActor(String id, Set<String> roles) {
	public static final RuntimeActor SYSTEM = new RuntimeActor("aperture:system", Set.of("system"));

	public RuntimeActor {
		Objects.requireNonNull(id, "id");
		if (id.isBlank()) {
			throw new IllegalArgumentException("Runtime actor id must not be blank");
		}
		roles = Set.copyOf(roles);
	}

	public boolean hasRole(String role) {
		return roles.contains(role);
	}
}
