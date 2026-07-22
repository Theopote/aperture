package dev.aperture.client.editor;

import dev.aperture.editor.model.session.EditorSession;

import java.util.Optional;

/** Client-owned access point for world render/input adapters around the active editor session. */
public final class ClientEditorWorkspace {
	private static volatile EditorSession session;

	private ClientEditorWorkspace() { }

	public static void bind(EditorSession editorSession) { session = editorSession; }

	public static Optional<EditorSession> session() { return Optional.ofNullable(session); }

	public static void clear() { session = null; }
}
