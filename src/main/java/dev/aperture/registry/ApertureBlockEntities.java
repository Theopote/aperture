package dev.aperture.registry;

import dev.aperture.Aperture;
import dev.aperture.block.entity.OpeningBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

/**
 * Registers Aperture block entity types.
 */
public final class ApertureBlockEntities {
	public static final BlockEntityType<OpeningBlockEntity> OPENING = register(
		"opening",
		new BlockEntityType<>(OpeningBlockEntity::new, Set.of(ApertureBlocks.OPENING))
	);

	private ApertureBlockEntities() {
	}

	public static void registerAll() {
		// Triggers static initialization.
	}

	private static BlockEntityType<OpeningBlockEntity> register(String path, BlockEntityType<OpeningBlockEntity> type) {
		return Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(Aperture.MOD_ID, path),
			type
		);
	}
}
