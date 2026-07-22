package dev.aperture.editor.imgui;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Thread-safe handoff from a host viewport hit-test to the ImGui dimension popup. */
public final class DimensionEditRequests {
	private static final AtomicReference<Request> pending = new AtomicReference<>();

	private DimensionEditRequests() { }
	public static void publish(Request request) { pending.set(request); }
	public static Optional<Request> consume() { return Optional.ofNullable(pending.getAndSet(null)); }

	public record Request(ArchitecturalObjectId objectId, String parameterKey, double baseMillimeters,
		long objectRevision, long stateRevision) { }
}
