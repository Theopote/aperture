package dev.aperture.client.editor;

import dev.aperture.editor.interaction.PerspectiveProjector;
import dev.aperture.editor.interaction.ScreenPoint;
import dev.aperture.math.Vec3d;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Adapts Minecraft camera state to the tested frontend-neutral perspective projector. */
public final class WorldToScreenProjector {
	private final PerspectiveProjector projector = new PerspectiveProjector();

	public Optional<ScreenPoint> project(Minecraft client, Vec3 worldPosition) {
		if (client.player == null) return Optional.empty();
		var camera = client.gameRenderer.getMainCamera().position();
		var forward = client.player.getViewVector(1.0F).normalize();
		double width = client.getWindow().getScreenWidth();
		double height = client.getWindow().getScreenHeight();
		if (width <= 0 || height <= 0) return Optional.empty();
		var view = new PerspectiveProjector.View(
			new Vec3d(ClientWorldUnits.toMillimeters(camera.x), ClientWorldUnits.toMillimeters(camera.y),
				ClientWorldUnits.toMillimeters(camera.z)),
			new Vec3d(forward.x, forward.y, forward.z), new Vec3d(0, 1, 0),
			client.options.fov().get(), width, height);
		return projector.project(new Vec3d(ClientWorldUnits.toMillimeters(worldPosition.x),
			ClientWorldUnits.toMillimeters(worldPosition.y), ClientWorldUnits.toMillimeters(worldPosition.z)), view);
	}
}