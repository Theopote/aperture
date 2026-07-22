package dev.aperture.client.editor;

import dev.aperture.editor.interaction.WorldUnitConverter;
import dev.aperture.math.Vec3d;
import net.minecraft.world.phys.Vec3;

/** Unit conversion boundary between architectural millimetres and Minecraft blocks. */
public final class ClientWorldUnits {
	private ClientWorldUnits() { }

	public static double toMillimeters(double blocks) {
		return WorldUnitConverter.blocksToLength(blocks);
	}

	public static double toBlocks(double millimeters) {
		return WorldUnitConverter.lengthToBlocks(millimeters);
	}

	public static Vec3d toMillimeters(Vec3 blocks) {
		return new Vec3d(toMillimeters(blocks.x), toMillimeters(blocks.y), toMillimeters(blocks.z));
	}

	public static Vec3 toBlocks(Vec3d millimeters) {
		return new Vec3(toBlocks(millimeters.x()), toBlocks(millimeters.y()), toBlocks(millimeters.z()));
	}
}
