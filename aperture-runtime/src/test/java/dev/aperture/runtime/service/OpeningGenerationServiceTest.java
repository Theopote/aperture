package dev.aperture.runtime.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.geometry.export.gltf.GltfExporter;
import dev.aperture.geometry.profile.ProfileCatalogLoader;
import dev.aperture.geometry.recipe.EmitSolidOp;
import dev.aperture.geometry.recipe.io.GeometryRecipeCodec;
import dev.aperture.geometry.recipe.shape.BoxRecipe;
import dev.aperture.kernel.ApertureKernel;
import dev.aperture.parameter.ParameterSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpeningGenerationServiceTest {
	private ApertureKernel kernel;
	private OpeningGenerationService service;

	@BeforeEach
	void setUp() {
		OpeningTypeRegistry openingTypes = new OpeningTypeRegistry();
		openingTypes.register(BuiltinOpeningTypes.fixedWindow());
		openingTypes.register(BuiltinOpeningTypes.door());
		kernel = ApertureKernel.builder()
			.withRegistry(openingTypes)
			.withProfiles(new ProfileCatalogLoader().loadClasspathCatalog())
			.withCacheCapacity(0)
			.build();
		service = new OpeningGenerationService(kernel);
	}

	@AfterEach
	void tearDown() {
		kernel.close();
	}

	@Test
	void generateReturnsKernelOutputWithRecipe() {
		OpeningInstance instance = OpeningInstance.builder(BuiltinOpeningTypes.FIXED_WINDOW_ID)
			.parameters(ParameterSet.empty())
			.build();

		var result = service.generate(instance).asSuccess().output();
		assertNotNull(result.recipe());
		assertFalse(result.recipe().ops().isEmpty());
		assertNotNull(result.collision());
	}

	@Test
	void exportRecipeJsonRoundTrips() {
		OpeningInstance instance = OpeningInstance.builder(BuiltinOpeningTypes.FIXED_WINDOW_ID)
			.parameters(ParameterSet.empty())
			.build();

		String json = service.exportRecipeJson(instance);
		var restored = GeometryRecipeCodec.fromJson(json);
		assertTrue(restored.ops().stream().anyMatch(op -> op instanceof EmitSolidOp emit
			&& emit.componentPath().equals("glazing")
			&& emit.shape() instanceof BoxRecipe));
	}

	@Test
	void exportGltfProducesValidDocument() {
		OpeningInstance instance = OpeningInstance.builder(BuiltinOpeningTypes.DOOR_ID)
			.parameters(ParameterSet.empty())
			.build();

		JsonObject gltf = JsonParser.parseString(service.exportGltf(instance)).getAsJsonObject();
		assertEquals(GltfExporter.GENERATOR, gltf.getAsJsonObject("asset").get("generator").getAsString());
		assertFalse(gltf.getAsJsonArray("meshes").isEmpty());
	}
}