package dev.aperture.editor.interaction;

/** Higher ranks win before distance is considered. */
public enum PickPriority {
	MINECRAFT_BLOCK(0),
	ARCHITECTURAL_OBJECT(100),
	PREVIEW_GEOMETRY(200),
	SELECTED_COMPONENT(300),
	ACTIVE_MANIPULATOR(400);

	private final int rank;
	PickPriority(int rank) { this.rank = rank; }
	public int rank() { return rank; }
}
