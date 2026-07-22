package dev.aperture.editor.interaction;

import dev.aperture.math.Vec3d;

import java.util.Objects;

/** A normalized ray in host-world units. */
public record WorldRay(Vec3d origin, Vec3d direction) {
	public WorldRay {
		Objects.requireNonNull(origin, "origin");
		Objects.requireNonNull(direction, "direction");
		if (direction.lengthSquared() == 0.0) throw new IllegalArgumentException("direction must not be zero");
		direction = direction.normalize();
	}
}
