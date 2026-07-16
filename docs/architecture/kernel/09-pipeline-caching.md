---
status: active
implementation_version: kernel-pipeline-v1
last_verified: 2026-07-17
---

# Pipeline Caching

The production pipeline caches only the expensive `GEOMETRY` and `MESH` stages. Definition, parameter, constraint, component, collision, and placement stages always execute; widening the cache requires a measured need and an explicit invalidation design.

## Structural key

Cache entries use `StageCacheKey`, never an arbitrary stage input's `equals()` or `hashCode()`. The key contains:

- `StageId`
- pipeline version
- opening type ID
- opening-definition fingerprint/revision
- canonical parameter fingerprint
- profile/asset registry revision
- compiler version
- quality level

Changing any of these dimensions creates a cache miss. Parameter fingerprints are deterministic and independent of map iteration order.

## Ownership and diagnostics

`PipelineCache` is created by `KernelBuilder` and shared with the assembled pipeline. Stages may provide an explicit key through `PipelineStage.cacheKey`; a stage without a key is not cached. `OpeningPipelineAdapter.cacheStats()` returns the cache's real size, capacity, hits, and misses, and kernel statistics use this value directly.

The cache is an access-ordered, bounded LRU. A capacity of zero disables storage. Type registration and explicit kernel cache clearing invalidate stored results.