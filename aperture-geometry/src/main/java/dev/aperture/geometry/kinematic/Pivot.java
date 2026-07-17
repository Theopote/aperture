package dev.aperture.geometry.kinematic;

import dev.aperture.math.Vec3d;
import java.util.Objects;

public record Pivot(Vec3d position) {
	public Pivot { Objects.requireNonNull(position, "position"); }
}
