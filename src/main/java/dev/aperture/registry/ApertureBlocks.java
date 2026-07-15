package dev.aperture.registry;

import dev.aperture.Aperture;
import dev.aperture.block.OpeningBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Registers Aperture blocks.
 */
public final class ApertureBlocks {
	public static final Block OPENING = register(
		"opening",
		new OpeningBlock(
			BlockBehaviour.Properties.of()
				.noCollision()
				.noOcclusion()
				.noLootTable()
				.strength(-1.0F, 3600000.0F)
		)
	);

	private ApertureBlocks() {
	}

	public static void registerAll() {
		// Triggers static initialization.
	}

	private static Block register(String path, Block block) {
		return Registry.register(
			BuiltInRegistries.BLOCK,
			Identifier.fromNamespaceAndPath(Aperture.MOD_ID, path),
			block
		);
	}
}
