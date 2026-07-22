package dev.aperture.editor.model.inspector;

import dev.aperture.parameter.ParameterType;
import dev.aperture.parameter.ParameterValue;

import java.util.List;
import java.util.OptionalDouble;

/** Complete, frontend-neutral description of one Inspector property. */
public record PropertyDescriptor(
	String key,
	String displayName,
	String description,
	ParameterType type,
	Object value,
	Object defaultValue,
	String unit,
	OptionalDouble minimum,
	OptionalDouble maximum,
	OptionalDouble step,
	int precision,
	List<String> enumOptions,
	boolean readOnly,
	boolean visible,
	String group,
	List<String> validation,
	PreferredWidget preferredWidget
) {
	public enum PreferredWidget { DRAG, SLIDER, CHECKBOX, COMBO, MATERIAL, TEXT }

	public PropertyDescriptor {
		displayName = displayName == null || displayName.isBlank() ? key : displayName;
		description = description == null ? "" : description;
		unit = unit == null ? "" : unit;
		minimum = minimum == null ? OptionalDouble.empty() : minimum;
		maximum = maximum == null ? OptionalDouble.empty() : maximum;
		step = step == null ? OptionalDouble.empty() : step;
		enumOptions = List.copyOf(enumOptions == null ? List.of() : enumOptions);
		group = group == null ? "" : group;
		validation = List.copyOf(validation == null ? List.of() : validation);
	}

	public static PropertyDescriptor readOnlyText(String key, String label, Object value, String group) {
		return new PropertyDescriptor(key, label, "", null, value, null, "", OptionalDouble.empty(),
			OptionalDouble.empty(), OptionalDouble.empty(), 0, List.of(), true, true, group, List.of(), PreferredWidget.TEXT);
	}

	public ParameterValue parameterValue() {
		return value instanceof ParameterValue parameter ? parameter : null;
	}
}
