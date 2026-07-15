package dev.aperture.runtime.material;

import dev.aperture.runtime.catalog.MaterialCatalogLoader;
import dev.aperture.runtime.catalog.MaterialCatalogRegistry;
import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.core.material.BlendMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogMaterialResolverTest {
	@Test
	void resolvesCatalogSlotDefaults() {
		MaterialCatalogRegistry catalog = new MaterialCatalogLoader().loadClasspathCatalog();
		CatalogMaterialResolver resolver = new CatalogMaterialResolver(catalog);
		var definition = BuiltinOpeningTypes.fixedWindow();

		var frame = resolver.resolve(new MaterialResolveContext(
			"frame",
			GeometryLayer.OPAQUE,
			definition.id(),
			definition,
			ParameterSet.empty()
		));
		var glazing = resolver.resolve(new MaterialResolveContext(
			"glazing",
			GeometryLayer.TRANSLUCENT,
			definition.id(),
			definition,
			ParameterSet.empty()
		));

		assertEquals("minecraft:block/oak_planks", frame.definition().albedoTexture());
		assertEquals(BlendMode.OPAQUE, frame.definition().blendMode());
		assertEquals("minecraft:block/light_blue_stained_glass", glazing.definition().albedoTexture());
		assertEquals(BlendMode.TRANSLUCENT, glazing.definition().blendMode());
	}
}
