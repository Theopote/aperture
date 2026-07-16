package dev.aperture.registry;

import dev.aperture.Aperture;
import dev.aperture.block.entity.OpeningBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Registers Aperture block entity types.
 */
public final class ApertureBlockEntities {
	public static BlockEntityType<OpeningBlockEntity> OPENING;

	private ApertureBlockEntities() {
	}

	public static void registerAll() {
		OPENING = register(
			"opening",
			FabricBlockEntityTypeBuilder.create(OpeningBlockEntity::new, ApertureBlocks.OPENING).build()
		);
	}

	private static BlockEntityType<OpeningBlockEntity> register(String path, BlockEntityType<OpeningBlockEntity> type) {
		return Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(Aperture.MOD_ID, path),
			type
		);
	}
}
