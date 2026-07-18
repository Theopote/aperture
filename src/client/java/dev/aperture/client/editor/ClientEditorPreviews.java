package dev.aperture.client.editor;

import dev.aperture.editor.model.preview.LocalPreviewCoordinator;

/** Shared client-only preview overlay read by both ImGui and world mesh extraction. */
public final class ClientEditorPreviews {
	private static final LocalPreviewCoordinator INSTANCE = new LocalPreviewCoordinator();
	private ClientEditorPreviews() { }
	public static LocalPreviewCoordinator get() { return INSTANCE; }
}
