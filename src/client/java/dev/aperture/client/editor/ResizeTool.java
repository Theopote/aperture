package dev.aperture.client.editor;

import dev.aperture.editor.interaction.*;
import dev.aperture.editor.model.read.ObjectEditorView;
import dev.aperture.editor.model.session.EditorSession;
import dev.aperture.editor.model.session.ToolController;
import dev.aperture.parameter.ParameterType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Generic multi-manipulator linear-parameter resize tool. */
public final class ResizeTool implements EditorTool {
	private static final double MILLIMETERS_PER_BLOCK = 1000.0;
	private final EditorSession session;
	private final ManipulatorDescriptorProvider descriptors;
	private final ManipulatorGeometryEvaluator geometry = new ManipulatorGeometryEvaluator();
	private final WorldToScreenProjector projector = new WorldToScreenProjector();
	private final GizmoHitTester hitTester = new GizmoHitTester();
	private Optional<String> hoveredManipulatorId = Optional.empty();
	private ActiveDrag drag;
	private PendingManipulation pending;

	public ResizeTool(EditorSession session, ManipulatorDescriptorProvider descriptors) {
		this.session = session;
		this.descriptors = descriptors;
	}

	@Override public ToolController.Tool id() { return ToolController.Tool.RESIZE; }

	@Override
	public void update(EditorInputFrame input) {
		if (input.cancelPressed()) {
			cancel();
			return;
		}
		ObjectEditorView view = selectedView();
		if (view != null && pending != null) {
			pending.session().refresh(view);
			if (pending.session().state() == ToolInteractionState.IDLE) pending = null;
			else { hoveredManipulatorId = Optional.empty(); return; }
		}
		if (view == null || input.worldRay() == null) {
			cancel();
			return;
		}
		List<ManipulatorGeometryEvaluator.EvaluatedManipulator> evaluated = manipulators(view);
		if (evaluated.isEmpty()) {
			cancel();
			return;
		}
		Map<String, ManipulatorGeometryEvaluator.EvaluatedManipulator> byId = new HashMap<>();
		List<ScreenSpaceHandle> handles = new ArrayList<>();
		for (var manipulator : evaluated) {
			projector.project(Minecraft.getInstance(), manipulator.handle()).ifPresent(center -> {
				String id = manipulator.descriptor().id();
				if (byId.put(id, manipulator) != null) throw new IllegalStateException("Duplicate manipulator ID: " + id);
				handles.add(new ScreenSpaceHandle(id, center, 9, 12, 14, true,
					ScreenSpaceHandle.OcclusionPolicy.IGNORE_SCENE_DEPTH,
					ScreenSpaceHandle.DisplayPolicy.ALWAYS_ON_TOP));
			});
		}
		hoveredManipulatorId = input.cursor() == null ? Optional.empty()
			: hitTester.hit(input.cursor(), handles).map(ScreenSpaceHandle::id);
		Vec3 origin = origin(input.worldRay());
		Vec3 direction = direction(input.worldRay());
		if (input.primaryPressed()) hoveredManipulatorId.map(byId::get)
			.ifPresent(manipulator -> start(view, manipulator, origin, direction));
		if (input.primaryDown() && drag != null) updateDrag(input, origin, direction);
		if (input.primaryReleased() && drag != null) finish();
	}

	@Override public void suspend() {
		if (drag != null) drag.session().cancel();
		drag = null;
		hoveredManipulatorId = Optional.empty();
	}

	@Override public void cancel() {
		suspend();
		if (pending != null && pending.session().state() != ToolInteractionState.PENDING
			&& pending.session().state() != ToolInteractionState.ACCEPTED_WAITING_REPLICA) {
			pending.session().dismiss();
			pending = null;
		}
	}
	@Override public void deactivate() { hoveredManipulatorId = Optional.empty(); }

	public boolean available() {
		ObjectEditorView view = selectedView();
		return view != null && !manipulators(view).isEmpty();
	}

	public Optional<String> hoveredManipulatorId() { return hoveredManipulatorId; }
	public Optional<String> activeManipulatorId() {
		return drag == null ? Optional.empty() : Optional.of(drag.manipulatorId());
	}
	public boolean hovered() { return hoveredManipulatorId.isPresent(); }
	public boolean dragging() { return drag != null; }
	public Optional<String> pendingManipulatorId() { return pending == null ? Optional.empty() : Optional.of(pending.manipulatorId()); }
	public ToolInteractionState interactionState() {
		if (drag != null) return ToolInteractionState.DRAGGING;
		return pending == null ? (hovered() ? ToolInteractionState.HOVER : ToolInteractionState.IDLE) : pending.session().state();
	}

	private ObjectEditorView selectedView() {
		var primary = session.selection().snapshot().primaryObject();
		return primary == null ? null : session.readModel().object(primary).orElse(null);
	}

	public List<ManipulatorGeometryEvaluator.EvaluatedManipulator> manipulators(ObjectEditorView view) {
		return descriptors.descriptors(view).stream()
			.filter(value -> value.kind() == ManipulatorDescriptor.Kind.LINEAR_PARAMETER)
			.filter(value -> value.unit() == ParameterType.LENGTH)
			.map(value -> geometry.evaluate(view, value))
			.flatMap(Optional::stream)
			.toList();
	}

	private void start(ObjectEditorView view, ManipulatorGeometryEvaluator.EvaluatedManipulator manipulator,
		Vec3 rayOrigin, Vec3 rayDirection) {
		double anchor = GizmoDragMath.axisPositionBlocks(rayOrigin, rayDirection,
			manipulator.handle(), manipulator.worldAxis());
		LinearParameterDragSession.begin(session, view, manipulator.descriptor()).ifPresent(value ->
			drag = new ActiveDrag(manipulator.descriptor().id(), value, anchor,
				manipulator.handle(), manipulator.worldAxis()));
	}

	private void updateDrag(EditorInputFrame input, Vec3 rayOrigin, Vec3 rayDirection) {
		double position = GizmoDragMath.axisPositionBlocks(rayOrigin, rayDirection, drag.axisOrigin(), drag.axis());
		double delta = GizmoDragMath.blocksToMillimeters(position - drag.anchorBlocks());
		drag.session().updateDelta(delta, input.shiftDown(), input.controlDown());
	}

	private void finish() {
		ActiveDrag completed = drag;
		drag = null;
		completed.session().finish();
		if (completed.session().state() != ToolInteractionState.CANCELLED) {
			pending = new PendingManipulation(completed.manipulatorId(), completed.session());
		}
	}

	private static Vec3 origin(WorldRay ray) {
		return new Vec3(ray.origin().x() / MILLIMETERS_PER_BLOCK,
			ray.origin().y() / MILLIMETERS_PER_BLOCK, ray.origin().z() / MILLIMETERS_PER_BLOCK);
	}
	private static Vec3 direction(WorldRay ray) {
		return new Vec3(ray.direction().x(), ray.direction().y(), ray.direction().z());
	}

	private record ActiveDrag(String manipulatorId, LinearParameterDragSession session,
		double anchorBlocks, Vec3 axisOrigin, Vec3 axis) { }
	private record PendingManipulation(String manipulatorId, LinearParameterDragSession session) { }
}