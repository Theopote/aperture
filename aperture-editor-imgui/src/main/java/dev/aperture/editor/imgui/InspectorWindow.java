package dev.aperture.editor.imgui;

import dev.aperture.editor.model.inspector.InspectorSection;
import dev.aperture.editor.model.inspector.PropertyDescriptor;
import dev.aperture.editor.model.read.ObjectEditorView;
import dev.aperture.editor.model.read.SyncStatus;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import imgui.ImGui;

final class InspectorWindow {
	private final ApertureUiContext context;
	private final DearImGuiPropertyEditor editor;

	InspectorWindow(ApertureUiContext context, DearImGuiPropertyEditor editor) {
		this.context = context;
		this.editor = editor;
	}

	void cancelEdit() { editor.cancel(); }

	void render() {
		if (!ImGui.begin("Inspector")) { ImGui.end(); return; }
		var selection = context.session.selection().snapshot();
		if (selection.objectIds().isEmpty()) empty("No object selected", "Select an architectural object in the world or Project Navigator.");
		else if (selection.objectIds().size() > 1) empty(selection.objectIds().size() + " objects selected", "Multi-object editing is not available yet.");
		else context.session.readModel().object(selection.primaryObject()).ifPresentOrElse(
			view -> renderObject(selection.primaryObject(), view),
			() -> empty("Object unavailable", "The selected replica has not been synchronized."));
		ImGui.end();
	}

	private void renderObject(ArchitecturalObjectId id, ObjectEditorView view) {
		ImGui.text(view.displayName());
		ImGui.textDisabled(view.typeId().toString());
		ImGui.textDisabled("Family  " + view.familyId());
		float[] syncColor = view.syncStatus() == SyncStatus.SYNCHRONIZED ? ApertureStyle.SUCCESS
			: view.syncStatus() == SyncStatus.PREVIEW ? ApertureStyle.BLUE : ApertureStyle.WARNING;
		ImGui.textColored(syncColor[0], syncColor[1], syncColor[2], 1, syncLabel(view.syncStatus()));
		ImGui.sameLine();
		ImGui.textDisabled("Revision " + view.objectRevision());
		if (!view.diagnostics().isEmpty()) {
			ImGui.sameLine();
			ImGui.textColored(ApertureStyle.WARNING[0], ApertureStyle.WARNING[1], ApertureStyle.WARNING[2], 1,
				view.diagnostics().size() + " issue" + (view.diagnostics().size() == 1 ? "" : "s"));
		}
		ImGui.separator();
		for (InspectorSection section : context.session.inspector().sections(id)) {
			if (ImGui.collapsingHeader(section.label())) {
				for (PropertyDescriptor property : section.properties()) if (property.visible()) property(id, property);
			}
		}
	}

	private void property(ArchitecturalObjectId id, PropertyDescriptor property) {
		ImGui.pushID(property.key());
		ImGui.textDisabled(property.displayName());
		if (ImGui.isItemHovered() && !property.description().isBlank()) ImGui.setTooltip(property.description());
		ImGui.sameLine(ApertureStyle.LABEL_WIDTH);
		Object value = property.value();
		if (!(value instanceof ParameterValue parameter && editor.render(id, property, parameter))) {
			ImGui.text(format(value) + (property.unit().isBlank() ? "" : " " + property.unit()));
		}
		for (String validation : property.validation()) {
			ImGui.textColored(ApertureStyle.ERROR[0], ApertureStyle.ERROR[1], ApertureStyle.ERROR[2], 1, validation);
		}
		ImGui.popID();
	}

	private static void empty(String title, String detail) {
		ImGui.text(title);
		ImGui.textDisabled(detail);
	}

	private static String syncLabel(SyncStatus status) {
		return switch (status) {
			case SYNCHRONIZED -> "Synchronized";
			case PREVIEW -> "Previewing changes";
			case RESYNC_REQUIRED -> "Resynchronization required";
		};
	}

	private static String format(Object value) {
		if (!(value instanceof ParameterValue parameter)) return String.valueOf(value);
		return switch (parameter) {
			case ParameterValue.LengthValue v -> Double.toString(v.millimeters());
			case ParameterValue.AngleValue v -> Double.toString(v.degrees());
			case ParameterValue.CountValue v -> Integer.toString(v.value());
			case ParameterValue.NumberValue v -> Double.toString(v.value());
			case ParameterValue.EnumValue v -> v.value();
			case ParameterValue.BoolValue v -> Boolean.toString(v.value());
			case ParameterValue.MaterialRefValue v -> v.raw();
		};
	}
}
