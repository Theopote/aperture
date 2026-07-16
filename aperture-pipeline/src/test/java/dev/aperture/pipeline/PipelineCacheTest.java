package dev.aperture.pipeline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PipelineCacheTest {
	private static StageCacheKey key(StageId stage, String fingerprint) {
		return new StageCacheKey(stage, "pipeline-v1", "aperture:test", 1L,
			fingerprint, 2L, "compiler-v1", "normal");
	}

	@Test
	void storesValuesByExplicitStructuralKey() {
		PipelineCache cache = new PipelineCache(5);
		StageCacheKey key = key(StageId.GEOMETRY, "parameters-a");
		cache.put(key, "geometry");
		assertEquals("geometry", cache.get(key));
		assertNull(cache.get(key(StageId.GEOMETRY, "parameters-b")));
		assertNull(cache.get(key(StageId.MESH, "parameters-a")));
	}

	@Test
	void keyIncludesEveryRevisionDimension() {
		StageCacheKey base = key(StageId.GEOMETRY, "parameters");
		assertNotEquals(base, new StageCacheKey(StageId.GEOMETRY, "pipeline-v2", "aperture:test", 1L,
			"parameters", 2L, "compiler-v1", "normal"));
		assertNotEquals(base, new StageCacheKey(StageId.GEOMETRY, "pipeline-v1", "aperture:test", 2L,
			"parameters", 2L, "compiler-v1", "normal"));
		assertNotEquals(base, new StageCacheKey(StageId.GEOMETRY, "pipeline-v1", "aperture:test", 1L,
			"parameters", 3L, "compiler-v1", "normal"));
		assertNotEquals(base, new StageCacheKey(StageId.GEOMETRY, "pipeline-v1", "aperture:test", 1L,
			"parameters", 2L, "compiler-v2", "normal"));
	}

	@Test
	void evictsLeastRecentlyUsedEntry() {
		PipelineCache cache = new PipelineCache(2);
		StageCacheKey a = key(StageId.GEOMETRY, "a");
		StageCacheKey b = key(StageId.GEOMETRY, "b");
		StageCacheKey c = key(StageId.GEOMETRY, "c");
		cache.put(a, "a"); cache.put(b, "b"); cache.get(a); cache.put(c, "c");
		assertEquals("a", cache.get(a));
		assertNull(cache.get(b));
		assertEquals("c", cache.get(c));
	}

	@Test
	void reportsRealStatisticsAndCanResetThem() {
		PipelineCache cache = new PipelineCache(5);
		StageCacheKey present = key(StageId.MESH, "present");
		cache.put(present, "mesh");
		cache.get(present);
		cache.get(key(StageId.MESH, "missing"));
		PipelineCache.CacheStats stats = cache.getStats();
		assertEquals(1, stats.currentSize());
		assertEquals(1, stats.hits());
		assertEquals(1, stats.misses());
		assertEquals(0.5, stats.hitRate());
		cache.resetStatistics();
		assertEquals(0, cache.getHits());
		assertEquals(0, cache.getMisses());
	}

	@Test
	void zeroCapacityDisablesStorage() {
		PipelineCache cache = new PipelineCache(0);
		StageCacheKey key = key(StageId.GEOMETRY, "parameters");
		cache.put(key, "geometry");
		assertEquals(0, cache.size());
		assertNull(cache.get(key));
	}

	@Test
	void rejectsImplicitOrInvalidKeys() {
		PipelineCache cache = new PipelineCache(1);
		assertThrows(NullPointerException.class, () -> cache.get(null));
		assertThrows(NullPointerException.class, () -> cache.put(null, "value"));
		assertThrows(NullPointerException.class,
			() -> cache.put(key(StageId.GEOMETRY, "parameters"), null));
	}
}