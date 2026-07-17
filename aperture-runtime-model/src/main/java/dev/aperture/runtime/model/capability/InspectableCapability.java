package dev.aperture.runtime.model.capability;

import java.util.Map;

public interface InspectableCapability extends Capability {
	Map<String, String> inspectionMetadata();
}
