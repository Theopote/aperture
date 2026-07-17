package dev.aperture.runtime.model.world;

import dev.aperture.runtime.model.event.SpatialRef;
import java.util.Objects;

public record SpawnParticleEffect(SpatialRef location, String particleId, int count) implements WorldEffect {
	public SpawnParticleEffect {
		Objects.requireNonNull(location, "location"); Objects.requireNonNull(particleId, "particleId");
		if (count < 1) throw new IllegalArgumentException("count must be positive");
	}
	@Override public String effectType() { return "aperture:spawn_particle"; }
}
