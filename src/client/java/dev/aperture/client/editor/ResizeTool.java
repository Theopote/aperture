package dev.aperture.client.editor;

import dev.aperture.editor.interaction.EditorInputFrame;
import dev.aperture.editor.interaction.EditorTool;
import dev.aperture.editor.model.command.ExpectedRevision;
import dev.aperture.editor.model.preview.DefaultParameterEditSession;
import dev.aperture.editor.model.read.ObjectEditorView;
import dev.aperture.editor.model.session.EditorSession;
import dev.aperture.editor.model.session.ToolController;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import net.minecraft.world.phys.Vec3;

/** Instance-owned width manipulator migrated from the original static controller. */
public final class ResizeTool implements EditorTool {
	private static final double PICK_RADIUS_BLOCKS = .14;
	private static final double DEFAULT_SNAP_MM = 10.0;
	private final EditorSession session;
	private boolean hovered;
	private ActiveDrag drag;

	public ResizeTool(EditorSession session) {
		this.session = session;
	}

	@Override public ToolController.Tool id() { return ToolController.Tool.RESIZE; }

	@Override
	public void update(EditorInputFrame input) {
		if (input.cancelPressed()) {
			cancelDrag();
			return;
		}
		ObjectEditorView view = selectedView();
		if (view == null || input.worldRay() == null) {
			cancelDrag();
			hovered = false;
			return;
		}
		var geometryOptional = OpeningWorldGeometry.from(view);
		if (geometryOptional.isEmpty()) {
			cancelDrag();
			hovered = false;
			return;
		}
		var geometry = geometryOptional.get();
		Vec3 origin = origin(input.worldRay());
		Vec3 direction = direction(input.worldRay());
		hovered = rayDistanceToPoint(origin, direction, geometry.rightWidthHandle()) <= PICK_RADIUS_BLOCKS;
		if (input.primaryPressed() && hovered) start(view, geometry, origin, direction);
		if (input.primaryDown() && drag != null) updateDrag(input, origin, direction);
		if (input.primaryReleased() && drag != null) finish();
	}

	@Override public void cancel() { cancelDrag(); hovered = false; }
	@Override public void deactivate() { hovered = false; }

	public boolean hovered() { return hovered; }
	public boolean dragging() { return drag != null; }

	private ObjectEditorView selectedView() {
		var primary = session.selection().snapshot().primaryObject();
		return primary == null ? null : session.readModel().object(primary).orElse(null);
	}

	private void start(ObjectEditorView view, OpeningWorldGeometry.Presentation geometry,
		Vec3 rayOrigin, Vec3 rayDirection) {
		if (!(view.parameters().get("width").orElse(null) instanceof ParameterValue.LengthValue width)) return;
		Vec3 axis = geometry.dimensionEnd().subtract(geometry.dimensionStart()).normalize();
		double anchor = GizmoDragMath.axisPositionBlocks(rayOrigin, rayDirection, geometry.rightWidthHandle(), axis);
		var edit = new DefaultParameterEditSession(view.objectId(), "width", width,
			new ExpectedRevision(view.objectRevision(), view.stateRevision()), session.preview(), session.commands());
		drag = new ActiveDrag(edit, width.millimeters(), anchor, geometry.rightWidthHandle(), axis,
			limits(view.objectId()), width.millimeters());
	}

	private void updateDrag(EditorInputFrame input, Vec3 rayOrigin, Vec3 rayDirection) {
		double position = GizmoDragMath.axisPositionBlocks(rayOrigin, rayDirection, drag.axisOrigin(), drag.axis());
		double raw = drag.baseWidthMm() + GizmoDragMath.blocksToMillimeters(position - drag.anchorBlocks());
		double increment = input.shiftDown() ? 1.0 : DEFAULT_SNAP_MM;
		double snapped = input.controlDown() ? raw : Math.round(raw / increment) * increment;
		double constrained = Math.max(drag.limits().minimum(), Math.min(drag.limits().maximum(), snapped));
		if (Math.abs(constrained - drag.previewWidthMm()) >= .001) {
			drag.edit().updatePreview(ParameterValue.length(constrained));
			drag = drag.withPreview(constrained);
		}
	}

	private void finish() {
		ActiveDrag completed = drag;
		drag = null;
		if (Math.abs(completed.previewWidthMm() - completed.baseWidthMm()) < .001) completed.edit().cancel();
		else completed.edit().commit();
	}

	private Limits limits(ArchitecturalObjectId id) {
		for (var section : session.inspector().sections(id)) for (var property : section.properties()) {
			if (property.key().equals("width")) {
				return new Limits(property.minimum().orElse(1.0), property.maximum().orElse(Double.MAX_VALUE));
			}
		}
		return new Limits(1.0, Double.MAX_VALUE);
	}

	private void cancelDrag() {
		if (drag != null) drag.edit().cancel();
		drag = null;
	}

	private static Vec3 origin(EditorInputFrame.WorldRay ray) {
		return new Vec3(ray.originX(), ray.originY(), ray.originZ());
	}

	private static Vec3 direction(EditorInputFrame.WorldRay ray) {
		return new Vec3(ray.directionX(), ray.directionY(), ray.directionZ());
	}

	private static double rayDistanceToPoint(Vec3 origin, Vec3 direction, Vec3 point) {
		double projection = point.subtract(origin).dot(direction);
		if (projection < 0) return Double.MAX_VALUE;
		return origin.add(direction.scale(projection)).distanceTo(point);
	}

	private record Limits(double minimum, double maximum) { }
	private record ActiveDrag(DefaultParameterEditSession edit, double baseWidthMm, double anchorBlocks,
		Vec3 axisOrigin, Vec3 axis, Limits limits, double previewWidthMm) {
		ActiveDrag withPreview(double value) {
			return new ActiveDrag(edit, baseWidthMm, anchorBlocks, axisOrigin, axis, limits, value);
		}
	}
}
