package dev.aperture.runtime.model.world;

import dev.aperture.runtime.model.event.ObjectRef;
import java.util.Objects;

public record UpdateCollisionEffect(ObjectRef target, String collisionDefinitionId) implements WorldEffect {
	public UpdateCollisionEffect { Objects.requireNonNull(target, "target"); Objects.requireNonNull(collisionDefinitionId, "collisionDefinitionId"); }
	@Override public String effectType() { return "aperture:update_collision"; }
}
