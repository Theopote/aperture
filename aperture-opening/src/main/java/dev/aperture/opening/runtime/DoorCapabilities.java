package dev.aperture.opening.runtime;

import dev.aperture.runtime.model.capability.CapabilitySet;
import dev.aperture.runtime.model.capability.LockableCapability;
import dev.aperture.runtime.model.capability.OpenableCapability;
import dev.aperture.runtime.model.capability.StandardCapabilities;
import dev.aperture.runtime.model.state.RuntimeState;

public final class DoorCapabilities {
	private DoorCapabilities() { }

	public static CapabilitySet from(RuntimeState state) {
		boolean enabled = DoorStateSchema.bool(state, DoorStateSchema.ENABLED);
		boolean locked = DoorStateSchema.bool(state, DoorStateSchema.LOCKED);
		OpenableCapability openable = new OpenableCapability() {
			@Override public double currentRatio() { return DoorStateSchema.number(state, DoorStateSchema.OPEN_RATIO); }
			@Override public double targetRatio() { return DoorStateSchema.number(state, DoorStateSchema.TARGET_OPEN_RATIO); }
			@Override public boolean canRequestOpen() { return enabled && !locked && currentRatio() < 1; }
			@Override public boolean canRequestClose() { return enabled && currentRatio() > 0; }
		};
		LockableCapability lockable = new LockableCapability() {
			@Override public boolean locked() { return locked; }
			@Override public boolean canChangeLock() { return enabled && openable.currentRatio() == 0; }
		};
		return CapabilitySet.builder()
			.add(StandardCapabilities.OPENABLE, openable)
			.add(StandardCapabilities.LOCKABLE, lockable)
			.build();
	}
}
