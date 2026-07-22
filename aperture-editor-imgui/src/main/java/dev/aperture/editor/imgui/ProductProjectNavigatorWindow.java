package dev.aperture.editor.imgui;

import dev.aperture.editor.model.command.ExpectedRevision;
import dev.aperture.editor.model.read.ObjectSummary;
import dev.aperture.editor.model.read.ObjectEditorView;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;

import java.util.*;

/** Project browser organized around inventory, location semantics and actionable issues. */
final class ProductProjectNavigatorWindow {
	private final ApertureUiContext context;
	private final ImString search = new ImString(96);
	private final ImBoolean warningsOnly = new ImBoolean(false);
	private String familyFilter = "All families";
	private String typeFilter = "All types";
	private boolean expandSelected;

	ProductProjectNavigatorWindow(ApertureUiContext context) { this.context = context; }

	void render() {
		NavigatorInteractionState.beginFrame();
		if (!ImGui.begin("Project Navigator")) { ImGui.end(); return; }
		ImGui.setNextItemWidth(-1);
		ImGui.inputText("Search objects...##navigator-search", search);
		List<Node> all = nodes();
		filters(all);
		ImGui.checkbox("Issues only", warningsOnly);
		ImGui.sameLine();
		if (ImGui.smallButton("Expand Selected")) expandSelected = true;
		ImGui.separator();
		List<Node> visible = all.stream().filter(this::visible).toList();
		if (visible.isEmpty()) ImGui.textDisabled(all.isEmpty() ? "No architectural objects in this world" : "No objects match the current filters");
		else tree(visible);
		expandSelected = false;
		ImGui.end();
	}

	private List<Node> nodes() {
		return context.session.readModel().visibleObjects().stream().map(summary -> context.session.readModel()
			.object(summary.objectId()).map(view -> new Node(summary, view, family(view), type(summary))).orElse(null))
			.filter(Objects::nonNull).sorted(Comparator.comparing(node -> node.summary().displayName())).toList();
	}

	private void filters(List<Node> nodes) {
		List<String> families = nodes.stream().map(Node::family).distinct().sorted().toList();
		ImGui.setNextItemWidth((ImGui.getContentRegionAvailX() - 8) * .5f);
		if (ImGui.beginCombo("##family-filter", familyFilter)) {
			if (ImGui.selectable("All families", familyFilter.equals("All families"))) familyFilter = "All families";
			for (String family : families) if (ImGui.selectable(family, family.equals(familyFilter))) familyFilter = family;
			ImGui.endCombo();
		}
		ImGui.sameLine();
		List<String> types = nodes.stream().filter(node -> familyFilter.equals("All families") || node.family().equals(familyFilter))
			.map(Node::type).distinct().sorted().toList();
		if (!typeFilter.equals("All types") && !types.contains(typeFilter)) typeFilter = "All types";
		ImGui.setNextItemWidth(-1);
		if (ImGui.beginCombo("##type-filter", typeFilter)) {
			if (ImGui.selectable("All types", typeFilter.equals("All types"))) typeFilter = "All types";
			for (String type : types) if (ImGui.selectable(type, type.equals(typeFilter))) typeFilter = type;
			ImGui.endCombo();
		}
	}

	private void tree(List<Node> nodes) {
		Set<ArchitecturalObjectId> selected = context.session.selection().snapshot().objectIds();
		if (expandSelected && nodes.stream().anyMatch(node -> selected.contains(node.summary().objectId()))) ImGui.setNextItemOpen(true);
		if (!ImGui.treeNode("Project  (" + nodes.size() + ")##navigator-root")) return;
		Map<String, List<Node>> families = new TreeMap<>();
		nodes.forEach(node -> families.computeIfAbsent(node.family(), ignored -> new ArrayList<>()).add(node));
		families.forEach((family, familyNodes) -> family(family, familyNodes, selected));
		ImGui.treePop();
	}

	private void family(String family, List<Node> nodes, Set<ArchitecturalObjectId> selected) {
		if (expandSelected && nodes.stream().anyMatch(node -> selected.contains(node.summary().objectId()))) ImGui.setNextItemOpen(true);
		if (!ImGui.treeNode(family + "  (" + nodes.size() + ")##family:" + family)) return;
		Map<String, List<Node>> types = new TreeMap<>();
		nodes.forEach(node -> types.computeIfAbsent(node.type(), ignored -> new ArrayList<>()).add(node));
		types.forEach((type, typeNodes) -> type(type, typeNodes, selected));
		ImGui.treePop();
	}

	private void type(String type, List<Node> nodes, Set<ArchitecturalObjectId> selected) {
		if (expandSelected && nodes.stream().anyMatch(node -> selected.contains(node.summary().objectId()))) ImGui.setNextItemOpen(true);
		if (!ImGui.treeNode(type + "  (" + nodes.size() + ")##type:" + type)) return;
		for (Node node : nodes) object(node, selected);
		ImGui.treePop();
	}

	private void object(Node node, Set<ArchitecturalObjectId> selected) {
		ObjectSummary summary = node.summary();
		String status = summary.warning() ? "  !" : summary.syncStatus() == dev.aperture.editor.model.read.SyncStatus.SYNCHRONIZED ? "" : "  ~";
		if (ImGui.selectable(summary.displayName() + status + "##" + summary.objectId(), selected.contains(summary.objectId()))) {
			if (ImGui.getIO().getKeyCtrl()) {
				if (selected.contains(summary.objectId())) context.session.selection().removeObject(summary.objectId());
				else context.session.selection().addObject(summary.objectId());
			} else context.session.selection().selectObject(summary.objectId());
		}
		if (ImGui.isItemHovered()) {
			NavigatorInteractionState.hover(summary.objectId());
			if (ImGui.isMouseDoubleClicked(0)) focus(summary.objectId());
			var origin = node.view().transform().origin();
			ImGui.setTooltip(summary.typeId() + "\n" + summary.syncStatus() + "\nPosition  "
				+ Math.round(origin.x()) + ", " + Math.round(origin.y()) + ", " + Math.round(origin.z()) + " mm");
		}
		contextMenu(node);
	}

	private void contextMenu(Node node) {
		if (!ImGui.beginPopupContextItem("object-actions##" + node.summary().objectId())) return;
		if (ImGui.menuItem("Focus")) focus(node.summary().objectId());
		if (ImGui.menuItem("Copy ID")) ImGui.setClipboardText(node.summary().objectId().toString());
		var lock = node.view().runtimeActions().stream().filter(action -> action.id().equals("set_locked") || action.id().equals("set_unlocked")).findFirst();
		if (lock.isPresent()) {
			var action = lock.orElseThrow();
			ImGui.beginDisabled(!action.enabled() || action.pending());
			try {
				if (ImGui.menuItem(action.label())) context.session.commands().submitRuntimeAction(node.summary().objectId(), action.id(),
					new ExpectedRevision(node.view().objectRevision(), node.view().stateRevision()));
			} finally { ImGui.endDisabled(); }
			if (ImGui.isItemHovered() && !action.enabled()) ImGui.setTooltip(action.disabledReason());
		}
		disabled("Rename", "Rename command is not available in the authoritative protocol");
		disabled("Delete", "Delete command is not available in the authoritative protocol");
		ImGui.endPopup();
	}

	private void focus(ArchitecturalObjectId id) {
		context.session.selection().selectObject(id);
		NavigatorFocusRequests.publish(id);
	}

	private boolean visible(Node node) {
		String query = search.get().trim().toLowerCase(Locale.ROOT);
		return (query.isBlank() || (node.summary().displayName() + " " + node.summary().objectId() + " " + node.family() + " " + node.type())
			.toLowerCase(Locale.ROOT).contains(query))
			&& (familyFilter.equals("All families") || node.family().equals(familyFilter))
			&& (typeFilter.equals("All types") || node.type().equals(typeFilter))
			&& (!warningsOnly.get() || node.summary().warning());
	}

	private static void disabled(String label, String reason) {
		ImGui.beginDisabled();
		try { ImGui.menuItem(label); } finally { ImGui.endDisabled(); }
		if (ImGui.isItemHovered()) ImGui.setTooltip(reason);
	}

	private static String family(ObjectEditorView view) {
		String id = view.familyId().toString().toLowerCase(Locale.ROOT);
		if (id.contains("opening")) return "Openings";
		if (id.contains("struct")) return "Structural";
		if (id.contains("equipment")) return "Equipment";
		if (id.contains("system")) return "Systems";
		return "Unassigned";
	}

	private static String type(ObjectSummary summary) {
		String path = summary.typeId().path().replace('_', ' ');
		return Character.toUpperCase(path.charAt(0)) + path.substring(1);
	}

	private record Node(ObjectSummary summary, ObjectEditorView view, String family, String type) { }
}
