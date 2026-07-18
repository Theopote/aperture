package dev.aperture.editor.imgui.workspace;

import imgui.ImGui;
import imgui.flag.ImGuiDockNodeFlags;
import imgui.flag.ImGuiWindowFlags;

/** Full-viewport root dockspace. Individual windows remain registered in MainDockspace. */
public final class DearImGuiDockspaceRenderer implements EditorWindow {
	private final MainDockspace content;
	public DearImGuiDockspaceRenderer(MainDockspace content){this.content=content;}
	@Override public void render(){int flags=ImGuiWindowFlags.NoDocking|ImGuiWindowFlags.NoTitleBar|ImGuiWindowFlags.NoCollapse|ImGuiWindowFlags.NoResize|ImGuiWindowFlags.NoMove|ImGuiWindowFlags.NoBringToFrontOnFocus|ImGuiWindowFlags.NoNavFocus;var viewport=ImGui.getMainViewport();ImGui.setNextWindowPos(viewport.getPosX(),viewport.getPosY());ImGui.setNextWindowSize(viewport.getSizeX(),viewport.getSizeY());ImGui.setNextWindowViewport(viewport.getID());ImGui.begin("Aperture Editor Dockspace",flags);ImGui.dockSpace(ImGui.getID("ApertureMainDockspace"),0,0,ImGuiDockNodeFlags.PassthruCentralNode);content.render();ImGui.end();}
}
