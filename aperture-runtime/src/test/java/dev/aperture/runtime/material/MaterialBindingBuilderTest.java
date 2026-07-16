package dev.aperture.runtime.material;

import dev.aperture.runtime.registry.MaterialResolverRegistry;
import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.opening.compile.OpeningGeometryCompiler;
import dev.aperture.opening.component.ComponentPlanBuilder;
import dev.aperture.geometry.profile.ProfileCatalogLoader;
import dev.aperture.geometry.model.PartId;
import dev.aperture.geometry.material.MaterialBindingSet;
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
		var profiles = new ProfileCatalogLoader().loadClasspathCatalog();
		var parameters = definition.resolveParameters(instance.parameters());
		var plan = new ComponentPlanBuilder().build(definition.components());
		var geometry = new OpeningGeometryCompiler()
			.compile(definition, parameters, plan, profiles)
			.result();
		var materials = new MaterialResolverRegistry(VanillaMaterialResolver.INSTANCE);

		MaterialBindingSet bindings = MaterialBindingBuilder.build(definition, instance, geometry, materials);

		assertEquals(geometry.solids().size(), bindings.partIds().size());
		assertTrue(bindings.get(PartId.of("frame.bottom")).isPresent());
		assertTrue(bindings.get(PartId.of("glazing")).isPresent());
		assertEquals("frame", bindings.get(PartId.of("frame.bottom")).orElseThrow().materialSlot());
	}
}
