package dev.aperture.client.editor;

import dev.aperture.editor.interaction.EditorInputFrame;
import dev.aperture.editor.interaction.EditorTool;
import dev.aperture.editor.interaction.ManipulatorDescriptor;
import dev.aperture.editor.interaction.ManipulatorDescriptorProvider;
import dev.aperture.editor.interaction.WorldRay;
import dev.aperture.editor.model.command.ExpectedRevision;
import dev.aperture.editor.model.preview.DefaultParameterEditSession;
import dev.aperture.editor.model.read.ObjectEditorView;
import dev.aperture.editor.model.session.EditorSession;
import dev.aperture.editor.model.session.ToolController;
import dev.aperture.parameter.ParameterType;
import dev.aperture.parameter.ParameterValue;
import net.minecraft.world.phys.Vec3;

/** Generic linear-parameter resize tool driven by family-provided descriptors. */
public final class ResizeTool implements EditorTool {
	private static final double MILLIMETERS_PER_BLOCK = 1000.0;
	private static final double PICK_RADIUS_BLOCKS = .14;
	private final EditorSession session;
	private final ManipulatorDescriptorProvider descriptors;
	private final ManipulatorGeometryEvaluator geometry = new ManipulatorGeometryEvaluator();
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
		hovered = rayDistanceToPoint(origin, direction, manipulator.handle()) <= PICK_RADIUS_BLOCKS;
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
		var descriptor = manipulator.descriptor();
		if (!(view.parameters().get(descriptor.parameterKey()).orElse(null) instanceof ParameterValue.LengthValue value)) return;
		double anchor = GizmoDragMath.axisPositionBlocks(rayOrigin, rayDirection,
			manipulator.handle(), manipulator.worldAxis());
		var edit = new DefaultParameterEditSession(view.objectId(), descriptor.parameterKey(), value,
			new ExpectedRevision(view.objectRevision(), view.stateRevision()), session.preview(), session.commands());
		drag = new ActiveDrag(edit, descriptor, value.millimeters(), anchor, manipulator.handle(),
			manipulator.worldAxis(), value.millimeters());
	}

	private void updateDrag(EditorInputFrame input, Vec3 rayOrigin, Vec3 rayDirection) {
		double position = GizmoDragMath.axisPositionBlocks(rayOrigin, rayDirection, drag.axisOrigin(), drag.axis());
		double raw = drag.baseValue() + GizmoDragMath.blocksToMillimeters(position - drag.anchorBlocks());
		double increment = input.shiftDown() ? drag.descriptor().fineSnapIncrement() : drag.descriptor().snapIncrement();
		double snapped = input.controlDown() ? raw : Math.round(raw / increment) * increment;
		double minimum = drag.descriptor().minimum().orElse(1.0);
		double maximum = drag.descriptor().maximum().orElse(Double.MAX_VALUE);
		double constrained = Math.max(minimum, Math.min(maximum, snapped));
		if (Math.abs(constrained - drag.previewValue()) >= .001) {
			drag.edit().updatePreview(ParameterValue.length(constrained));
			drag = drag.withPreview(constrained);
		}
	}

	private void finish() {
		ActiveDrag completed = drag;
		drag = null;
		if (Math.abs(completed.previewValue() - completed.baseValue()) < .001) completed.edit().cancel();
		else completed.edit().commit();
	}

	private void cancelDrag() {
		if (drag != null) drag.edit().cancel();
		drag = null;
	}

	private static Vec3 origin(WorldRay ray) {
		return new Vec3(ray.origin().x() / MILLIMETERS_PER_BLOCK,
			ray.origin().y() / MILLIMETERS_PER_BLOCK, ray.origin().z() / MILLIMETERS_PER_BLOCK);
	}

	private static Vec3 direction(WorldRay ray) {
		return new Vec3(ray.direction().x(), ray.direction().y(), ray.direction().z());
	}

	private static double rayDistanceToPoint(Vec3 origin, Vec3 direction, Vec3 point) {
		double projection = point.subtract(origin).dot(direction);
		if (projection < 0) return Double.MAX_VALUE;
		return origin.add(direction.scale(projection)).distanceTo(point);
	}

	private record ActiveDrag(DefaultParameterEditSession edit, ManipulatorDescriptor descriptor,
		double baseValue, double anchorBlocks, Vec3 axisOrigin, Vec3 axis, double previewValue) {
		ActiveDrag withPreview(double value) {
			return new ActiveDrag(edit, descriptor, baseValue, anchorBlocks, axisOrigin, axis, value);
		}
	}
}