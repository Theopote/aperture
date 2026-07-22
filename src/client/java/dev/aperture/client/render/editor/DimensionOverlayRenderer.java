package dev.aperture.client.render.editor;

import dev.aperture.client.editor.ClientEditorWorkspace;
import dev.aperture.client.editor.AuthoritativeResizeController;
import dev.aperture.client.editor.OpeningWorldGeometry;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;

/** Displays the primary opening's width dimension and future drag targets. */
public final class DimensionOverlayRenderer {
	private static final int DIMENSION = 0xFF46C2D3;
	private static final int HANDLE = 0xFFFFCC00;

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
		Gizmos.point(geometry.leftWidthHandle(), HANDLE, 9.0F).setAlwaysOnTop();
		int activeHandleColor = AuthoritativeResizeController.dragging() ? 0xFFFF8800
			: AuthoritativeResizeController.hovered() ? 0xFFFFFFFF : HANDLE;
		Gizmos.point(geometry.rightWidthHandle(), activeHandleColor, 11.0F).setAlwaysOnTop();
		String label = Math.round(geometry.widthMm()) + " mm";
		Gizmos.billboardText(label, geometry.dimensionLabel(), TextGizmo.Style.forColorAndCentered(DIMENSION).withScale(.8F))
			.setAlwaysOnTop();
	}
}
