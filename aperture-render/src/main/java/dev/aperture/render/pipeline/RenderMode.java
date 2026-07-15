package dev.aperture.render.pipeline;

/**
 * Composable preview and editing flags applied by the render pipeline.
 */
public enum RenderMode {
	COMMITTED,
	GHOST,
	INCREMENTAL,
	HIGHLIGHT_DIRTY,
	REBIND_ONLY,
	NO_COLLISION,
	NO_PICK,
	FILTER_LAYER
}
