package dev.aperture.runtime.model.world;

import dev.aperture.runtime.model.event.WorldRef;
import java.util.Objects;

public record GetWeatherQuery(WorldRef world) implements WorldQueryRequest<String> {
	public GetWeatherQuery { Objects.requireNonNull(world, "world"); }
	@Override public String queryType() { return "aperture:get_weather"; }
	@Override public Class<String> resultType() { return String.class; }
}
