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
		MaterialPreviewMode[] values = MaterialPreviewMode.values();
		mode = values[(mode.ordinal() + 1) % values.length];
	}

	public static void set(MaterialPreviewMode newMode) {
		mode = newMode;
	}
}
