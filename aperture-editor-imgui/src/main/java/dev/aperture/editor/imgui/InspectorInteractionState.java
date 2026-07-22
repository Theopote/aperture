package dev.aperture.editor.imgui;

import java.util.Optional;

/** Render-thread bridge from semantic Inspector hover to world presenters. */
public final class InspectorInteractionState {
	private static volatile String hoveredParameter;

	private InspectorInteractionState() { }

	public static void beginFrame() { hoveredParameter = null; }
	public static void hover(String parameterKey) { hoveredParameter = parameterKey; }
	public static Optional<String> hoveredParameter() { return Optional.ofNullable(hoveredParameter); }
}
