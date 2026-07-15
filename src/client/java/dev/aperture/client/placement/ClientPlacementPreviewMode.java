package dev.aperture.client.placement;

import dev.aperture.render.material.MaterialPreviewMode;

/**
 * Client-side placement preview material filter mode.
 */
public final class ClientPlacementPreviewMode {
	private static MaterialPreviewMode mode = MaterialPreviewMode.FULL;

	private ClientPlacementPreviewMode() {
	}

	public static MaterialPreviewMode current() {
		return mode;
	}

	public static void cycle() {
		mode = switch (mode) {
			case FULL -> MaterialPreviewMode.FRAME_ONLY;
			case FRAME_ONLY -> MaterialPreviewMode.GLASS_ONLY;
			case GLASS_ONLY -> MaterialPreviewMode.FULL;
			default -> MaterialPreviewMode.FULL;
		};
	}
}
