package dev.aperture.editor.interaction;

public enum ToolInteractionState {
	IDLE,
	HOVER,
	DRAGGING,
	PENDING,
	ACCEPTED_WAITING_REPLICA,
	REJECTED,
	CONFLICT,
	CANCELLED
}
