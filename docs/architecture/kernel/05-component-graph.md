# 05 — Component Graph

**Layer**: Kernel  
**Status**: 🎯 CRITICAL — Component-based Architecture  
**Dependencies**: Parameter Engine, Generation Pipeline

---

## Overview

**Components are not objects. Components are nodes in a graph.**

Aperture's component system is not traditional OOP composition. It's a **dataflow graph** where components:
- Expose **input ports** (parameters they consume)
- Expose **output ports** (geometry, constraints, or data they produce)
- Can **depend on other components**
- Are evaluated in **topological order**

**This is the same model as**:
- Houdini's node network
- Blender's Geometry Nodes
- Unreal's Material Graph
- Grasshopper's parametric components

**Design Principle**: A component is a **pure function** in a dependency graph, not a stateful object.

---

## Why Graph, Not Hierarchy?

### Traditional Hierarchy (What We Don't Want)

```
Opening
├── Frame
│   └── contains Rectangle
├── Glass
│   └── contains Plane
└── Panel
    └── contains Box
```

**Problems**:
- Fixed parent-child relationship
- Can't express "Panel depends on Frame bounds"
- Can't express "Mullion subdivides Glass"
- Hard to add cross-cutting concerns (e.g., all components need material)

### Component Graph (What We Want)

```
Parameters
    ↓
┌───────────┐
│   Frame   │ (produces bounds)
└─────┬─────┘
      │
      ├──→ Glass (consumes frame bounds)
      │      ↓
      │    Mullion (subdivides glass)
      │
      └──→ Panel (fits within frame bounds)
             ↓
           Handle (attaches to panel)
```

**Benefits**:
- ✅ Components declare dependencies explicitly
- ✅ Evaluation order is computed automatically (topological sort)
- ✅ Changes propagate correctly (frame resize → glass updates)
- ✅ New component types don't break existing code
- ✅ Can visualize as node graph (future NodeCraft)

---

## Core Concepts

### Component Node

A component node is a **pure function** with ports:

```java
public interface ComponentNode {
    /**
     * Unique ID within the opening.
     */
    ComponentRef ref();
    
    /**
     * Component type (FRAME, GLASS, PANEL, etc.)
     */
    ComponentKind kind();
    
    /**
     * Input ports (what this component needs).
     */
    List<InputPort> inputs();
    
    /**
     * Output ports (what this component produces).
     */
    List<OutputPort> outputs();
    
    /**
     * Evaluate this component given resolved inputs.
     * 
     * @param context Evaluation context with input values
     * @return Component result (geometry, bounds, etc.)
     */
    ComponentResult evaluate(EvaluationContext context);
}
```

### Ports

**Input Port**: Data the component consumes

```java
public record InputPort(
    String name,              // "bounds", "material", "parent"
    PortType type,            // BOUNDS, GEOMETRY, PARAMETER, etc.
    boolean required,         // Must be connected?
    Object defaultValue       // If optional and not connected
) {}
```

**Output Port**: Data the component produces

```java
public record OutputPort(
    String name,              // "geometry", "bounds", "cutVolume"
    PortType type
) {}
```

**Port Types**:
- `PARAMETER` — Parameter value (double, string, etc.)
- `BOUNDS` — BoundingBox
- `GEOMETRY` — GeometrySolid or list of solids
- `TRANSFORM` — Transform3d
- `CONSTRAINT` — Constraint rule
- `MATERIAL` — MaterialInstance

### Edges

An edge connects an output port to an input port:

```java
public record ComponentEdge(
    ComponentRef source,
    String sourcePort,
    ComponentRef target,
    String targetPort
) {}
```

**Example**:
```
Frame.output("bounds") → Glass.input("parent_bounds")
```

### Component Graph

```java
public record ComponentGraph(
    List<ComponentNode> nodes,
    List<ComponentEdge> edges,
    ComponentRef rootNode          // Entry point (usually Frame)
) {
    /**
     * Validates graph (no cycles, all required inputs connected).
     */
    public GraphValidation validate();
    
    /**
     * Computes evaluation order (topological sort).
     */
    public List<ComponentRef> evaluationOrder();
}
```

---

## Component Node Types

### 1. FrameNode

**Responsibility**: Defines opening boundary.

**Inputs**:
- `width` (PARAMETER, required)
- `height` (PARAMETER, required)
- `profile` (PARAMETER, optional, default: standard frame)
- `material` (MATERIAL, required)

**Outputs**:
- `bounds` (BOUNDS) — Outer bounds of opening
- `inner_bounds` (BOUNDS) — Bounds minus frame inset
- `geometry` (GEOMETRY) — Frame solids (4 extrusions)

**Evaluation**:
```java
public ComponentResult evaluate(EvaluationContext ctx) {
    var width = ctx.getParameter("width");
    var height = ctx.getParameter("height");
    var profile = ctx.getParameter("profile");
    
    var outerBounds = new BoundingBox(0, 0, 0, width, height, profile.depth);
    var innerBounds = outerBounds.inset(profile.width);
    
    var geometry = extrudeFrameProfile(profile, outerBounds);
    
    return ComponentResult.builder()
        .output("bounds", outerBounds)
        .output("inner_bounds", innerBounds)
        .output("geometry", geometry)
        .build();
}
```

---

### 2. GlassNode

**Responsibility**: Fill interior with transparent material.

**Inputs**:
- `parent_bounds` (BOUNDS, required) — From Frame.inner_bounds
- `material` (MATERIAL, required)
- `thickness` (PARAMETER, optional, default: 6mm)
- `subdivisions` (GEOMETRY list, optional) — Mullions to subtract

**Outputs**:
- `geometry` (GEOMETRY) — Glass solid(s)

**Evaluation**:
```java
public ComponentResult evaluate(EvaluationContext ctx) {
    var bounds = ctx.getBounds("parent_bounds");
    var thickness = ctx.getParameter("thickness");
    var subdivisions = ctx.getGeometryList("subdivisions");
    
    // Generate glass plane
    var glass = createGlassPlane(bounds, thickness);
    
    // Subtract mullion volumes
    for (var mullion : subdivisions) {
        glass = subtract(glass, mullion);
    }
    
    return ComponentResult.builder()
        .output("geometry", glass)
        .build();
}
```

---

### 3. MullionNode (Generator)

**Responsibility**: Generate multiple vertical dividers.

**Inputs**:
- `parent_bounds` (BOUNDS, required)
- `count` (PARAMETER, required)
- `profile` (PARAMETER, optional)
- `material` (MATERIAL, required)

**Outputs**:
- `geometry` (GEOMETRY list) — Multiple mullion solids
- `subdivisions` (GEOMETRY list) — Same as geometry (for glass consumption)

**Evaluation**:
```java
public ComponentResult evaluate(EvaluationContext ctx) {
    var bounds = ctx.getBounds("parent_bounds");
    var count = ctx.getInt("count");
    var profile = ctx.getParameter("profile");
    
    var mullions = new ArrayList<GeometrySolid>();
    var spacing = bounds.width() / (count + 1);
    
    for (int i = 1; i <= count; i++) {
        var x = spacing * i;
        var mullion = extrudeMullionProfile(profile, x, bounds.height());
        mullions.add(mullion);
    }
    
    return ComponentResult.builder()
        .output("geometry", mullions)
        .output("subdivisions", mullions)  // Same list
        .build();
}
```

---

### 4. PanelNode

**Responsibility**: Opaque infill or kinematic leaf.

**Inputs**:
- `parent_bounds` (BOUNDS, required)
- `material` (MATERIAL, required)
- `kinematic` (PARAMETER, optional) — "swing", "slide", "none"
- `open_ratio` (PARAMETER, optional, default: 0.0)

**Outputs**:
- `geometry` (GEOMETRY) — Panel solid
- `transform` (TRANSFORM) — Current transform (if kinematic)
- `anchor_points` (list of Vec3d) — Hardware attachment points

**Evaluation**:
```java
public ComponentResult evaluate(EvaluationContext ctx) {
    var bounds = ctx.getBounds("parent_bounds");
    var kinematicType = ctx.getString("kinematic");
    var openRatio = ctx.getDouble("open_ratio");
    
    var panel = createPanelSolid(bounds);
    
    Transform3d transform = Transform3d.identity();
    if ("swing".equals(kinematicType)) {
        transform = computeSwingTransform(openRatio);
    } else if ("slide".equals(kinematicType)) {
        transform = computeSlideTransform(openRatio);
    }
    
    var anchorPoints = computeHardwareAnchors(bounds, kinematicType);
    
    return ComponentResult.builder()
        .output("geometry", panel)
        .output("transform", transform)
        .output("anchor_points", anchorPoints)
        .build();
}
```

---

### 5. HandleNode

**Responsibility**: Door/window handle hardware.

**Inputs**:
- `anchor_point` (Vec3d, required) — From Panel.anchor_points
- `style` (PARAMETER, required) — "lever", "knob", "pull"
- `material` (MATERIAL, required)

**Outputs**:
- `geometry` (GEOMETRY) — Handle solid

**Evaluation**:
```java
public ComponentResult evaluate(EvaluationContext ctx) {
    var anchor = ctx.getVec3("anchor_point");
    var style = ctx.getString("style");
    
    var handleGeometry = loadHandleAsset(style);
    handleGeometry = handleGeometry.transform(Transform3d.translation(anchor));
    
    return ComponentResult.builder()
        .output("geometry", handleGeometry)
        .build();
}
```

---

## Graph Construction

### From ComponentAssembly to ComponentGraph

**Step 1: Parse Assembly**

```json
{
  "components": [
    { "kind": "frame", "id": "frame", "profile": "aperture:standard" },
    { "kind": "glass", "id": "glazing" },
    { "kind": "mullion", "id": "mullions", "count": { "source": "parameter:mullions" } }
  ]
}
```

**Step 2: Create Nodes**

```java
var frameNode = new FrameNode(ComponentRef.of("frame"));
var glassNode = new GlassNode(ComponentRef.of("glazing"));
var mullionNode = new MullionNode(ComponentRef.of("mullions"));
```

**Step 3: Infer Edges**

```java
// Automatic edge inference based on port types
edges.add(new ComponentEdge(
    frameNode.ref(), "inner_bounds",
    glassNode.ref(), "parent_bounds"
));

edges.add(new ComponentEdge(
    frameNode.ref(), "inner_bounds",
    mullionNode.ref(), "parent_bounds"
));

edges.add(new ComponentEdge(
    mullionNode.ref(), "subdivisions",
    glassNode.ref(), "subdivisions"
));
```

**Step 4: Build Graph**

```java
var graph = new ComponentGraph(
    List.of(frameNode, glassNode, mullionNode),
    edges,
    frameNode.ref()  // Root
);
```

---

## Graph Evaluation

### Topological Sort

**Algorithm**:
1. Find nodes with no incoming edges (roots)
2. Process each root, mark visited
3. Remove edges from visited nodes
4. Repeat until all nodes processed or cycle detected

**Example**:
```
Nodes: Frame, Mullion, Glass
Edges: Frame → Mullion, Frame → Glass, Mullion → Glass

Evaluation order:
1. Frame (no dependencies)
2. Mullion (depends on Frame only)
3. Glass (depends on Frame and Mullion)
```

**Code**:
```java
public List<ComponentRef> computeEvaluationOrder() {
    var order = new ArrayList<ComponentRef>();
    var visited = new HashSet<ComponentRef>();
    var inDegree = computeInDegree();
    
    // Start with nodes that have no dependencies
    var queue = new ArrayDeque<ComponentRef>();
    for (var node : nodes) {
        if (inDegree.get(node.ref()) == 0) {
            queue.add(node.ref());
        }
    }
    
    while (!queue.isEmpty()) {
        var current = queue.poll();
        order.add(current);
        visited.add(current);
        
        // Reduce in-degree for neighbors
        for (var edge : edges) {
            if (edge.source().equals(current)) {
                var target = edge.target();
                inDegree.merge(target, -1, Integer::sum);
                if (inDegree.get(target) == 0) {
                    queue.add(target);
                }
            }
        }
    }
    
    if (order.size() != nodes.size()) {
        throw new CyclicGraphException("Component graph contains cycle");
    }
    
    return order;
}
```

---

### Evaluation Context

**Context accumulates outputs as nodes are evaluated**:

```java
public class EvaluationContext {
    private final Map<ComponentRef, ComponentResult> results = new HashMap<>();
    private final ParameterSet parameters;
    private final Map<String, MaterialInstance> materials;
    
    /**
     * Get parameter value.
     */
    public double getParameter(String name) {
        return parameters.getDouble(name);
    }
    
    /**
     * Get bounds from another component's output.
     */
    public BoundingBox getBounds(String inputPort) {
        var edge = findEdgeToPort(inputPort);
        var sourceResult = results.get(edge.source());
        return (BoundingBox) sourceResult.getOutput(edge.sourcePort());
    }
    
    /**
     * Get geometry list from another component.
     */
    public List<GeometrySolid> getGeometryList(String inputPort) {
        var edge = findEdgeToPort(inputPort);
        var sourceResult = results.get(edge.source());
        return (List<GeometrySolid>) sourceResult.getOutput(edge.sourcePort());
    }
    
    /**
     * Store result after node evaluation.
     */
    public void putResult(ComponentRef ref, ComponentResult result) {
        results.put(ref, result);
    }
}
```

---

### Full Graph Evaluation

```java
public GeometryAssembly evaluateGraph(
    ComponentGraph graph,
    ParameterSet parameters,
    Map<String, MaterialInstance> materials
) {
    var context = new EvaluationContext(parameters, materials);
    var order = graph.evaluationOrder();
    
    // Evaluate each node in topological order
    for (var nodeRef : order) {
        var node = graph.getNode(nodeRef);
        var result = node.evaluate(context);
        context.putResult(nodeRef, result);
    }
    
    // Collect all geometry outputs
    var allGeometry = new ArrayList<GeometrySolid>();
    for (var nodeRef : order) {
        var result = context.getResult(nodeRef);
        var geom = result.getOutput("geometry");
        if (geom instanceof GeometrySolid solid) {
            allGeometry.add(solid);
        } else if (geom instanceof List<?> list) {
            allGeometry.addAll((List<GeometrySolid>) list);
        }
    }
    
    return new GeometryAssembly(
        allGeometry,
        computeBounds(allGeometry),
        computeCutVolume(allGeometry),
        materials
    );
}
```

---

## Incremental Update

**Problem**: When a parameter changes, we don't want to re-evaluate the entire graph.

**Solution**: Track which nodes depend on which parameters, only re-evaluate affected subgraph.

### Dependency Tracking

```java
public record NodeDependencies(
    ComponentRef nodeRef,
    Set<String> parameterDeps,    // Parameters this node reads
    Set<ComponentRef> nodeDeps     // Other nodes this node depends on
) {}
```

**Example**:
```
Frame depends on: { "width", "height", "frame_material" }
Mullion depends on: { "mullions" } + { Frame }
Glass depends on: { "glass_material" } + { Frame, Mullion }
```

### Incremental Evaluator

```java
public GeometryAssembly incrementalUpdate(
    ComponentGraph graph,
    EvaluationContext previousContext,
    Set<String> changedParameters
) {
    // Find affected nodes
    var affectedNodes = new HashSet<ComponentRef>();
    for (var node : graph.nodes()) {
        var deps = node.getDependencies();
        if (!Collections.disjoint(deps.parameterDeps(), changedParameters)) {
            affectedNodes.add(node.ref());
        }
    }
    
    // Add downstream nodes (cascade)
    var cascade = new HashSet<>(affectedNodes);
    for (var affected : affectedNodes) {
        cascade.addAll(graph.getDownstreamNodes(affected));
    }
    
    // Reuse unaffected results, re-evaluate affected
    var newContext = previousContext.clone();
    var order = graph.evaluationOrder();
    
    for (var nodeRef : order) {
        if (cascade.contains(nodeRef)) {
            var node = graph.getNode(nodeRef);
            var result = node.evaluate(newContext);
            newContext.putResult(nodeRef, result);
        }
        // else: reuse cached result
    }
    
    return assembleGeometry(newContext, graph);
}
```

**Example**:
```
Change: "mullions" parameter: 2 → 3

Affected directly: MullionNode
Affected by cascade: GlassNode (depends on MullionNode.subdivisions)
Unaffected: FrameNode (doesn't depend on "mullions")

Reused: Frame geometry (cached)
Re-evaluated: Mullion (new count), Glass (new subdivisions)
```

---

## Graph Validation

### Validation Rules

1. **No cycles**: Graph must be acyclic (DAG)
2. **Required inputs connected**: All required input ports must have edges
3. **Type matching**: Edge source/target port types must match
4. **Unique refs**: No duplicate ComponentRef
5. **Reachable**: All nodes must be reachable from root

### Validator

```java
public class GraphValidator {
    public GraphValidation validate(ComponentGraph graph) {
        var issues = new ArrayList<ValidationIssue>();
        
        // Check cycles
        try {
            graph.evaluationOrder();
        } catch (CyclicGraphException e) {
            issues.add(ValidationIssue.error("Graph contains cycle: " + e.getMessage()));
        }
        
        // Check required inputs
        for (var node : graph.nodes()) {
            for (var input : node.inputs()) {
                if (input.required() && !graph.hasEdgeToPort(node.ref(), input.name())) {
                    issues.add(ValidationIssue.error(
                        String.format("Required input '%s.%s' not connected", 
                            node.ref(), input.name())
                    ));
                }
            }
        }
        
        // Check type matching
        for (var edge : graph.edges()) {
            var sourcePort = graph.getOutputPort(edge.source(), edge.sourcePort());
            var targetPort = graph.getInputPort(edge.target(), edge.targetPort());
            if (!sourcePort.type().equals(targetPort.type())) {
                issues.add(ValidationIssue.error(
                    String.format("Type mismatch: %s.%s (%s) → %s.%s (%s)",
                        edge.source(), edge.sourcePort(), sourcePort.type(),
                        edge.target(), edge.targetPort(), targetPort.type())
                ));
            }
        }
        
        return new GraphValidation(issues.isEmpty(), issues);
    }
}
```

---

## JSON Representation

### Explicit Graph (Future)

```json
{
  "nodes": [
    {
      "ref": "frame",
      "kind": "frame",
      "inputs": {
        "width": { "source": "parameter:width" },
        "height": { "source": "parameter:height" }
      }
    },
    {
      "ref": "mullions",
      "kind": "mullion",
      "inputs": {
        "parent_bounds": { "source": "frame.inner_bounds" },
        "count": { "source": "parameter:mullions" }
      }
    },
    {
      "ref": "glass",
      "kind": "glass",
      "inputs": {
        "parent_bounds": { "source": "frame.inner_bounds" },
        "subdivisions": { "source": "mullions.subdivisions" }
      }
    }
  ]
}
```

### Implicit Graph (Current)

```json
{
  "components": [
    { "kind": "frame", "id": "frame" },
    { "kind": "glass", "id": "glass" },
    { "kind": "mullion", "id": "mullions", "count": { "source": "parameter:mullions" } }
  ]
}
```

**Edges inferred by convention**:
- Frame always provides `inner_bounds`
- Glass/Panel always consume `parent_bounds` from Frame
- Mullion/Divider provide `subdivisions` to Glass

---

## Current Status vs. Graph Model

### What Exists Today

**Components** ✅:
- Component types defined (Frame, Glass, Panel, etc.)
- ComponentAssembly (list of components)
- ComponentProperties (key-value data)

**Not Graph Yet** ❌:
- No explicit nodes/edges
- No ports
- No topological evaluation
- No incremental update
- Edges are implicit (hardcoded in generator)

### Migration Path

**Phase 1 (Current)**: ComponentAssembly with implicit dependencies
**Phase 2 (Kernel V1)**: Add ComponentPlanBuilder that resolves implicit graph
**Phase 3 (Platform V1)**: Formalize as ComponentGraph with explicit edges
**Phase 4 (NodeCraft)**: User-editable graph in UI

---

## Acceptance Criteria

### For Kernel V1 (Week 2)
- [ ] ComponentNode interface defined
- [ ] Graph validation (cycle detection, required inputs)
- [ ] Topological sort implementation
- [ ] Evaluation context that accumulates results
- [ ] Unit tests for graph evaluation

### For Platform V1 (Phase B)
- [ ] All component types implement ComponentNode
- [ ] Incremental update works (parameter change → partial re-evaluation)
- [ ] Graph visualization (debug output, not UI)

### For NodeCraft (Phase F)
- [ ] User-editable graph in UI
- [ ] Custom component nodes (user-defined)
- [ ] Graph import/export
- [ ] Live evaluation preview

---

## Related Documents

- [kernel/04-generation-pipeline.md](04-generation-pipeline.md) — Component Stage in pipeline
- [kernel/03-component-system.md](03-component-system.md) — Component types
- [kernel/02-parameter-engine.md](02-parameter-engine.md) — Parameter → component binding

---

**Document Status**: ✅ Complete  
**Last Updated**: 2026-07-16  
**Implementation**: 20% (types exist, graph model not implemented)  
**Next Review**: After Platform V1
