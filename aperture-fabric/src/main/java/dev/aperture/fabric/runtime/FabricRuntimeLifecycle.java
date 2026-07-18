package dev.aperture.fabric.runtime;

import dev.aperture.runtime.lifecycle.ArchitecturalRuntime;
import dev.aperture.runtime.lifecycle.RuntimeObjectSession;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.persistence.ArchitecturalObjectSnapshot;
import dev.aperture.runtime.model.event.WorldRef;
import dev.aperture.runtime.model.replication.CommandRequestMessage;
import dev.aperture.runtime.replication.AuthoritativeCommandGateway;

import java.util.Objects;
import java.util.Optional;

/** Minecraft-side lifecycle bridge; BlockEntity remains an anchor, never an authority. */
public final class FabricRuntimeLifecycle {
	private static ArchitecturalRuntime runtime;
	private static AuthoritativeCommandGateway commands;

	private FabricRuntimeLifecycle() { }

	public static synchronized void install(ArchitecturalRuntime value) {
		if (runtime != null) throw new IllegalStateException("Architectural runtime is already installed");
		runtime = Objects.requireNonNull(value, "value");
		commands = new AuthoritativeCommandGateway(runtime, new WorldRef("minecraft:server"));
	}

	public static synchronized boolean installed() { return runtime != null; }

	public static synchronized RuntimeObjectSession activate(ArchitecturalObjectSnapshot snapshot) {
		Objects.requireNonNull(snapshot, "snapshot");
		ArchitecturalRuntime active = requireRuntime();
		return active.find(snapshot.instance().objectId()).orElseGet(() -> active.restore(snapshot));
	}

	public static synchronized Optional<ArchitecturalObjectSnapshot> snapshot(ArchitecturalObjectId objectId) {
		if (runtime == null) return Optional.empty();
		return runtime.find(objectId).map(RuntimeObjectSession::snapshot);
	}

	public static synchronized void unload(ArchitecturalObjectId objectId) {
		if (runtime != null) runtime.unload(objectId);
	}

	public static synchronized void remove(ArchitecturalObjectId objectId) {
		if (runtime != null) runtime.remove(objectId);
	}

	public static synchronized Optional<RuntimeObjectSession> find(ArchitecturalObjectId objectId) {
		return runtime == null ? Optional.empty() : runtime.find(objectId);
	}

	public static synchronized AuthoritativeCommandGateway.Outcome submit(CommandRequestMessage request) {
		if (commands == null) throw new IllegalStateException("Architectural runtime is not installed");
		return commands.handle(request);
	}

	static synchronized void resetForTesting() { runtime = null; commands = null; }

	private static ArchitecturalRuntime requireRuntime() {
		if (runtime == null) throw new IllegalStateException("Architectural runtime is not installed");
		return runtime;
	}
}
