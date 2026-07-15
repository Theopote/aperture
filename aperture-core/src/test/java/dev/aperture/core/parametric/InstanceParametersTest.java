package dev.aperture.core.parametric;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.instance.OpeningState;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstanceParametersTest {
	@Test
	void resolveMergesSchemaDefaults() {
		OpeningTypeDefinition definition = BuiltinOpeningTypes.fixedWindow();
		ParameterSet resolved = InstanceParameters.resolve(definition, ParameterSet.empty());

		assertEquals(1200.0, resolved.requireLength("width"));
		assertEquals(1500.0, resolved.requireLength("height"));
	}

	@Test
	void extractOverridesStripsDefaults() {
		OpeningTypeDefinition definition = BuiltinOpeningTypes.fixedWindow();
		ParameterSet resolved = InstanceParameters.resolve(
			definition,
			ParameterSet.of("width", ParameterValue.length(900.0))
		);

		ParameterSet overrides = InstanceParameters.extractOverrides(definition, resolved);

		assertEquals(1, overrides.asMap().size());
		assertEquals(900.0, overrides.requireLength("width"));
	}

	@Test
	void setStoresSparseOverridesOnInstance() {
		OpeningTypeDefinition definition = BuiltinOpeningTypes.fixedWindow();
		OpeningInstance instance = OpeningInstance.builder(definition.id()).build();

		OpeningInstance updated = InstanceParameters.set(
			definition,
			instance,
			"width",
			ParameterValue.length(800.0)
		);

		assertEquals(1, updated.parameters().asMap().size());
		assertEquals(800.0, InstanceParameters.resolve(definition, updated).requireLength("width"));
	}

	@Test
	void forGenerationAppliesOpenRatioToMaxAngle() {
		OpeningTypeDefinition definition = BuiltinOpeningTypes.casementWindow();
		OpeningInstance halfOpen = OpeningInstance.builder(definition.id())
			.state(new OpeningState(0.5))
			.build();

		ParameterSet generated = InstanceParameters.forGeneration(definition, halfOpen);

		assertEquals(45.0, generated.angleOrDefault("open_angle", 0.0), 0.001);
	}

	@Test
	void forGenerationPreservesExplicitOpenAngleOverride() {
		OpeningTypeDefinition definition = BuiltinOpeningTypes.casementWindow();
		OpeningInstance preview = OpeningInstance.builder(definition.id())
			.parameters(ParameterSet.of("open_angle", ParameterValue.angle(30.0)))
			.state(new OpeningState(1.0))
			.build();

		ParameterSet generated = InstanceParameters.forGeneration(definition, preview);

		assertEquals(30.0, generated.angleOrDefault("open_angle", 0.0), 0.001);
	}

	@Test
	void patchAcceptsExternalValues() {
		OpeningTypeDefinition definition = BuiltinOpeningTypes.door();
		ParametricEditor editor = InstanceParameters.editor(definition, ParameterSet.empty());
		var result = editor.patch(java.util.Map.of(
			"width", 1100.0,
			"hinge_side", "right"
		));

		assertTrue(result.success());
		ParameterSet resolved = editor.resolve();
		assertEquals(1100.0, resolved.requireLength("width"));
		assertEquals("right", resolved.requireEnum("hinge_side"));
	}
}
