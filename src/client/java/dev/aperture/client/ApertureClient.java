package dev.aperture.client;

import dev.aperture.Aperture;
import dev.aperture.editor.ApertureEditor;
import dev.aperture.editor.service.EditorService;
import dev.aperture.editor.service.ParametricService;
import dev.aperture.client.editor.GizmoDragController;
import dev.aperture.client.placement.ClientPlacementPreview;
import dev.aperture.client.render.ApertureRenderers;
import dev.aperture.client.render.ClientMaterialPreview;
import dev.aperture.client.render.InstanceRenderCache;
import dev.aperture.client.render.editor.EditorGizmoRenderer;
import dev.aperture.client.render.editor.SelectionHighlightRenderer;
import dev.aperture.client.render.placement.GhostPreviewMeshRenderer;
import dev.aperture.client.render.placement.PlacementPreviewMeshService;
import dev.aperture.client.render.placement.PlacementPreviewRenderer;
import dev.aperture.client.runtime.ClientRuntimeReplicas;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ApertureClient implements ClientModInitializer {
	public static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
		Identifier.fromNamespaceAndPath(Aperture.MOD_ID, "placement")
	);
	public static KeyMapping commitPlacementKey;
	public static KeyMapping cyclePreviewModeKey;
	public static KeyMapping openParameterEditorKey;

	@Override
	public void onInitializeClient() {
		ClientRuntimeReplicas.registerReceiver();
		commitPlacementKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.aperture.commit_placement",
			GLFW.GLFW_KEY_P,
			KEY_CATEGORY
		));
		cyclePreviewModeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.aperture.cycle_preview_mode",
			GLFW.GLFW_KEY_M,
			KEY_CATEGORY
		));
		openParameterEditorKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.aperture.open_parameter_editor",
			GLFW.GLFW_KEY_O,
			KEY_CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
		LevelRenderEvents.COLLECT_SUBMITS.register(GhostPreviewMeshRenderer::emit);
		LevelRenderEvents.BEFORE_GIZMOS.register(context -> {
			PlacementPreviewRenderer.emit();
			EditorGizmoRenderer.emit();
			SelectionHighlightRenderer.emit();
		});
		ApertureEditor.init(new ApertureEditor(new EditorService(), new ParametricService()));
		ApertureRenderers.registerAll();
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
			Identifier.fromNamespaceAndPath(Aperture.MOD_ID, "render_cache"),
			(ResourceManagerReloadListener) resourceManager -> {
				InstanceRenderCache.get().clear();
				PlacementPreviewMeshService.clear();
			}
		);
		Aperture.LOGGER.info("Aperture client initialized — crosshair placement preview active");
	}

	private void onClientTick(Minecraft client) {
		ClientPlacementPreview.tick(client);
		GizmoDragController.tick(client);

		while (commitPlacementKey.consumeClick()) {
			if (ClientPlacementPreview.commitPreview()) {
				Aperture.LOGGER.info("Placement preview committed");
			}
		}

		while (cyclePreviewModeKey.consumeClick()) {
			ClientMaterialPreview.cycle();
			Aperture.LOGGER.debug("Placement preview material mode: {}", ClientMaterialPreview.mode());
		}

		while (openParameterEditorKey.consumeClick()) {
			ClientPlacementPreview.openParameterEditor(client);
		}
	}
}
