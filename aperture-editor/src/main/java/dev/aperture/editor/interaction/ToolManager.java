package dev.aperture.editor.interaction;

import dev.aperture.editor.model.session.ToolController;

import java.util.EnumMap;
import java.util.Optional;

/** Owns tool instances and guarantees cancel/deactivate ordering on every transition. */
public final class ToolManager {
	private final EnumMap<ToolController.Tool, EditorTool> tools = new EnumMap<>(ToolController.Tool.class);
	private EditorTool active;

	public ToolManager(EditorTool... tools) {
		for (EditorTool tool : tools) {
			if (this.tools.put(tool.id(), tool) != null) {
				throw new IllegalArgumentException("Duplicate editor tool: " + tool.id());
			}
		}
	}

	public boolean activate(ToolController.Tool id) {
		EditorTool next = tools.get(id);
		if (next == null) return false;
		if (next == active) return true;
		stopActive();
		active = next;
		active.activate();
		return true;
	}

	public void update(EditorInputFrame input) {
		if (active != null && input.worldInputAllowed()) active.update(input);
	}

	public void cancelActive() {
		stopActive();
	}

	public Optional<EditorTool> activeTool() {
		return Optional.ofNullable(active);
	}

	private void stopActive() {
		if (active == null) return;
		active.cancel();
		active.deactivate();
		active = null;
	}
}
