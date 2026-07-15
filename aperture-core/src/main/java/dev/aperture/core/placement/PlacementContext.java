package dev.aperture.core.placement;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.instance.HostBinding;
import dev.aperture.core.instance.OpeningInstance;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Host and neighborhood information used during placement validation.
 */
public record PlacementContext(
	HostBinding host,
	BoundingBox hostBounds,
	Collection<OpeningInstance> existingInstances
) {
	public PlacementContext {
		Objects.requireNonNull(host, "host");
		Objects.requireNonNull(hostBounds, "hostBounds");
		existingInstances = List.copyOf(existingInstances);
	}

	public static PlacementContext of(HostBinding host, BoundingBox hostBounds) {
		return new PlacementContext(host, hostBounds, List.of());
	}
}
