package dev.aperture.core.catalog;

import dev.aperture.core.component.ComponentAssemblyPresets;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.opening.OpeningCategory;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.core.parametric.NumberUnit;
import dev.aperture.core.parametric.RangeParameter;

/**
 * Non-catalog test fixtures. Not registered at runtime — use for pipeline/parameter tests only.
 */
public final class OpeningTestFixtures {
	private static final OpeningId CASEMENT_TEST_ID = OpeningId.aperture("test_casement");

	private OpeningTestFixtures() {
	}

	public static OpeningId casementTestId() {
		return CASEMENT_TEST_ID;
	}

	/**
	 * Panel + open-angle fixture for kinematics and generation tests. Not a shipped opening type.
	 */
	public static OpeningTypeDefinition casementWindow() {
		return OpeningTypeDefinition.builder(CASEMENT_TEST_ID, OpeningCategory.WINDOW, BuiltinOpeningTypes.RECTANGULAR_WINDOW_GENERATOR)
			.parameter("width", RangeParameter.builder(NumberUnit.LENGTH_MM)
				.defaultNumber(1200)
				.min(300)
				.max(6000)
				.build())
			.parameter("height", RangeParameter.builder(NumberUnit.LENGTH_MM)
				.defaultNumber(1500)
				.min(300)
				.max(4000)
				.build())
			.parameter("mullions", RangeParameter.builder(NumberUnit.COUNT)
				.defaultNumber(0)
				.min(0)
				.max(10)
				.build())
			.parameter("frame_width", RangeParameter.builder(NumberUnit.LENGTH_MM)
				.defaultNumber(50)
				.min(20)
				.max(150)
				.build())
			.parameter("frame_depth", RangeParameter.builder(NumberUnit.LENGTH_MM)
				.defaultNumber(80)
				.min(30)
				.max(200)
				.build())
			.parameter("open_angle", RangeParameter.builder(NumberUnit.ANGLE_DEG)
				.defaultNumber(0)
				.min(0)
				.max(90)
				.build())
			.materialSlot("frame")
			.materialSlot("glazing")
			.components(ComponentAssemblyPresets.casementWindow(
				"aperture:frame_l_50x80",
				"aperture:frame_standard_50",
				"aperture:single_glazed"
			))
			.build();
	}
}
