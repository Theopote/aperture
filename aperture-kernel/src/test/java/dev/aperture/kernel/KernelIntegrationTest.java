package dev.aperture.kernel;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningState;
import dev.aperture.core.opening.OpeningId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KernelIntegrationTest {
	private ApertureKernel kernel;

	@BeforeEach
	void setUp() {
		kernel = ApertureKernel.builder()
			.withCacheCapacity(100)
			.withAsyncThreadPoolSize(4)
			.build();
	}

	@AfterEach
	void tearDown() {
		if (kernel != null && !kernel.isClosed()) {
			kernel.close();
		}
	}

	@Test
	void completeWorkflowReturnsProductionPayload() {
		OpeningResult result = kernel.generate(new OpeningRequest(
			"aperture:door",
			Map.of("width", 1200.0, "height", 2100.0)
		));

		OpeningResult.Success success = result.asSuccess();
		assertNotNull(success.output().geometry());
		assertNotNull(success.output().meshes());
		assertNotNull(success.output().recipe());
		assertNotNull(success.output().collision());
		assertNotNull(success.placement());
	}

	@Test
	void repeatedGenerationUpdatesKernelCacheStatistics() {
		OpeningRequest request = new OpeningRequest("aperture:door", Map.of("width", 1200.0));
		assertTrue(kernel.generate(request).isSuccess());
		assertTrue(kernel.generate(request).isSuccess());
		assertTrue(kernel.getStats().cacheStats().hits() > 0);
	}

	@Test
	void batchPreservesSuccessAndFailureOrder() {
		List<OpeningResult> results = kernel.generateBatch(List.of(
			new OpeningRequest("aperture:door", Map.of()),
			new OpeningRequest("aperture:missing", Map.of()),
			new OpeningRequest("aperture:fixed_window", Map.of())
		));

		assertEquals(3, results.size());
		assertTrue(results.get(0).isSuccess());
		assertFalse(results.get(1).isSuccess());
		assertTrue(results.get(2).isSuccess());
		assertEquals(2, kernel.getStats().successfulRequests());
		assertEquals(1, kernel.getStats().failedRequests());
	}

	@Test
	void registeredTypeIsImmediatelyVisibleToDefinitionStage() {
		OpeningTypeDefinition source = BuiltinOpeningTypes.fixedWindow();
		OpeningTypeDefinition custom = new OpeningTypeDefinition(
			source.schemaVersion(),
			OpeningId.parse("test:registered_window"),
			source.category(),
			source.parametricSchema(),
			source.constraints(),
			source.generator(),
			source.components(),
			source.materialSlots()
		);

		kernel.registerType(custom);
		assertTrue(kernel.generate("test:registered_window", Map.of()).isSuccess());
	}
	@Test
	void asyncRequestsUseKernelExecutor() {
		CompletableFuture<OpeningResult> door = kernel.generateAsync(
			new OpeningRequest("aperture:door", Map.of())
		);
		CompletableFuture<OpeningResult> window = kernel.generateAsync(
			new OpeningRequest("aperture:fixed_window", Map.of())
		);

		CompletableFuture.allOf(door, window).join();
		assertTrue(door.join().isSuccess());
		assertTrue(window.join().isSuccess());
	}

	@Test
	void openingStateTravelsThroughKernelRequest() {
		OpeningRequest request = new OpeningRequest(
			"aperture:door",
			Map.of(),
			new OpeningState(1.0),
			OpeningOptions.DEFAULT
		);
		assertTrue(kernel.generate(request).isSuccess());
	}

	@Test
	void clearCacheRemovesEntries() {
		OpeningRequest request = new OpeningRequest("aperture:door", Map.of());
		kernel.generate(request);
		kernel.generate(request);
		long hitsBeforeClear = kernel.getStats().cacheStats().hits();
		assertTrue(hitsBeforeClear > 0);
		kernel.clearCache();
		kernel.generate(request);
		assertEquals(hitsBeforeClear, kernel.getStats().cacheStats().hits());
	}

	@Test
	void closeRejectsFurtherGeneration() {
		kernel.close();
		assertTrue(kernel.isClosed());
		assertThrows(IllegalStateException.class, () ->
			kernel.generate("aperture:door", Map.of())
		);
	}
}