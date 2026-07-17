package dev.aperture.runtime.model.world;

import dev.aperture.runtime.model.event.ActorRef;
import dev.aperture.runtime.model.event.SpatialRef;
import java.util.List;
import java.util.Objects;

public record FindNearbyActorsQuery(SpatialRef center, double radius) implements WorldQueryRequest<List<ActorRef>> {
	public FindNearbyActorsQuery {
		Objects.requireNonNull(center, "center");
		if (!Double.isFinite(radius) || radius < 0) throw new IllegalArgumentException("radius must be finite and non-negative");
	}
	@Override public String queryType() { return "aperture:find_nearby_actors"; }
	@SuppressWarnings("unchecked") @Override public Class<List<ActorRef>> resultType() { return (Class<List<ActorRef>>) (Class<?>) List.class; }
}
