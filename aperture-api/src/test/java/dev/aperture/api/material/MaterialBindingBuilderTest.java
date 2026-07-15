package dev.aperture.api.material;

import dev.aperture.api.registry.MaterialResolverRegistry;
import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.geometry.generator.RectangularWindowGenerator;
import dev.aperture.render.data.PartId;
import dev.aperture.render.material.MaterialBindingSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialBindingBuilderTest {
	@Test
	void buildsBindingsForGeneratedSolids() {
		var definition = BuiltinOpeningTypes.fixedWindow();
		var instance = OpeningInstance.builder(definition.id())
			.parameters(ParameterSet.empty())
			.build();
		ParameterSet parameters = ParameterSet.mergeDefaults(definition.parameters(), instance.parameters());
		var geometry = new RectangularWindowGenerator().generate(definition, parameters);
		var materials = new MaterialResolverRegistry(VanillaMaterialResolver.INSTANCE);

		MaterialBindingSet bindings = MaterialBindingBuilder.build(definition, instance, geometry, materials);

		assertEquals(geometry.solids().size(), bindings.partIds().size());
		assertTrue(bindings.get(PartId.of("frame")).isPresent());
		assertTrue(bindings.get(PartId.of("glazing")).isPresent());
		assertEquals("frame", bindings.get(PartId.of("frame")).orElseThrow().materialSlot());
	}
}
