package dev.aperture.runtime.lifecycle;

import dev.aperture.geometry.kinematic.KinematicPart;

import java.util.List;

/** Immutable dynamic-part metadata owned by one active runtime session. */
public record KinematicModel(List<KinematicPart> parts) {
	public static final KinematicModel EMPTY = new KinematicModel(List.of());

	public KinematicModel {
		parts = List.copyOf(parts);
	}
}
