package dev.aperture.client.placement;

import dev.aperture.api.ApertureApi;
import dev.aperture.client.render.placement.PlacementPreviewMeshService;
import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.instance.OpeningInstance;
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
			currentSession = api.placement().preview(
				BuiltinOpeningTypes.FIXED_WINDOW_ID,
				ParameterSet.empty(),
				currentTarget.suggestedTransform(),
				currentTarget.host(),
				currentTarget.placementContext()
			);
			PlacementPreviewMeshService.update(currentSession);
		} catch (IllegalStateException notInitialized) {
			clear();
		}
	}

	public static Optional<FabricPlacementTarget> target() {
		return Optional.ofNullable(currentTarget);
	}

	public static Optional<PlacementSession> session() {
		return Optional.ofNullable(currentSession);
	}

	public static boolean commitPreview() {
		Optional<PlacementSession> session = session();
		if (session.isEmpty() || !session.get().isValid()) {
			return false;
		}

		Minecraft client = Minecraft.getInstance();
		try {
			OpeningInstance committed = ApertureApi.get().placement().commit(session.get());
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
