package dev.aperture.editor.interaction;

import java.util.Objects;

/** Pixel-stable interactive handle independent of camera distance and FOV. */
public record ScreenSpaceHandle(String id, ScreenPoint center, double normalRadiusPixels,
	double hoverRadiusPixels, double activeRadiusPixels, boolean enabled,
	OcclusionPolicy occlusionPolicy, DisplayPolicy displayPolicy) {
	public enum OcclusionPolicy { REQUIRE_VISIBLE, IGNORE_SCENE_DEPTH }
	public enum DisplayPolicy { DEPTH_TESTED, ALWAYS_ON_TOP }

	public ScreenSpaceHandle {
		if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
		Objects.requireNonNull(center, "center");
		if (normalRadiusPixels <= 0 || hoverRadiusPixels < normalRadiusPixels
			|| activeRadiusPixels < hoverRadiusPixels) throw new IllegalArgumentException("invalid handle radii");
		Objects.requireNonNull(occlusionPolicy, "occlusionPolicy");
		Objects.requireNonNull(displayPolicy, "displayPolicy");
	}
}
