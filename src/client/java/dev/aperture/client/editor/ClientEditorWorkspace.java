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
		EditorSession currentSession = session;
		if (currentSession != null) NavigatorFocusController.update(currentSession);
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

	public record ResizeState(Optional<String> hoveredManipulatorId, Optional<String> activeManipulatorId, Optional<String> pendingManipulatorId, dev.aperture.editor.interaction.ToolInteractionState interactionState) {
		public ResizeState {
			hoveredManipulatorId = hoveredManipulatorId == null ? Optional.empty() : hoveredManipulatorId;
			activeManipulatorId = activeManipulatorId == null ? Optional.empty() : activeManipulatorId;
			pendingManipulatorId = pendingManipulatorId == null ? Optional.empty() : pendingManipulatorId;
			interactionState = interactionState == null ? dev.aperture.editor.interaction.ToolInteractionState.IDLE : interactionState;
		}
		public boolean hovered() { return hoveredManipulatorId.isPresent(); }
		public boolean dragging() { return activeManipulatorId.isPresent(); }
		private static final ResizeState IDLE = new ResizeState(Optional.empty(), Optional.empty(), Optional.empty(), dev.aperture.editor.interaction.ToolInteractionState.IDLE);
	}
}