package dev.aperture.editor.imgui;
import imgui.ImGui;
final class ProjectNavigatorWindow{
	private final ApertureUiContext c;ProjectNavigatorWindow(ApertureUiContext c){this.c=c;}
	void render(){if(!ImGui.begin("Project Navigator")){ImGui.end();return;}if(ImGui.beginTabBar("NavigatorTabs")){if(ImGui.beginTabItem("Objects")){objects();ImGui.endTabItem();}tab("Levels","Spatial levels are not available yet");tab("Spaces","Space classification is not available yet");tab("Systems","Access Control\nLighting\nVentilation\nCirculation\nAutomation");tab("Assets","Project assets are not available yet");ImGui.endTabBar();}ImGui.end();}
	private void objects(){var selected=c.session.selection().snapshot().objectIds();if(ImGui.treeNode("Project")){if(ImGui.treeNode("Unassigned Objects")){for(var o:c.session.readModel().visibleObjects()){String label=o.displayName()+"  ["+o.typeId()+"]"+(o.warning()?"  !":"")+"##"+o.objectId();if(ImGui.selectable(label,selected.contains(o.objectId()))){if(ImGui.getIO().getKeyCtrl())c.session.selection().addObject(o.objectId());else c.session.selection().selectObject(o.objectId());}}ImGui.treePop();}ImGui.treePop();}}
	private static void tab(String title,String text){if(ImGui.beginTabItem(title)){ImGui.textDisabled(text);ImGui.endTabItem();}}
}
