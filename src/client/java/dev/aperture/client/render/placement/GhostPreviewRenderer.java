package dev.aperture.client.render.placement;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Transform3d;
import dev.aperture.core.placement.PlacementSession;
import dev.aperture.fabric.placement.McBoundsConverter;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.render.mesh.MeshAsset;
import dev.aperture.render.mesh.MeshSection;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;

/**
 * Renders placement preview geometry as semi-transparent ghost solids.
 */
public final class GhostPreviewRenderer {
	private static final int COLOR_FRAME_VALID_STROKE = 0xFF66FF66;
	private static final int COLOR_FRAME_VALID_FILL = 0x4466FF66;
	private static final int COLOR_GLASS_VALID_STROKE = 0xFF66CCFF;
	private static final int COLOR_GLASS_VALID_FILL = 0x3366CCFF;
	private static final int COLOR_INVALID_STROKE = 0xFFFF6666;
	private static final int COLOR_INVALID_FILL = 0x44FF6666;

	private GhostPreviewRenderer() {
	}

	public static void emit(PlacementSession session, MeshAsset meshAsset) {
		Transform3d transform = session.previewInstance().transform();
		boolean valid = session.isValid();

		for (MeshSection section : meshAsset.sections().values()) {
			BoundingBox worldBounds = transform.applyTo(section.bounds());
			emitSolid(McBoundsConverter.toAabb(worldBounds), section.layer(), valid);
		}
	}

	private static void emitSolid(AABB bounds, GeometryLayer layer, boolean valid) {
		int stroke;
		int fill;
		if (!valid) {
			stroke = COLOR_INVALID_STROKE;
			fill = COLOR_INVALID_FILL;
		} else if (layer == GeometryLayer.TRANSLUCENT_GLASS) {
			stroke = COLOR_GLASS_VALID_STROKE;
			fill = COLOR_GLASS_VALID_FILL;
		} else {
			stroke = COLOR_FRAME_VALID_STROKE;
			fill = COLOR_FRAME_VALID_FILL;
		}

		Gizmos.cuboid(bounds, GizmoStyle.strokeAndFill(stroke, 1.5f, fill))
			.setAlwaysOnTop();
	}
}
