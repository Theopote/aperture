package dev.aperture.editor.imgui;

import dev.aperture.editor.model.history.EditorHistoryEntry;
import dev.aperture.editor.model.command.ExpectedRevision;
import dev.aperture.editor.model.inspector.PropertyDescriptor;
import dev.aperture.editor.model.inspector.InspectorSection;
import dev.aperture.editor.model.read.EditorDiagnostic;
import dev.aperture.editor.model.session.EditorSession;
import dev.aperture.parameter.ParameterValue;
import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.internal.flag.ImGuiDockNodeFlags;

import java.util.Objects;

import dev.aperture.editor.imgui.input.EditorInputPolicy;
import dev.aperture.editor.imgui.input.ShortcutDispatcher;

/** Concrete Dear ImGui implementation of the K2.3 editor shell. */
public final class DearImGuiEditor {
	private static final String DOCKSPACE_ID = "ApertureMainDockspace";
	private final EditorSession session;
	private final DearImGuiPropertyEditor propertyEditor;
	private boolean defaultLayoutPending;
	private final ShortcutDispatcher shortcuts = new ShortcutDispatcher();

	public DearImGuiEditor(EditorSession session) {
		this(session, true);
	}

	public DearImGuiEditor(EditorSession session, boolean defaultLayoutPending) {
		this.session = Objects.requireNonNull(session, "session");
		this.propertyEditor = new DearImGuiPropertyEditor(session);
		this.defaultLayoutPending = defaultLayoutPending;
		configureShortcuts();
	}

	public void render() {
		handleShortcuts();
		renderDockspace();
		var visibility = session.workspace().windowVisibility();
		if (visibility.getOrDefault("outliner", true)) renderOutliner();
		if (visibility.getOrDefault("inspector", true)) renderInspector();
		if (visibility.getOrDefault("runtime", true)) renderRuntimeState();
		if (visibility.getOrDefault("history", true)) renderHistory();
		if (visibility.getOrDefault("diagnostics", true)) renderDiagnostics();
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
			| ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.MenuBar;
		ImGui.begin("Aperture Editor##Root", flags);
		renderMenuBar();
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

	private void renderMenuBar() {
		if (!ImGui.beginMenuBar()) return;
		if (ImGui.beginMenu("View")) {
			toggleWindow("Object Outliner", "outliner");
			toggleWindow("Inspector", "inspector");
			toggleWindow("Runtime State", "runtime");
			toggleWindow("Command History", "history");
			toggleWindow("Diagnostics", "diagnostics");
			ImGui.separator();
			if (ImGui.menuItem("Reset Dock Layout")) {
				session.workspace().reset();
				defaultLayoutPending = true;
			}
			ImGui.endMenu();
		}
		ImGui.endMenuBar();
	}

	private void toggleWindow(String label, String id) {
		boolean visible = session.workspace().windowVisibility().getOrDefault(id, true);
		if (ImGui.menuItem(label, "", visible)) session.workspace().setWindowVisible(id, !visible);
	}

	private void configureShortcuts() {
		shortcuts.bind("ESCAPE", () -> { propertyEditor.cancel(); session.tools().cancelActiveTool(); });
		shortcuts.bind("CTRL+A", () -> {
			session.selection().clear();
			session.readModel().visibleObjects().forEach(object -> session.selection().addObject(object.objectId()));
		});
		shortcuts.bind("CTRL+Z", () -> session.commands().undo(session.history()));
		shortcuts.bind("CTRL+SHIFT+Z", () -> session.commands().redo(session.history()));
		shortcuts.bind("CTRL+Y", () -> shortcuts.dispatch("CTRL+SHIFT+Z", new EditorInputPolicy(false, false, false)));
	}

	private void handleShortcuts() {
		var io = ImGui.getIO();
		var policy = new EditorInputPolicy(io.getWantCaptureMouse(), io.getWantCaptureKeyboard(), io.getWantTextInput());
		if (ImGui.isKeyPressed(ImGuiKey.Escape, false)) shortcuts.dispatch("ESCAPE", policy);
		if (io.getKeyCtrl() && ImGui.isKeyPressed(ImGuiKey.A, false)) shortcuts.dispatch("CTRL+A", policy);
		if (io.getKeyCtrl() && io.getKeyShift() && ImGui.isKeyPressed(ImGuiKey.Z, false)) shortcuts.dispatch("CTRL+SHIFT+Z", policy);
		else if (io.getKeyCtrl() && ImGui.isKeyPressed(ImGuiKey.Z, false)) shortcuts.dispatch("CTRL+Z", policy);
		if (io.getKeyCtrl() && ImGui.isKeyPressed(ImGuiKey.Y, false)) shortcuts.dispatch("CTRL+Y", policy);
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
			if (ImGui.collapsingHeader(section.label())) for (PropertyDescriptor property : section.properties()) renderProperty(selection.primaryObject(), property);
		}
		ImGui.end();
	}

	private void renderProperty(dev.aperture.runtime.model.object.ArchitecturalObjectId objectId, PropertyDescriptor property) {
		ImGui.textDisabled(property.displayName()); ImGui.sameLine(180);
		Object value = property.value();
		if (value instanceof ParameterValue parameter && propertyEditor.render(objectId, property, parameter)) return;
		String display = value instanceof ParameterValue parameter ? formatParameter(parameter) : String.valueOf(value);
		ImGui.text(display + (property.unit().isBlank() ? "" : " " + property.unit()));
	}

	private void renderRuntimeState() {
		if (!ImGui.begin("Runtime State")) { ImGui.end(); return; }
		var id = session.selection().snapshot().primaryObject();
		if (id == null) ImGui.textDisabled("No architectural object selected");
		else session.readModel().object(id).ifPresentOrElse(view -> {
			view.runtimeState().values().forEach((key,value) -> {
				ImGui.textDisabled(key); ImGui.sameLine(180); ImGui.text(String.valueOf(value));
			});
			if (!view.runtimeActions().isEmpty()) {
				ImGui.separator(); ImGui.textDisabled("Actions");
				String group=null;
				for (var action : view.runtimeActions()) {
					if(!action.group().equals(group)){if(group!=null)ImGui.newLine();group=action.group();ImGui.textDisabled(group);}
					boolean disabled=!action.enabled()||action.pending();if(disabled)ImGui.beginDisabled();
					String prefix=action.icon().isBlank()?"":action.icon()+" ";boolean pressed=ImGui.button(prefix+action.label()+"##"+action.id());
					if(disabled)ImGui.endDisabled();
					if(ImGui.isItemHovered()){String tip=action.pending()?"Command pending":!action.enabled()&&!action.disabledReason().isBlank()?action.disabledReason():action.tooltip();if(!tip.isBlank())ImGui.setTooltip(tip);}
					if(pressed)submitRuntimeAction(view,action.id());
					if(ImGui.getContentRegionAvailX()>120)ImGui.sameLine();else ImGui.newLine();
				}
				ImGui.newLine();
			}
		}, () -> ImGui.textDisabled("Replica unavailable"));
		ImGui.end();
	}

	private void submitRuntimeAction(dev.aperture.editor.model.read.ObjectEditorView view, String action) {
		session.commands().submitRuntimeAction(view.objectId(), action,
			new ExpectedRevision(view.objectRevision(), view.stateRevision()));
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
