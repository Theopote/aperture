package dev.aperture.editor.imgui;

import dev.aperture.editor.interaction.DimensionValueParser;
import dev.aperture.editor.model.command.ExpectedRevision;
import dev.aperture.editor.model.inspector.PropertyDescriptor;
import dev.aperture.editor.model.preview.DefaultParameterEditSession;
import dev.aperture.editor.model.preview.ParameterEditSession;
import dev.aperture.editor.model.session.EditorSession;
import dev.aperture.parameter.ParameterType;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;

import java.util.HashMap;
import java.util.Map;

/** Complete schema-driven editor for numeric, boolean, choice and material properties. */
final class InspectorPropertyEditor {
	private final EditorSession session;
	private final DimensionValueParser lengths = new DimensionValueParser();
	private final Map<String, ImString> inputs = new HashMap<>();
	private ParameterEditSession active;
	private ParameterValue activeBase;
	private String activeKey;
	private String error = "";

	InspectorPropertyEditor(EditorSession session) { this.session = session; }

	boolean render(ArchitecturalObjectId id, PropertyDescriptor property, ParameterValue value) {
		if (property.readOnly()) return false;
		if (value instanceof ParameterValue.BoolValue bool) return checkbox(id, property, bool);
		if (value instanceof ParameterValue.EnumValue choice) return choice(id, property, choice);
		if (value instanceof ParameterValue.MaterialRefValue material) return text(id, property, material);
		if (value.type() == ParameterType.LENGTH || value.type() == ParameterType.ANGLE
			|| value.type() == ParameterType.NUMBER || value.type() == ParameterType.COUNT)
			return text(id, property, value);
		return false;
	}

	String error(String key) { return activeProperty(key) ? error : ""; }
	boolean editing(String key) { return activeProperty(key) && active != null; }

	void cancel() {
		if (active != null && active.active()) active.cancel();
		active = null;
		activeBase = null;
		activeKey = null;
		error = "";
	}

	private boolean text(ArchitecturalObjectId id, PropertyDescriptor property, ParameterValue value) {
		String token = token(id, property);
		ImString input = inputs.computeIfAbsent(token, ignored -> new ImString(96));
		if (!token.equals(activeKey)) input.set(format(value, property.precision()));
		ImGui.setNextItemWidth(-54);
		boolean enter = ImGui.inputText("##value", input, ImGuiInputTextFlags.EnterReturnsTrue);
		if (ImGui.isItemActivated()) begin(id, property, value, token);
		if (ImGui.isItemEdited()) preview(property, input.get());
		if (enter || ImGui.isItemDeactivatedAfterEdit()) commit();
		resetButton(id, property, value);
		return true;
	}

	private boolean checkbox(ArchitecturalObjectId id, PropertyDescriptor property, ParameterValue.BoolValue value) {
		ImBoolean data = new ImBoolean(value.value());
		if (ImGui.checkbox("##value", data)) submit(id, property, value, ParameterValue.bool(data.get()));
		resetButton(id, property, value);
		return true;
	}

	private boolean choice(ArchitecturalObjectId id, PropertyDescriptor property, ParameterValue.EnumValue value) {
		ImGui.setNextItemWidth(-54);
		if (ImGui.beginCombo("##value", value.value())) {
			for (String option : property.enumOptions()) if (ImGui.selectable(option, option.equals(value.value()))) {
				submit(id, property, value, ParameterValue.enumValue(option));
			}
			ImGui.endCombo();
		}
		resetButton(id, property, value);
		return true;
	}

	private void resetButton(ArchitecturalObjectId id, PropertyDescriptor property, ParameterValue value) {
		ImGui.sameLine();
		ImGui.beginDisabled(value.equals(property.defaultValue()));
		try {
			if (ImGui.smallButton("Reset##default") && property.defaultValue() instanceof ParameterValue next)
				submit(id, property, value, next);
		} finally { ImGui.endDisabled(); }
		if (ImGui.isItemHovered()) ImGui.setTooltip("Reset to default");
	}

	private void preview(PropertyDescriptor property, String text) {
		if (active == null || !active.active()) return;
		try {
			ParameterValue next;
			if (activeBase instanceof ParameterValue.MaterialRefValue) next = ParameterValue.materialRef(text.trim());
			else {
				double parsed = activeBase.type() == ParameterType.LENGTH
					? lengths.parse(text, activeBase.asNumber()).millimeters() : Double.parseDouble(text.trim());
				if (activeBase.type() == ParameterType.COUNT && parsed != Math.rint(parsed))
					throw new IllegalArgumentException("Count must be a whole number");
				if (property.minimum().isPresent() && parsed < property.minimum().getAsDouble())
					throw new IllegalArgumentException("Minimum allowed value is " + property.minimum().getAsDouble() + " " + property.unit());
				if (property.maximum().isPresent() && parsed > property.maximum().getAsDouble())
					throw new IllegalArgumentException("Maximum allowed value is " + property.maximum().getAsDouble() + " " + property.unit());
				next = activeBase.type() == ParameterType.LENGTH ? ParameterValue.length(parsed)
					: activeBase.type() == ParameterType.ANGLE ? ParameterValue.angle(parsed)
					: activeBase.type() == ParameterType.COUNT ? ParameterValue.count((int) parsed) : ParameterValue.number(parsed);
			}
			active.updatePreview(next);
			error = "";
		} catch (IllegalArgumentException exception) { error = exception.getMessage(); }
	}

	private void submit(ArchitecturalObjectId id, PropertyDescriptor property, ParameterValue before, ParameterValue after) {
		begin(id, property, before, token(id, property));
		active.updatePreview(after);
		commit();
	}

	private void begin(ArchitecturalObjectId id, PropertyDescriptor property, ParameterValue base, String token) {
		cancel();
		var view = session.readModel().object(id).orElseThrow();
		activeBase = base;
		active = new DefaultParameterEditSession(id, property.key(), base,
			new ExpectedRevision(view.objectRevision(), view.stateRevision()), session.preview(), session.commands());
		activeKey = token;
	}

	private void commit() {
		if (active == null || !active.active() || !error.isBlank()) return;
		active.commit();
		active = null;
		activeBase = null;
		activeKey = null;
	}

	private boolean activeProperty(String key) { return activeKey != null && activeKey.endsWith(":" + key); }
	private static String token(ArchitecturalObjectId id, PropertyDescriptor property) { return id + ":" + property.key(); }
	private static String format(ParameterValue value, int precision) {
		if (value instanceof ParameterValue.MaterialRefValue material) return material.raw();
		if (value instanceof ParameterValue.CountValue count) return Integer.toString(count.value());
		return String.format(java.util.Locale.ROOT, "%." + precision + "f", value.asNumber());
	}
}
