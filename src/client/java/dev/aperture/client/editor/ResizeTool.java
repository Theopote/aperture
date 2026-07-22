package dev.aperture.client.editor;

import dev.aperture.editor.interaction.EditorInputFrame;
import dev.aperture.editor.interaction.EditorTool;
import dev.aperture.editor.interaction.ManipulatorDescriptor;
import dev.aperture.editor.interaction.ManipulatorDescriptorProvider;
import dev.aperture.editor.interaction.LinearParameterDragSession;
import dev.aperture.editor.interaction.GizmoHitTester;
import dev.aperture.editor.interaction.ScreenSpaceHandle;
import dev.aperture.editor.interaction.WorldRay;
import dev.aperture.editor.model.read.ObjectEditorView;
import dev.aperture.editor.model.session.EditorSession;
import dev.aperture.editor.model.session.ToolController;
import dev.aperture.parameter.ParameterType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;

/** Generic linear-parameter resize tool driven by family-provided descriptors. */
public final class ResizeTool implements EditorTool {
	private static final double MILLIMETERS_PER_BLOCK = 1000.0;
	private final EditorSession session;
	private final ManipulatorDescriptorProvider descriptors;
	private final ManipulatorGeometryEvaluator geometry = new ManipulatorGeometryEvaluator();
	private final WorldToScreenProjector projector = new WorldToScreenProjector();
	private final GizmoHitTester hitTester = new GizmoHitTester();
	private boolean hovered;
	private ActiveDrag drag;

	public ResizeTool(EditorSession session, ManipulatorDescriptorProvider descriptors) {
		this.session = session;
		this.descriptors = descriptors;
	}

	@Override public ToolController.Tool id() { return ToolController.Tool.RESIZE; }

	@Override
	public void update(EditorInputFrame input) {
		if (input.cancelPressed()) {
			cancelDrag();
			return;
		}
		ObjectEditorView view = selectedView();
		var evaluated = view == null ? java.util.Optional.<ManipulatorGeometryEvaluator.EvaluatedManipulator>empty()
			: activeManipulator(view);
		if (evaluated.isEmpty() || input.worldRay() == null) {
			cancelDrag();
			hovered = false;
			return;
		}
		var manipulator = evaluated.get();
		Vec3 origin = origin(input.worldRay());
		Vec3 direction = direction(input.worldRay());
		hovered = input.cursor() != null && projector.project(Minecraft.getInstance(), manipulator.handle())
			.map(center -> new ScreenSpaceHandle(manipulator.descriptor().id(), center, 9, 12, 14, true,
				ScreenSpaceHandle.OcclusionPolicy.IGNORE_SCENE_DEPTH,
				ScreenSpaceHandle.DisplayPolicy.ALWAYS_ON_TOP))
			.flatMap(handle -> hitTester.hit(input.cursor(), java.util.List.of(handle)))
			.isPresent();
		if (input.primaryPressed() && hovered) start(view, manipulator, origin, direction);
		if (input.primaryDown() && drag != null) updateDrag(input, origin, direction);
		if (input.primaryReleased() && drag != null) finish();
	}

	@Override public void cancel() { cancelDrag(); hovered = false; }
	@Override public void deactivate() { hovered = false; }

	public boolean available() {
		ObjectEditorView view = selectedView();
		return view != null && activeManipulator(view).isPresent();
	}

	public boolean hovered() { return hovered; }
	public boolean dragging() { return drag != null; }

	private ObjectEditorView selectedView() {
		var primary = session.selection().snapshot().primaryObject();
		return primary == null ? null : session.readModel().object(primary).orElse(null);
	}

	private java.util.Optional<ManipulatorGeometryEvaluator.EvaluatedManipulator> activeManipulator(ObjectEditorView view) {
		return descriptors.descriptors(view).stream()
			.filter(value -> value.kind() == ManipulatorDescriptor.Kind.LINEAR_PARAMETER)
			.filter(value -> value.unit() == ParameterType.LENGTH)
			.map(value -> geometry.evaluate(view, value))
			.flatMap(java.util.Optional::stream)
			.findFirst();
	}

	private void start(ObjectEditorView view, ManipulatorGeometryEvaluator.EvaluatedManipulator manipulator,
		Vec3 rayOrigin, Vec3 rayDirection) {
		double anchor = GizmoDragMath.axisPositionBlocks(rayOrigin, rayDirection,
			manipulator.handle(), manipulator.worldAxis());
		LinearParameterDragSession.begin(session, view, manipulator.descriptor()).ifPresent(value ->
			drag = new ActiveDrag(value, anchor, manipulator.handle(), manipulator.worldAxis()));
	}

	private void updateDrag(EditorInputFrame input, Vec3 rayOrigin, Vec3 rayDirection) {
		double position = GizmoDragMath.axisPositionBlocks(rayOrigin, rayDirection, drag.axisOrigin(), drag.axis());
		double delta = GizmoDragMath.blocksToMillimeters(position - drag.anchorBlocks());
		drag.session().updateDelta(delta, input.shiftDown(), input.controlDown());
	}

	private void finish() {
		LinearParameterDragSession completed = drag.session();
		drag = null;
		completed.finish();
	}

	private void cancelDrag() {
		if (drag != null) drag.session().cancel();
		drag = null;
	}

	private static Vec3 origin(WorldRay ray) {
		return new Vec3(ray.origin().x() / MILLIMETERS_PER_BLOCK,
			ray.origin().y() / MILLIMETERS_PER_BLOCK, ray.origin().z() / MILLIMETERS_PER_BLOCK);
	}

	private static Vec3 direction(WorldRay ray) {
		return new Vec3(ray.direction().x(), ray.direction().y(), ray.direction().z());
	}


	private record ActiveDrag(LinearParameterDragSession session, double anchorBlocks,
		Vec3 axisOrigin, Vec3 axis) { }
}