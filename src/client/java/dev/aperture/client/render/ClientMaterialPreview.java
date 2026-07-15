package dev.aperture.client.render;

import dev.aperture.render.material.MaterialPreviewMode;

/**
 * Client-side material preview mode for frame/glass filtering and debug views.
 */
public final class ClientMaterialPreview {
	private static MaterialPreviewMode mode = MaterialPreviewMode.FULL;

	private ClientMaterialPreview() {
	}

	public static MaterialPreviewMode mode() {
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

	public static void set(MaterialPreviewMode newMode) {
		mode = newMode;
	}
}
