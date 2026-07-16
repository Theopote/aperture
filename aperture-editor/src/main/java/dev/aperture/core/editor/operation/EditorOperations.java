package dev.aperture.core.editor.operation;

import dev.aperture.core.editor.EditorObject;
import dev.aperture.core.editor.manipulation.MirrorAxis;
import dev.aperture.core.editor.manipulation.ResizeAxis;
import dev.aperture.core.editor.manipulation.ResizeHandle;
import dev.aperture.math.Facing;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterType;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.core.parametric.NumberParameter;
import dev.aperture.core.parametric.ParametricEditor;
import dev.aperture.core.parametric.RangeParameter;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Pure operations used by the editor session to transform openings.
 */
public final class EditorOperations {
	private EditorOperations() {
	}

	public static double currentLength(EditorObject object, String parameterName) {
		ParameterSet resolved = object.definition().resolveParameters(object.instance().parameters());
		return resolved.get(parameterName)
			.filter(value -> value.type() == ParameterType.LENGTH)
			.map(value -> ((ParameterValue.LengthValue) value).millimeters())
			.orElseThrow(() -> new IllegalArgumentException("Missing length parameter: " + parameterName));
	}

	public static double clampLength(EditorObject object, String parameterName, double valueMm) {
		var parameter = object.definition().parametricSchema().require(parameterName);
		return switch (parameter) {
			case RangeParameter range -> Math.max(range.min(), Math.min(range.max(), valueMm));
			case NumberParameter number -> {
				double clamped = valueMm;
				if (number.min().isPresent()) {
					clamped = Math.max(clamped, number.min().getAsDouble());
				}
				if (number.max().isPresent()) {
					clamped = Math.min(clamped, number.max().getAsDouble());
				}
				yield clamped;
			}
			default -> throw new IllegalArgumentException("Not a numeric parameter: " + parameterName);
		};
	}

	public static Optional<ResizeHandle> handleForAxis(EditorObject object, ResizeAxis axis) {
		return object.resizeHandles().stream()
			.filter(handle -> handle.axis() == axis)
			.findFirst();
	}

	public static Transform3d rotateTransform(Transform3d current, double degrees) {
		int quarterTurns = ((int) Math.round(degrees / 90.0)) % 4;
		if (quarterTurns < 0) {
			quarterTurns += 4;
		}
		Facing facing = current.facing();
		for (int i = 0; i < quarterTurns; i++) {
			facing = facing.rotateClockwise();
		}
		return new Transform3d(current.origin(), facing, current.rotationAxisOrigin(), current.rotationAxisDirection(), current.rotationRadians());
	}

	public static Transform3d mirrorTransform(Transform3d current, MirrorAxis axis) {
		Vec3d origin = current.origin();
		Vec3d mirroredOrigin = switch (axis) {
			case X -> new Vec3d(-origin.x(), origin.y(), origin.z());
			case Y -> new Vec3d(origin.x(), -origin.y(), origin.z());
			case Z -> new Vec3d(origin.x(), origin.y(), -origin.z());
		};
		Facing facing = switch (axis) {
			case X -> current.facing().getOpposite();
			case Y -> current.facing();
			case Z -> current.facing().getOpposite();
		};
		return new Transform3d(mirroredOrigin, facing, current.rotationAxisOrigin(), current.rotationAxisDirection(), current.rotationRadians());
	}

	public static OpeningInstance duplicateInstance(EditorObject source, Vec3d offset) {
		Transform3d transform = source.instance().transform();
		Vec3d origin = transform.origin().add(offset);
		return OpeningInstance.builder(source.instance().typeId())
			.instanceId(UUID.randomUUID())
			.parameters(source.instance().parameters())
			.transform(new Transform3d(origin, transform.facing(), transform.rotationAxisOrigin(), transform.rotationAxisDirection(), transform.rotationRadians()))
			.host(source.instance().host())
			.state(source.instance().state())
			.build();
	}

	public static Optional<ParameterValue> mirroredHingeSide(EditorObject object) {
		ParameterSet resolved = object.definition().resolveParameters(object.instance().parameters());
		if (resolved.get("hinge_side").isEmpty()) {
			return Optional.empty();
		}
		String current = ((ParameterValue.EnumValue) resolved.get("hinge_side").orElseThrow()).value();
		String mirrored = switch (current) {
			case "left" -> "right";
			case "right" -> "left";
			default -> current;
		};
		return Optional.of(ParameterValue.enumValue(mirrored));
	}

	public static ParametricEditor parametricEditor(EditorObject object) {
		return ParametricEditor.fromInstance(object.definition(), object.instance());
	}

	public static Map<String, Object> parameterSnapshot(EditorObject object) {
		return parametricEditor(object).snapshot();
	}
}
