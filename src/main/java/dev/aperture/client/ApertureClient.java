package dev.aperture.client;

import dev.aperture.Aperture;
import dev.aperture.client.placement.ClientPlacementPreview;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class ApertureClient implements ClientModInitializer {
	public static final String KEY_CATEGORY = "key.category.aperture";
	public static KeyMapping commitPlacementKey;

	@Override
	public void onInitializeClient() {
		commitPlacementKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.aperture.commit_placement",
			GLFW.GLFW_KEY_P,
			KEY_CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
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
