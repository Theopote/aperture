package dev.aperture.registry;

import dev.aperture.Aperture;
import dev.aperture.block.OpeningBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

/**
 * Registers Aperture blocks.
 */
public final class ApertureBlocks {
	public static Block OPENING;

	private ApertureBlocks() {
	}

	public static void registerAll() {
		OPENING = register(
			"opening",
			OpeningBlock::new,
			BlockBehaviour.Properties.of()
				.noCollision()
				.noOcclusion()
				.noLootTable()
				.strength(-1.0F, 3600000.0F)
		);
	}

	private static Block register(
		String path,
		Function<BlockBehaviour.Properties, Block> factory,
		BlockBehaviour.Properties properties
	) {
		ResourceKey<Block> blockKey = ResourceKey.create(
			Registries.BLOCK,
			Identifier.fromNamespaceAndPath(Aperture.MOD_ID, path)
		);
		Block block = factory.apply(properties.setId(blockKey));
		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}
}
