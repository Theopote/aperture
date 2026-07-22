package dev.aperture.editor.imgui;

import imgui.ImGui;

final class ApertureMainMenuBar {
	private final ApertureUiContext c;private final Runnable reset;
	ApertureMainMenuBar(ApertureUiContext c,Runnable reset){this.c=c;this.reset=reset;}
	void render(){if(!ImGui.beginMenuBar())return;file();edit();design();analyze();run();view();help();ImGui.endMenuBar();}
	private void file(){if(!ImGui.beginMenu("File"))return;items("New Aperture Project","Open Project","Save");ImGui.separator();items("Import Definition","Export Selection","Project Settings");ImGui.endMenu();}
	private void edit(){if(!ImGui.beginMenu("Edit"))return;if(ImGui.menuItem("Undo","Ctrl+Z"))c.session.commands().undo(c.session.history());if(ImGui.menuItem("Redo","Ctrl+Shift+Z"))c.session.commands().redo(c.session.history());ImGui.separator();items("Duplicate","Delete");if(ImGui.menuItem("Select All","Ctrl+A")){c.session.selection().clear();c.session.readModel().visibleObjects().forEach(o->c.session.selection().addObject(o.objectId()));}items("Preferences");ImGui.endMenu();}
	private void design(){if(!ImGui.beginMenu("Design"))return;items("Place Object","Edit Parameters","Transform","Attach to Host","Detach","Replace Type");ImGui.separator();items("Systems","Behaviors","Runtime State","Connections","Zones","Sensors","Automation");ImGui.endMenu();}
	private void analyze(){if(!ImGui.beginMenu("Analyze"))return;items("Validate","Constraints","Collision","Accessibility","Circulation","Simulation");ImGui.endMenu();}
	private void run(){if(!ImGui.beginMenu("Run"))return;if(ImGui.menuItem("Enter Runtime Mode"))c.mode=ApertureUiContext.Mode.RUNTIME;items("Pause Runtime","Reset State","Inspect Events");ImGui.endMenu();}
	private void view(){if(!ImGui.beginMenu("View"))return;toggle("Project Navigator","navigator");toggle("Inspector","inspector");toggle("Context Runtime","runtime");toggle("Activity Console","activity");ImGui.separator();if(ImGui.menuItem("Reset Dock Layout"))reset.run();ImGui.endMenu();}
	private void help(){if(!ImGui.beginMenu("Help"))return;items("Getting Started","Keyboard Shortcuts","Architecture Object Reference","Diagnostics Guide","About Aperture");ImGui.endMenu();}
	private void toggle(String label,String id){boolean on=c.session.workspace().windowVisibility().getOrDefault(id,true);if(ImGui.menuItem(label,"",on))c.session.workspace().setWindowVisible(id,!on);}
	private static void items(String...labels){for(String label:labels)ApertureStyle.unavailable(label);}
}
