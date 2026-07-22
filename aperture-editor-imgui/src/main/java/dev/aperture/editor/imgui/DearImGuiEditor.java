package dev.aperture.editor.imgui;

import dev.aperture.editor.imgui.input.EditorInputPolicy;
import dev.aperture.editor.imgui.input.ShortcutDispatcher;
import dev.aperture.editor.model.session.EditorSession;
import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.internal.flag.ImGuiDockNodeFlags;

import java.util.Objects;

/** Product workspace orchestrator. Individual windows own their presentation. */
public final class DearImGuiEditor {
	public static final int LAYOUT_VERSION = 2;
	private static final String DOCKSPACE_ID = "ApertureMainDockspace";
	private final ApertureUiContext context;
	private final InspectorWindow inspector;
	private final ProjectNavigatorWindow navigator;
	private final RuntimeWindow runtime;
	private final ActivityConsoleWindow activity;
	private final ApertureToolbar toolbar;
	private final ApertureMainMenuBar menu;
	private final EditorStatusBar statusBar;
	private final ViewportOverlay viewportOverlay;
	private final ShortcutDispatcher shortcuts = new ShortcutDispatcher();
	private boolean defaultLayoutPending;

	public DearImGuiEditor(EditorSession session) { this(session, true); }

	public DearImGuiEditor(EditorSession session, boolean defaultLayoutPending) {
		this.context = new ApertureUiContext(Objects.requireNonNull(session, "session"));
		this.inspector = new InspectorWindow(context, new DearImGuiPropertyEditor(session));
		this.navigator = new ProjectNavigatorWindow(context);
		this.runtime = new RuntimeWindow(context);
		this.activity = new ActivityConsoleWindow(context);
		this.toolbar = new ApertureToolbar(context);
		this.menu = new ApertureMainMenuBar(context, this::resetLayout);
		this.statusBar = new EditorStatusBar(context);
		this.viewportOverlay = new ViewportOverlay(context);
		this.defaultLayoutPending = defaultLayoutPending;
		configureShortcuts();
	}

	public void render() {
		ApertureStyle.push();
		try {
			handleShortcuts();
			int dockspace = renderDockspace();
			var visibility = context.session.workspace().windowVisibility();
			if (visibility.getOrDefault("navigator", true)) navigator.render();
			if (visibility.getOrDefault("inspector", true)) inspector.render();
			if (visibility.getOrDefault("runtime", true)) runtime.render();
			if (visibility.getOrDefault("activity", true)) activity.render();
			viewportOverlay.render(dockspace);
			statusBar.render();
		} finally {
			ApertureStyle.pop();
		}
	}

	private int renderDockspace() {
		ImGuiViewport viewport = ImGui.getMainViewport();
		ImGui.setNextWindowPos(viewport.getWorkPosX(), viewport.getWorkPosY());
		ImGui.setNextWindowSize(viewport.getWorkSizeX(), viewport.getWorkSizeY() - ApertureStyle.STATUS_BAR_HEIGHT);
		ImGui.setNextWindowViewport(viewport.getID());
		ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0);
		ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0);
		ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);
		ImGui.pushStyleColor(ImGuiCol.WindowBg, 0, 0, 0, 0);
		int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize
			| ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoNavFocus
			| ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.MenuBar;
		ImGui.begin("Aperture Editor##Root", flags);
		menu.render();
		toolbar.render();
		int dockspace = ImGui.getID(DOCKSPACE_ID);
		if (defaultLayoutPending) {
			DearImGuiLayout.buildDefault(dockspace, ImGui.getContentRegionAvailX(), ImGui.getContentRegionAvailY());
			defaultLayoutPending = false;
		}
		ImGui.dockSpace(dockspace, 0, 0, ImGuiDockNodeFlags.PassthruCentralNode);
		ImGui.end();
		ImGui.popStyleColor();
		ImGui.popStyleVar(3);
		return dockspace;
	}

	private void resetLayout() {
		context.session.workspace().reset();
		defaultLayoutPending = true;
	}

	private void configureShortcuts() {
		shortcuts.bind("ESCAPE", () -> { inspector.cancelEdit(); context.session.tools().cancelActiveTool(); });
		shortcuts.bind("CTRL+A", () -> {
			context.session.selection().clear();
			context.session.readModel().visibleObjects().forEach(object -> context.session.selection().addObject(object.objectId()));
		});
		shortcuts.bind("CTRL+Z", () -> context.session.commands().undo(context.session.history()));
		shortcuts.bind("CTRL+SHIFT+Z", () -> context.session.commands().redo(context.session.history()));
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
}
