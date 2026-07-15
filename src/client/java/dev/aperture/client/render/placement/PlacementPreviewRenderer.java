package dev.aperture.client.render.placement;

import dev.aperture.runtime.ApertureRuntime;
import dev.aperture.client.placement.ClientPlacementPreview;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.math.BoundingBox;
import dev.aperture.core.placement.OpeningFootprint;
import dev.aperture.core.placement.PlacementSession;
import dev.aperture.fabric.placement.FabricPlacementTarget;
import dev.aperture.fabric.placement.McBoundsConverter;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;

/**
 * Draws placement preview overlays using the Minecraft Gizmos API.
 */
public final class PlacementPreviewRenderer {
	private static final int COLOR_HOST = 0xFF00BFFF;
	private static final int COLOR_VALID = 0xFF00FF00;
	private static final int COLOR_INVALID = 0xFFFF0000;

	private PlacementPreviewRenderer() {
	}

	public static void emit() {
		ClientPlacementPreview.target().ifPresent(PlacementPreviewRenderer::emitForTarget);
	}

	private static void emitForTarget(FabricPlacementTarget target) {
		emitHostBounds(target.hostBounds());

		ClientPlacementPreview.session().ifPresent(session -> {
			emitOpeningFootprint(session);
		});
	}

	private static void emitHostBounds(BoundingBox hostBounds) {
		drawWireframe(McBoundsConverter.toAabb(hostBounds), COLOR_HOST);
	}

	private static void emitOpeningFootprint(PlacementSession session) {
		try {
			ApertureRuntime runtime = ApertureRuntime.get();
			OpeningTypeDefinition definition = runtime.openingTypes().require(session.selectedTypeId());
			OpeningFootprint.worldBounds(definition, session.previewInstance()).ifPresent(bounds -> {
				int color = session.isValid() ? COLOR_VALID : COLOR_INVALID;
				drawWireframe(McBoundsConverter.toAabb(bounds), color);
			});
		} catch (IllegalStateException notInitialized) {
			// Mod not bootstrapped yet on client.
		}
	}

	private static void drawWireframe(AABB bounds, int color) {
		Gizmos.cuboid(bounds, GizmoStyle.stroke(color))
			.setAlwaysOnTop();
	}
}
