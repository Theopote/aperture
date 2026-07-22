package dev.aperture.client.editor;

import dev.aperture.editor.interaction.ScreenPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Projects Minecraft world coordinates to physical window pixels for editor hit testing. */
public final class WorldToScreenProjector {
	private static final double NEAR_EPSILON = 1.0e-4;

	public Optional<ScreenPoint> project(Minecraft client, Vec3 worldPosition) {
		if (client.player == null) return Optional.empty();
		Vec3 camera = client.gameRenderer.getMainCamera().position();
		Vec3 forward = client.player.getViewVector(1.0F).normalize();
		Vec3 right = forward.cross(new Vec3(0, 1, 0));
		if (right.lengthSqr() < NEAR_EPSILON) right = new Vec3(1, 0, 0);
		right = right.normalize();
		Vec3 up = right.cross(forward).normalize();
		Vec3 relative = worldPosition.subtract(camera);
		double depth = relative.dot(forward);
		if (depth <= NEAR_EPSILON) return Optional.empty();

		double width = client.getWindow().getScreenWidth();
		double height = client.getWindow().getScreenHeight();
		if (width <= 0 || height <= 0) return Optional.empty();
		double verticalFov = Math.toRadians(client.options.fov().get());
		double focalY = height / (2.0 * Math.tan(verticalFov / 2.0));
		double x = width / 2.0 + relative.dot(right) * focalY / depth;
		double y = height / 2.0 - relative.dot(up) * focalY / depth;
		if (!Double.isFinite(x) || !Double.isFinite(y)) return Optional.empty();
		return Optional.of(new ScreenPoint(x, y));
	}
}
