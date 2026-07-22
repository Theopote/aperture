package dev.aperture.client.editor;

import dev.aperture.editor.interaction.PickContext;
import dev.aperture.editor.interaction.PickResult;
import dev.aperture.editor.interaction.WorldRay;
import dev.aperture.math.Vec3d;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Optional;

/** Minecraft composition root for the frontend-neutral architectural picking pipeline. */
public final class ArchitecturalPickingService {

	public Optional<PickResult> pick(Minecraft client, PickContext context) {
		if (client.player == null || client.level == null) return Optional.empty();
		var origin = client.gameRenderer.getMainCamera().position();
		var direction = client.player.getViewVector(1.0F);
		WorldRay ray = new WorldRay(new Vec3d(ClientWorldUnits.toMillimeters(origin.x),
			ClientWorldUnits.toMillimeters(origin.y), ClientWorldUnits.toMillimeters(origin.z)),
			new Vec3d(direction.x, direction.y, direction.z));
		var pipeline = new dev.aperture.editor.interaction.ArchitecturalPickingService(
			List.of(new ArchitecturalObjectAnchorPickSource(client)));
		return pipeline.pick(ray, context);
	}
}