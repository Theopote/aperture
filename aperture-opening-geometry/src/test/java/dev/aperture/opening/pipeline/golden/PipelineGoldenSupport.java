package dev.aperture.opening.pipeline.golden;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.parametric.InstanceParameters;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.geometry.profile.ProfileCatalogLoader;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.opening.component.ComponentPlan;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.opening.pipeline.OpeningGenerationPipeline;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Captures and compares {@link PipelineGoldenSnapshot} fixtures for the opening pipeline.
 */
public final class PipelineGoldenSupport {
	public static final String UPDATE_PROPERTY = "aperture.updateGolden";
	public static final double DEFAULT_EPSILON_MM = 0.01;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final ProfileCatalogRegistry PROFILES = new ProfileCatalogLoader().loadClasspathCatalog();
	private static final OpeningGenerationPipeline PIPELINE = OpeningGenerationPipeline.standard();

	private PipelineGoldenSupport() {
	}

	public static PipelineGoldenSnapshot capture(OpeningTypeDefinition definition, ParameterSet overrides) {
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(overrides, "overrides");

		GenerationContext context = new GenerationContext(
			definition,
			InstanceParameters.resolve(definition, overrides),
			PROFILES
		);
		PipelineResult result = PIPELINE.generate(context);
		ComponentPlan plan = PIPELINE.planFor(context);
		return from(definition.id().toString(), plan.stepIds(), result);
	}

	public static PipelineGoldenSnapshot from(
		String openingTypeId,
		List<String> componentPlanStepIds,
		PipelineResult result
	) {
		List<PipelineGoldenSnapshot.PartSnapshot> parts = new ArrayList<>();
		for (GeometrySolid solid : result.geometry().solids()) {
			var bounds = solid.bounds();
			int triangleCount = result.meshes().partsByPath().containsKey(solid.componentPath())
				? result.meshes().partsByPath().get(solid.componentPath()).triangleCount()
				: 0;
			parts.add(new PipelineGoldenSnapshot.PartSnapshot(
				solid.componentPath(),
				solid.materialSlot(),
				layerName(solid.layer()),
				boundsSnapshot(bounds.width(), bounds.height(), bounds.depth()),
				triangleCount
			));
		}

		var assembly = result.geometry().bounds();
		var cutVolume = result.geometry().cutVolume();
		return new PipelineGoldenSnapshot(
			PipelineGoldenSnapshot.CURRENT_SCHEMA_VERSION,
			openingTypeId,
			componentPlanStepIds,
			boundsSnapshot(assembly.width(), assembly.height(), assembly.depth()),
			boundsSnapshot(cutVolume.width(), cutVolume.height(), cutVolume.depth()),
			parts
		);
	}

	public static PipelineGoldenSnapshot loadClasspath(String resourcePath) {
		InputStream stream = PipelineGoldenSupport.class.getClassLoader().getResourceAsStream(resourcePath);
		if (stream == null) {
			throw new IllegalArgumentException("Missing golden resource: " + resourcePath);
		}
		try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			return GSON.fromJson(reader, PipelineGoldenSnapshot.class);
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to read golden resource: " + resourcePath, exception);
		}
	}

	public static void assertMatchesResource(
		PipelineGoldenSnapshot actual,
		String resourcePath,
		double epsilon
	) {
		PipelineGoldenSnapshot expected = loadClasspath(resourcePath);
		actual.assertMatches(expected, epsilon);
	}

	public static void writeClasspathFixture(PipelineGoldenSnapshot snapshot, String resourcePath) {
		Path output = Path.of("src", "test", "resources", resourcePath);
		try {
			Files.createDirectories(output.getParent());
			Files.writeString(output, GSON.toJson(snapshot) + System.lineSeparator(), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to write golden fixture: " + output, exception);
		}
	}

	public static boolean shouldUpdateGolden() {
		return Boolean.parseBoolean(System.getProperty(UPDATE_PROPERTY, "false"));
	}

	private static PipelineGoldenSnapshot.BoundsSnapshot boundsSnapshot(double width, double height, double depth) {
		return new PipelineGoldenSnapshot.BoundsSnapshot(width, height, depth);
	}

	private static String layerName(GeometryLayer layer) {
		return layer.name();
	}
}
