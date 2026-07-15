package dev.aperture.core.editor.snap;

import dev.aperture.core.geometry.Vec3d;

import java.util.Objects;

/**
 * A point in space that manipulators can snap to while dragging.
 */
public record SnapPoint(
	String id,
	SnapKind kind,
	Vec3d position,
	Vec3d normal,
	double strength
) {
	public SnapPoint {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(position, "position");
		Objects.requireNonNull(normal, "normal");
		if (strength < 0) {
			throw new IllegalArgumentException("strength must be non-negative");
		}
	}

	public static SnapPoint grid(String id, Vec3d position, double gridSizeMm) {
		return new SnapPoint(id, SnapKind.GRID, position, new Vec3d(0, 1, 0), gridSizeMm);
	}
}
