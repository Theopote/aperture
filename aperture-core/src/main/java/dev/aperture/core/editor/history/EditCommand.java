package dev.aperture.core.editor.history;

import dev.aperture.core.editor.session.EditorContext;

/**
 * One undoable edit operation in the editor history stack.
 */
public interface EditCommand {
	String description();

	EditResult execute(EditorContext context);

	EditResult undo(EditorContext context);
}
