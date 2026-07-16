package dev.aperture.client.editor;

import dev.aperture.editor.interaction.GizmoPickTarget;

import dev.aperture.core.editor.EditorObject;
import dev.aperture.core.editor.manipulation.Manipulator;
import dev.aperture.core.editor.manipulation.ManipulatorKind;
import dev.aperture.core.editor.manipulation.ResizeHandle;
import dev.aperture.core.instance.OpeningInstance;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Raycasts the crosshair against editor gizmo handles and manipulators.
 */
public final class GizmoPickService {
	private static final double PICK_RADIUS_BLOCKS = 0.12;
	private static final double MAX_PICK_DISTANCE_BLOCKS = 64.0;

	private GizmoPickService() {
	}

	public static Optional<GizmoPickTarget> pick(Minecraft client, EditorObject object) {
		Camera camera = client.gameRenderer.getMainCamera();
		Vec3 origin = camera.position();
		Vec3 direction = client.player.getViewVector(1.0F).normalize();

		Optional<GizmoPickTarget> best = Optional.empty();
		double bestDistance = Double.MAX_VALUE;

		OpeningInstance instance = object.instance();
		for (ResizeHandle handle : object.resizeHandles()) {
			Vec3 position = GizmoCoordinates.localMmToWorldBlocks(instance, handle.localPosition());
			double distance = rayDistanceToPoint(origin, direction, position);
			if (distance <= PICK_RADIUS_BLOCKS && distance < bestDistance && withinReach(origin, position)) {
				bestDistance = distance;
				best = Optional.of(GizmoPickTarget.resizeHandle(handle.id()));
			}
		}

		for (Manipulator manipulator : object.manipulators()) {
			if (manipulator.kind() == ManipulatorKind.RESIZE || manipulator.kind() == ManipulatorKind.TRANSLATE) {
				continue;
			}
			Vec3 position = GizmoCoordinates.localMmToWorldBlocks(instance, manipulator.localAnchor());
			double distance = rayDistanceToPoint(origin, direction, position);
			if (distance <= PICK_RADIUS_BLOCKS && distance < bestDistance && withinReach(origin, position)) {
				bestDistance = distance;
				best = Optional.of(GizmoPickTarget.manipulator(manipulator.id(), manipulator.kind()));
			}
		}

		return best;
	}

	private static boolean withinReach(Vec3 origin, Vec3 target) {
		return origin.distanceToSqr(target) <= MAX_PICK_DISTANCE_BLOCKS * MAX_PICK_DISTANCE_BLOCKS;
	}

	private static double rayDistanceToPoint(Vec3 origin, Vec3 direction, Vec3 point) {
		Vec3 toPoint = point.subtract(origin);
		double projection = toPoint.dot(direction);
		if (projection < 0.0) {
			return Double.MAX_VALUE;
		}
		Vec3 closest = origin.add(direction.scale(projection));
		return closest.distanceTo(point);
	}
}
