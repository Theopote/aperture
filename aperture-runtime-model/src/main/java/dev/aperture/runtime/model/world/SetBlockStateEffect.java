package dev.aperture.runtime.model.world;

import dev.aperture.runtime.model.event.SpatialRef;
import java.util.Map;
import java.util.Objects;

public record SetBlockStateEffect(SpatialRef location, String blockType, Map<String, String> properties) implements WorldEffect {
	public SetBlockStateEffect { Objects.requireNonNull(location, "location"); Objects.requireNonNull(blockType, "blockType"); properties = Map.copyOf(properties); }
	@Override public String effectType() { return "aperture:set_block_state"; }
}
