package dev.aperture.client.editor;

import dev.aperture.editor.bridge.ClientEditorBridge;

import dev.aperture.editor.interaction.GizmoPickTarget;

import dev.aperture.client.placement.ClientPlacementPreview;
import dev.aperture.core.editor.EditorObject;
import dev.aperture.core.editor.EditorObjectId;
import dev.aperture.core.editor.history.EditResult;
import dev.aperture.core.editor.manipulation.MirrorAxis;
import dev.aperture.core.editor.manipulation.ResizeHandle;
import dev.aperture.core.editor.session.EditorSession;
import dev.aperture.core.instance.OpeningInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

/**
 * Handles mouse picking and dragging of editor gizmos during placement preview.
 */
public final class GizmoDragController {
	private static GizmoPickTarget hoveredTarget;
	private static ActiveDrag activeDrag;
	private static boolean wasMouseDown;

	private GizmoDragController() {
	}

	public static void tick(Minecraft client) {
		if (client.player == null || client.level == null || client.screen != null) {
			clearInteraction();
			return;
		}

		Optional<ClientEditorBridge> bridge = ClientPlacementPreview.editorBridge();
		Optional<EditorObject> object = bridge.flatMap(ClientEditorBridge::object);
		if (object.isEmpty()) {
			clearInteraction();
			return;
		}

		EditorObject editorObject = object.get();
		hoveredTarget = GizmoPickService.pick(client, editorObject).orElse(null);

		long window = client.getWindow().handle();
		boolean mouseDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean mousePressed = mouseDown && !wasMouseDown;
		boolean mouseReleased = !mouseDown && wasMouseDown;
		wasMouseDown = mouseDown;

		if (mousePressed && hoveredTarget != null) {
			startDrag(client, bridge.get(), editorObject, hoveredTarget);
		}

		if (activeDrag != null && mouseDown) {
			continueDrag(client, bridge.get(), editorObject);
		}

		if (mouseReleased) {
			if (activeDrag == null && hoveredTarget != null && hoveredTarget.kind() == GizmoPickTarget.Kind.MANIPULATOR) {
				clickManipulator(bridge.get(), hoveredTarget);
			}
			activeDrag = null;
		}
	}

	public static Optional<GizmoPickTarget> hoveredTarget() {
		return Optional.ofNullable(hoveredTarget);
	}

	public static Optional<GizmoPickTarget> activeTarget() {
		if (activeDrag != null) {
			return Optional.of(activeDrag.target());
		}
		return Optional.empty();
	}

	public static boolean isDragging() {
		return activeDrag != null;
	}

	private static void startDrag(
		Minecraft client,
		ClientEditorBridge bridge,
		EditorObject object,
		GizmoPickTarget target
	) {
		if (!target.isResizeHandle()) {
			return;
		}

		ResizeHandle handle = object.resizeHandle(target.id()).orElse(null);
		if (handle == null) {
			return;
		}

		OpeningInstance instance = object.instance();
		Vec3 axisOrigin = GizmoCoordinates.localMmToWorldBlocks(instance, handle.localPosition());
		Vec3 axisDirection = GizmoCoordinates.localDirectionToWorld(instance, handle.dragDirection());
		Vec3 rayOrigin = client.gameRenderer.getMainCamera().position();
		Vec3 rayDirection = client.player.getViewVector(1.0F).normalize();
		double anchor = GizmoDragMath.axisPositionBlocks(rayOrigin, rayDirection, axisOrigin, axisDirection);
		activeDrag = new ActiveDrag(target, handle.parameterName(), axisOrigin, axisDirection, anchor);
	}

	private static void continueDrag(Minecraft client, ClientEditorBridge bridge, EditorObject object) {
		if (activeDrag == null || !activeDrag.target().isResizeHandle()) {
			return;
		}

		Vec3 rayOrigin = client.gameRenderer.getMainCamera().position();
		Vec3 rayDirection = client.player.getViewVector(1.0F).normalize();
		double position = GizmoDragMath.axisPositionBlocks(
			rayOrigin,
			rayDirection,
			activeDrag.axisOrigin(),
			activeDrag.axisDirection()
		);
		double deltaBlocks = position - activeDrag.anchorBlocks();
		if (Math.abs(deltaBlocks) < 1.0E-4) {
			return;
		}

		double deltaMm = GizmoDragMath.blocksToMillimeters(deltaBlocks);
		EditorSession session = bridge.session().orElseThrow();
		EditorObjectId objectId = bridge.objectId().orElseThrow();
		EditResult result = session.resizeByHandle(objectId, activeDrag.target().id(), deltaMm);
		if (!result.success()) {
			return;
		}

		ClientPlacementPreview.applyEditorInstance(bridge.object().orElseThrow().instance());

		EditorObject updated = bridge.object().orElseThrow();
		ResizeHandle updatedHandle = updated.resizeHandle(activeDrag.target().id()).orElseThrow();
		Vec3 newAxisOrigin = GizmoCoordinates.localMmToWorldBlocks(updated.instance(), updatedHandle.localPosition());
		Vec3 newAxisDirection = GizmoCoordinates.localDirectionToWorld(updated.instance(), updatedHandle.dragDirection());
		double newAnchor = GizmoDragMath.axisPositionBlocks(rayOrigin, rayDirection, newAxisOrigin, newAxisDirection);
		activeDrag = new ActiveDrag(activeDrag.target(), activeDrag.parameterName(), newAxisOrigin, newAxisDirection, newAnchor);
	}

	private static void clickManipulator(ClientEditorBridge bridge, GizmoPickTarget target) {
		EditorSession session = bridge.session().orElseThrow();
		EditorObjectId objectId = bridge.objectId().orElseThrow();
		EditResult result = switch (target.manipulatorKind()) {
			case ROTATE -> session.rotate(objectId, 90.0);
			case MIRROR -> session.mirror(objectId, MirrorAxis.X);
			default -> EditResult.failed("Unsupported gizmo click", "gizmo.unsupported", "Unsupported manipulator");
		};
		if (result.success()) {
			ClientPlacementPreview.applyEditorInstance(bridge.object().orElseThrow().instance());
		}
	}

	private static void clearInteraction() {
		hoveredTarget = null;
		activeDrag = null;
		wasMouseDown = false;
	}

	public static void reset() {
		clearInteraction();
	}

	private record ActiveDrag(
		GizmoPickTarget target,
		String parameterName,
		Vec3 axisOrigin,
		Vec3 axisDirection,
		double anchorBlocks
	) {
	}
}
