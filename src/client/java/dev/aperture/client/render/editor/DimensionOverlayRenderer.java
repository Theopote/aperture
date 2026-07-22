package dev.aperture.client.render.editor;

import dev.aperture.client.editor.ClientEditorWorkspace;
import dev.aperture.client.editor.OpeningWorldGeometry;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;

/** Displays the primary opening's width dimension and future drag targets. */
public final class DimensionOverlayRenderer {
	private static final int DIMENSION = 0xFF46C2D3;
	private static final int HANDLE = 0xFFFFCC00;
	private static final int FIXED_ANCHOR = 0xFF87949A;

	private DimensionOverlayRenderer() { }

	public static void emit() {
		ClientEditorWorkspace.session().ifPresent(session -> {
			var primary = session.selection().snapshot().primaryObject();
			if (primary == null) return;
			session.readModel().object(primary).flatMap(OpeningWorldGeometry::from).ifPresent(DimensionOverlayRenderer::emit);
		});
	}

	private static void emit(OpeningWorldGeometry.Presentation geometry) {
		Gizmos.line(geometry.dimensionStart(), geometry.dimensionEnd(), DIMENSION, 2.0F).setAlwaysOnTop();
		Gizmos.line(geometry.dimensionStart().add(0, -.08, 0), geometry.dimensionStart().add(0, .08, 0), DIMENSION, 2.0F).setAlwaysOnTop();
		Gizmos.line(geometry.dimensionEnd().add(0, -.08, 0), geometry.dimensionEnd().add(0, .08, 0), DIMENSION, 2.0F).setAlwaysOnTop();
		var anchor = geometry.leftWidthHandle();
		Gizmos.line(anchor.add(0, -.06, -.06), anchor.add(0, .06, .06), FIXED_ANCHOR, 2.0F).setAlwaysOnTop();
		Gizmos.line(anchor.add(0, -.06, .06), anchor.add(0, .06, -.06), FIXED_ANCHOR, 2.0F).setAlwaysOnTop();
		var resize = ClientEditorWorkspace.resizeState();
		Gizmos.point(geometry.rightWidthHandle(), handleColor(resize, "door.width.right"), 11.0F).setAlwaysOnTop();
		Gizmos.point(geometry.topHeightHandle(), handleColor(resize, "door.height.top"), 11.0F).setAlwaysOnTop();
		var bottom = geometry.bottomHeightHandle();
		Gizmos.line(bottom.add(-.06, 0, -.06), bottom.add(.06, 0, .06), FIXED_ANCHOR, 2.0F).setAlwaysOnTop();
		Gizmos.line(bottom.add(-.06, 0, .06), bottom.add(.06, 0, -.06), FIXED_ANCHOR, 2.0F).setAlwaysOnTop();
		String label = Math.round(geometry.widthMm()) + " mm";
		Gizmos.billboardText(label, geometry.dimensionLabel(), TextGizmo.Style.forColorAndCentered(DIMENSION).withScale(.8F))
			.setAlwaysOnTop();
		String heightLabel = Math.round(geometry.heightMm()) + " mm";
		Gizmos.billboardText(heightLabel, geometry.heightDimensionLabel(),
			TextGizmo.Style.forColorAndCentered(DIMENSION).withScale(.8F)).setAlwaysOnTop();
	}

	private static int handleColor(ClientEditorWorkspace.ResizeState state, String id) {
		if (state.activeManipulatorId().filter(id::equals).isPresent()) return 0xFFFF8800;
		if (state.pendingManipulatorId().filter(id::equals).isPresent()) return switch (state.interactionState()) {
			case REJECTED, CONFLICT -> 0xFFFF4D5A;
			case PENDING, ACCEPTED_WAITING_REPLICA -> 0xFF3DAEE9;
			default -> HANDLE;
		};
		if (state.hoveredManipulatorId().filter(id::equals).isPresent()) return 0xFFFFFFFF;
		return HANDLE;
	}
}
