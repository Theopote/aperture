# 01 — Opening Pipeline

**Layer**: Platform  
**Status**: 🎯 CRITICAL — Kernel ↔ Minecraft Bridge  
**Dependencies**: All Kernel systems, Minecraft APIs

---

## Overview

The Opening Pipeline is the **bridge between pure Kernel abstractions and Minecraft reality**. It orchestrates the transformation:

```
Opening Definition (JSON)
    ↓
Parameters (resolved with defaults/constraints)
    ↓
Component Graph (nodes with dependencies)
    ↓
Geometry Solids (pure math, millimeters)
    ↓
Mesh (triangulated surfaces)
    ↓
Block Grid (voxelized to Minecraft blocks)
    ↓
Collision Bounds (AABB for physics)
    ↓
Placement Context (world position, orientation)
    ↓
Rendering (BlockState placement, ghost preview)
```

**This is the full chain from "user selects door type" to "door appears in world".**

---

## Why Platform Layer?

### The Boundary

**Kernel** (pure):
- No Minecraft imports
- No world access
- No rendering
- Pure functions: `Definition → Geometry`

**Platform** (Minecraft-aware):
- Imports `net.minecraft.*`
- Reads/writes world blocks
- Handles rendering, networking, persistence
- Orchestrates: `Definition → World State`

**The Pipeline lives in Platform because it coordinates both layers.**

---

## Pipeline Stages

### Stage 1: Definition Loading

**Input**: JSON file (data pack)  
**Output**: `OpeningTypeDefinition`

```java
public record OpeningTypeDefinition(
    String id,
    Map<String, ParameterSchema> parameters,
    List<ConstraintExpression> constraints,
    List<ComponentDefinition> components,
    String generator  // "aperture:rectangular_window_v1"
) {}
```

**Loader**:
```java
public class OpeningTypeLoader {
    public OpeningTypeDefinition load(Path jsonPath) {
        var json = Files.readString(jsonPath);
        return OpeningTypeCodec.decode(json);
    }
}
```

**Current Status**: ✅ Complete

**Example**:
```json
// aperture-data/aperture/opening_types/fixed_window.json
{
  "id": "aperture:fixed_window",
  "parameters": {
    "width": { "type": "length", "default": 1200, "min": 300, "max": 6000 },
    "height": { "type": "length", "default": 1500, "min": 300, "max": 4000 }
  },
  "components": [
    { "kind": "frame", "profile": "aperture:frame_l_50x80" },
    { "kind": "glass", "thickness": 6 }
  ]
}
```

---

### Stage 2: Parameter Resolution

**Input**: `OpeningTypeDefinition` + user overrides  
**Output**: `ResolvedParameters`

```java
public class OpeningParameterResolver {
    public ResolvedParameters resolve(
        OpeningTypeDefinition type,
        Map<String, Object> userValues
    ) {
        var resolved = new HashMap<String, Object>();
        
        // 1. Start with defaults
        for (var entry : type.parameters().entrySet()) {
            resolved.put(entry.getKey(), entry.getValue().defaultValue());
        }
        
        // 2. Apply user overrides
        resolved.putAll(userValues);
        
        // 3. Validate constraints
        for (var constraint : type.constraints()) {
            if (!constraint.evaluate(resolved)) {
                throw new ConstraintViolationException(constraint);
            }
        }
        
        return new ResolvedParameters(resolved);
    }
}
```

**Current Status**: ✅ Complete

**Example**:
```java
var userValues = Map.of("width", 1500);
var resolved = resolver.resolve(fixedWindowType, userValues);
// Result: { width: 1500, height: 1500 (default) }
```

---

### Stage 3: Component Graph Construction

**Input**: `ComponentDefinition[]` from type  
**Output**: `ComponentGraph` (DAG with topological order)

```java
public class ComponentGraphBuilder {
    public ComponentGraph build(List<ComponentDefinition> definitions) {
        var graph = new ComponentGraph();
        
        // Create nodes
        for (var def : definitions) {
            var node = new ComponentNode(def.kind(), def.properties());
            graph.addNode(node);
        }
        
        // Detect dependencies (e.g., glass depends on frame bounds)
        for (var node : graph.nodes()) {
            if (node.kind() == ComponentKind.GLASS) {
                var frameNode = graph.findByKind(ComponentKind.FRAME);
                graph.addEdge(frameNode, node);  // glass depends on frame
            }
        }
        
        // Compute evaluation order
        graph.computeTopologicalOrder();
        
        return graph;
    }
}
```

**Current Status**: ✅ Complete (see kernel/05-component-graph.md)

---

### Stage 4: Component Generation

**Input**: `ComponentGraph` + `ResolvedParameters`  
**Output**: `Map<ComponentNode, GeometrySolid>`

```java
public class ComponentGenerationOrchestrator {
    private final Map<ComponentKind, ComponentGenerator> generators;
    
    public Map<ComponentNode, GeometrySolid> generate(
        ComponentGraph graph,
        ResolvedParameters params
    ) {
        var results = new HashMap<ComponentNode, GeometrySolid>();
        
        // Evaluate nodes in topological order
        for (var node : graph.topologicalOrder()) {
            var generator = generators.get(node.kind());
            
            // Collect inputs from dependencies
            var inputs = new GenerationContext(params);
            for (var dep : graph.dependencies(node)) {
                inputs.addComponent(dep.kind(), results.get(dep));
            }
            
            // Generate geometry
            var geometry = generator.generate(node, inputs);
            results.put(node, geometry);
        }
        
        return results;
    }
}
```

**Current Status**: ⏳ Partial (generators exist, orchestration incomplete)

**Example**:
```java
// Frame generator
public class FrameComponentGenerator implements ComponentGenerator {
    @Override
    public GeometrySolid generate(ComponentNode node, GenerationContext ctx) {
        var profile = loadProfile(node.properties().getString("profile"));
        var width = ctx.parameters().getLength("width");
        var height = ctx.parameters().getLength("height");
        
        // Extrude profile along frame path
        return extrudeFrameProfile(profile, width, height);
    }
}
```

---

### Stage 5: Mesh Generation

**Input**: `GeometrySolid[]`  
**Output**: `Mesh` (triangulated)

```java
public class MeshGenerator {
    public Mesh generate(List<GeometrySolid> solids) {
        var vertices = new ArrayList<Point3D>();
        var triangles = new ArrayList<Triangle>();
        
        for (var solid : solids) {
            var mesh = solid.toMesh();
            
            int offset = vertices.size();
            vertices.addAll(mesh.vertices());
            
            // Offset triangle indices
            for (var tri : mesh.triangles()) {
                triangles.add(new Triangle(
                    tri.v0() + offset,
                    tri.v1() + offset,
                    tri.v2() + offset
                ));
            }
        }
        
        return new Mesh(vertices, triangles);
    }
}
```

**Current Status**: ✅ Basic implementation exists

---

### Stage 6: Voxelization

**Input**: `Mesh` + `MaterialBindings`  
**Output**: `BlockGrid` (Minecraft blocks)

```java
public class OpeningVoxelizer {
    public BlockGrid voxelize(Mesh mesh, MaterialBindings materials) {
        var grid = new BlockGrid();
        var bounds = mesh.bounds();
        
        // Sample at block centers (1 block = 1000mm)
        for (int x = (int)bounds.min().x(); x <= bounds.max().x(); x += 1000) {
            for (int y = (int)bounds.min().y(); y <= bounds.max().y(); y += 1000) {
                for (int z = (int)bounds.min().z(); z <= bounds.max().z(); z += 1000) {
                    var center = new Point3D(x + 500, y + 500, z + 500);
                    
                    // Check if center is inside mesh
                    if (isInsideMesh(mesh, center)) {
                        var material = determineMaterial(mesh, center, materials);
                        grid.set(toBlockPos(center), material.blockState());
                    }
                }
            }
        }
        
        return grid;
    }
    
    private boolean isInsideMesh(Mesh mesh, Point3D point) {
        // Ray casting algorithm
        // Cast ray from point to infinity, count intersections
        // Odd count = inside, even count = outside
    }
}
```

**Current Status**: ⏳ Partial (basic voxelization, needs material assignment)

---

### Stage 7: Collision Calculation

**Input**: `Mesh` or `BlockGrid`  
**Output**: `CollisionBounds` (AABB for physics)

```java
public class CollisionCalculator {
    public CollisionBounds calculate(BlockGrid grid) {
        // Find occupied blocks, compute tight AABB
        var occupiedBlocks = grid.occupiedPositions();
        
        var minX = occupiedBlocks.stream().mapToInt(BlockPos::getX).min().orElse(0);
        var minY = occupiedBlocks.stream().mapToInt(BlockPos::getY).min().orElse(0);
        var minZ = occupiedBlocks.stream().mapToInt(BlockPos::getZ).min().orElse(0);
        var maxX = occupiedBlocks.stream().mapToInt(BlockPos::getX).max().orElse(0);
        var maxY = occupiedBlocks.stream().mapToInt(BlockPos::getY).max().orElse(0);
        var maxZ = occupiedBlocks.stream().mapToInt(BlockPos::getZ).max().orElse(0);
        
        return new CollisionBounds(
            new BlockPos(minX, minY, minZ),
            new BlockPos(maxX + 1, maxY + 1, maxZ + 1)
        );
    }
}
```

**Current Status**: ⏳ Partial (basic AABB, needs refinement)

---

### Stage 8: Placement

**Input**: `BlockGrid` + `PlacementContext` (position, orientation)  
**Output**: Blocks placed in world

```java
public class OpeningPlacer {
    public void place(
        World world,
        BlockGrid grid,
        PlacementContext context
    ) {
        var transform = context.transform();  // Position + rotation
        
        for (var entry : grid.entries()) {
            var localPos = entry.position();
            var worldPos = transform.apply(localPos);
            
            world.setBlockState(worldPos, entry.blockState());
        }
        
        // Create block entity to store opening data
        var blockEntity = new OpeningBlockEntity(context.position());
        blockEntity.setOpeningType(context.type());
        blockEntity.setParameters(context.parameters());
        world.setBlockEntity(context.position(), blockEntity);
    }
}
```

**Current Status**: ✅ Basic placement works, ⏳ NBT persistence incomplete

---

### Stage 9: Rendering

**Input**: `OpeningInstance` (from block entity)  
**Output**: Rendered geometry on screen

```java
public class OpeningRenderer {
    public void render(
        OpeningInstance instance,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers
    ) {
        // Retrieve cached mesh from pipeline
        var mesh = pipelineCache.getMesh(instance);
        
        // Apply world transform
        matrices.push();
        matrices.translate(instance.position().getX(), ...);
        matrices.multiply(instance.rotation());
        
        // Render mesh
        var vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getSolid());
        for (var triangle : mesh.triangles()) {
            renderTriangle(triangle, mesh.vertices(), matrices, vertexConsumer);
        }
        
        matrices.pop();
    }
}
```

**Current Status**: ✅ Complete (aperture-render)

---

## Pipeline Orchestration

### OpeningGenerationPipeline

**The coordinator that runs all stages**:

```java
public class OpeningGenerationPipeline {
    private final OpeningParameterResolver paramResolver;
    private final ComponentGraphBuilder graphBuilder;
    private final ComponentGenerationOrchestrator componentGenerator;
    private final MeshGenerator meshGenerator;
    private final OpeningVoxelizer voxelizer;
    private final CollisionCalculator collisionCalc;
    
    public PipelineResult execute(OpeningTypeDefinition type, Map<String, Object> userParams) {
        // Stage 1: Already loaded (type definition)
        
        // Stage 2: Resolve parameters
        var params = paramResolver.resolve(type, userParams);
        
        // Stage 3: Build component graph
        var graph = graphBuilder.build(type.components());
        
        // Stage 4: Generate components
        var solids = componentGenerator.generate(graph, params);
        
        // Stage 5: Generate mesh
        var mesh = meshGenerator.generate(solids.values().toList());
        
        // Stage 6: Voxelize
        var materials = resolveMaterials(params);
        var blockGrid = voxelizer.voxelize(mesh, materials);
        
        // Stage 7: Calculate collision
        var collision = collisionCalc.calculate(blockGrid);
        
        return new PipelineResult(mesh, blockGrid, collision, params);
    }
}
```

**Current Status**: ⏳ Skeleton exists, some stages incomplete

---

## Caching Strategy

### Why Cache?

Running the full pipeline is **expensive** (ms to hundreds of ms). Cache results to avoid recomputation.

### Cache Key

```java
public record PipelineCacheKey(
    String openingTypeId,
    ResolvedParameters parameters
) {}
```

### Cache Implementation

```java
public class PipelineCache {
    private final Map<PipelineCacheKey, PipelineResult> cache = new ConcurrentHashMap<>();
    
    public PipelineResult getOrCompute(
        OpeningTypeDefinition type,
        Map<String, Object> userParams
    ) {
        var params = paramResolver.resolve(type, userParams);
        var key = new PipelineCacheKey(type.id(), params);
        
        return cache.computeIfAbsent(key, k -> pipeline.execute(type, userParams));
    }
    
    public void invalidate(String openingTypeId) {
        cache.keySet().removeIf(key -> key.openingTypeId().equals(openingTypeId));
    }
}
```

**Invalidation**:
- When opening type asset reloaded (hot reload)
- When parameter edited (editor)
- Manual: `/aperture cache clear`

**Current Status**: ⏳ Basic caching exists, needs refinement

---

## Incremental Updates

### The Problem

User drags resize handle → width changes → **recompute entire pipeline?**

**No.** Only recompute affected stages.

### Dependency Tracking

```java
public class IncrementalPipeline {
    public PipelineResult update(
        PipelineResult previous,
        ParameterChange change
    ) {
        // Which stages are affected by this parameter?
        var affected = analyzeImpact(change);
        
        if (affected.contains(Stage.COMPONENT_GENERATION)) {
            // Re-run from component generation onward
            return pipeline.executeFrom(Stage.COMPONENT_GENERATION, ...);
        } else if (affected.contains(Stage.MESH_GENERATION)) {
            // Re-run from mesh generation onward (reuse components)
            return pipeline.executeFrom(Stage.MESH_GENERATION, previous.solids());
        }
        
        // No stages affected (e.g., cosmetic parameter)
        return previous;
    }
}
```

**Current Status**: ❌ Planned (Phase 3)

---

## Preview Pipeline

### Ghost Preview (Pre-placement)

**Before user confirms placement**, show translucent preview.

```java
public class PreviewPipeline {
    public void renderPreview(
        PlacementContext context,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers
    ) {
        // Run pipeline without placing blocks
        var result = pipeline.execute(context.type(), context.parameters());
        
        // Render mesh as ghost (translucent, no collision)
        var transform = context.transform();
        renderGhostMesh(result.mesh(), transform, matrices, vertexConsumers);
    }
}
```

**Current Status**: ✅ Complete (aperture-render, placement-preview)

---

## Validation Pipeline

### Pre-placement Checks

**Before allowing placement**, validate:

```java
public class PlacementValidator {
    public ValidationResult validate(PlacementContext context) {
        var issues = new ArrayList<ValidationIssue>();
        
        // 1. Parameters valid?
        if (!constraintsSatisfied(context.parameters())) {
            issues.add(ValidationIssue.error("Constraints violated"));
        }
        
        // 2. Space available?
        var collision = pipeline.execute(context.type(), context.parameters()).collision();
        if (worldBlocksOccupied(context.world(), collision, context.transform())) {
            issues.add(ValidationIssue.error("Space occupied"));
        }
        
        // 3. Attachable to surface?
        if (!isSupportedSurface(context.targetSurface())) {
            issues.add(ValidationIssue.warning("Unsupported surface"));
        }
        
        return new ValidationResult(issues.isEmpty(), issues);
    }
}
```

**Current Status**: ⏳ Partial (basic validation exists)

---

## Persistence Pipeline

### Save to World

```java
public class OpeningPersistence {
    public NbtCompound serialize(OpeningInstance instance) {
        var nbt = new NbtCompound();
        nbt.putString("type", instance.type().id());
        nbt.put("parameters", serializeParameters(instance.parameters()));
        nbt.putInt("rotation", instance.rotation().ordinal());
        return nbt;
    }
    
    public OpeningInstance deserialize(NbtCompound nbt) {
        var typeId = nbt.getString("type");
        var type = assetRegistry.openingTypes().get(AssetId.of(typeId))
            .orElseThrow(() -> new AssetNotFoundException(typeId));
        
        var params = deserializeParameters(nbt.getCompound("parameters"));
        var rotation = Rotation.values()[nbt.getInt("rotation")];
        
        return new OpeningInstance(type, params, rotation);
    }
}
```

**Current Status**: ❌ Critical gap (placed openings lost on reload)

**Priority**: 🔥 Week 2

---

## Network Synchronization

### Client ↔ Server

**Client** (preview, editor):
- User edits parameters
- Run pipeline locally for instant preview
- Send edit command to server

**Server** (authority):
- Validate parameters
- Run pipeline
- Place blocks
- Broadcast to clients

```java
// Client → Server
public record EditOpeningParametersPacket(
    BlockPos position,
    Map<String, Object> newParameters
) {}

// Server → Client
public record OpeningUpdatedPacket(
    BlockPos position,
    OpeningInstance newState
) {}
```

**Current Status**: ⏳ Basic networking exists, needs refinement

---

## Performance Targets

| Stage | Target | Current | Status |
|-------|--------|---------|--------|
| Definition load | < 1ms | ~0.5ms | ✅ |
| Parameter resolution | < 1ms | ~0.2ms | ✅ |
| Component generation | < 50ms | ~100ms | ⚠️ |
| Mesh generation | < 20ms | ~30ms | ⚠️ |
| Voxelization | < 50ms | ~80ms | ⚠️ |
| Collision calc | < 5ms | ~10ms | ⚠️ |
| Placement | < 10ms | ~15ms | ⚠️ |
| **Total (cold)** | **< 150ms** | **~235ms** | ⚠️ |
| **Total (cached)** | **< 5ms** | **~3ms** | ✅ |

**Optimization Plan** (Phase 3):
- Profile extrusion optimization
- Parallel component generation
- Spatial caching (BVH for voxelization)
- GPU mesh generation (compute shaders)

---

## Error Handling

### Pipeline Failures

```java
public class PipelineException extends RuntimeException {
    private final PipelineStage failedStage;
    private final OpeningTypeDefinition type;
    private final Map<String, Object> parameters;
    
    public String diagnosticMessage() {
        return String.format(
            "Pipeline failed at %s for opening type %s with parameters %s",
            failedStage, type.id(), parameters
        );
    }
}
```

**Error Recovery**:
- Log full diagnostic context
- Show user-friendly error in UI
- Fall back to default/last-known-good
- Allow manual retry

**Current Status**: ⏳ Basic error handling, needs improvement

---

## Testing Strategy

### Golden Tests

**Purpose**: Verify pipeline output stability.

```java
@Test
void pipeline_fixedWindow_matchesGolden() {
    var type = loadOpeningType("aperture:fixed_window");
    var params = Map.of("width", 1200, "height", 1500);
    
    var result = pipeline.execute(type, params);
    
    // Compare mesh vertices with golden file
    var golden = loadGoldenMesh("fixed_window_1200x1500.json");
    assertMeshEquals(golden, result.mesh());
}
```

**Golden Files**: `src/test/resources/golden/`

**Current Status**: ❌ Critical gap (no golden tests yet)

**Priority**: 🔥 Week 2

---

### Integration Tests

**End-to-end placement**:

```java
@Test
void pipeline_placeWindow_persistsCorrectly() {
    var world = createTestWorld();
    var context = new PlacementContext(
        world,
        new BlockPos(0, 64, 0),
        fixedWindowType,
        Map.of("width", 1200)
    );
    
    placer.place(world, context);
    
    // Reload world from NBT
    var reloadedWorld = reloadWorld(world);
    
    // Verify opening still exists
    var blockEntity = reloadedWorld.getBlockEntity(new BlockPos(0, 64, 0));
    assertNotNull(blockEntity);
    assertInstanceOf(OpeningBlockEntity.class, blockEntity);
}
```

**Current Status**: ❌ Blocked by NBT persistence

---

## Acceptance Criteria

### For Kernel V1 (Week 2)
- [ ] All pipeline stages implemented
- [ ] NBT persistence works (place → save → reload)
- [ ] Golden tests for fixed_window and door
- [ ] Performance: < 150ms cold, < 5ms cached

### For Platform V1 (Phase B)
- [ ] Incremental updates (parameter edit → partial recompute)
- [ ] Network sync (client preview → server authority)
- [ ] Error recovery (graceful degradation)
- [ ] Performance: < 100ms cold

### For Production (Phase C+)
- [ ] Parallel pipeline (multi-threaded component generation)
- [ ] GPU acceleration (compute shaders for mesh/voxel)
- [ ] Advanced caching (spatial BVH, texture atlas)

---

## Related Documents

- [kernel/04-generation-pipeline.md](../kernel/04-generation-pipeline.md) — 8-stage pipeline abstraction
- [kernel/05-component-graph.md](../kernel/05-component-graph.md) — Component evaluation order
- [05-rendering.md](../05-rendering.md) — Rendering pipeline details
- [06-placement.md](../06-placement.md) — Placement workflow
- [07-serialization.md](../07-serialization.md) — NBT persistence

---

**Document Status**: ✅ Complete  
**Last Updated**: 2026-07-16  
**Implementation**: ~60% (most stages exist, orchestration incomplete)  
**Next Review**: After NBT persistence implementation
