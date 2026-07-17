package dev.aperture.runtime.model.world;

import dev.aperture.math.BoundingBox;
import dev.aperture.runtime.model.event.WorldRef;
import java.util.Objects;

public record IsAreaClearQuery(WorldRef world, BoundingBox area) implements WorldQueryRequest<Boolean> {
	public IsAreaClearQuery { Objects.requireNonNull(world, "world"); Objects.requireNonNull(area, "area"); }
	@Override public String queryType() { return "aperture:is_area_clear"; }
	@Override public Class<Boolean> resultType() { return Boolean.class; }
}
