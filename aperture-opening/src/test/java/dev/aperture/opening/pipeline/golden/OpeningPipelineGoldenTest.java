package dev.aperture.opening.pipeline.golden;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.parameter.ParameterSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Golden snapshots for the full opening pipeline:
 * Definition → Resolve → ComponentPlan → Geometry → Mesh.
 *
 * <p>Fixtures live under {@code src/test/resources/golden/pipeline/}.
 * Refresh with:
 * <pre>{@code
 * ./gradlew :aperture-opening:test --tests OpeningPipelineGoldenTest.refreshGoldenFixtures -Daperture.updateGolden=true
 * }</pre>
 */
class OpeningPipelineGoldenTest {
	private static final String GOLDEN_ROOT = "golden/pipeline/";

	@Test
	void fixedWindowDefaultMatchesGoldenSnapshot() {
		var snapshot = PipelineGoldenSupport.capture(BuiltinOpeningTypes.fixedWindow(), ParameterSet.empty());
		PipelineGoldenSupport.assertMatchesResource(
			snapshot,
			GOLDEN_ROOT + "fixed_window_default.json",
			PipelineGoldenSupport.DEFAULT_EPSILON_MM
		);
	}

	@Test
	void doorDefaultMatchesGoldenSnapshot() {
		var snapshot = PipelineGoldenSupport.capture(BuiltinOpeningTypes.door(), ParameterSet.empty());
		PipelineGoldenSupport.assertMatchesResource(
			snapshot,
			GOLDEN_ROOT + "door_default.json",
			PipelineGoldenSupport.DEFAULT_EPSILON_MM
		);
	}

	@Test
	void goldenSnapshotsIncludeMeshesForEverySolid() {
		var snapshot = PipelineGoldenSupport.capture(BuiltinOpeningTypes.fixedWindow(), ParameterSet.empty());
		assertFalse(snapshot.parts().isEmpty());
		for (var part : snapshot.parts()) {
			assertTrue(part.meshTriangleCount() > 0, () -> "Expected mesh triangles for " + part.path());
		}
	}

	@Test
	@EnabledIfSystemProperty(named = PipelineGoldenSupport.UPDATE_PROPERTY, matches = "true")
	void refreshGoldenFixtures() {
		PipelineGoldenSupport.writeClasspathFixture(
			PipelineGoldenSupport.capture(BuiltinOpeningTypes.fixedWindow(), ParameterSet.empty()),
			GOLDEN_ROOT + "fixed_window_default.json"
		);
		PipelineGoldenSupport.writeClasspathFixture(
			PipelineGoldenSupport.capture(BuiltinOpeningTypes.door(), ParameterSet.em