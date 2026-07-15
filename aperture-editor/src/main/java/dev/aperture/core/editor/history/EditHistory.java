package dev.aperture.core.editor.history;

import dev.aperture.core.editor.session.EditorContext;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Undo / redo stack for editor commands.
 */
public final class EditHistory {
	private final Deque<EditCommand> undoStack = new ArrayDeque<>();
	private final Deque<EditCommand> redoStack = new ArrayDeque<>();
	private final int maxDepth;

	public EditHistory() {
		this(100);
	}

	public EditHistory(int maxDepth) {
		if (maxDepth < 1) {
			throw new IllegalArgumentException("maxDepth must be >= 1");
		}
		this.maxDepth = maxDepth;
	}

	public EditResult execute(EditCommand command, EditorContext context) {
		EditResult result = command.execute(context);
		if (!result.success()) {
			return result;
		}
		undoStack.push(command);
		trim(undoStack);
		redoStack.clear();
		return result;
	}

	public EditResult undo(EditorContext context) {
		if (undoStack.isEmpty()) {
			return EditResult.failed("Nothing to undo", "history.empty", "Undo stack is empty");
		}
		EditCommand command = undoStack.pop();
		EditResult result = command.undo(context);
		if (result.success()) {
			redoStack.push(command);
		} else {
			undoStack.push(command);
		}
		return result;
	}

	public EditResult redo(EditorContext context) {
		if (redoStack.isEmpty()) {
			return EditResult.failed("Nothing to redo", "history.empty", "Redo stack is empty");
		}
		EditCommand command = redoStack.pop();
		EditResult result = command.execute(context);
		if (result.success()) {
			undoStack.push(command);
			trim(undoStack);
		} else {
			redoStack.push(command);
		}
		return result;
	}

	public boolean canUndo() {
		return !undoStack.isEmpty();
	}

	public boolean canRedo() {
		return !redoStack.isEmpty();
	}

	private void trim(Deque<EditCommand> stack) {
		while (stack.size() > maxDepth) {
			stack.removeLast();
		}
	}
}
