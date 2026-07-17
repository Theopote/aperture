package dev.aperture.runtime.model.world;

import dev.aperture.runtime.model.event.WorldRef;
import java.time.Instant;
import java.util.Objects;

public record GetTimeQuery(WorldRef world) implements WorldQueryRequest<Instant> {
	public GetTimeQuery { Objects.requireNonNull(world, "world"); }
	@Override public String queryType() { return "aperture:get_time"; }
	@Override public Class<Instant> resultType() { return Instant.class; }
}
