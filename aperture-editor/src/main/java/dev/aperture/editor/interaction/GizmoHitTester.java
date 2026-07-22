package dev.aperture.editor.interaction;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/** Deterministically resolves enabled handles using their pixel hover radius. */
public final class GizmoHitTester {
	public Optional<ScreenSpaceHandle> hit(ScreenPoint cursor, Collection<ScreenSpaceHandle> handles) {
		return handles.stream()
			.filter(ScreenSpaceHandle::enabled)
			.filter(handle -> handle.center().distanceSquared(cursor)
				<= handle.hoverRadiusPixels() * handle.hoverRadiusPixels())
			.min(Comparator.comparingDouble(handle -> handle.center().distanceSquared(cursor)));
	}
}
