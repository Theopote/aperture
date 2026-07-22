package dev.aperture.editor.interaction;

import dev.aperture.parameter.ParameterType;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;

class ManipulatorDescriptorTest {
	@Test
	void rejectsInvalidSnapConfiguration() {
		assertThrows(IllegalArgumentException.class, () -> new ManipulatorDescriptor("door.width.right",
			ManipulatorDescriptor.Kind.LINEAR_PARAMETER, "Width", "width", ManipulatorDescriptor.Axis.LOCAL_X,
			ManipulatorDescriptor.Anchor.RIGHT_MIDPOINT, ManipulatorDescriptor.Anchor.LEFT_MIDPOINT,
			ManipulatorDescriptor.DirectionPolicy.POSITIVE, OptionalDouble.empty(), OptionalDouble.empty(),
			0, 1, ParameterType.LENGTH));
	}
}
