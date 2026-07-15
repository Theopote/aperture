package dev.aperture.client.editor;

import dev.aperture.math.Vec3d;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.fabric.placement.McUnits;
import net.minecraft.world.phys.Vec3;

/**
 * Converts opening-local millimeter coordinates to Minecraft world block units.
 */
public final class GizmoCoordinates {
	private GizmoCoordinates() {
	}

	public static Vec3 localMmToWorldBlocks(OpeningInstance instance, Vec3d localMm) {
		Vec3d worldMm = instance.transform().origin().add(localMm);
		return mmToBlocks(worldMm);
	}

	public static Vec3 mmToBlocks(Vec3d millimeters) {
		return new Vec3(
			millimeters.x() / McUnits.MILLIMETERS_PER_BLOCK,
			millimeters.y() / McUnits.MILLIMETERS_PER_BLOCK,
			millimeters.z() / McUnits.MILLIMETERS_PER_BLOCK
		);
	}

	public static Vec3 localDirectionToWorld(OpeningInstance instance, Vec3d localDirection) {
		Vec3d worldDir = instance.transform().transformDirection(localDirection);
		return new Vec3(worldDir.x(), worldDir.y(), worldDir.z()).normalize();
	}
}
