package dev.aperture.editor.interaction;

import dev.aperture.editor.model.session.ToolController;

/** One instance-owned world interaction tool. */
public interface EditorTool {
	ToolController.Tool id();

	default void activate() { }

	void update(EditorInputFrame input);

	default void cancel() { }

	default void deactivate() { }
}
