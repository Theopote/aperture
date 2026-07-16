# Performance Benchmarking Guide

## Overview

Performance benchmarks measure pipeline generation speed, memory usage, and mesh complexity to ensure Aperture meets real-time interactive requirements.

## Targets

### Generation Performance

- **Cold generation**: < 150ms (first time, no caching)
- **Cached generation**: < 5ms (with result caching)
- **Interactive threshold**: < 16ms for 60 FPS responsiveness

### Memory Usage

- **Per instance**: < 100KB average
- **100 instances**: < 10MB total

### Mesh Complexity

- **Fixed Window**: ~200-400 vertices, ~100-200 faces
- **Single Door**: ~400-800 vertices, ~200-400 faces
- **Double Door**: ~800-1200 vertices, ~400-600 faces
- **Curtain Wall**: ~1000-2000 vertices per unit

## Running Benchmarks

### Full Suite

```bash
./scripts/run-benchmarks.sh
```

### Individual Tests

```bash
# Cold generation benchmark
./gradlew :aperture-opening:test \
  --tests "ComprehensiveBenchmarkTest.benchmark_fixedWindow_coldGeneration"

# Memory usage
./gradlew :aperture-opening:test \
  --tests "ComprehensiveBenchmarkTest.benchmark_memoryUsage"

# Mesh complexity
./gradlew :aperture-opening:test \
  --tests "ComprehensiveBenchmarkTest.benchmark_meshComplexity"
```

## Test Categories

### 1. Cold Generation

Measures first-time generation without any caching:

- Fixed windows (small, medium, large)
- Doors (single, double)
- Curtain walls (multi-unit)

**What it tests:**
- Geometry calculation overhead
- Mesh generation performance
- Profile extrusion speed
- Constraint solving time

### 2. Cached Generation

Measures generation with result caching enabled:

- Same parameters → cache hit
- Different parameters → cache miss
- Cache invalidation logic

**What it tests:**
- Cache lookup performance
- Serialization overhead
- Cache hit ratio

### 3. Varying Complexity

Compares generation time across different sizes and configurations:

- Small vs. large openings
- Single vs. multi-panel
- Simple vs. complex profiles

**What it reveals:**
- Scaling behavior (linear vs. quadratic)
- Bottlenecks in specific operations
- Size-dependent performance issues

### 4. Parallel Generation

Tests thread safety and parallel performance:

- Multiple threads generating simultaneously
- Speedup factor vs. sequential
- Resource contention

**What it reveals:**
- Thread safety of geometry operations
- Scalability for batch operations
- Lock contention issues

### 5. Memory Usage

Measures heap allocation and retention:

- Memory per instance
- Peak memory for batch operations
- GC pressure

**What it reveals:**
- Memory leaks
- Excessive allocations
- Object retention issues

### 6. Mesh Complexity

Analyzes output mesh characteristics:

- Vertex/face counts
- Parts per opening
- Triangle density

**What it reveals:**
- Rendering cost expectations
- Mesh optimization opportunities
- Complexity trends

## Interpreting Results

### Benchmark Output

```
=== Fixed Window Cold Generation ===
Median: 87.5ms
Mean: 92.3ms
Min: 78.2ms
Max: 134.7ms
P95: 118.4ms
StdDev: 15.2ms
```

**Key metrics:**

- **Median**: Typical performance (more stable than mean)
- **P95**: Worst-case for 95% of requests
- **StdDev**: Consistency (lower is better)

### Performance Analysis

#### Good Performance
- Median < 100ms
- StdDev < 20ms
- P95 < 150ms

#### Concerning Performance
- Median > 150ms (may cause noticeable lag)
- StdDev > 50ms (inconsistent, possibly GC pauses)
- P95 > 300ms (bad worst-case experience)

### Memory Analysis

```
Generated 100 instances in 8234ms
Total memory used: 8.47 MB
Average per instance: 86.73 KB
Average generation time: 82.3ms
```

**Good memory usage:**
- < 100KB per instance
- Linear scaling with instance count
- No memory leaks after GC

**Concerning memory usage:**
- > 500KB per instance (excessive allocations)
- Non-linear growth (memory leak)
- High GC pressure (many small objects)

### Mesh Complexity

```
Fixed Window 1200x1500:
  Parts: 5
  Total Vertices: 324
  Total Faces: 168
  Avg Vertices/Part: 64.8
  Avg Faces/Part: 33.6
```

**Rendering implications:**
- < 500 vertices: Very light, renders easily
- 500-2000 vertices: Moderate, acceptable for most scenes
- > 2000 vertices: Heavy, consider LOD or optimization

## Optimization Workflow

### 1. Identify Bottlenecks

Run benchmarks and identify slow operations:

```bash
./gradlew :aperture-opening:test \
  --tests "ComprehensiveBenchmarkTest.benchmark_varyingComplexity" \
  --debug
```

Look for:
- Operations taking > 50ms
- Quadratic scaling with size
- Excessive allocations

### 2. Profile Hot Paths

Use Java profiler (YourKit, JProfiler, or VisualVM):

```bash
# Enable profiling
./gradlew :aperture-opening:test \
  -Dorg.gradle.debug=true
```

Focus on:
- CPU hot spots
- Allocation hot spots
- Lock contention

### 3. Apply Optimizations

Common optimizations:

**Geometry caching:**
```java
private final Map<ParameterSet, PipelineResult> cache = new HashMap<>();

public PipelineResult generate(ParameterSet params) {
    return cache.computeIfAbsent(params, this::generateUncached);
}
```

**Object pooling:**
```java
private final ObjectPool<Vec3d> vectorPool = new ObjectPool<>();
```

**Lazy evaluation:**
```java
public Mesh getMesh() {
    if (mesh == null) {
        mesh = computeMesh();
    }
    return mesh;
}
```

### 4. Verify Improvements

Re-run benchmarks and compare:

```
Before: Median 145.2ms, P95 189.3ms
After:  Median  87.5ms, P95 118.4ms
Improvement: 39.7% faster median, 37.4% faster P95
```

### 5. Regression Testing

Add benchmark to CI to prevent regressions:

```yaml
- name: Performance Regression Check
  run: |
    ./scripts/run-benchmarks.sh > bench_results.txt
    # Compare against baseline
```

## Best Practices

### Writing Benchmarks

1. **Warmup JIT**: Run 5-10 iterations before measuring
2. **Multiple iterations**: 20+ iterations for stable statistics
3. **Isolated tests**: One operation per benchmark
4. **Realistic data**: Use typical parameter values
5. **Clean state**: Clear caches between runs

### Avoiding Pitfalls

**Dead code elimination:**
```java
// Bad: result unused, may be optimized away
generate(params);

// Good: use result
PipelineResult result = generate(params);
assertNotNull(result);
```

**GC interference:**
```java
// Force GC before measurement
System.gc();
System.gc();
Thread.yield();
```

**Measurement overhead:**
```java
// Measure coarse-grained operations (>1ms)
// Nanosecond timing for fine-grained (<1ms)
```

## Continuous Monitoring

### Tracking Over Time

Commit benchmark results to track trends:

```bash
./scripts/run-benchmarks.sh | tee benchmarks/$(date +%Y%m%d).txt
git add benchmarks/
git commit -m "Benchmark results for $(date +%Y-%m-%d)"
```

### Performance Dashboard

Create dashboard showing:
- Median generation time over commits
- Memory usage trends
- Regression alerts

### Regression Detection

Fail CI if performance degrades:

```bash
# Compare against baseline
if [ $current_median -gt $((baseline_median * 120 / 100)) ]; then
    echo "Performance regression: >20% slower"
    exit 1
fi
```

## Future Enhancements

- [ ] JMH integration for micro-benchmarks
- [ ] Continuous benchmark tracking
- [ ] Performance regression alerts
- [ ] Flamegraph generation
- [ ] Comparison against previous commits
- [ ] GPU rendering benchmarks
- [ ] Network serialization benchmarks

## Resources

- [Java Performance Tuning Guide](https://docs.oracle.com/en/java/javase/21/gctuning/)
- [JMH Samples](https://github.com/openjdk/jmh)
- [Minecraft Performance Wiki](https://minecraft.fandom.com/wiki/Performance)

## Contact

For performance issues or optimization questions, open an issue with:
- Benchmark results
- Profiler output
- System specifications
- Minecraft version
