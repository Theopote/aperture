package dev.aperture.client.editor;

import dev.aperture.fabric.placement.McUnits;
import net.minecraft.world.phys.Vec3;

/** Projects a view ray onto a world-space drag axis. */
public final class GizmoDragMath {
	private GizmoDragMath() { }

	public static double axisPositionBlocks(Vec3 rayOrigin, Vec3 rayDirection,
		Vec3 axisOriginBlocks, Vec3 axisDirectionBlocks) {
		Vec3 ray = rayDirection.normalize();
		Vec3 axis = axisDirectionBlocks.normalize();
		Vec3 betweenOrigins = rayOrigin.subtract(axisOriginBlocks);
		double parallel = ray.dot(axis);
		double denominator = 1.0 - parallel * parallel;
		if (denominator < 1.0E-8) {
			return betweenOrigins.dot(axis);
		}
		double rayOffset = ray.dot(betweenOrigins);
		double axisOffset = axis.dot(betweenOrigins);
		return (axisOffset - parallel * rayOffset) / denominator;
	}

	public static double blocksToMillimeters(double blocks) {
		return blocks * McUnits.MILLIMETERS_PER_BLOCK;
	}
}
