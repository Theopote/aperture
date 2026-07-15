package dev.aperture.render.pipeline;

/**
 * Ordered render passes submitted to the backend.
 */
public enum RenderPass {
	OPAQUE,
	CUTOUT,
	GHOST,
	TRANSLUCENT,
	DEBUG_OVERLAY
}
