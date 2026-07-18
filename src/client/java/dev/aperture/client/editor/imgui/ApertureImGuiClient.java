package dev.aperture.client.editor.imgui;

import dev.aperture.Aperture;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Client-only bootstrap for the concrete Dear ImGui editor. */
public final class ApertureImGuiClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
		Identifier.fromNamespaceAndPath(Aperture.MOD_ID, "editor")
	);
	private final ApertureImGuiRuntime runtime = new ApertureImGuiRuntime();
	private KeyMapping toggle;

	@Override public void onInitializeClient() {
		toggle = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.aperture.toggle_imgui_editor", GLFW.GLFW_KEY_F4, CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> runtime.close());
	}

	private void tick(Minecraft client) {
		while (toggle.consumeClick()) {
			if (client.screen instanceof ApertureImGuiScreen) client.setScreen(null);
			else client.setScreen(new ApertureImGuiScreen(runtime));
		}
	}
}
