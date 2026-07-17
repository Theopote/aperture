package dev.aperture.runtime.model.world;

import dev.aperture.runtime.model.event.SpatialRef;
import java.util.Objects;

public record PlaySoundEffect(SpatialRef location, String soundId, double volume, double pitch) implements WorldEffect {
	public PlaySoundEffect {
		Objects.requireNonNull(location, "location"); Objects.requireNonNull(soundId, "soundId");
		if (!Double.isFinite(volume) || volume < 0 || !Double.isFinite(pitch) || pitch < 0) throw new IllegalArgumentException("volume and pitch must be finite and non-negative");
	}
	@Override public String effectType() { return "aperture:play_sound"; }
}
