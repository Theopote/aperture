package dev.aperture.core.catalog;

import dev.aperture.core.component.ComponentAssemblyPresets;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.opening.GeneratorId;
import dev.aperture.core.opening.OpeningCategory;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.core.parametric.BooleanParameter;
import dev.aperture.core.parametric.ChoiceOption;
import dev.aperture.core.parametric.ChoiceParameter;
import dev.aperture.core.parametric.MaterialParameter;
import dev.aperture.core.parametric.NumberUnit;
import dev.aperture.core.parametric.ParameterMetadata;
import dev.aperture.core.parametric.RangeParameter;

import java.util.List;
import java.util.Set;

/**
 * Built-in reference opening types for platform development and integration testing.
 *
 * <p><strong>Family library freeze:</strong> only {@link #fixedWindow()}, {@link #door()}, and
 * {@link #curtainWall()} are allowed reference types while Phase B/C platform work is in progress.
 * Do not add new door/window variants here — see {@code docs/architecture/13-platform-roadmap-af.md}.
 *
 * <p>Runtime catalog entries load from {@code aperture-data/aperture/opening_types/*.json}.
 * These Java factories mirror those packs for unit tests and stay in sync with them.
 */
public final class BuiltinOpeningTypes {
	public static final OpeningId FIXED_WINDOW_ID = OpeningId.aperture("fixed_window");
	public static final OpeningId DOOR_ID = OpeningId.aperture("door");
	public static final OpeningId CURTAIN_WALL_ID = OpeningId.aperture("curtain_wall");
	public static final GeneratorId RECTANGULAR_WINDOW_GENERATOR = GeneratorId.parse("aperture:rectangular_window_v1");

	private static final Set<OpeningId> REFERENCE_IDS = Set.of(FIXED_WINDOW_ID, DOOR_ID, CURTAIN_WALL_ID);

	public static Set<OpeningId> referenceIds() {
		return REFERENCE_IDS;
	}

	public static boolean isReferenceType(OpeningId id) {
		return REFERENCE_IDS.contains(id);
	}

	public static List<OpeningTypeDefinition> referenceDefinitions() {
		return List.of(fixedWindow(), door(), curtainWall());
	}

	public static OpeningTypeDefinition fixedWindow() {
		return OpeningTypeDefinition.builder(FIXED_WINDOW_ID, OpeningCategory.WINDOW, RECTANGULAR_WINDOW_GENERATOR)
			.parameter("width", RangeParameter.builder(NumberUnit.LENGTH_MM)
				.defaultNumber(1200)
				.min(300)
				.max(6000)
				.step(10)
				.metadata(ParameterMetadata.grouped("Width", "Dimensions"))
				.build())
			.parameter("height", RangeParameter.builder(NumberUnit.LENGTH_MM)
				.defaultNumber(1500)
				.min(300)
				.max(4000)
				.step(10)
				.metadata(ParameterMetadata.grouped("Height", "Dimensions"))
				.build())
			.parameter("mullions", RangeParameter.builder(NumberUnit.COUNT)
				.defaultNumber(0)
				.min(0)
				.max(10)
				.metadata(ParameterMetadata.grouped("Mullions", "Layout"))
				.build())
			.parameter("frame_width", RangeParameter.builder(NumberUnit.LENGTH_MM)
				.defaultNumber(50)
				.min(20)
				.max(150)
				.metadata(ParameterMetadata.grouped("Frame Width", "Frame"))
				.build())
			.materialSlot("frame")
			.materialSlot("glazing")
			.components(ComponentAssemblyPresets.fixedWindow(
				"aperture:frame_l_50x80",
				"aperture:single_glazed"
			))
			.build();
	}

	/**
	 * Parametric door family — a component composition, not a separate generator.
	 */
	public static OpeningTypeDefinition door() {
		return OpeningTypeDefinition.builder(DOOR_ID, OpeningCategory.DOOR, RECTANGULAR_WINDOW_GENERATOR)
			.parameter("width", RangeParameter.builder(NumberUnit.LENGTH_MM)
				.defaultNumber(1200)
				.min(600)
				.max(2400)
				.step(10)
				.metadata(ParameterMetadata.grouped("Width", "Dimensions"))
				.build())
			.parameter("height", RangeParameter.builder(NumberUnit.LENGTH_MM)
				.defaultNumber(2300)
				.min(1800)
				.max(3000)
				.step(10)
				.metadata(ParameterMetadata.grouped("Height", "Dimensions"))
				.build())
			.parameter("thickness", RangeParameter.builder(NumberUnit.LENGTH_MM)
				.defaultNumber(60)
				.min(35)
				.max(120)
				.metadata(ParameterMetadata.grouped("Thickness", "Dimensions"))
				.build())
			.parameter("panel_count", RangeParameter.builder(NumberUnit.COUNT)
				.defaultNumber(2)
				.min(1)
				.max(6)
				.metadata(ParameterMetadata.grouped("Panel Count", "Layout"))
				.build())
			.parameter("glass_ratio", RangeParameter.builder(NumberUnit.RATIO)
				.defaultNumber(0.35)
				.min(0)
				.max(1)
				.step(0.05)
				.metadata(ParameterMetadata.grouped("Glass Ratio", "Layout"))
				.build())
			.parameter("frame_width", RangeParameter.builder(NumberUnit.LENGTH_MM)
				.defaultNumber(80)
				.min(40)
				.max(200)
				.metadata(ParameterMetadata.grouped("Frame Width", "Frame"))
				.build())
			.parameter("hinge_side", ChoiceParameter.of(
				List.of(
					new ChoiceOption("left", "Left"),
					new ChoiceOption("right", "Right")
				),
				"left"
			))
			.parameter("has_transom", BooleanParameter.of(false, ParameterMetadata.labeled("Transom")))
			.parameter("frame_material", MaterialParameter.of(
				"minecraft:oak_planks",
				ParameterMetadata.grouped("Frame Material", "Materials")
			))
			.constraint("width > height * 0.5", "Width must exceed half the height")
			.constraint("panel_count >= 1", "At least one panel is required")
			.constraint("glass_ratio >= 0 and glass_ratio <= 1", "Glass ratio must stay between 0 and 1")
			.materialSlot("frame")
			.materialSlot("glazing")
			.materialSlot("hardware")
			.components(ComponentAssemblyPresets.door(
				"aperture:frame_standard_50",
				"aperture:frame_standard_50",
				"aperture:single_glazed",
				"left"
			))
			.build();
	}

	public static OpeningTypeDefinition curtainWall() {
		return OpeningTypeDefinition.builder(CURTAIN_WALL_ID, OpeningCategory.CURTAIN_WALL, RECTANGULAR_WINDOW_GENERATOR)
			.parameter("width", RangeParameter.builder(NumberUnit.LENGTH_MM)
				.defaultNumber(3600)
				.min(600)
				.max(12000)
				.step(50)
				.metadata(ParameterMetadata.grouped("Width", "Dimensions"))
				.build())
			.parameter("height", RangeParameter.builder(NumberUnit.LENGTH_MM)
				.defaultNumber(3000)
				.min(600)
				.max(6000)
				.step(50)
				.metadata(ParameterMetadata.grouped("Height", "Dimensions"))
				.build())
			.parameter("cols", RangeParameter.builder(NumberUnit.COUNT)
				.defaultNumber(4)
				.min(1)
				.max(12)
				.metadata(ParameterMetadata.grouped("Columns", "Grid"))
				.build())
			.parameter("rows", RangeParameter.builder(NumberUnit.COUNT)
				.defaultNumber(3)
				.min(1)
				.max(20)
				.metadata(ParameterMetadata.grouped("Rows", "Grid"))
				.build())
			.parameter("frame_width", RangeParameter.builder(NumberUnit.LENGTH_MM)
				.defaultNumber(50)
				.min(20)
				.max(120)
				.metadata(ParameterMetadata.grouped("Frame Width", "Frame"))
				.build())
			.parameter("frame_depth", RangeParameter.builder(NumberUnit.LENGTH_MM)
				.defaultNumber(80)
				.min(30)
				.max(200)
				.metadata(ParameterMetadata.grouped("Frame Depth", "Frame"))
				.build())
			.constraint("width > height * 0.2", "Curtain wall span must exceed 20% of the height")
			.constraint("cols >= 1 and rows >= 1", "Grid must contain at least one cell")
			.materialSlot("frame")
			.materialSlot("glazing")
			.components(ComponentAssemblyPresets.curtainWall(
				"aperture:frame_l_50x80",
				"aperture:single_glazed"
			))
			.build();
	}

	private BuiltinOpeningTypes() {
	}
}
