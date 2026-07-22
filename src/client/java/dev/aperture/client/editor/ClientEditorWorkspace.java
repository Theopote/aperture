package dev.aperture.client.editor;

import dev.aperture.editor.interaction.EditorInputFrame;
import dev.aperture.editor.model.session.EditorSession;

import java.util.Optional;

/** Client-owned access point for world render/input adapters around the active editor session. */
public final class ClientEditorWorkspace {
	private static volatile EditorSession session;
	private static volatile WorkspaceTools tools;

	private ClientEditorWorkspace() { }

	public static void bind(EditorSession editorSession, WorkspaceTools workspaceTools) {
		session = editorSession;
		tools = workspaceTools;
	}

	public static Optional<EditorSession> session() { return Optional.ofNullable(session); }

	public static void update(EditorInputFrame input) {
		WorkspaceTools current = tools;
		if (current != null) current.update(input);
	}

	public static ResizeState resizeState() {
		WorkspaceTools current = tools;
		return current == null ? ResizeState.IDLE : current.resizeState();
	}

	public static void clear() {
		WorkspaceTools current = tools;
		if (current != null) current.cancelTools();
		tools = null;
		session = null;
	}

	public interface WorkspaceTools {
		void update(EditorInputFrame input);
		void cancelTools();
		ResizeState resizeState();
	}

	public record ResizeState(boolean hovered, boolean dragging) {
		private static final ResizeState IDLE = new ResizeState(false, false);
	}
}