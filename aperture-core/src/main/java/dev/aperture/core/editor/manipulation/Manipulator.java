package dev.aperture.core.editor.manipulation;

import dev.aperture.core.geometry.Vec3d;

import java.util.Objects;
import java.util.Optional;

/**
 * Describes one interactive editing affordance on an {@link dev.aperture.core.editor.EditorObject}.
 * GUI, AI, and NodeCraft all target manipulators — not Java implementation classes.
 */
public record Manipulator(
	String id,
	ManipulatorKind kind,
	Optional<String> parameterName,
	Vec3d localAnchor,
	Vec3d localDirection
) {
	public Manipulator {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(parameterName, "parameterName");
		Objects.requireNonNull(localAnchor, "localAnchor");
		Objects.requireNonNull(localDirection, "localDirection");
	}

	public static Manipulator translate(Vec3d anchor) {
		return new Manipulator(
			"translate",
			ManipulatorKind.TRANSLATE,
			Optional.empty(),
			anchor,
			new Vec3d(1, 0, 0)
		);
	}

	public static Manipulator rotate(Vec3d anchor) {
		return new Manipulator(
			"rotate",
			ManipulatorKind.ROTATE,
			Optional.empty(),
			anchor,
			new Vec3d(0, 1, 0)
		);
	}

	public static Manipulator mirror(MirrorAxis axis, Vec3d anchor) {
		Vec3d direction = switch (axis) {
			case X -> new Vec3d(1, 0, 0);
			case Y -> new Vec3d(0, 1, 0);
			case Z -> new Vec3d(0, 0, 1);
		};
		return new Manipulator(
			"mirror." + axis.name().toLowerCase(),
			ManipulatorKind.MIRROR,
			Optional.empty(),
			anchor,
			direction
		);
	}

	public static Manipulator resize(ResizeHandle handle) {
		return new Manipulator(
			"resize." + handle.id(),
			ManipulatorKind.RESIZE,
			Optional.of(handle.parameterName()),
			handle.localPosition(),
			handle.dragDirection()
		);
	}
}
