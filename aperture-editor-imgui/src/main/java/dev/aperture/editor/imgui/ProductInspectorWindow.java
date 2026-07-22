package dev.aperture.editor.imgui;

import dev.aperture.editor.model.inspector.InspectorSection;
import dev.aperture.editor.model.inspector.PropertyDescriptor;
import dev.aperture.editor.model.read.ObjectEditorView;
import dev.aperture.editor.model.read.SyncStatus;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImString;

import java.util.Locale;

/** Workflow-oriented Inspector: discover, preview, validate, commit and correlate with world handles. */
final class ProductInspectorWindow {
	private final ApertureUiContext context;
	private final InspectorPropertyEditor editor;
	private final ImString search = new ImString(96);
	private final ImBoolean advanced = new ImBoolean(false);

	ProductInspectorWindow(ApertureUiContext context) {
		this.context = context;
		this.editor = new InspectorPropertyEditor(context.session);
	}

	void cancelEdit() { editor.cancel(); }

	void render() {
		InspectorInteractionState.beginFrame();
		if (!ImGui.begin("Inspector")) { ImGui.end(); return; }
		var selection = context.session.selection().snapshot();
		if (selection.objectIds().isEmpty()) empty("No object selected", "Select an object in the world or Project Navigator.");
		else if (selection.objectIds().size() > 1) empty(selection.objectIds().size() + " objects selected", "Multi-object editing is not available yet.");
		else context.session.readModel().object(selection.primaryObject()).ifPresentOrElse(
			view -> object(selection.primaryObject(), view),
			() -> empty("Object unavailable", "Waiting for the synchronized replica."));
		ImGui.end();
	}

	private void object(ArchitecturalObjectId id, ObjectEditorView view) {
		ImGui.text(view.displayName());
		ImGui.sameLine(ImGui.getWindowWidth() - 34);
		ImGui.textDisabled("...");
		ImGui.textDisabled(view.typeId().path().replace('_', ' '));
		status(view);
		ImGui.separator();
		ImGui.setNextItemWidth(-1);
		ImGui.inputText("##property-search", search);
		if (search.get().isBlank() && !ImGui.isItemActive()) {
			ImGui.sameLine(18);
			ImGui.textDisabled("Search properties...");
		}
		ImGui.checkbox("Show advanced properties", advanced);
		String query = search.get().trim().toLowerCase(Locale.ROOT);
		for (InspectorSection section : context.session.inspector().sections(id)) {
			var visible = section.properties().stream().filter(PropertyDescriptor::visible)
				.filter(property -> advanced.get() || !advanced(property))
				.filter(property -> matches(section, property, query)).toList();
			if (visible.isEmpty()) continue;
			boolean open = !query.isBlank() || !advanced(section);
			if (ImGui.collapsingHeader(section.label(), open ? imgui.flag.ImGuiTreeNodeFlags.DefaultOpen : 0)) {
				for (PropertyDescriptor property : visible) property(id, view, property);
			}
		}
		if (context.session.inspector().sections(id).stream().flatMap(section -> section.properties().stream())
			.filter(PropertyDescriptor::visible).noneMatch(property -> matches(null, property, query))) {
			ImGui.textDisabled("No properties match '" + search.get() + "'.");
		}
	}

	private void status(ObjectEditorView view) {
		float[] color = view.syncStatus() == SyncStatus.SYNCHRONIZED ? ApertureStyle.SUCCESS
			: view.syncStatus() == SyncStatus.PREVIEW ? ApertureStyle.BLUE : ApertureStyle.WARNING;
		ImGui.textColored(color[0], color[1], color[2], 1, syncLabel(view.syncStatus()));
		ImGui.sameLine();
		ImGui.textDisabled("Revision " + view.objectRevision());
		if (!view.diagnostics().isEmpty()) {
			ImGui.sameLine();
			ImGui.textColored(ApertureStyle.WARNING[0], ApertureStyle.WARNING[1], ApertureStyle.WARNING[2], 1,
				view.diagnostics().size() + " issue" + (view.diagnostics().size() == 1 ? "" : "s"));
		}
	}

	private void property(ArchitecturalObjectId id, ObjectEditorView view, PropertyDescriptor property) {
		ImGui.pushID(property.key());
		ImGui.textDisabled(property.displayName());
		boolean labelHovered = ImGui.isItemHovered();
		if (labelHovered && !property.description().isBlank()) ImGui.setTooltip(property.description());
		ImGui.sameLine(ApertureStyle.LABEL_WIDTH);
		Object value = property.value();
		boolean rendered = value instanceof ParameterValue parameter && editor.render(id, property, parameter);
		boolean valueHovered = rendered && ImGui.isItemHovered();
		if (!rendered) ImGui.text(format(value) + unit(property));
		if ((labelHovered || valueHovered) && (property.key().equals("width") || property.key().equals("height")))
			InspectorInteractionState.hover(property.key());
		String editError = editor.error(property.key());
		for (String validation : property.validation()) error(validation);
		if (!editError.isBlank()) error(editError);
		InspectorDiagnosticPresenter.render(view, property);
		if (property.readOnly()) statusChip("Read-only", ApertureStyle.WARNING);
		else if (editor.editing(property.key())) statusChip("Preview", ApertureStyle.BLUE);
		else if (context.session.preview().value(view.objectId(), property.key()).isPresent()) statusChip("Pending", ApertureStyle.BLUE);
		ImGui.popID();
	}

	private static void statusChip(String label, float[] color) {
		ImGui.textColored(color[0], color[1], color[2], 1, label);
	}

	private static void error(String message) {
		ImGui.textColored(ApertureStyle.ERROR[0], ApertureStyle.ERROR[1], ApertureStyle.ERROR[2], 1, message);
	}

	private static boolean matches(InspectorSection section, PropertyDescriptor property, String query) {
		if (query.isBlank()) return true;
		String haystack = property.key() + " " + property.displayName() + " " + property.description() + " "
			+ property.group() + " " + (section == null ? "" : section.label());
		return haystack.toLowerCase(Locale.ROOT).contains(query);
	}

	private static boolean advanced(PropertyDescriptor property) { return advanced(property.group()); }
	private static boolean advanced(InspectorSection section) { return advanced(section.label()); }
	private static boolean advanced(String value) {
		String normalized = value.toLowerCase(Locale.ROOT);
		return normalized.contains("advanced") || normalized.contains("runtime") || normalized.contains("unschematized");
	}

	private static String unit(PropertyDescriptor property) { return property.unit().isBlank() ? "" : " " + property.unit(); }
	private static void empty(String title, String detail) { ImGui.text(title); ImGui.textDisabled(detail); }
	private static String syncLabel(SyncStatus status) { return switch (status) {
		case SYNCHRONIZED -> "Synchronized";
		case PREVIEW -> "Previewing changes";
		case RESYNC_REQUIRED -> "Resynchronization required";
	}; }
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
