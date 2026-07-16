# Pipeline Result Caching

## Overview

Pipeline result caching stores computed geometry and mesh outputs to avoid redundant generation when the same opening is requested multiple times with identical parameters.

## Architecture

### Cache Key

Cache entries are keyed by:
- **Opening type ID**: e.g., `aperture:door`, `aperture:fixed_window`
- **Parameter set**: Complete set of parameter values

Two cache keys are equal if both type ID and all parameter values match exactly.

### Cache Implementation

```java
public class PipelineResultCache {
    private final Map<CacheKey, PipelineResult> cache = new ConcurrentHashMap<>();
    
    public PipelineResult getOrCompute(
        String typeId,
        ParameterSet parameters,
        PipelineGenerator generator
    ) {
        CacheKey key = new CacheKey(typeId, parameters);
        return cache.computeIfAbsent(key, k -> generator.generate());
    }
}
```

**Thread-safe**: Uses `ConcurrentHashMap` for concurrent access without external synchronization.

## Usage

### Basic Usage

```java
PipelineResultCache cache = new PipelineResultCache();

// First call: cache miss, generates result
PipelineResult result1 = cache.getOrCompute(
    "aperture:door",
    params,
    () -> generator.generate(context)
);

// Second call: cache hit, returns cached result
PipelineResult result2 = cache.getOrCompute(
    "aperture:door",
    params,
    () -> generator.generate(context)
);

// result1 == result2 (same instance)
```

### With Custom Size Limit

```java
// Limit cache to 500 entries
PipelineResultCache cache = new PipelineResultCache(500);
```

When the cache exceeds the size limit, it is cleared entirely. This is a simple eviction strategy; more sophisticated LRU eviction could be added later.

### Invalidation

```java
// Invalidate specific entry
cache.invalidate("aperture:door", params);

// Invalidate all entries for a type
cache.invalidateType("aperture:door");

// Clear entire cache
cache.clear();
```

**When to invalidate:**
- Asset reload (profiles, materials changed)
- Definition updates (opening type modified)
- Hot reload during development

### Statistics

```java
var stats = cache.stats();
System.out.printf("Cache: %d entries, %.1f%% hit rate%n",
    stats.size(), stats.hitRate() * 100);
```

Statistics include:
- **Size**: Current entry count
- **Hits**: Cache hits (returned cached result)
- **Misses**: Cache misses (generated new result)
- **Hit rate**: hits / (hits + misses)

## Performance Impact

### Cold Generation (No Cache)

- **Fixed Window**: ~80-120ms
- **Door**: ~100-150ms
- **Curtain Wall**: ~200-300ms

### Cached Generation

- **Lookup time**: < 1ms
- **Memory overhead**: ~50-100KB per cached result

### Hit Rate Expectations

Typical scenarios:

| Scenario | Expected Hit Rate |
|----------|------------------|
| Player places same door repeatedly | 95%+ |
| Editor with type cycling | 60-80% |
| Random parameter variations | 10-30% |
| Batch world generation | 40-60% |

## Memory Management

### Memory Usage

Each cached entry stores:
- `GeometryResult`: Solid shapes, bounds
- `MeshAssembly`: Baked mesh data per part
- `GeometryRecipe`: Optional IR for replay

**Typical size per entry:**
- Small opening (window): 30-50KB
- Medium opening (door): 50-100KB
- Large opening (curtain wall): 100-200KB

**Cache capacity planning:**
- 1000 entries: ~50-100MB
- 5000 entries: ~250-500MB
- 10000 entries: ~500MB-1GB

### Size Limit

Configure based on available heap:

```java
// Conservative (for 2GB heap)
new PipelineResultCache(1000);

// Moderate (for 4GB heap)
new PipelineResultCache(5000);

// Aggressive (for 8GB+ heap)
new PipelineResultCache(10000);
```

When limit is exceeded, cache is cleared. This prevents unbounded growth but may cause temporary performance dip.

## Integration

### In Opening System

```java
public class OpeningPipelineManager {
    private final PipelineResultCache cache = new PipelineResultCache();
    
    public PipelineResult generate(OpeningInstance instance) {
        return cache.getOrCompute(
            instance.typeId(),
            instance.parameters(),
            () -> generateUncached(instance)
        );
    }
    
    public void onAssetReload() {
        cache.clear();
    }
}
```

### In Block Entity

```java
public class OpeningBlockEntity extends BlockEntity {
    private PipelineResult cachedResult;
    
    public PipelineResult getOrGeneratePipeline() {
        if (cachedResult == null) {
            cachedResult = pipelineManager.generate(instance);
        }
        return cachedResult;
    }
}
```

### In Editor

```java
public class ParameterEditorScreen {
    public void onParameterChange(String key, ParameterValue value) {
        // Invalidate preview cache
        previewCache.invalidate(currentType, updatedParams);
        
        // Regenerate preview
        refreshPreview();
    }
}
```

## Eviction Strategies

### Current: Simple Clear

When size limit reached, clear entire cache.

**Pros:**
- Simple implementation
- No overhead during normal operation

**Cons:**
- Loses all cached data
- Temporary performance dip

### Future: LRU Eviction

Use LinkedHashMap with access order:

```java
private final Map<CacheKey, PipelineResult> cache = 
    new LinkedHashMap<>(maxSize, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > maxCacheSize;
        }
    };
```

**Pros:**
- Keeps frequently used entries
- Smooth eviction

**Cons:**
- Higher overhead per access
- More complex implementation

### Future: TTL Eviction

Expire entries after time limit:

```java
record CacheEntry(PipelineResult result, long timestamp) {}

public PipelineResult getOrCompute(...) {
    CacheEntry entry = cache.get(key);
    if (entry != null && System.currentTimeMillis() - entry.timestamp < TTL_MS) {
        return entry.result;
    }
    // Generate new
}
```

**Use cases:**
- Development (expire after 30s for hot reload)
- Dynamic content (expire when underlying data changes)

## Best Practices

### Do Cache

- Standard opening instances with stable parameters
- Repeated placements by player
- Common configurations (default sizes)

### Don't Cache

- Opening instances with dynamic state (open/closed)
- Temporary editor previews
- One-off procedural variations

### Invalidation Guidelines

**Invalidate specific entry:**
```java
// Parameter changed in editor
cache.invalidate(typeId, oldParams);
```

**Invalidate type:**
```java
// Opening definition modified
cache.invalidateType("aperture:door");
```

**Clear all:**
```java
// Asset pack reloaded
cache.clear();
```

## Debugging

### Enable Cache Logging

```java
var stats = cache.stats();
if (stats.hitRate() < 0.3) {
    logger.warn("Low cache hit rate: {}", stats);
}
```

### Monitor Memory

```java
Runtime runtime = Runtime.getRuntime();
long usedMemory = runtime.totalMemory() - runtime.freeMemory();
logger.info("Cache size: {}, memory: {} MB", 
    cache.stats().size(), 
    usedMemory / (1024 * 1024));
```

### Trace Cache Behavior

```java
cache.getOrCompute(typeId, params, () -> {
    logger.debug("Cache miss for {}: generating", typeId);
    return generator.generate();
});
```

## Future Enhancements

- [ ] Persistent cache (save to disk)
- [ ] Distributed cache (multiplayer server)
- [ ] Incremental updates (parameter deltas)
- [ ] Compression (reduce memory footprint)
- [ ] Smart eviction (LRU, LFU, TTL)
- [ ] Cache warming (preload common types)
- [ ] Analytics (most/least cached types)

## Testing

See `PipelineResultCacheTest.java` for comprehensive test coverage:
- Cache hits and misses
- Concurrent access safety
- Invalidation correctness
- Size limit enforcement
- Statistics accuracy
