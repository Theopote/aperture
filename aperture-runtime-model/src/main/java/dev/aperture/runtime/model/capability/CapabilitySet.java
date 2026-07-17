package dev.aperture.runtime.model.capability;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable heterogeneous map whose keys retain the capability runtime type. */
public final class CapabilitySet implements CapabilityProvider {
	private static final CapabilitySet EMPTY = new CapabilitySet(Map.of());
	private final Map<CapabilityKey<?>, Capability> capabilities;

	private CapabilitySet(Map<CapabilityKey<?>, Capability> capabilities) {
		this.capabilities = Map.copyOf(capabilities);
	}

	public static CapabilitySet empty() { return EMPTY; }
	public static Builder builder() { return new Builder(); }

	@Override
	public <T extends Capability> Optional<T> capability(CapabilityKey<T> key) {
		Objects.requireNonNull(key, "key");
		Capability value = capabilities.get(key);
		return value == null ? Optional.empty() : Optional.of(key.cast(value));
	}

	public Set<CapabilityKey<?>> keys() { return capabilities.keySet(); }

	public static final class Builder {
		private final Map<CapabilityKey<?>, Capability> capabilities = new LinkedHashMap<>();

		public <T extends Capability> Builder add(CapabilityKey<T> key, T capability) {
			Objects.requireNonNull(key, "key");
			Objects.requireNonNull(capability, "capability");
			if (!key.type().isInstance(capability)) {
				throw new IllegalArgumentException("Capability does not implement " + key.type().getName());
			}
			if (capabilities.putIfAbsent(key, capability) != null) {
				throw new IllegalArgumentException("Duplicate capability: " + key.id());
			}
			return this;
		}

		public CapabilitySet build() { return new CapabilitySet(capabilities); }
	}
}
