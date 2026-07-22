package dev.aperture.editor.imgui;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;

import java.util.Optional;

/** Semantic Navigator hover consumed by the world selection presenter. */
public final class NavigatorInteractionState {
	private static volatile ArchitecturalObjectId hoveredObject;
	private NavigatorInteractionState() { }
	public static void beginFrame() { hoveredObject = null; }
	public static void hover(ArchitecturalObjectId objectId) { hoveredObject = objectId; }
	public static Optional<ArchitecturalObjectId> hoveredObject() { return Optional.ofNullable(hoveredObject); }
}
