package dev.aperture.runtime.model.state;

import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;

import java.time.Instant;
import java.util.Objects;

/** Closed set of platform-neutral values allowed in runtime state. */
public sealed interface StateValue permits StateValue.BooleanValue, StateValue.NumberValue,
	StateValue.EnumValue, StateValue.StringValue, StateValue.ReferenceValue,
	StateValue.VectorValue, StateValue.TransformValue, StateValue.TimestampValue {
	StateValueType type();

	record BooleanValue(boolean value) implements StateValue {
		@Override public StateValueType type() { return StateValueType.BOOLEAN; }
	}

	record NumberValue(double value) implements StateValue {
		public NumberValue {
			if (!Double.isFinite(value)) throw new IllegalArgumentException("State number must be finite");
		}
		@Override public StateValueType type() { return StateValueType.NUMBER; }
	}

	record EnumValue(String value) implements StateValue {
		public EnumValue { value = requireText(value, "enum value"); }
		@Override public StateValueType type() { return StateValueType.ENUM; }
	}

	record StringValue(String value) implements StateValue {
		public StringValue { Objects.requireNonNull(value, "value"); }
		@Override public StateValueType type() { return StateValueType.STRING; }
	}

	record ReferenceValue(String value) implements StateValue {
		public ReferenceValue { value = requireText(value, "reference value"); }
		@Override public StateValueType type() { return StateValueType.REFERENCE; }
	}

	record VectorValue(Vec3d value) implements StateValue {
		public VectorValue { Objects.requireNonNull(value, "value"); }
		@Override public StateValueType type() { return StateValueType.VECTOR; }
	}

	record TransformValue(Transform3d value) implements StateValue {
		public TransformValue { Objects.requireNonNull(value, "value"); }
		@Override public StateValueType type() { return StateValueType.TRANSFORM; }
	}

	record TimestampValue(Instant value) implements StateValue {
		public TimestampValue { Objects.requireNonNull(value, "value"); }
		@Override public StateValueType type() { return StateValueType.TIMESTAMP; }
	}

	private static String requireText(String value, String label) {
		Objects.requireNonNull(value, label);
		if (value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
		return value;
	}

	static StateValue bool(boolean value) { return new BooleanValue(value); }
	static StateValue number(double value) { return new NumberValue(value); }
	static StateValue enumeration(String value) { return new EnumValue(value); }
	static StateValue string(String value) { return new StringValue(value); }
	static StateValue reference(String value) { return new ReferenceValue(value); }
	static StateValue vector(Vec3d value) { return new VectorValue(value); }
	static StateValue transform(Transform3d value) { return new TransformValue(value); }
	static StateValue timestamp(Instant value) { return new TimestampValue(value); }
}
