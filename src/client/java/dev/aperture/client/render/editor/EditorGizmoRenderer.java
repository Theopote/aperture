package dev.aperture.client.render.editor;

import dev.aperture.client.editor.GizmoCoordinates;
import dev.aperture.client.editor.GizmoDragController;
import dev.aperture.editor.interaction.GizmoPickTarget;
import dev.aperture.client.placement.ClientPlacementPreview;
import dev.aperture.core.editor.EditorObject;
import dev.aperture.core.editor.manipulation.Manipulator;
import dev.aperture.core.editor.manipulation.ManipulatorKind;
import dev.aperture.core.editor.manipulation.ResizeHandle;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterType;
import dev.aperture.parameter.ParameterValue;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;

/**
 * Renders {@link EditorObject} manipulators and resize handles as world-space gizmos.
 */
public final class EditorGizmoRenderer {
	private static final int COLOR_RESIZE = 0xFFFFCC00;
	private static final int COLOR_ROTATE = 0xFF00FF88;
	private static final int COLOR_MIRROR = 0xFFFF44FF;
	private static final int COLOR_HOVER = 0xFFFFFFFF;
	private static final int COLOR_ACTIVE = 0xFFFF8800;

	private static final float HANDLE_POINT_SIZE = 10.0F;
	private static final float ARROW_WIDTH = 3.0F;
	private static final double ARROW_LENGTH_BLOCKS = 0.12;

	private EditorGizmoRenderer() {
	}

	public static void emit() {
		ClientPlacementPreview.editorBridge().flatMap(bridge -> bridge.object()).ifPresent(EditorGizmoRenderer::emitForObject);
	}

	private static void emitForObject(EditorObject object) {
		OpeningInstance instance = object.instance();
		GizmoPickTarget hovered = GizmoDragController.hoveredTarget().orElse(null);
		GizmoPickTarget active = GizmoDragController.activeTarget().orElse(null);

		for (ResizeHandle handle : object.resizeHandles()) {
			emitResizeHandle(instance, handle, isTarget(hovered, handle.id()), isTarget(active, handle.id()));
		}

		for (Manipulator manipulator : object.manipulators()) {
			emitManipulator(instance, manipulator, hovered, active);
		}
	}

	private static void emitResizeHandle(
		OpeningInstance instance,
		ResizeHandle handle,
		boolean hovered,
		boolean active
	) {
		Vec3 position = GizmoCoordinates.localMmToWorldBlocks(instance, handle.localPosition());
		int color = active ? COLOR_ACTIVE : hovered ? COLOR_HOVER : COLOR_RESIZE;
		Gizmos.point(position, color, HANDLE_POINT_SIZE).setAlwaysOnTop();

		Vec3 direction = GizmoCoordinates.localDirectionToWorld(instance, handle.dragDirection());
		Vec3 arrowEnd = position.add(direction.scale(ARROW_LENGTH_BLOCKS));
		Gizmos.arrow(position, arrowEnd, color, ARROW_WIDTH).setAlwaysOnTop();
	}

	private static void emitManipulator(
		OpeningInstance instance,
		Manipulator manipulator,
		GizmoPickTarget hovered,
		GizmoPickTarget active
	) {
		switch (manipulator.kind()) {
			case TRANSLATE, RESIZE, COPY -> {
				// Resize handles are drawn separately; translate/copy are not shown during placement preview.
			}
			case ROTATE -> emitRotateRing(instance, manipulator, isManipulatorTarget(hovered, manipulator, active));
			case MIRROR -> emitMirrorHandle(instance, manipulator, isManipulatorTarget(hovered, manipulator, active));
		}
	}

	private static void emitRotateRing(
		OpeningInstance instance,
		Manipulator manipulator,
		boolean highlighted
	) {
		Vec3 center = GizmoCoordinates.localMmToWorldBlocks(instance, manipulator.localAnchor());
		double radiusBlocks = rotateRadiusBlocks(instance);
		int color = highlighted ? COLOR_HOVER : COLOR_ROTATE;
		Gizmos.circle(center, (float) radiusBlocks, GizmoStyle.stroke(color, 2.0F)).setAlwaysOnTop();
	}

	private static void emitMirrorHandle(
		OpeningInstance instance,
		Manipulator manipulator,
		boolean highlighted
	) {
		Vec3 position = GizmoCoordinates.localMmToWorldBlocks(instance, manipulator.localAnchor());
		Vec3 direction = GizmoCoordinates.localDirectionToWorld(instance, manipulator.localDirection());
		int color = highlighted ? COLOR_HOVER : COLOR_MIRROR;
		Gizmos.point(position, color, HANDLE_POINT_SIZE * 0.8F).setAlwaysOnTop();
		Vec3 arrowEnd = position.add(direction.scale(ARROW_LENGTH_BLOCKS * 0.75));
		Gizmos.arrow(position, arrowEnd, color, ARROW_WIDTH).setAlwaysOnTop();
	}

	private static double rotateRadiusBlocks(OpeningInstance instance) {
		ParameterSet parameters = instance.parameters();
		double width = length(parameters, "width").orElse(1000.0);
		double depth = length(parameters, "thickness").orElse(length(parameters, "frame_width").orElse(80.0));
		return Math.max(width, depth) / 2000.0 * 0.55;
	}

	private static java.util.Optional<Double> length(ParameterSet parameters, String name) {
		return parameters.get(name)
			.filter(value -> value.type() == ParameterType.LENGTH)
			.map(value -> ((ParameterValue.LengthValue) value).millimeters());
	}

	private static boolean isTarget(GizmoPickTarget target, String handleId) {
		return target != null && target.isResizeHandle() && target.id().equals(handleId);
	}

	private static boolean isManipulatorTarget(
		GizmoPickTarget hovered,
		Manipulator manipulator,
		GizmoPickTarget active
	) {
		if (active != null && active.kind() == GizmoPickTarget.Kind.MANIPULATOR && active.id().equals(manipulator.id())) {
			return true;
		}
		return hovered != null
			&& hovered.kind() == GizmoPickTarget.Kind.MANIPULATOR
			&& hovered.id().equals(manipulator.id());
	}
}
