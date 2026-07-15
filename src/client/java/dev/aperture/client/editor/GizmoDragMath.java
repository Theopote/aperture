package dev.aperture.client.editor;

import dev.aperture.fabric.placement.McUnits;
import net.minecraft.world.phys.Vec3;

/**
 * Projects cursor ray movement onto a drag axis for resize gizmos.
 */
public final class GizmoDragMath {
	private GizmoDragMath() {
	}

	public static double axisPositionBlocks(
		Vec3 rayOrigin,
		Vec3 rayDirection,
		Vec3 axisOriginBlocks,
		Vec3 axisDirectionBlocks
	) {
		Vec3 axisDir = axisDirectionBlocks.normalize();
		Vec3 planeNormal = rayDirection.cross(axisDir);
		if (planeNormal.lengthSqr() < 1.0E-8) {
			planeNormal = axisDir.cross(new Vec3(0.0, 1.0, 0.0));
			if (planeNormal.lengthSqr() < 1.0E-8) {
				planeNormal = axisDir.cross(new Vec3(1.0, 0.0, 0.0));
			}
		}
		planeNormal = planeNormal.normalize();

		double denominator = planeNormal.dot(rayDirection);
		if (Math.abs(denominator) < 1.0E-8) {
			return 0.0;
		}

		double rayDistance = planeNormal.dot(axisOriginBlocks.subtract(rayOrigin)) / denominator;
		Vec3 hit = rayOrigin.add(rayDirection.scale(rayDistance));
		return hit.subtract(axisOriginBlocks).dot(axisDir);
	}

	public static double blocksToMillimeters(double blocks) {
		return blocks * McUnits.MILLIMETERS_PER_BLOCK;
	}
}
