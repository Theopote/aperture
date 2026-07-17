package dev.aperture.core.instance;

import dev.aperture.core.state.StatePropertyDefinition;
import dev.aperture.core.state.StateSchema;

import java.util.Set;

/** Built-in compatibility schema until each opening definition supplies its own state schema. */
public final class OpeningStateSchemas {
	public static final StateSchema OPERABLE = StateSchema.builder()
		.property("openRatio", StatePropertyDefinition.number(0.0, 0.0, 1.0, true))
		.property("locked", StatePropertyDefinition.bool(false, true))
		.property("enabled", StatePropertyDefinition.bool(true, true))
		.property("operatingMode", StatePropertyDefinition.enumeration("manual", Set.of("manual", "automatic", "redstone", "maintenance"), true))
		.property("durability", StatePropertyDefinition.number(1.0, 0.0, 1.0, true))
		.property("accessPolicy", StatePropertyDefinition.string("public", true))
		.property("targetOpenRatio", StatePropertyDefinition.number(0.0, 0.0, 1.0, false))
		.property("velocity", StatePropertyDefinition.number(0.0, null, null, false))
		.property("motion", StatePropertyDefinition.enumeration("idle", Set.of("idle", "opening", "closing", "blocked"), false))
		.property("activeInteractor", StatePropertyDefinition.string("", false))
		.property("lastEventTime", StatePropertyDefinition.number(0.0, 0.0, null, false))
		.build();

	private OpeningStateSchemas() { }
}
