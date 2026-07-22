package dev.aperture.editor.imgui;

import dev.aperture.editor.model.read.ObjectSummary;
import imgui.ImGui;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

final class ProjectNavigatorWindow {
	private final ApertureUiContext context;

	ProjectNavigatorWindow(ApertureUiContext context) { this.context = context; }

	void render() {
		if (!ImGui.begin("Project Navigator")) { ImGui.end(); return; }
		if (ImGui.beginTabBar("NavigatorTabs")) {
			if (ImGui.beginTabItem("Objects")) { objects(); ImGui.endTabItem(); }
			placeholder("Levels", "Spatial levels are not available yet");
			placeholder("Spaces", "Space classification is not available yet");
			if (ImGui.beginTabItem("Systems")) { systems(); ImGui.endTabItem(); }
			placeholder("Assets", "Project assets are not available yet");
			ImGui.endTabBar();
		}
		ImGui.end();
	}

	private void objects() {
		var visible = context.session.readModel().visibleObjects();
		if (visible.isEmpty()) {
			ImGui.textDisabled("No architectural objects in this world");
			return;
		}
		Map<String, Map<String, ArrayList<ObjectSummary>>> families = new TreeMap<>();
		for (ObjectSummary object : visible) {
			String family = context.session.readModel().object(object.objectId())
				.map(view -> view.familyId().toString()).orElse("Unassigned");
			families.computeIfAbsent(family, ignored -> new TreeMap<>())
				.computeIfAbsent(object.typeId().toString(), ignored -> new ArrayList<>()).add(object);
		}
		if (ImGui.treeNode("Project##NavigatorRoot")) {
			families.forEach(this::family);
			ImGui.treePop();
		}
	}

	private void family(String family, Map<String, ArrayList<ObjectSummary>> types) {
		int count = types.values().stream().mapToInt(ArrayList::size).sum();
		if (!ImGui.treeNode(family + "  (" + count + ")##family:" + family)) return;
		types.forEach(this::type);
		ImGui.treePop();
	}

	private void type(String type, ArrayList<ObjectSummary> objects) {
		if (!ImGui.treeNode(type + "  (" + objects.size() + ")##type:" + type)) return;
		objects.sort(java.util.Comparator.comparing(ObjectSummary::displayName));
		var selected = context.session.selection().snapshot().objectIds();
		for (ObjectSummary object : objects) {
			String label = object.displayName() + (object.warning() ? "  !" : "") + "##" + object.objectId();
			if (ImGui.selectable(label, selected.contains(object.objectId()))) {
				if (ImGui.getIO().getKeyCtrl()) context.session.selection().addObject(object.objectId());
				else context.session.selection().selectObject(object.objectId());
			}
			if (ImGui.isItemHovered()) ImGui.setTooltip(object.typeId().toString());
		}
		ImGui.treePop();
	}

	private static void systems() {
		ImGui.textDisabled("BUILDING SYSTEMS");
		ImGui.bulletText("Access Control");
		ImGui.bulletText("Lighting");
		ImGui.bulletText("Ventilation");
		ImGui.bulletText("Circulation");
		ImGui.bulletText("Automation");
		ImGui.textDisabled("System membership is not connected yet");
	}

	private static void placeholder(String title, String text) {
		if (ImGui.beginTabItem(title)) {
			ImGui.textDisabled(text);
			ImGui.endTabItem();
		}
	}
}
