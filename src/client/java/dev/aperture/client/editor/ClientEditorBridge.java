package dev.aperture.client.editor;

import dev.aperture.editor.ApertureEditor;
import dev.aperture.runtime.ApertureRuntime;
import dev.aperture.core.editor.EditorObject;
import dev.aperture.core.editor.EditorObjectId;
import dev.aperture.core.editor.session.EditorSession;
import dev.aperture.core.placement.PlacementSession;

import java.util.Optional;

/**
 * Keeps a headless {@link EditorSession} in sync with the active placement preview.
 */
public final class ClientEditorBridge {
	private EditorSession editorSession;
	private EditorObjectId objectId;

	public void syncFromPreview(PlacementSession session) {
		this.editorSession = ApertureEditor.get().editor().sessionWithInstance(
			ApertureRuntime.get().openingTypes(),
			session.previewInstance()
		);
		this.objectId = editorSession.selection().primary().orElseThrow();
	}

	public Optional<EditorSession> session() {
		return Optional.ofNullable(editorSession);
	}

	public Optional<EditorObjectId> objectId() {
		return Optional.ofNullable(objectId);
	}

	public Optional<EditorObject> object() {
		if (editorSession == null || objectId == null) {
			return Optional.empty();
		}
		return editorSession.object(objectId);
	}

	public void clear() {
		editorSession = null;
		objectId = null;
	}
}
