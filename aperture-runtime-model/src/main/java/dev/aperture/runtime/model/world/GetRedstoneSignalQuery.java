package dev.aperture.runtime.model.world;

import dev.aperture.runtime.model.event.SpatialRef;
import java.util.Objects;

public record GetRedstoneSignalQuery(SpatialRef location) implements WorldQueryRequest<Integer> {
	public GetRedstoneSignalQuery { Objects.requireNonNull(location, "location"); }
	@Override public String queryType() { return "aperture:get_redstone_signal"; }
	@Override public Class<Integer> resultType() { return Integer.class; }
}
