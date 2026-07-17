package dev.aperture.runtime.model.world;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.state.RuntimeState;
import java.util.Objects;

public record GetHostStateQuery(ArchitecturalObjectId hostId) implements WorldQueryRequest<RuntimeState> {
	public GetHostStateQuery { Objects.requireNonNull(hostId, "hostId"); }
	@Override public String queryType() { return "aperture:get_host_state"; }
	@Override public Class<RuntimeState> resultType() { return RuntimeState.class; }
}
