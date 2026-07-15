package dev.aperture.runtime.material;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.render.material.BlendMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VanillaMaterialResolverTest {
	@Test
	void resolvesDefaultFrameTexture() {
		var definition = BuiltinOpeningTypes.fixedWindow();
		var context = new MaterialResolveContext(
			"frame",
			GeometryLayer.OPAQUE,
			definition.id(),
			definition,
			ParameterSet.empty()
		);

		var material = VanillaMaterialResolver.INSTANCE.resolve(context);

		assertEquals("minecraft:block/oak_planks", material.definition().albedoTexture());
		assertEquals(BlendMode.OPAQUE, material.definition().blendMode());
	}

	@Test
	void resolvesGlazingAsTranslucent() {
		var definition = BuiltinOpeningTypes.fixedWindow();
		var context = new MaterialResolveContext(
			"glazing",
			GeometryLayer.TRANSLUCENT,
			definition.id(),
			definition,
			ParameterSet.empty()
		);

		var material = VanillaMaterialResolver.INSTANCE.resolve(context);

		assertEquals("minecraft:block/light_blue_stained_glass", material.definition().albedoTexture());
		assertEquals(BlendMode.TRANSLUCENT, material.definition().blendMode());
	}

	@Test
	void materialRefOverrideUsesInstanceParameter() {
		var definition = BuiltinOpeningTypes.fixedWindow();
		var parameters = ParameterSet.builder()
			.put("frame_material", ParameterValue.materialRef("minecraft:iron_block"))
			.build();
		var context = new MaterialResolveContext(
			"frame",
			GeometryLayer.OPAQUE,
			definition.id(),
			definition,
			parameters
		);

		var material = VanillaMaterialResolver.INSTANCE.resolve(context);

		assertEquals("minecraft:block/iron_block", material.definition().albedoTexture());
	}

	@Test
	void convertsBareBlockIdToTexturePath() {
		assertEquals("minecraft:block/oak_planks", VanillaMaterialResolver.toAlbedoTexture("minecraft:oak_planks"));
	}
}
