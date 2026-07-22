package dev.aperture.client.editor;

import dev.aperture.block.entity.OpeningBlockEntity;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;

/** Resolves the architectural identity anchored at the current Minecraft hit. */
public final class ArchitecturalPickingService {
	public Optional<ArchitecturalObjectId> pick(Minecraft client) {
		if (client.level == null || !(client.hitResult instanceof BlockHitResult hit)) return Optional.empty();
		if (!(client.level.getBlockEntity(hit.getBlockPos()) instanceof OpeningBlockEntity opening)) return Optional.empty();
		return opening.resolveRuntimeSnapshot().map(snapshot -> snapshot.instance().objectId());
	}
}
