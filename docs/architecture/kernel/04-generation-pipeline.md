# 04 — Generation Pipeline

**Layer**: Kernel  
**Status**: 🎯 CRITICAL — The CPU of Aperture  
**Dependencies**: Parameter Engine, Component System, Geometry Kernel

---

## Overview

**The Generation Pipeline is Aperture's CPU.**

It defines how an opening transforms from abstract definition to concrete world entity. Every door, window, curtain wall, and future building component flows through this exact pipeline.

**Design Principle**: Data flows through discrete stages. Each stage has a single responsibility, explicit inputs/outputs, and cannot bypass other stages.

**This is NOT optional architecture**. This IS the architecture.

---

## The Pipeline

```
┌─────────────────┐
│   Definition    │  JSON opening type
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Parameter Stage │  Resolve defaults, merge overrides
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Constraint Stage│  Validate parameter constraints
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Component Stage │  Build component graph
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Geometry Stage  │  Generate geometric solids
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Mesh Stage    │  Triangulate to renderable mesh
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Collision Stage │  Generate collision proxies
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Placement Stage │  Validate host, compute footprint
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Render Stage   │  Commit to GPU
└─────────────────┘
```

---

## Stage 1: Parameter Stage

### Responsibility
Resolve complete parameter set from sparse overrides.

### Input
- `OpeningTypeDefinition` (schema with defaults)
- `ParameterSet` (sparse overrides from instance or editor)

### Output
- `ResolvedOpening` (complete, immutable parameter snapshot)

### Contract
```java
public interface ParameterStage {
    /**
     * Resolves sparse parameter overrides into a complete parameter set.
     * 
     * @param definition Opening type schema
     * @param overrides Sparse parameter values
     * @return Complete resolved parameters
     * @throws ParameterResolutionException if merge fails
     */
    ResolvedOpening resolve(OpeningTypeDefinition definition, ParameterSet overrides);
}
```

### Implementation Rules
1. **Merge with defaults**: Every parameter in schema must have a value in output
2. **Type safety**: Values must match parameter type (length → double, boolean → boolean)
3. **Immutability**: Output is immutable (future stages cannot modify)
4. **No constraint checking**: Only merge, don't validate (that's next stage)

### Data Structure
```java
public record ResolvedOpening(
    OpeningId typeId,
    ParameterSet parameters,  // Complete, all keys present
    long timestamp            // When resolved
) {}
```

### Error Handling
- Missing parameter in override → use default (not an error)
- Type mismatch → `ParameterResolutionException`
- Unknown parameter key → log warning, ignore

### Caching
- **Cacheable**: Yes (pure function)
- **Cache key**: `(typeId, overrides.hashCode())`
- **Invalidation**: Definition schema change

### Ownership
- **Module**: `aperture-core/parametric`
- **Class**: `OpeningParameterResolver`

---

## Stage 2: Constraint Stage

### Responsibility
Validate parameter values against constraints.

### Input
- `OpeningTypeDefinition` (constraint rules)
- `ResolvedOpening` (complete parameters)

### Output
- `ValidationResult` (pass/fail + issues)

### Contract
```java
public interface ConstraintStage {
    /**
     * Validates resolved parameters against definition constraints.
     * 
     * @param definition Opening type with constraint rules
     * @param resolved Complete parameter set
     * @return Validation result (may contain errors/warnings)
     */
    ValidationResult validate(OpeningTypeDefinition definition, ResolvedOpening resolved);
}
```

### Implementation Rules
1. **Range validation**: Check parameter min/max (from metadata)
2. **Expression validation**: Evaluate constraint expressions (from definition.constraints)
3. **Dependency order**: Evaluate constraints in declaration order
4. **Non-blocking warnings**: Warnings don't stop pipeline, errors do

### Data Structure
```java
public record ValidationResult(
    boolean valid,
    List<ValidationIssue> issues
) {
    public boolean hasErrors();
    public boolean hasWarnings();
    public List<ValidationIssue> getErrors();
}

public record ValidationIssue(
    ValidationSeverity severity,  // ERROR, WARNING, INFO
    String parameterKey,           // Which parameter
    String message,                // Human-readable
    String constraint              // Which constraint rule
) {}
```

### Error Handling
- Constraint expression syntax error → `ConstraintEvaluationException`
- Missing parameter in expression → `ConstraintEvaluationException`
- Division by zero in expression → treat as constraint failure (validation error)

### Caching
- **Cacheable**: Yes (pure function)
- **Cache key**: `(typeId, resolved.parameters.hashCode())`
- **Invalidation**: Definition constraint rules change

### Ownership
- **Module**: `aperture-core/constraint`
- **Class**: `ParametricValidator`

---

## Stage 3: Component Stage

### Responsibility
Build component graph from definition and resolved parameters.

### Input
- `OpeningTypeDefinition` (component assembly)
- `ResolvedOpening` (complete parameters)

### Output
- `ComponentPlan` (expanded component instances with layout)

### Contract
```java
public interface ComponentStage {
    /**
     * Expands component assembly into a component plan.
     * Resolves parameter references, computes positions, expands counts.
     * 
     * @param definition Opening type with component assembly
     * @param resolved Complete parameter set
     * @return Component plan ready for geometry generation
     * @throws ComponentPlanException if layout computation fails
     */
    ComponentPlan plan(OpeningTypeDefinition definition, ResolvedOpening resolved);
}
```

### Implementation Rules
1. **Parameter resolution**: Replace `"source": "parameter:X"` with actual values
2. **Count expansion**: Expand count-based components (e.g., 3 mullions → 3 instances)
3. **Position computation**: Compute layout (evenly spaced mullions, centered handles, etc.)
4. **Dependency order**: Process components in assembly order
5. **Bounds computation**: Each component knows its bounding box

### Data Structure
```java
public record ComponentPlan(
    List<ComponentInstance> instances,
    BoundingBox globalBounds,
    Map<ComponentRef, BoundingBox> componentBounds
) {}

public record ComponentInstance(
    ComponentRef ref,
    ComponentKind kind,
    ComponentProperties properties,
    Transform3d localTransform,
    BoundingBox bounds
) {}
```

### Error Handling
- Unknown component kind → log warning, skip
- Invalid layout (e.g., mullions too wide) → `ComponentPlanException`
- Circular component dependency → `ComponentPlanException`

### Caching
- **Cacheable**: Partially (layout computation expensive)
- **Cache key**: `(typeId, resolved.parameters.hashCode())`
- **Invalidation**: Definition component assembly change

### Ownership
- **Module**: `aperture-core/component`, `aperture-opening-geometry`
- **Class**: `ComponentPlanBuilder`

---

## Stage 4: Geometry Stage

### Responsibility
Generate geometric solids from component plan.

### Input
- `ComponentPlan` (expanded components with layout)
- `ResolvedOpening` (for material slot resolution)

### Output
- `GeometryAssembly` (solid shapes with material bindings)

### Contract
```java
public interface GeometryStage {
    /**
     * Generates geometric solids from component plan.
     * Each component becomes one or more GeometrySolid.
     * 
     * @param plan Component plan with layout
     * @param resolved Parameter set for material binding
     * @return Geometry assembly
     * @throws GeometryGenerationException if generation fails
     */
    GeometryAssembly generate(ComponentPlan plan, ResolvedOpening resolved);
}
```

### Implementation Rules
1. **Per-component generation**: Each component generates its own solids
2. **Material binding**: Resolve material slots to material IDs
3. **No mesh**: Geometry stage outputs abstract shapes (BoundingBox, Extrusion, etc.), NOT triangles
4. **Coordinate system**: All geometry in opening-local coordinates (origin at anchor)
5. **Boolean operations**: Handle overlaps (frame corners, mullion intersections)

### Data Structure
```java
public record GeometryAssembly(
    List<GeometrySolid> solids,
    BoundingBox bounds,
    CutVolume hostCut,           // Volume to cut from host
    Map<String, MaterialInstance> materials
) {}

public record GeometrySolid(
    ComponentRef componentRef,
    String materialSlot,
    SolidShape shape,            // BoundingBox, Extrusion, BRep, etc.
    Transform3d localTransform,
    RenderLayer layer            // OPAQUE, CUTOUT, TRANSLUCENT
) {}

public sealed interface SolidShape permits 
    BoundingBox, Extrusion, Sweep, BRep {}
```

### Error Handling
- Unsupported shape type → fallback to BoundingBox, log warning
- Material not found → use default material, log warning
- Geometry overlap (Z-fighting) → log warning, continue
- Invalid profile → `GeometryGenerationException`

### Caching
- **Cacheable**: Yes (expensive operation)
- **Cache key**: `(typeId, plan.hashCode())`
- **Invalidation**: Component plan change OR material catalog change

### Ownership
- **Module**: `aperture-opening-geometry`, `aperture-geometry`
- **Class**: `GeometryBuilder`

---

## Stage 5: Mesh Stage

### Responsibility
Triangulate geometric solids into renderable mesh.

### Input
- `GeometryAssembly` (abstract solid shapes)

### Output
- `MeshAssembly` (triangulated, GPU-ready)

### Contract
```java
public interface MeshStage {
    /**
     * Compiles geometric solids into triangulated mesh.
     * 
     * @param geometry Geometry assembly
     * @return Mesh assembly with vertices, normals, UVs
     * @throws MeshCompilationException if triangulation fails
     */
    MeshAssembly compile(GeometryAssembly geometry);
}
```

### Implementation Rules
1. **Triangulation**: Convert all shapes to triangles
2. **UV mapping**: Generate texture coordinates
3. **Normal computation**: Per-vertex normals for smooth shading
4. **Vertex deduplication**: Share vertices where possible
5. **Material batching**: Group by material for GPU efficiency
6. **LOD generation**: (future) Generate multiple detail levels

### Data Structure
```java
public record MeshAssembly(
    List<MeshPart> parts,
    BoundingBox bounds,
    int totalVertices,
    int totalTriangles
) {}

public record MeshPart(
    ComponentRef componentRef,
    String materialSlot,
    RenderLayer layer,
    VertexBuffer vertices,       // Positions, normals, UVs, colors
    IndexBuffer indices,         // Triangle indices
    BoundingBox bounds
) {}
```

### Error Handling
- Degenerate triangle → skip, log warning
- UV mapping failure → use default UVs
- Normal computation failure → use flat normals
- Vertex limit exceeded → split into multiple parts

### Caching
- **Cacheable**: Yes (most expensive stage)
- **Cache key**: `(typeId, geometry.hashCode())`
- **Invalidation**: Geometry assembly change

### Ownership
- **Module**: `aperture-render`
- **Class**: `MeshCompiler`

---

## Stage 6: Collision Stage

### Responsibility
Generate simplified collision shapes.

### Input
- `GeometryAssembly` (full geometry)

### Output
- `CollisionProxy` (simplified shapes for physics)

### Contract
```java
public interface CollisionStage {
    /**
     * Generates collision proxy from geometry.
     * Simplifies geometry for physics queries.
     * 
     * @param geometry Full geometry assembly
     * @return Collision proxy
     */
    CollisionProxy buildCollision(GeometryAssembly geometry);
}
```

### Implementation Rules
1. **Simplification**: Reduce triangle count (target: <50% of visual mesh)
2. **Convex decomposition**: Split concave shapes into convex hulls
3. **Bounding volume hierarchy**: Organize for fast queries
4. **Opening state**: Handle kinematic components (door swing volume)

### Data Structure
```java
public record CollisionProxy(
    List<ConvexHull> hulls,
    BoundingBox bounds,
    SweptVolume kinematicSweep   // For moving parts (doors, windows)
) {}

public record SweptVolume(
    List<ConvexHull> keyframes,  // Collision at different open_ratio
    double minRatio,              // 0.0 = closed
    double maxRatio               // 1.0 = fully open
) {}
```

### Error Handling
- Convex decomposition failure → use bounding box fallback
- Too many hulls → merge smallest, log warning

### Caching
- **Cacheable**: Yes
- **Cache key**: `(typeId, geometry.bounds.hashCode())`
- **Invalidation**: Geometry assembly change

### Ownership
- **Module**: `aperture-render/collision`
- **Class**: `CollisionBuilder`

---

## Stage 7: Placement Stage

### Responsibility
Validate placement in world, compute footprint.

### Input
- `GeometryAssembly` (geometry with cut volume)
- `PlacementContext` (target location, host surface)

### Output
- `PlacementResult` (validation + footprint)

### Contract
```java
public interface PlacementStage {
    /**
     * Validates placement and computes world footprint.
     * 
     * @param geometry Geometry assembly
     * @param context Placement context (location, host)
     * @return Placement result (valid + footprint or invalid + errors)
     */
    PlacementResult validate(GeometryAssembly geometry, PlacementContext context);
}
```

### Implementation Rules
1. **Host validation**: Check if host surface is valid
2. **Bounds validation**: Check if opening fits within host
3. **Overlap validation**: Check for collision with existing openings
4. **Footprint computation**: Compute block positions to modify
5. **Cut volume application**: Determine which blocks to remove

### Data Structure
```java
public record PlacementResult(
    boolean valid,
    List<ValidationIssue> issues,
    OpeningFootprint footprint   // null if invalid
) {}

public record OpeningFootprint(
    BlockPos anchor,
    Direction facing,
    Set<BlockPos> occupiedBlocks,   // Blocks occupied by opening
    Set<BlockPos> cutBlocks,        // Blocks to remove from host
    HostBinding hostBinding
) {}
```

### Error Handling
- No valid host surface → validation error
- Opening too large for host → validation error
- Overlap with existing opening → validation error
- Invalid facing direction → validation error

### Caching
- **Cacheable**: No (world-dependent)
- **Cache key**: N/A
- **Invalidation**: N/A

### Ownership
- **Module**: `aperture-fabric/placement`
- **Class**: `PlacementService`

---

## Stage 8: Render Stage

### Responsibility
Upload mesh to GPU, register for rendering.

### Input
- `MeshAssembly` (GPU-ready mesh)
- `OpeningInstance` (world entity)

### Output
- `RenderHandle` (GPU resource ID)

### Contract
```java
public interface RenderStage {
    /**
     * Commits mesh to GPU for rendering.
     * 
     * @param mesh Mesh assembly
     * @param instance Opening instance in world
     * @return Render handle for updates/deletion
     */
    RenderHandle commit(MeshAssembly mesh, OpeningInstance instance);
}
```

### Implementation Rules
1. **GPU upload**: Copy mesh data to vertex/index buffers
2. **Material binding**: Bind textures and shader uniforms
3. **Transform**: Apply world transform (anchor + facing)
4. **Render queue**: Register for appropriate render pass (opaque/cutout/translucent)
5. **Culling**: Register bounding box for frustum culling

### Data Structure
```java
public record RenderHandle(
    long meshId,                  // GPU buffer ID
    long renderRevision,          // Increments on update
    BoundingBox worldBounds
) {}
```

### Error Handling
- GPU buffer allocation failure → retry or use fallback mesh
- Texture missing → use default texture, log warning
- Shader compilation failure → use fallback shader

### Caching
- **Cacheable**: GPU-side only
- **Cache key**: N/A (GPU manages)
- **Invalidation**: Triggered by parameter edit or deletion

### Ownership
- **Module**: `aperture-render`, `src/client`
- **Class**: `FabricRenderBackend`, `OpeningInstanceRenderer`

---

## Pipeline Orchestration

### PipelineExecutor

The pipeline executor is responsible for:
1. Running stages in order
2. Short-circuiting on errors
3. Caching intermediate results
4. Incremental updates

```java
public class PipelineExecutor {
    private final ParameterStage parameterStage;
    private final ConstraintStage constraintStage;
    private final ComponentStage componentStage;
    private final GeometryStage geometryStage;
    private final MeshStage meshStage;
    private final CollisionStage collisionStage;
    
    /**
     * Executes full pipeline from definition to geometry.
     * 
     * @return PipelineResult containing all stage outputs
     */
    public PipelineResult execute(
        OpeningTypeDefinition definition,
        ParameterSet overrides
    ) {
        // Stage 1: Parameter resolution
        var resolved = parameterStage.resolve(definition, overrides);
        
        // Stage 2: Constraint validation
        var validation = constraintStage.validate(definition, resolved);
        if (!validation.valid()) {
            return PipelineResult.invalid(validation);
        }
        
        // Stage 3: Component planning
        var plan = componentStage.plan(definition, resolved);
        
        // Stage 4: Geometry generation
        var geometry = geometryStage.generate(plan, resolved);
        
        // Stage 5: Mesh compilation
        var mesh = meshStage.compile(geometry);
        
        // Stage 6: Collision building
        var collision = collisionStage.buildCollision(geometry);
        
        return PipelineResult.success(resolved, validation, plan, geometry, mesh, collision);
    }
}
```

### PipelineResult

```java
public record PipelineResult(
    boolean success,
    ResolvedOpening resolved,
    ValidationResult validation,
    ComponentPlan componentPlan,
    GeometryAssembly geometry,
    MeshAssembly mesh,
    CollisionProxy collision
) {
    public static PipelineResult invalid(ValidationResult validation) {
        return new PipelineResult(false, null, validation, null, null, null, null);
    }
    
    public static PipelineResult success(...) {
        return new PipelineResult(true, ...);
    }
}
```

---

## Incremental Updates

**Problem**: Changing a single parameter shouldn't regenerate the entire opening.

**Solution**: Track which stages depend on which parameters.

### Invalidation Rules

| Parameter Change | Invalidate From |
|------------------|-----------------|
| Width, Height | Stage 3 (Component) |
| Mullion count | Stage 3 (Component) |
| Material | Stage 5 (Mesh) |
| Open ratio (kinematic state) | Stage 4 (Geometry) |

### Implementation

```java
public class IncrementalPipeline {
    private PipelineCache cache;
    
    public PipelineResult update(
        OpeningInstance instance,
        ParameterSet newOverrides
    ) {
        var oldOverrides = instance.getParameters();
        var changedKeys = diffParameters(oldOverrides, newOverrides);
        
        // Determine invalidation point
        var invalidateFrom = computeInvalidationStage(changedKeys);
        
        // Reuse cached results up to invalidation point
        switch (invalidateFrom) {
            case PARAMETER -> {
                // Full pipeline
                return executor.execute(instance.getDefinition(), newOverrides);
            }
            case COMPONENT -> {
                // Reuse resolved parameters, rerun from component stage
                var resolved = cache.getResolved(instance.getTypeId(), newOverrides);
                // ...
            }
            case MESH -> {
                // Reuse geometry, only recompile mesh
                var geometry = cache.getGeometry(instance.getInstanceId());
                var mesh = meshStage.compile(geometry);
                // ...
            }
        }
    }
}
```

---

## Error Propagation

**Rule**: Errors stop the pipeline. Warnings continue.

### Error Types

| Stage | Error Type | Action |
|-------|------------|--------|
| Parameter | `ParameterResolutionException` | Abort, log |
| Constraint | Validation errors | Abort, return errors to user |
| Component | `ComponentPlanException` | Abort, log |
| Geometry | `GeometryGenerationException` | Abort, log |
| Mesh | `MeshCompilationException` | Fallback to bounding box, log |
| Collision | Failure | Use bounding box fallback |
| Placement | Validation errors | Abort, return errors to user |
| Render | GPU failure | Retry or skip, log |

### Error Context

Every exception carries context:

```java
public class PipelineException extends RuntimeException {
    private final PipelineStage stage;
    private final OpeningId typeId;
    private final Map<String, Object> context;
    
    public PipelineException(PipelineStage stage, String message, Throwable cause) {
        super(String.format("[%s] %s", stage, message), cause);
        this.stage = stage;
    }
}
```

---

## Thread Safety

**Rule**: Pipeline stages are **stateless** and **thread-safe**.

### Guarantees

1. **No shared mutable state**: All stage implementations are stateless
2. **Immutable data**: All stage inputs/outputs are immutable records
3. **Safe caching**: Cache uses concurrent data structures
4. **Parallel execution**: (future) Independent instances can be generated in parallel

### Concurrency Model

```java
public class ParallelPipelineExecutor {
    private final ExecutorService executor;
    
    public CompletableFuture<PipelineResult> executeAsync(
        OpeningTypeDefinition definition,
        ParameterSet overrides
    ) {
        return CompletableFuture.supplyAsync(() -> 
            execute(definition, overrides), executor
        );
    }
}
```

---

## Dependency Rules

### What Each Stage Can Access

| Stage | Can Access | Cannot Access |
|-------|------------|---------------|
| Parameter | Definition, Overrides | Nothing else |
| Constraint | Definition, Resolved | Nothing else |
| Component | Definition, Resolved | Minecraft world |
| Geometry | Plan, Resolved | Minecraft blocks, Render state |
| Mesh | Geometry | Minecraft, GPU |
| Collision | Geometry | Minecraft, Physics engine |
| Placement | Geometry, Context | Render state |
| Render | Mesh, Instance | Generation stages |

### Forbidden Dependencies

- ❌ Geometry stage cannot import `net.minecraft.*`
- ❌ Component stage cannot access GPU
- ❌ Mesh stage cannot access world state
- ❌ Render stage cannot re-run parameter resolution

**Enforcement**: CI checks module imports.

---

## Testing Strategy

### Stage Unit Tests

Each stage has isolated unit tests:

```java
@Test
void parameterStage_resolvesDefaults() {
    var definition = createTestDefinition();
    var overrides = ParameterSet.builder()
        .put("width", 1800.0)
        .build();
    
    var resolved = parameterStage.resolve(definition, overrides);
    
    assertEquals(1800.0, resolved.parameters().getDouble("width"));
    assertEquals(1500.0, resolved.parameters().getDouble("height")); // default
}
```

### Integration Tests

Pipeline flow tests:

```java
@Test
void pipeline_fromDefinitionToMesh_producesValidMesh() {
    var definition = loadDefinition("aperture:fixed_window");
    var overrides = ParameterSet.builder()
        .put("width", 1800.0)
        .put("mullions", 2)
        .build();
    
    var result = pipelineExecutor.execute(definition, overrides);
    
    assertTrue(result.success());
    assertNotNull(result.mesh());
    assertTrue(result.mesh().totalVertices() > 0);
}
```

### Golden Tests

Snapshot-based geometry validation:

```java
@Test
void pipeline_fixedWindow_matchesGoldenGeometry() {
    var result = pipelineExecutor.execute(fixedWindowDef, standardParams);
    
    assertGeometryMatches(result.geometry(), "golden/fixed_window_1800x1500.json");
}
```

---

## Current Status

| Stage | Interface | Implementation | Tests | Status |
|-------|-----------|----------------|-------|--------|
| Parameter | ✅ Exists | ✅ Complete | ✅ Good | DONE |
| Constraint | ✅ Exists | ✅ Complete | ⏳ Partial | DONE |
| Component | ✅ Exists | ⏳ Partial | ❌ Missing | IN PROGRESS |
| Geometry | ⏳ Partial | ⏳ Partial | ❌ Missing | IN PROGRESS |
| Mesh | ✅ Exists | ⏳ Box only | ❌ Missing | IN PROGRESS |
| Collision | ❌ Missing | ❌ Missing | ❌ Missing | PLANNED |
| Placement | ✅ Exists | ⏳ Partial | ❌ Missing | IN PROGRESS |
| Render | ✅ Exists | ✅ Complete | ❌ Missing | DONE |
| **Orchestrator** | ❌ Missing | ❌ Missing | ❌ Missing | **CRITICAL GAP** |

---

## Acceptance Criteria

### For Kernel V1 (Week 2)
- [ ] **PipelineExecutor exists and orchestrates all stages**
- [ ] All stage interfaces are defined and documented
- [ ] ParameterStage → ConstraintStage → ComponentStage works end-to-end
- [ ] Unit tests for each stage
- [ ] One golden test (fixed_window)

### For Platform V1 (Phase B)
- [ ] All stages fully implemented
- [ ] Incremental update works (change parameter → only rerun affected stages)
- [ ] Error propagation tested
- [ ] Golden tests for fixed_window, door, curtain_wall
- [ ] CI enforces dependency rules

### For Production (Phase C+)
- [ ] Parallel execution for independent instances
- [ ] GPU async upload (non-blocking render stage)
- [ ] Comprehensive error recovery
- [ ] Performance profiling per stage

---

## Related Documents

- [kernel/02-parameter-engine.md](02-parameter-engine.md) — Stage 1 detail
- [kernel/03-component-system.md](03-component-system.md) — Stage 3 detail
- [kernel/01-geometry-kernel.md](01-geometry-kernel.md) — Stage 4 detail
- [platform/01-opening-pipeline.md](../platform/01-opening-pipeline.md) — Platform integration
- [05-rendering.md](../05-rendering.md) — Stages 5 & 8 detail

---

## Next Steps

### Week 1 (Documentation)
1. ✅ This document (Generation Pipeline)
2. ⏳ Component Graph document (expand component stage)
3. ⏳ Constraint Solver document (expand constraint stage)
4. ⏳ Geometry Kernel document (expand geometry stage)

### Week 2 (Implementation)
1. **Implement PipelineExecutor** (orchestrator)
2. **Separate ComponentPlanBuilder** from generator
3. **Formalize stage interfaces** (extract to `aperture-core/pipeline`)
4. **Write golden tests** (fixed_window baseline)

### Week 3 (Validation)
1. End-to-end test (Definition → Mesh)
2. Incremental update test
3. Error propagation test
4. Performance profiling

### Week 4 (Refinement)
1. Geometry stage implementations (Profile extrusion)
2. Collision stage implementation
3. Mesh stage (Extrusion compiler)
4. Documentation updates

---

**Document Status**: ✅ Complete  
**Last Updated**: 2026-07-16  
**Implementation**: ~30% (stages exist but not orchestrated)  
**Next Review**: After Week 2 implementation

---

## Appendix: Why This Matters

### Before Pipeline Architecture

```java
// Monolithic generator
class RectangularWindowGenerator {
    public Mesh generate(OpeningInstance instance) {
        var params = instance.getParameters();
        var width = params.getDouble("width");
        // ... 200 lines of mixed parameter/geometry/mesh logic
        return mesh;
    }
}
```

**Problems**:
- Can't cache intermediate results
- Can't incrementally update
- Can't test stages independently
- Hard to add new opening types
- Impossible to optimize

### After Pipeline Architecture

```java
// Clean stages
var resolved = parameterStage.resolve(definition, overrides);
var validation = constraintStage.validate(definition, resolved);
var plan = componentStage.plan(definition, resolved);
var geometry = geometryStage.generate(plan, resolved);
var mesh = meshStage.compile(geometry);
```

**Benefits**:
- ✅ Each stage is testable
- ✅ Caching per stage
- ✅ Incremental updates
- ✅ Clear ownership
- ✅ Easy to optimize (parallelize stages)

**This is why the pipeline is Aperture's CPU.**
