package dev.aperture.pipeline.adapter;

import dev.aperture.pipeline.PipelineResult;
import dev.aperture.pipeline.stage.DefinitionStage;
import dev.aperture.pipeline.stage.PlacementStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpeningPipelineAdapterTest {
	private OpeningPipelineAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = OpeningPipelineAdapter.standard();
	}

	@Test
	void executesAllEightStagesWithMillimeterParameters() {
		PipelineResult result = adapter.execute(
			"aperture:door",
			Map.of("width", 1200.0, "height", 2100.0)
		);

		assertTrue(result.isSuccess(), result.getFailureMessage());
		assertEquals(8, result.stageCount());
		assertInstanceOf(PlacementStage.PlacementInfo.class, result.getFinalOutput());
	}

	@Test
	void acceptsPreparedRequest() {
		PipelineResult result = adapter.execute(new DefinitionStage.OpeningRequest(
			"aperture:fixed_window",
			Map.of("width", 1500.0, "height", 1800.0)
		));

		assertTrue(result.isSuccess(), result.getFailureMessage());
	}

	@Test
	void schemaDefaultsMakeAnEmptyOverrideSetValid() {
		PipelineResult result = adapter.execute("aperture:door", Map.of());
		assertTrue(result.isSuccess(), result.getFailureMessage());
	}

	@Test
	void repeatedRequestUsesCacheAndClearInvalidatesIt() {
		Map<String, Object> parameters = Map.of("width", 1200.0);
		adapter.execute("aperture:door", parameters);
		PipelineResult warm = adapter.execute("aperture:door", parameters);
		assertTrue(warm.cacheHits() > 0);

		adapter.clearCache();
		PipelineResult cold = adapter.execute("aperture:door", parameters);
		assertEquals(0, cold.cacheHits());
	}

	@Test
	void adapterReportsAggregateCacheStatistics() {
		Map<String, Object> parameters = Map.of("width", 1200.0);
		adapter.execute("aperture:door", parameters);
		adapter.execute("aperture:door", parameters);
		assertTrue(adapter.getCacheStats().hits() > 0);
	}

	@Test
	void invalidTypeFailsAtDefinitionStage() {
		PipelineResult result = adapter.execute("aperture:missing", Map.of());
		assertFalse(result.isSuccess());
		assertEquals("definition", result.getFailedStageName());
	}

	@Test
	void invalidDimensionsFailAtDefinitionStage() {
		PipelineResult result = adapter.execute("aperture:door", Map.of("width", -1.0));
		assertFalse(result.isSuccess());
		assertEquals("definition", result.getFailedStageName());
	}

	@Test
	void exposesGeometryAndPlacementStageOutputs() {
		PipelineResult result = adapter.execute("aperture:door", Map.of());
		PipelineResult.Success success = assertInstanceOf(PipelineResult.Success.class, result);
		assertNotNull(success.getStageValue("geometry").orElse(null));
		assertNotNull(success.getStageValue("placement").orElse(null));
	}

	@Test
	void noCacheAdapterHasNoHits() {
		OpeningPipelineAdapter noCache = OpeningPipelineAdapter.withoutCache();
		PipelineResult result = noCache.execute("aperture:door", Map.of());
		assertTrue(result.isSuccess(), result.getFailureMessage());
		assertEquals(0, result.cacheHits());
	}

	@Test
	void unwrapExposesConfiguredStageOrder() {
		assertEquals(
			java.util.List.of("definition", "parameter", "constraint", "component", "geometry", "mesh", "collision", "placement"),
			adapter.unwrap().stageNames()
		);
	}
}