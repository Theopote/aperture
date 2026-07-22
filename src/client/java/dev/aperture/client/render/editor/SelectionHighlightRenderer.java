package dev.aperture.client.render.editor;

import dev.aperture.client.editor.ClientEditorWorkspace;
import dev.aperture.client.editor.OpeningWorldGeometry;
import dev.aperture.editor.model.read.EditorDiagnostic;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;

/** Draws the shared editor selection back into the Minecraft world. */
public final class SelectionHighlightRenderer {
	private static final int SELECTED = 0xFF4C9AFF;
	private static final int PRIMARY = 0xFF46C2D3;
	private static final int WARNING = 0xFFE5B454;
	private static final int ERROR = 0xFFE06C75;
	private static final double MILLIMETERS_PER_BLOCK = 1000.0;

	private SelectionHighlightRenderer() { }

	public static void emit() {
		ClientEditorWorkspace.session().ifPresent(session -> {
			var selection = session.selection().snapshot();
			for (var id : selection.objectIds()) session.readModel().object(id).ifPresent(view -> {
				var origin = view.transform().origin();
				double x = origin.x() / MILLIMETERS_PER_BLOCK;
				double y = origin.y() / MILLIMETERS_PER_BLOCK;
				double z = origin.z() / MILLIMETERS_PER_BLOCK;
				AABB fallback = new AABB(x, y, z, x + 1, y + 1, z + 1);
				AABB bounds = OpeningWorldGeometry.from(view).map(OpeningWorldGeometry.Presentation::bounds).orElse(fallback);
				int color = color(view.diagnostics(), id.equals(selection.primaryObject()));
				Gizmos.cuboid(bounds, GizmoStyle.stroke(color, 2.5F)).setAlwaysOnTop();
			});
		});
	}

	private static int color(java.util.List<EditorDiagnostic> diagnostics, boolean primary) {
		if (diagnostics.stream().anyMatch(d -> d.severity() == EditorDiagnostic.Severity.ERROR)) return ERROR;
		if (diagnostics.stream().anyMatch(d -> d.severity() == EditorDiagnostic.Severity.WARNING)) return WARNING;
		return primary ? PRIMARY : SELECTED;
	}
}
