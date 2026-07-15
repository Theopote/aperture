package dev.aperture.client.placement;

import dev.aperture.api.ApertureApi;
import dev.aperture.client.editor.ClientEditorBridge;
import dev.aperture.client.editor.GizmoDragController;
import dev.aperture.client.parameter.ParameterEditorScreen;
import dev.aperture.client.render.placement.PlacementPreviewMeshService;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.math.Transform3d;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parameter.ParameterDefinition;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.placement.PlacementSession;
import dev.aperture.fabric.placement.FabricPlacementAdapter;
import dev.aperture.fabric.placement.FabricPlacementTarget;
import dev.aperture.fabric.placement.OpeningWorldPlacement;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Client-side placement preview driven by crosshair raycasts.
 */
public final class ClientPlacementPreview {
	private static final Logger LOGGER = LoggerFactory.getLogger("aperture/client/placement");
	private static final FabricPlacementAdapter ADAPTER = new FabricPlacementAdapter();

	private static FabricPlacementTarget currentTarget;
	private static PlacementSession currentSession;
	private static final ClientEditorBridge editorBridge = new ClientEditorBridge();
	private static OpeningId selectedTypeId = BuiltinOpeningTypes.DOOR_ID;
	private static ParameterSet parameterOverrides = ParameterSet.empty();
	private static Transform3d transformOverride;
	private static Object lastTargetKey;

	private ClientPlacementPreview() {
	}

	public static void tick(Minecraft client) {
		if (client.player == null || client.level == null) {
			clear();
			return;
		}

		try {
			ApertureApi api = ApertureApi.get();
			Optional<FabricPlacementTarget> target = ADAPTER.fromCrosshair(
				client.level,
				client.player,
				client.hitResult,
				api.instances()
			);

			if (target.isEmpty()) {
				clear();
				return;
			}

			currentTarget = target.get();
			Object targetKey = targetKey(currentTarget);
			if (lastTargetKey == null || !targetKey.equals(lastTargetKey)) {
				lastTargetKey = targetKey;
				if (!GizmoDragController.isDragging()) {
					transformOverride = null;
				}
			}
			refreshSession(api);
		} catch (IllegalStateException notInitialized) {
			clear();
		}
	}

	public static void openParameterEditor(Minecraft client) {
		if (currentSession == null) {
			return;
		}

		ApertureApi api = ApertureApi.get();
		var definition = api.openingTypes().require(selectedTypeId);
		client.setScreen(new ParameterEditorScreen(definition, parameterOverrides, overrides -> {
			parameterOverrides = overrides;
			refreshSession(api);
		}));
	}

	private static void refreshSession(ApertureApi api) {
		if (currentTarget == null) {
			return;
		}

		Transform3d transform = transformOverride != null
			? transformOverride
			: currentTarget.suggestedTransform();
		currentSession = api.placement().preview(
			selectedTypeId,
			parameterOverrides,
			transform,
			currentTarget.host(),
			currentTarget.placementContext()
		);
		editorBridge.syncFromPreview(api, currentSession);
		PlacementPreviewMeshService.update(currentSession);
	}

	public static void applyEditorInstance(OpeningInstance instance) {
		try {
			ApertureApi api = ApertureApi.get();
			OpeningTypeDefinition definition = api.openingTypes().require(instance.typeId());
			transformOverride = instance.transform();
			parameterOverrides = overridesFromInstance(definition, instance);
			refreshSession(api);
		} catch (IllegalStateException notInitialized) {
			// Mod not bootstrapped yet on client.
		}
	}

	private static Object targetKey(FabricPlacementTarget target) {
		return target.host().anchor();
	}

	private static ParameterSet overridesFromInstance(OpeningTypeDefinition definition, OpeningInstance instance) {
		ParameterSet.Builder builder = ParameterSet.builder();
		for (var entry : instance.parameters().asMap().entrySet()) {
			ParameterDefinition schema = definition.parameters().get(entry.getKey());
			if (schema == null) {
				builder.put(entry.getKey(), entry.getValue());
				continue;
			}
			ParameterValue defaultValue = schema.defaultValue();
			if (!defaultValue.equals(entry.getValue())) {
				builder.put(entry.getKey(), entry.getValue());
			}
		}
		return builder.build();
	}

	public static java.util.Optional<ClientEditorBridge> editorBridge() {
		if (currentSession == null) {
			return java.util.Optional.empty();
		}
		return java.util.Optional.of(editorBridge);
	}

	public static Optional<FabricPlacementTarget> target() {
		return Optional.ofNullable(currentTarget);
	}

	public static Optional<PlacementSession> session() {
		return Optional.ofNullable(currentSession);
	}

	public static ParameterSet parameterOverrides() {
		return parameterOverrides;
	}

	public static boolean commitPreview() {
		Optional<PlacementSession> session = session();
		if (session.isEmpty() || !session.get().isValid()) {
			return false;
		}

		Minecraft client = Minecraft.getInstance();
		try {
			var committed = ApertureApi.get().placement().commit(session.get());
			if (client.level != null) {
				OpeningWorldPlacement.placeCommittedInstance(client.level, committed);
			}
			LOGGER.info("Committed opening at host {}", currentTarget.host().anchor());
			return true;
		} catch (IllegalStateException exception) {
			LOGGER.warn("Failed to commit placement preview", exception);
			return false;
		}
	}

	private static void clear() {
		currentTarget = null;
		currentSession = null;
		lastTargetKey = null;
		transformOverride = null;
		editorBridge.clear();
		GizmoDragController.reset();
		PlacementPreviewMeshService.clear();
	}
}
