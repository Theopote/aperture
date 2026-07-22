package dev.aperture.editor.model.session;

/** Frontend-neutral gateway to tools implemented by the host environment. */
public interface ToolController {
	enum Tool { SELECT, PLACE, MOVE, ROTATE, RESIZE, ATTACH, MEASURE }

	void cancelActiveTool();

	default Tool activeTool() { return Tool.SELECT; }

	default boolean activate(Tool tool) { return tool == Tool.SELECT; }

	default boolean available(Tool tool) { return tool == Tool.SELECT; }

	default String disabledReason(Tool tool) { return "Tool transport is not connected in this build"; }

	default String hint() { return "Click to select   |   Esc to cancel"; }
}
