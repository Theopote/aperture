package dev.aperture.runtime.model.capability;

/** Canonical capability identities shared by families, runtime services, and adapters. */
public final class StandardCapabilities {
	public static final CapabilityKey<OpenableCapability> OPENABLE = key("openable", OpenableCapability.class);
	public static final CapabilityKey<LockableCapability> LOCKABLE = key("lockable", LockableCapability.class);
	public static final CapabilityKey<InteractableCapability> INTERACTABLE = key("interactable", InteractableCapability.class);
	public static final CapabilityKey<HostAwareCapability> HOST_AWARE = key("host_aware", HostAwareCapability.class);
	public static final CapabilityKey<RenderableCapability> RENDERABLE = key("renderable", RenderableCapability.class);
	public static final CapabilityKey<CollidableCapability> COLLIDABLE = key("collidable", CollidableCapability.class);
	public static final CapabilityKey<PersistableCapability> PERSISTABLE = key("persistable", PersistableCapability.class);
	public static final CapabilityKey<ReplicableCapability> REPLICABLE = key("replicable", ReplicableCapability.class);
	public static final CapabilityKey<InspectableCapability> INSPECTABLE = key("inspectable", InspectableCapability.class);

	private StandardCapabilities() { }

	private static <T extends Capability> CapabilityKey<T> key(String path, Class<T> type) {
		return CapabilityKey.of("aperture:" + path, type);
	}
}
