package dev.aperture.client.placement;

import dev.aperture.api.ApertureApi;
import dev.aperture.client.parameter.ParameterEditorScreen;
import dev.aperture.client.render.placement.PlacementPreviewMeshService;
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
	private static OpeningId selectedTypeId = BuiltinOpeningTypes.DOOR_ID;
	private static ParameterSet parameterOverrides = ParameterSet.empty();

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

		currentSession = api.placement().preview(
			selectedTypeId,
			parameterOverrides,
			currentTarget.suggestedTransform(),
			currentTarget.host(),
			currentTarget.placementContext()
		);
		PlacementPreviewMeshService.update(currentSession);
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
		PlacementPreviewMeshService.clear();
	}
}
