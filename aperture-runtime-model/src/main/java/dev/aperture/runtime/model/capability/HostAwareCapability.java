package dev.aperture.runtime.model.capability;

import dev.aperture.runtime.model.object.HostBinding;

import java.util.List;

public interface HostAwareCapability extends Capability {
	List<HostBinding> hostBindings();
}
