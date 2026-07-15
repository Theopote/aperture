package dev.aperture.core.editor.manipulation;

import dev.aperture.core.geometry.Vec3d;

import java.util.Objects;

/**
 * A draggable handle that resizes one parametric dimension (width, height, thickness).
 */
public record ResizeHandle(
	String id,
	ResizeAxis axis,
	String parameterName,
	Vec3d localPosition,
	Vec3d dragDirection
) {
	public ResizeHandle {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(axis, "axis");
		Objects.requireNonNull(parameterName, "parameterName");
		Objects.requireNonNull(localPosition, "localPosition");
		Objects.requireNonNull(dragDirection, "dragDirection");
	}
}
