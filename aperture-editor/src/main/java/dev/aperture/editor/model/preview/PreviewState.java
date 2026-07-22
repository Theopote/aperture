package dev.aperture.editor.model.preview;

/** Lifecycle of an overlay after a continuous edit starts. */
public enum PreviewState {
	EDITING, PENDING, ACCEPTED_WAITING_REPLICA, REJECTED, CONFLICT, RESYNCING, COMPLETED
}
