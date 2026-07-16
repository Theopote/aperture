package dev.aperture.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for pipeline caching functionality.
 */
@DisplayName("Pipeline Cache")
class PipelineCacheTest {

	private PipelineCache cache;

	@BeforeEach
	void setUp() {
		cache = new PipelineCache(5); // Small capacity for testing
	}

	@Test
	@DisplayName("Store and retrieve cached value")
	void testBasicCaching() {
		// Arrange
		String stageName = "test-stage";
		Object input = "input-data";
		Object output = "output-data";

		// Act
		cache.put(stageName, input, output);
		Object retrieved = cache.get(stageName, input);

		// Assert
		assertNotNull(retrieved, "Should retrieve cached value");
		assertEquals(output, retrieved, "Retrieved value should match stored value");
	}

	@Test
	@DisplayName("Return null for cache miss")
	void testCacheMiss() {
		// Act
		Object result = cache.get("stage", "input");

		// Assert
		assertNull(result, "Should return null for cache miss");
	}

	@Test
	@DisplayName("Different inputs produce different cache keys")
	void testInputDifferentiation() {
		// Arrange
		String stageName = "stage";
		cache.put(stageName, "input1", "output1");
		cache.put(stageName, "input2", "output2");

		// Act & Assert
		assertEquals("output1", cache.get(stageName, "input1"));
		assertEquals("output2", cache.get(stageName, "input2"));
	}

	@Test
	@DisplayName("Different stages produce different cache keys")
	void testStageDifferentiation() {
		// Arrange
		Object input = "shared-input";
		cache.put("stage1", input, "output1");
		cache.put("stage2", input, "output2");

		// Act & Assert
		assertEquals("output1", cache.get("stage1", input));
		assertEquals("output2", cache.get("stage2", input));
	}

	@Test
	@DisplayName("LRU eviction removes least recently used entry")
	void testLRUEviction() {
		// Arrange - Fill cache to capacity
		for (int i = 0; i < 5; i++) {
			cache.put("stage", "input" + i, "output" + i);
		}

		// Act - Access input1 to make it recently used
		cache.get("stage", "input1");

		// Add new entry, should evict input0 (least recently used)
		cache.put("stage", "input5", "output5");

		// Assert
		assertNull(cache.get("stage", "input0"), "input0 should be evicted");
		assertNotNull(cache.get("stage", "input1"), "input1 should still be cached (was accessed)");
		assertNotNull(cache.get("stage", "input5"), "input5 should be cached (just added)");
	}

	@Test
	@DisplayName("Clear removes all entries")
	void testClear() {
		// Arrange
		cache.put("stage1", "input1", "output1");
		cache.put("stage2", "input2", "output2");
		assertEquals(2, cache.size());

		// Act
		cache.clear();

		// Assert
		assertEquals(0, cache.size(), "Cache should be empty after clear");
		assertNull(cache.get("stage1", "input1"), "Entries should be removed");
		assertNull(cache.get("stage2", "input2"), "Entries should be removed");
	}

	@Test
	@DisplayName("Size reflects number of entries")
	void testSize() {
		// Assert initial state
		assertEquals(0, cache.size(), "Cache should start empty");

		// Add entries
		cache.put("stage", "input1", "output1");
		assertEquals(1, cache.size());

		cache.put("stage", "input2", "output2");
		assertEquals(2, cache.size());

		// Clear
		cache.clear();
		assertEquals(0, cache.size());
	}

	@Test
	@DisplayName("Handles null inputs safely")
	void testNullInputHandling() {
		// Act & Assert - Should not throw
		assertDoesNotThrow(() -> {
			cache.put("stage", null, "output");
			Object result = cache.get("stage", null);
			assertEquals("output", result);
		});
	}

	@Test
	@DisplayName("Handles complex objects as input")
	void testComplexInputObjects() {
		// Arrange
		record ComplexInput(String id, int value) {}
		ComplexInput input1 = new ComplexInput("test", 42);
		ComplexInput input2 = new ComplexInput("test", 42); // Equal but different instance

		cache.put("stage", input1, "output1");

		// Act
		Object result = cache.get("stage", input2);

		// Assert
		assertEquals("output1", result, "Should match based on object equality");
	}

	@Test
	@DisplayName("Capacity of zero disables caching")
	void testZeroCapacity() {
		// Arrange
		PipelineCache noCache = new PipelineCache(0);

		// Act
		noCache.put("stage", "input", "output");

		// Assert
		assertEquals(0, noCache.size(), "Zero-capacity cache should not store entries");
		assertNull(noCache.get("stage", "input"), "Should not retrieve from zero-capacity cache");
	}

	@Test
	@DisplayName("Updating existing entry does not increase size")
	void testUpdateExistingEntry() {
		// Arrange
		cache.put("stage", "input", "output1");
		assertEquals(1, cache.size());

		// Act
		cache.put("stage", "input", "output2"); // Update same key

		// Assert
		assertEquals(1, cache.size(), "Size should not increase for updates");
		assertEquals("output2", cache.get("stage", "input"), "Should have updated value");
	}

	@Test
	@DisplayName("Access order affects eviction")
	void testAccessOrderEviction() {
		// Arrange - Fill cache
		cache.put("stage", "a", "output-a");
		cache.put("stage", "b", "output-b");
		cache.put("stage", "c", "output-c");
		cache.put("stage", "d", "output-d");
		cache.put("stage", "e", "output-e");

		// Act - Access 'a' to make it most recently used
		cache.get("stage", "a");

		// Add new entry - should evict 'b' (now least recently used)
		cache.put("stage", "f", "output-f");

		// Assert
		assertNotNull(cache.get("stage", "a"), "a should remain (recently accessed)");
		assertNull(cache.get("stage", "b"), "b should be evicted");
		assertNotNull(cache.get("stage", "f"), "f should be present");
	}

	@Test
	@DisplayName("Cache statistics track hits and misses")
	void testCacheStatistics() {
		// Arrange
		cache.put("stage", "input1", "output1");

		// Act
		cache.get("stage", "input1"); // Hit
		cache.get("stage", "input2"); // Miss
		cache.get("stage", "input1"); // Hit

		// Assert
		assertEquals(2, cache.getHits(), "Should count cache hits");
		assertEquals(1, cache.getMisses(), "Should count cache misses");

		double hitRate = cache.getHitRate();
		assertEquals(2.0 / 3.0, hitRate, 0.01, "Hit rate should be 66.7%");
	}

	@Test
	@DisplayName("Reset statistics clears counters")
	void testResetStatistics() {
		// Arrange
		cache.put("stage", "input", "output");
		cache.get("stage", "input"); // Hit
		cache.get("stage", "other"); // Miss

		assertTrue(cache.getHits() > 0);
		assertTrue(cache.getMisses() > 0);

		// Act
		cache.resetStatistics();

		// Assert
		assertEquals(0, cache.getHits(), "Hits should be reset");
		assertEquals(0, cache.getMisses(), "Misses should be reset");
		assertEquals(0.0, cache.getHitRate(), "Hit rate should be reset");
	}
}
