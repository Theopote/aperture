package dev.aperture.editor.imgui;

import dev.aperture.editor.model.history.EditorHistoryEntry;
import dev.aperture.editor.model.inspector.InspectorProperty;
import dev.aperture.editor.model.inspector.InspectorSection;
import dev.aperture.editor.model.read.EditorDiagnostic;
import dev.aperture.editor.model.session.EditorSession;
import dev.aperture.parameter.ParameterValue;
import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.internal.flag.ImGuiDockNodeFlags;

import java.util.Objects;

/** Concrete Dear ImGui implementation of the K2.3 editor shell. */
public final class DearImGuiEditor {
	private static final String DOCKSPACE_ID = "ApertureMainDockspace";
	private final EditorSession session;
	private final DearImGuiPropertyEditor propertyEditor;
	private boolean defaultLayoutPending;

	public DearImGuiEditor(EditorSession session) {
		this(session, true);
	}

	public DearImGuiEditor(EditorSession session, boolean defaultLayoutPending) {
		this.session = Objects.requireNonNull(session, "session");
		this.propertyEditor = new DearImGuiPropertyEditor(session);
		this.defaultLayoutPending = defaultLayoutPending;
	}

	public void render() {
		renderDockspace();
		renderOutliner();
		renderInspector();
		renderRuntimeState();
		renderHistory();
		renderDiagnostics();
	}

	private void renderDockspace() {
		ImGuiViewport viewport = ImGui.getMainViewport();
		ImGui.setNextWindowPos(viewport.getWorkPosX(), viewport.getWorkPosY());
		ImGui.setNextWindowSize(viewport.getWorkSizeX(), viewport.getWorkSizeY());
		ImGui.setNextWindowViewport(viewport.getID());
		ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0);
		ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0);
		ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);
		ImGui.pushStyleColor(ImGuiCol.WindowBg, 0, 0, 0, 0);
		int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize
			| ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoNavFocus
			| ImGuiWindowFlags.NoDocking;
		ImGui.begin("Aperture Editor##Root", flags);
		int dockspace = ImGui.getID(DOCKSPACE_ID);
		ImGui.dockSpace(dockspace, 0, 0, ImGuiDockNodeFlags.PassthruCentralNode);
		if (defaultLayoutPending) {
			DearImGuiLayout.buildDefault(dockspace);
			defaultLayoutPending = false;
		}
		ImGui.end();
		ImGui.popStyleColor();
		ImGui.popStyleVar(3);
	}

	private void renderOutliner() {
		if (!ImGui.begin("Object Outliner")) { ImGui.end(); return; }
		var selected = session.selection().snapshot().objectIds();
		for (var object : session.readModel().visibleObjects()) {
			String warning = object.warning() ? " !" : "";
			String label = object.displayName() + "  [" + object.typeId() + "]" + warning + "##" + object.objectId();
			if (ImGui.selectable(label, selected.contains(object.objectId()))) {
				if (ImGui.getIO().getKeyCtrl()) session.selection().addObject(object.objectId());
				else session.selection().selectObject(object.objectId());
			}
		}
		ImGui.end();
	}

	private void renderInspector() {
		if (!ImGui.begin("Inspector")) { ImGui.end(); return; }
		var selection = session.selection().snapshot();
		if (selection.objectIds().isEmpty()) ImGui.textDisabled("No architectural object selected");
		else if (selection.objectIds().size() > 1) ImGui.text(selection.objectIds().size() + " objects selected (read-only)");
		else for (InspectorSection section : session.inspector().sections(selection.primaryObject())) {
			if (ImGui.collapsingHeader(section.label())) for (InspectorProperty property : section.properties()) renderProperty(selection.primaryObject(), property);
		}
		ImGui.end();
	}

	private void renderProperty(dev.aperture.runtime.model.object.ArchitecturalObjectId objectId, InspectorProperty property) {
		ImGui.textDisabled(property.label()); ImGui.sameLine(180);
		Object value = property.value();
		if (value instanceof ParameterValue parameter && propertyEditor.render(objectId, property, parameter)) return;
		String display = value instanceof ParameterValue parameter ? formatParameter(parameter) : String.valueOf(value);
		ImGui.text(display + (property.unit().isBlank() ? "" : " " + property.unit()));
	}

	private void renderRuntimeState() {
		if (!ImGui.begin("Runtime State")) { ImGui.end(); return; }
		var id = session.selection().snapshot().primaryObject();
		if (id == null) ImGui.textDisabled("No architectural object selected");
		else session.readModel().object(id).ifPresentOrElse(view -> view.runtimeState().values().forEach((key,value) -> {
			ImGui.textDisabled(key); ImGui.sameLine(180); ImGui.text(String.valueOf(value));
		}), () -> ImGui.textDisabled("Replica unavailable"));
		ImGui.end();
	}

	private void renderHistory() {
		if (!ImGui.begin("Command History")) { ImGui.end(); return; }
		if (ImGui.beginTabBar("HistoryTabs")) {
			if (ImGui.beginTabItem("Design")) { session.history().designCommands().forEach(this::renderHistoryEntry); ImGui.endTabItem(); }
			if (ImGui.beginTabItem("Runtime")) { session.history().runtimeEvents().forEach(this::renderHistoryEntry); ImGui.endTabItem(); }
			if (ImGui.beginTabItem("Rejected")) { session.history().rejectedCommands().forEach(this::renderHistoryEntry); ImGui.endTabItem(); }
			ImGui.endTabBar();
		}
		ImGui.end();
	}

	private void renderHistoryEntry(EditorHistoryEntry entry) {
		ImGui.bulletText(entry.summary() + "  r" + entry.revision() + "  " + entry.result());
	}

	private void renderDiagnostics() {
		if (!ImGui.begin("Diagnostics")) { ImGui.end(); return; }
		for (EditorDiagnostic diagnostic : session.diagnostics().all()) {
			float[] color = diagnostic.severity() == EditorDiagnostic.Severity.ERROR ? new float[]{0.95f,0.3f,0.3f,1}
				: diagnostic.severity() == EditorDiagnostic.Severity.WARNING ? new float[]{0.95f,0.7f,0.2f,1}
				: new float[]{0.65f,0.75f,0.9f,1};
			ImGui.textColored(color[0],color[1],color[2],color[3],diagnostic.code() + ": " + diagnostic.message());
		}
		ImGui.end();
	}

	private static String formatParameter(ParameterValue value) {
		return switch (value) {
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
