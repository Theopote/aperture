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

/** Professional property-row editing with preview, exact length input and reset-to-default. */
final class InspectorPropertyEditor {
	private final EditorSession session;
	private final DimensionValueParser lengths = new DimensionValueParser();
	private final Map<String, ImString> inputs = new HashMap<>();
	private ParameterEditSession active;
	private ParameterValue activeBase;
	private String activeKey;
	private String error = "";

	InspectorPropertyEditor(EditorSession session) { this.session = session; }

	boolean render(ArchitecturalObjectId objectId, PropertyDescriptor property, ParameterValue value) {
		if (property.readOnly()) return false;
		if (value instanceof ParameterValue.BoolValue bool) return checkbox(objectId, property, bool);
		if (value.type() != ParameterType.LENGTH && value.type() != ParameterType.ANGLE
			&& value.type() != ParameterType.NUMBER) return false;
		String token = objectId + ":" + property.key();
		ImString input = inputs.computeIfAbsent(token, ignored -> new ImString(64));
		if (!token.equals(activeKey)) input.set(format(value, property.precision()));
		ImGui.setNextItemWidth(-54);
		boolean enter = ImGui.inputText("##value", input, ImGuiInputTextFlags.EnterReturnsTrue);
		if (ImGui.isItemActivated()) begin(objectId, property, value, token);
		if (ImGui.isItemEdited()) preview(property, input.get());
		if (enter || ImGui.isItemDeactivatedAfterEdit()) commit();
		ImGui.sameLine();
		ImGui.beginDisabled(equal(value, property.defaultValue()));
		try {
			if (ImGui.smallButton("Reset##default")) reset(objectId, property, value);
		} finally { ImGui.endDisabled(); }
		if (ImGui.isItemHovered()) ImGui.setTooltip("Reset to default");
		return true;
	}

	String error(String key) { return isActiveProperty(key) ? error : ""; }
	boolean editing(String key) { return isActiveProperty(key) && active != null; }

	void cancel() {
		if (active != null && active.active()) active.cancel();
		active = null;
		activeBase = null;
		activeKey = null;
		error = "";
	}

	private boolean checkbox(ArchitecturalObjectId id, PropertyDescriptor property, ParameterValue.BoolValue value) {
		ImBoolean data = new ImBoolean(value.value());
		if (!ImGui.checkbox("##value", data)) return true;
		begin(id, property, value, id + ":" + property.key());
		active.updatePreview(ParameterValue.bool(data.get()));
		commit();
		return true;
	}

	private void preview(PropertyDescriptor property, String text) {
		if (active == null || !active.active()) return;
		try {
			ParameterValue base = activeBase;
			double parsed = base.type() == ParameterType.LENGTH
				? lengths.parse(text, base.asNumber()).millimeters() : Double.parseDouble(text.trim());
			if (property.minimum().isPresent() && parsed < property.minimum().getAsDouble())
				throw new IllegalArgumentException("Minimum allowed value is " + property.minimum().getAsDouble() + " " + property.unit());
			if (property.maximum().isPresent() && parsed > property.maximum().getAsDouble())
				throw new IllegalArgumentException("Maximum allowed value is " + property.maximum().getAsDouble() + " " + property.unit());
			active.updatePreview(base.type() == ParameterType.LENGTH ? ParameterValue.length(parsed)
				: base.type() == ParameterType.ANGLE ? ParameterValue.angle(parsed) : ParameterValue.number(parsed));
			error = "";
		} catch (IllegalArgumentException exception) { error = exception.getMessage(); }
	}

	private void reset(ArchitecturalObjectId id, PropertyDescriptor property, ParameterValue authoritative) {
		if (!(property.defaultValue() instanceof ParameterValue value)) return;
		begin(id, property, authoritative, id + ":" + property.key());
		active.updatePreview(value);
		commit();
	}

	private void begin(ArchitecturalObjectId id, PropertyDescriptor property, ParameterValue authoritative, String token) {
		cancel();
		var view = session.readModel().object(id).orElseThrow();
		activeBase = authoritative;
		active = new DefaultParameterEditSession(id, property.key(), authoritative,
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

	private boolean isActiveProperty(String key) {
		return activeKey != null && activeKey.endsWith(":" + key);
	}

	private static boolean equal(ParameterValue value, Object defaultValue) { return value.equals(defaultValue); }
	private static String format(ParameterValue value, int precision) {
		if (value instanceof ParameterValue.CountValue count) return Integer.toString(count.value());
		return String.format(java.util.Locale.ROOT, "%." + precision + "f", value.asNumber());
	}
}
