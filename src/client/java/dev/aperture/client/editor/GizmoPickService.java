package dev.aperture.client.editor;

import dev.aperture.core.editor.EditorObject;
import dev.aperture.core.editor.manipulation.ManipulatorKind;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.editor.interaction.GizmoHitTester;
import dev.aperture.editor.interaction.GizmoPickTarget;
import dev.aperture.editor.interaction.ScreenPoint;
import dev.aperture.editor.interaction.ScreenSpaceHandle;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Pixel-stable picking for legacy placement gizmos. */
public final class GizmoPickService {
	private static final double MAX_PICK_DISTANCE_BLOCKS = 64.0;
	private static final WorldToScreenProjector PROJECTOR = new WorldToScreenProjector();
	private static final GizmoHitTester HIT_TESTER = new GizmoHitTester();

	private GizmoPickService() { }

	public static Optional<GizmoPickTarget> pick(Minecraft client, EditorObject object) {
		if (client.player == null) return Optional.empty();
		Vec3 origin = client.gameRenderer.getMainCamera().position();
		OpeningInstance instance = object.instance();
		var handles = new ArrayList<ScreenSpaceHandle>();
		Map<String, GizmoPickTarget> targets = new HashMap<>();

		for (var resize : object.resizeHandles()) {
			add(client, origin, GizmoCoordinates.localMmToWorldBlocks(instance, resize.localPosition()),
				"resize:" + resize.id(), GizmoPickTarget.resizeHandle(resize.id()), handles, targets);
		}
		for (var manipulator : object.manipulators()) {
			if (manipulator.kind() == ManipulatorKind.RESIZE || manipulator.kind() == ManipulatorKind.TRANSLATE) continue;
			add(client, origin, GizmoCoordinates.localMmToWorldBlocks(instance, manipulator.localAnchor()),
				"manipulator:" + manipulator.id(), GizmoPickTarget.manipulator(manipulator.id(), manipulator.kind()),
				handles, targets);
		}
		return HIT_TESTER.hit(cursor(client), handles).map(hit -> targets.get(hit.id()));
	}

	private static void add(Minecraft client, Vec3 origin, Vec3 position, String id, GizmoPickTarget target,
		java.util.List<ScreenSpaceHandle> handles, Map<String, GizmoPickTarget> targets) {
		if (origin.distanceToSqr(position) > MAX_PICK_DISTANCE_BLOCKS * MAX_PICK_DISTANCE_BLOCKS) return;
		PROJECTOR.project(client, position).ifPresent(center -> {
			handles.add(new ScreenSpaceHandle(id, center, 9, 12, 14, true,
				ScreenSpaceHandle.OcclusionPolicy.IGNORE_SCENE_DEPTH,
				ScreenSpaceHandle.DisplayPolicy.ALWAYS_ON_TOP));
			targets.put(id, target);
		});
	}

	private static ScreenPoint cursor(Minecraft client) {
		int[] windowWidth = new int[1];
		int[] windowHeight = new int[1];
		int[] framebufferWidth = new int[1];
		int[] framebufferHeight = new int[1];
		long window = client.getWindow().handle();
		GLFW.glfwGetWindowSize(window, windowWidth, windowHeight);
		GLFW.glfwGetFramebufferSize(window, framebufferWidth, framebufferHeight);
		return new ScreenPoint(scale(client.mouseHandler.xpos(), windowWidth[0], framebufferWidth[0]),
			scale(client.mouseHandler.ypos(), windowHeight[0], framebufferHeight[0]));
	}

	private static double scale(double value, int windowSize, int framebufferSize) {
		return windowSize > 0 && framebufferSize > 0 ? value * framebufferSize / windowSize : value;
	}
}