package dev.aperture.client.editor;

import dev.aperture.editor.model.command.ExpectedRevision;
import dev.aperture.editor.model.preview.DefaultParameterEditSession;
import dev.aperture.editor.model.session.EditorSession;
import dev.aperture.editor.model.session.ToolController;
import dev.aperture.parameter.ParameterValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Door-width resize vertical slice: hover, preview drag, cancel, and single commit. */
public final class AuthoritativeResizeController {
	private static final double PICK_RADIUS_BLOCKS = .14;
	private static final double DEFAULT_SNAP_MM = 10.0;
	private static boolean hovered;
	private static ActiveDrag drag;
	private static boolean wasMouseDown;
	private static boolean wasEscapeDown;

	private AuthoritativeResizeController() { }

	public static void tick(Minecraft client) {
		var sessionOptional = ClientEditorWorkspace.session();
		if (sessionOptional.isEmpty() || client.player == null || client.level == null) { reset(); return; }
		EditorSession session = sessionOptional.get();
		if (session.tools().activeTool() != ToolController.Tool.RESIZE) { cancelDrag(); hovered = false; return; }
		if (client.screen != null) { cancelDrag(); hovered = false; return; }
		var primary = session.selection().snapshot().primaryObject();
		if (primary == null) { cancelDrag(); hovered = false; return; }
		var viewOptional = session.readModel().object(primary);
		if (viewOptional.isEmpty()) { cancelDrag(); hovered = false; return; }
		var view = viewOptional.get();
		var geometryOptional = OpeningWorldGeometry.from(view);
		if (geometryOptional.isEmpty()) { cancelDrag(); hovered = false; return; }
		var geometry = geometryOptional.get();

		Vec3 rayOrigin = client.gameRenderer.getMainCamera().position();
		Vec3 rayDirection = client.player.getViewVector(1.0F).normalize();
		hovered = rayDistanceToPoint(rayOrigin, rayDirection, geometry.rightWidthHandle()) <= PICK_RADIUS_BLOCKS;
		long window = client.getWindow().handle();
		boolean mouseDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean escapeDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;
		if (escapeDown && !wasEscapeDown) cancelDrag();
		if (mouseDown && !wasMouseDown && hovered) start(session, view, geometry, rayOrigin, rayDirection);
		if (mouseDown && drag != null) update(client, view, geometry, rayOrigin, rayDirection);
		if (!mouseDown && wasMouseDown && drag != null) finish();
		wasMouseDown = mouseDown;
		wasEscapeDown = escapeDown;
	}

	public static boolean hovered() { return hovered; }
	public static boolean dragging() { return drag != null; }

	private static void start(EditorSession session, dev.aperture.editor.model.read.ObjectEditorView view,
		OpeningWorldGeometry.Presentation geometry, Vec3 rayOrigin, Vec3 rayDirection) {
		if (!(view.parameters().get("width").orElse(null) instanceof ParameterValue.LengthValue width)) return;
		Vec3 axis = geometry.dimensionEnd().subtract(geometry.dimensionStart()).normalize();
		double anchor = GizmoDragMath.axisPositionBlocks(rayOrigin, rayDirection, geometry.rightWidthHandle(), axis);
		var edit = new DefaultParameterEditSession(view.objectId(), "width", width,
			new ExpectedRevision(view.objectRevision(), view.stateRevision()), session.preview(), session.commands());
		drag = new ActiveDrag(edit, width.millimeters(), anchor, geometry.rightWidthHandle(), axis, limits(session, view.objectId()));
	}

	private static void update(Minecraft client, dev.aperture.editor.model.read.ObjectEditorView view,
		OpeningWorldGeometry.Presentation geometry, Vec3 rayOrigin, Vec3 rayDirection) {
		double position = GizmoDragMath.axisPositionBlocks(rayOrigin, rayDirection, drag.axisOrigin(), drag.axis());
		double raw = drag.baseWidthMm() + GizmoDragMath.blocksToMillimeters(position - drag.anchorBlocks());
		double increment = keyDown(client, GLFW.GLFW_KEY_LEFT_SHIFT) || keyDown(client, GLFW.GLFW_KEY_RIGHT_SHIFT) ? 1.0 : DEFAULT_SNAP_MM;
		boolean snapDisabled = keyDown(client, GLFW.GLFW_KEY_LEFT_CONTROL) || keyDown(client, GLFW.GLFW_KEY_RIGHT_CONTROL);
		double snapped = snapDisabled ? raw : Math.round(raw / increment) * increment;
		double constrained = Math.max(drag.limits().minimum(), Math.min(drag.limits().maximum(), snapped));
		if (Math.abs(constrained - drag.previewWidthMm()) >= .001) {
			drag.edit().updatePreview(ParameterValue.length(constrained));
			drag = drag.withPreview(constrained);
		}
	}

	private static void finish() {
		ActiveDrag completed = drag;
		drag = null;
		if (Math.abs(completed.previewWidthMm() - completed.baseWidthMm()) < .001) completed.edit().cancel();
		else completed.edit().commit();
	}

	private static Limits limits(EditorSession session, dev.aperture.runtime.model.object.ArchitecturalObjectId id) {
		for (var section : session.inspector().sections(id)) for (var property : section.properties()) {
			if (property.key().equals("width")) return new Limits(property.minimum().orElse(1.0), property.maximum().orElse(Double.MAX_VALUE));
		}
		return new Limits(1.0, Double.MAX_VALUE);
	}

	private static boolean keyDown(Minecraft client, int key) {
		return GLFW.glfwGetKey(client.getWindow().handle(), key) == GLFW.GLFW_PRESS;
	}

	private static double rayDistanceToPoint(Vec3 origin, Vec3 direction, Vec3 point) {
		double projection = point.subtract(origin).dot(direction);
		if (projection < 0) return Double.MAX_VALUE;
		return origin.add(direction.scale(projection)).distanceTo(point);
	}

	private static void cancelDrag() {
		if (drag != null) drag.edit().cancel();
		drag = null;
	}

	public static void reset() {
		cancelDrag(); hovered = false; wasMouseDown = false; wasEscapeDown = false;
	}

	private record Limits(double minimum, double maximum) { }
	private record ActiveDrag(DefaultParameterEditSession edit, double baseWidthMm, double anchorBlocks,
		Vec3 axisOrigin, Vec3 axis, Limits limits, double previewWidthMm) {
		ActiveDrag(DefaultParameterEditSession edit, double baseWidthMm, double anchorBlocks,
			Vec3 axisOrigin, Vec3 axis, Limits limits) {
			this(edit, baseWidthMm, anchorBlocks, axisOrigin, axis, limits, baseWidthMm);
		}
		ActiveDrag withPreview(double value) {
			return new ActiveDrag(edit, baseWidthMm, anchorBlocks, axisOrigin, axis, limits, value);
		}
	}
}
