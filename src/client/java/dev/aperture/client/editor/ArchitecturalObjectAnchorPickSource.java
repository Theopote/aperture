package dev.aperture.client.editor;

import dev.aperture.block.entity.OpeningBlockEntity;
import dev.aperture.editor.interaction.*;
import dev.aperture.math.Vec3d;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.Optional;

/** Current anchor-backed source; other object hosts can be added without changing arbitration. */
public final class ArchitecturalObjectAnchorPickSource implements PickSource {
	private final Minecraft client;

	public ArchitecturalObjectAnchorPickSource(Minecraft client) { this.client = client; }
	@Override public String id() { return "architectural-object-anchor"; }

	@Override
	public List<PickCandidate> pick(WorldRay ray, PickContext context) {
		if (client.level == null || !(client.hitResult instanceof BlockHitResult hit)) return List.of();
		if (!(client.level.getBlockEntity(hit.getBlockPos()) instanceof OpeningBlockEntity opening)) return List.of();
		return opening.resolveRuntimeSnapshot().map(snapshot -> {
			Vec3d position = new Vec3d(ClientWorldUnits.toMillimeters(hit.getLocation().x),
				ClientWorldUnits.toMillimeters(hit.getLocation().y), ClientWorldUnits.toMillimeters(hit.getLocation().z));
			var normal = hit.getDirection().getUnitVec3();
			double distance = Math.sqrt(position.subtract(ray.origin()).lengthSquared());
			return List.of(new PickCandidate(snapshot.instance().objectId(), Optional.empty(), Optional.empty(),
				PickCandidate.HitKind.OBJECT, position, new Vec3d(normal.x, normal.y, normal.z), distance,
				PickPriority.ARCHITECTURAL_OBJECT));
		}).orElseGet(List::of);
	}
}
