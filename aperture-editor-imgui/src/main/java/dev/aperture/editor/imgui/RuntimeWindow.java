package dev.aperture.editor.imgui;

import dev.aperture.editor.model.command.ExpectedRevision;
import dev.aperture.editor.model.read.ObjectEditorView;
import dev.aperture.editor.model.read.RuntimeActionDescriptor;
import imgui.ImGui;
import imgui.flag.ImGuiCol;

final class RuntimeWindow {
	private final ApertureUiContext context;

	RuntimeWindow(ApertureUiContext context) { this.context = context; }

	void render() {
		if (!ImGui.begin("Context Runtime")) { ImGui.end(); return; }
		var selection = context.session.selection().snapshot();
		if (selection.objectIds().isEmpty()) empty("No runtime context", "Select an object to inspect its live state and capabilities.");
		else if (selection.objectIds().size() > 1) empty(selection.objectIds().size() + " objects selected", "Runtime actions require a single object.");
		else context.session.readModel().object(selection.primaryObject()).ifPresentOrElse(this::renderObject,
			() -> empty("Runtime unavailable", "The selected replica has not been synchronized."));
		ImGui.end();
	}

	private void renderObject(ObjectEditorView view) {
		ImGui.text(view.displayName());
		ImGui.sameLine();
		ImGui.textColored(ApertureStyle.RUNTIME[0], ApertureStyle.RUNTIME[1], ApertureStyle.RUNTIME[2], 1, "LIVE");
		ImGui.textDisabled("State revision " + view.stateRevision());
		ImGui.separator();
		if (view.runtimeState().values().isEmpty()) ImGui.textDisabled("This object exposes no runtime state.");
		else {
			ImGui.textDisabled("CURRENT STATE");
			view.runtimeState().values().forEach((key, value) -> stateRow(key, value));
		}
		if (!view.runtimeActions().isEmpty()) {
			ImGui.separator();
			renderActions(view);
		}
	}

	private void renderActions(ObjectEditorView view) {
		String currentGroup = null;
		boolean firstInGroup = true;
		for (RuntimeActionDescriptor action : view.runtimeActions()) {
			if (!action.group().equals(currentGroup)) {
				currentGroup = action.group();
				firstInGroup = true;
				ImGui.textDisabled(currentGroup.toUpperCase());
			}
			if (!firstInGroup && ImGui.getContentRegionAvailX() > 130) ImGui.sameLine();
			firstInGroup = false;
			renderAction(view, action);
		}
	}

	private void renderAction(ObjectEditorView view, RuntimeActionDescriptor action) {
		boolean disabled = !action.enabled() || action.pending();
		if (action.severity() == RuntimeActionDescriptor.Severity.DANGER) {
			ImGui.pushStyleColor(ImGuiCol.Button, ApertureStyle.ERROR[0] * .65f, ApertureStyle.ERROR[1] * .65f, ApertureStyle.ERROR[2] * .65f, 1);
		}
		if (disabled) ImGui.beginDisabled();
		String label = (action.icon().isBlank() ? "" : action.icon() + " ") + action.label()
			+ (action.pending() ? "..." : "") + "##" + action.id();
		boolean pressed = ImGui.button(label);
		if (disabled) ImGui.endDisabled();
		if (action.severity() == RuntimeActionDescriptor.Severity.DANGER) ImGui.popStyleColor();
		if (ImGui.isItemHovered()) {
			String tooltip = action.pending() ? "Command pending"
				: !action.enabled() && !action.disabledReason().isBlank() ? action.disabledReason() : action.tooltip();
			if (!tooltip.isBlank()) ImGui.setTooltip(tooltip);
		}
		if (pressed) context.session.commands().submitRuntimeAction(view.objectId(), action.id(),
			new ExpectedRevision(view.objectRevision(), view.stateRevision()));
	}

	private static void stateRow(String key, Object value) {
		ImGui.textDisabled(humanize(key));
		ImGui.sameLine(ApertureStyle.LABEL_WIDTH);
		String display = String.valueOf(value);
		if (value instanceof Boolean flag) {
			float[] color = flag ? ApertureStyle.SUCCESS : ApertureStyle.WARNING;
			ImGui.textColored(color[0], color[1], color[2], 1, flag ? "Yes" : "No");
		} else ImGui.text(display);
	}

	private static String humanize(String key) {
		if (key == null || key.isBlank()) return "State";
		String spaced = key.replace('_', ' ').replace('-', ' ');
		return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
	}

	private static void empty(String title, String detail) {
		ImGui.text(title);
		ImGui.textDisabled(detail);
	}
}
