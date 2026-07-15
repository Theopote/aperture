package dev.aperture.opening.resolve;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.component.FrameComponent;
import dev.aperture.core.component.PanelComponent;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.geometry.profile.ProfileDefinition;
import dev.aperture.opening.geometry.generator.GenerationTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ComponentProfileResolverTest {
	@Test
	void resolvesScaledProfilePerComponentInstance() {
		var context = GenerationTestSupport.context(BuiltinOpeningTypes.door(), ParameterSet.empty());

		ProfileDefinition frame = ComponentProfileResolver.resolve(
			context,
			FrameComponent.of("door_frame", "aperture:frame_standard_50")
		);
		ProfileDefinition panel = ComponentProfileResolver.resolve(
			context,
			PanelComponent.of("door_leaf", "aperture:frame_standard_50", "left")
		);

		assertEquals(80, frame.bounds().width(), 0.01);
		assertNotEquals(frame.bounds().width(), panel.bounds().width(), 0.01);
	}
}
