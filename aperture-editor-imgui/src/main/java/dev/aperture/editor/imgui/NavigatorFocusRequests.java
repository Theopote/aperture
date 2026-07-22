package dev.aperture.editor.imgui;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Frontend-neutral intent bridge for focusing a world object from UI navigation. */
public final class NavigatorFocusRequests {
	private static final AtomicReference<ArchitecturalObjectId> PENDING = new AtomicReference<>();
	private NavigatorFocusRequests() { }
	public static void publish(ArchitecturalObjectId objectId) { PENDING.set(objectId); }
	public static Optional<ArchitecturalObjectId> consume() { return Optional.ofNullable(PENDING.getAndSet(null)); }
}
