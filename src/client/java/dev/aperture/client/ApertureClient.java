package dev.aperture.client;

import dev.aperture.Aperture;
import dev.aperture.client.placement.ClientPlacementPreview;
import dev.aperture.client.render.ApertureRenderers;
import dev.aperture.client.render.placement.PlacementPreviewRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ApertureClient implements ClientModInitializer {
	public static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
		Identifier.fromNamespaceAndPath(Aperture.MOD_ID, "placement")
	);
	public static KeyMapping commitPlacementKey;

	@Override
	public void onInitializeClient() {
		commitPlacementKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.aperture.commit_placement",
			GLFW.GLFW_KEY_P,
			KEY_CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
		LevelRenderEvents.BEFORE_GIZMOS.register(context -> PlacementPreviewRenderer.emit());
		ApertureRenderers.registerAll();
		Aperture.LOGGER.info("Aperture client initialized — crosshair placement preview active");
	}

	private void onClientTick(Minecraft client) {
		ClientPlacementPreview.tick(client);

		while (commitPlacementKey.consumeClick()) {
			if (ClientPlacementPreview.commitPreview()) {
				Aperture.LOGGER.info("Placement preview committed");
			}
		}
	}
}
