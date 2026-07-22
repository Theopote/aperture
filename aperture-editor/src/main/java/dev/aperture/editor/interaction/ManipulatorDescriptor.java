package dev.aperture.editor.interaction;

import dev.aperture.parameter.ParameterType;

import java.util.Objects;
import java.util.OptionalDouble;

/** Declarative contract used by tools to edit one architectural parameter. */
public record ManipulatorDescriptor(
	String id,
	Kind kind,
	String label,
	String parameterKey,
	Axis axis,
	Anchor anchor,
	Anchor fixedAnchor,
	DirectionPolicy direction,
	OptionalDouble minimum,
	OptionalDouble maximum,
	double snapIncrement,
	double fineSnapIncrement,
	ParameterType unit
) {
	public enum Kind { LINEAR_PARAMETER }
	public enum Axis { LOCAL_X, LOCAL_Y, LOCAL_Z }
	public enum Anchor { LEFT_MIDPOINT, RIGHT_MIDPOINT, BOTTOM_MIDPOINT, TOP_MIDPOINT, FRONT_MIDPOINT, BACK_MIDPOINT }
	public enum DirectionPolicy { POSITIVE, NEGATIVE }

	public ManipulatorDescriptor {
		if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
		Objects.requireNonNull(kind, "kind");
		if (label == null || label.isBlank()) throw new IllegalArgumentException("label is required");
		if (parameterKey == null || parameterKey.isBlank()) throw new IllegalArgumentException("parameterKey is required");
		Objects.requireNonNull(axis, "axis");
		Objects.requireNonNull(anchor, "anchor");
		Objects.requireNonNull(fixedAnchor, "fixedAnchor");
		Objects.requireNonNull(direction, "direction");
		minimum = minimum == null ? OptionalDouble.empty() : minimum;
		maximum = maximum == null ? OptionalDouble.empty() : maximum;
		if (snapIncrement <= 0 || fineSnapIncrement <= 0) throw new IllegalArgumentException("snap increments must be positive");
		Objects.requireNonNull(unit, "unit");
	}
}
