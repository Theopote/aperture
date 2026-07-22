package dev.aperture.client.render.editor;

import dev.aperture.client.editor.ClientEditorWorkspace;
import dev.aperture.client.editor.ClientWorldUnits;
import dev.aperture.client.editor.OpeningWorldGeometry;
import dev.aperture.editor.imgui.NavigatorInteractionState;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;

/** Temporary world outline for the object currently inspected in Project Navigator. */
final class NavigatorHoverRenderer {
	private static final int HOVER = 0xFFFFFFFF;
	private NavigatorHoverRenderer() { }

	static void emit() {
		ClientEditorWorkspace.session().ifPresent(session -> NavigatorInteractionState.hoveredObject().ifPresent(id -> {
			if (session.selection().snapshot().objectIds().contains(id)) return;
			session.readModel().object(id).ifPresent(view -> {
				var origin = ClientWorldUnits.toBlocks(view.transform().origin());
				AABB fallback = new AABB(origin.x, origin.y, origin.z, origin.x + 1, origin.y + 1, origin.z + 1);
				AABB bounds = OpeningWorldGeometry.from(view).map(OpeningWorldGeometry.Presentation::bounds).orElse(fallback);
				Gizmos.cuboid(bounds, GizmoStyle.stroke(HOVER, 2.0F)).setAlwaysOnTop();
			});
		}));
	}
}
