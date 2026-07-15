package dev.aperture.client.render;

import dev.aperture.registry.ApertureBlockEntities;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/**
 * Registers Aperture client renderers.
 */
public final class ApertureRenderers {
	private ApertureRenderers() {
	}

	public static void registerAll() {
		BlockEntityRenderers.register(ApertureBlockEntities.OPENING, OpeningInstanceRenderer::new);
	}
}
