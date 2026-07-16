# 03 — Component System

**Layer**: Kernel  
**Module**: `aperture-core/component`  
**Status**: ✅ Implementation ~70% complete, Documentation new  
**Dependencies**: Parameter Engine

---

## Overview

The Component System is Aperture's compositional architecture for building openings. Instead of hard-coding door and window generation logic, every opening is **assembled from reusable components**.

**Core Insight**: A door and a window are not fundamentally different types. They are both compositions of:
- Frame (structural boundary)
- Panel (opaque infill or kinematic leaf)
- Glass (transparent infill)
- Hardware (handles, hinges, locks)
- Trim (decorative molding)
- Other components

**The difference is in the assembly, not the code.**

**Design Principle**: Components are **declarative recipes**, not imperative instructions. A `FrameComponent` says "there should be a frame with these properties", not "draw a frame at these coordinates".

---

## Why Components?

### Before (Monolithic Generators)

```java
class DoorGenerator {
    public Geometry generate(...) {
        // 200 lines of door-specific logic
        drawFrame();
        drawPanel();
        addHinges();
        addHandle();
        ...
    }
}

class WindowGenerator {
    public Geometry generate(...) {
        // 180 lines of window-specific logic
        drawFrame();  // duplicated!
        drawGlass();
        addMullions();
        ...
    }
}
```

**Problems**:
- Code duplication (frame logic repeated)
- Hard to add new opening types
- Can't reuse door panels in windows
- Generator logic grows exponentially

### After (Component Assembly)

```json
{
  "id": "aperture:single_door",
  "components": [
    { "kind": "frame", "id": "frame", "profile": "aperture:door_frame" },
    { "kind": "panel", "id": "door_leaf", "kinematic": "swing" },
    { "kind": "hardware", "id": "hinges", "count": 3 },
    { "kind": "hardware", "id": "handle" }
  ]
}
```

```json
{
  "id": "aperture:fixed_window",
  "components": [
    { "kind": "frame", "id": "frame", "profile": "aperture:window_frame" },
    { "kind": "glass", "id": "glazing", "system": "double_glazed" },
    { "kind": "mullion", "id": "mullions", "source": "parameter:mullions" }
  ]
}
```

**Benefits**:
- Frame logic written once, reused everywhere
- New opening = new component assembly (no new generator code)
- Components can be mixed (door with glass panel, window with operable panel)
- Generator logic is generic and stable

---

## Core Concepts

### OpeningComponent

Base interface for all components:

```java
public interface OpeningComponent {
    /**
     * Unique identifier within the opening.
     * Used to reference this component from parameters, materials, etc.
     */
    ComponentRef ref();
    
    /**
     * Component category (FRAME, GLASS, PANEL, etc.)
     */
    ComponentKind kind();
    
    /**
     * Component-specific properties.
     * Interpreted by the generator based on component kind.
     */
    ComponentProperties properties();
}
```

**Key Points**:
- Components are **immutable data** (records)
- Components don't generate geometry themselves (that's the generator's job)
- Components are **declarative**: they describe what should exist, not how to build it

### ComponentKind

The taxonomy of component categories:

```java
public enum ComponentKind {
    FRAME,       // Structural boundary (always rectangular currently)
    PANEL,       // Opaque infill (door leaf, window sash)
    GLASS,       // Transparent infill
    MULLION,     // Vertical divider
    DIVIDER,     // Horizontal divider (transom)
    HANDLE,      // Door/window handle
    HARDWARE,    // Generic hardware (hinges, locks, tracks, etc.)
    TRIM,        // Decorative molding
    SILL,        // Bottom ledge (window sill, door threshold)
    HEADER,      // Top trim
    DECORATION   // Generic decoration component
}
```

**Extensibility**: New kinds can be added without breaking existing code. Generators that don't recognize a kind can skip it.

### ComponentRef

Unique identifier for a component within an opening:

```java
public record ComponentRef(String id) {
    public static ComponentRef of(String id);
}
```

**Usage**:
- Parameters reference components: `"source": "component:door_leaf"`
- Materials bind to components: `"frame_material" → component "frame"`
- Editor selects components by ref

### ComponentProperties

Key-value properties specific to each component kind:

```java
public record ComponentProperties(Map<String, Object> values) {
    public String getString(String key);
    public double getDouble(String key);
    public int getInt(String key);
    public boolean getBoolean(String key);
    // ...
}
```

**Example**:
```java
var frameProps = ComponentProperties.builder()
    .put("profile", "aperture:door_frame_l_80x120")
    .put("material_slot", "frame")
    .build();
```

---

## Component Types

### 1. FrameComponent

**Purpose**: Defines the structural boundary of the opening.

**Properties**:
- `profile`: Profile ID (defines frame cross-section)
- `material_slot`: Which material parameter to bind to
- `inset`: Distance from opening boundary to frame inner edge (optional)

**Example**:
```java
FrameComponent.builder()
    .id("frame")
    .profile("aperture:window_frame_l_50x80")
    .materialSlot("frame")
    .build();
```

**JSON**:
```json
{
  "kind": "frame",
  "id": "frame",
  "profile": "aperture:window_frame_l_50x80",
  "material_slot": "frame"
}
```

**Generation**:
1. Resolve profile (L-shaped, rectangular, etc.)
2. Extrude profile around opening perimeter
3. Apply material from slot
4. Generate corner joints (miter, cope, or butt)

**Current Status**: ✅ Basic implementation exists

---

### 2. PanelComponent

**Purpose**: Opaque infill or kinematic leaf (door leaf, operable window sash).

**Properties**:
- `count`: Number of panels (1 = single door, 2 = double door)
- `kinematic`: Motion type (`"none"`, `"swing"`, `"slide"`, `"fold"`)
- `material_slot`: Material binding
- `thickness`: Panel depth (mm)
- `raised`: Boolean (traditional raised panel vs flat)

**Example**:
```java
PanelComponent.builder()
    .id("door_leaf")
    .count(1)
    .kinematic(KinematicType.SWING)
    .hingeSide(HingeSide.LEFT)
    .materialSlot("panel")
    .build();
```

**JSON**:
```json
{
  "kind": "panel",
  "id": "door_leaf",
  "count": 1,
  "kinematic": "swing",
  "hinge_side": "left",
  "material_slot": "panel"
}
```

**Kinematic Types**:
- `none`: Fixed panel (non-operable)
- `swing`: Hinged rotation (doors, casement windows)
- `slide`: Linear horizontal motion (sliding doors)
- `fold`: Multi-segment folding (bifold doors)

**Generation**:
1. Divide opening into panel sections (based on count)
2. For each panel:
   - Generate panel geometry (flat or raised profile)
   - If kinematic: attach motion constraint
   - Apply material
3. Generate swept collision volume (if kinematic)

**Current Status**: ✅ Basic non-kinematic panels work, ⏳ Kinematic motion planned

---

### 3. GlassComponent

**Purpose**: Transparent infill.

**Properties**:
- `system`: Glazing system (`"single_glazed"`, `"double_glazed"`, `"triple_glazed"`)
- `material_slot`: Material binding
- `thickness`: Total glazing unit thickness (mm)
- `tint`: Optional tint color
- `low_e`: Boolean (low-emissivity coating)

**Example**:
```java
GlassComponent.builder()
    .id("glazing")
    .system("double_glazed")
    .materialSlot("glass")
    .build();
```

**JSON**:
```json
{
  "kind": "glass",
  "id": "glazing",
  "system": "double_glazed",
  "material_slot": "glass"
}
```

**Generation**:
1. Compute glazing bounds (opening minus frame inset)
2. Subtract mullion/divider volumes
3. Generate thin box geometry (or layered for multi-pane)
4. Apply transparent material
5. Set render mode (translucent pass)

**Current Status**: ✅ Basic single-layer glass works

---

### 4. MullionComponent

**Purpose**: Vertical dividers (split glass into multiple lites).

**Properties**:
- `count`: Number of mullions (derived from parameter or explicit)
- `source`: `"parameter:mullions"` (count from parameter) or `"explicit"` (fixed count)
- `profile`: Mullion cross-section
- `material_slot`: Material binding

**Example**:
```java
MullionComponent.builder()
    .id("mullions")
    .source("parameter:mullions")
    .profile("aperture:mullion_standard_50")
    .materialSlot("frame")
    .build();
```

**JSON**:
```json
{
  "kind": "mullion",
  "id": "mullions",
  "source": "parameter:mullions",
  "profile": "aperture:mullion_standard_50"
}
```

**Generation**:
1. Resolve count (from parameter or explicit value)
2. Compute mullion positions (evenly spaced)
3. Extrude profile vertically for each mullion
4. Subtract from glass component bounds

**Current Status**: ✅ Count-based mullions work, ⏳ Custom positioning future

---

### 5. DividerComponent

**Purpose**: Horizontal dividers (transoms).

**Properties**: Same as MullionComponent, but horizontal orientation.

**Example**:
```json
{
  "kind": "divider",
  "id": "transom",
  "position": 0.6,
  "profile": "aperture:transom_standard_60"
}
```

**Generation**: Same as mullion, but horizontal extrusion.

**Current Status**: ⏸️ Planned, not yet implemented

---

### 6. HandleComponent

**Purpose**: Door/window handle hardware.

**Properties**:
- `style`: Handle style (`"lever"`, `"knob"`, `"pull"`, `"recessed"`)
- `position`: Vertical position (0.0 = bottom, 1.0 = top, default 0.5)
- `side`: Which panel side (`"left"`, `"right"`, `"both"`)
- `material_slot`: Material binding

**Example**:
```json
{
  "kind": "handle",
  "id": "handle",
  "style": "lever",
  "position": 0.5,
  "side": "left"
}
```

**Generation**:
1. Compute anchor point on panel face
2. Load handle geometry from asset library (or generate procedurally)
3. Orient based on panel normal
4. Apply material

**Current Status**: ⏸️ Planned, not yet implemented

---

### 7. HardwareComponent

**Purpose**: Generic hardware (hinges, locks, tracks, closers, etc.).

**Properties**:
- `type`: Hardware type (`"hinge"`, `"lock"`, `"track"`, `"closer"`)
- `count`: Number of units (e.g., 3 hinges)
- `positions`: Explicit positions or auto-spaced

**Example**:
```json
{
  "kind": "hardware",
  "id": "hinges",
  "type": "hinge",
  "count": 3,
  "material_slot": "hardware"
}
```

**Generation**: Similar to HandleComponent, but may have multiple instances.

**Current Status**: ⏸️ Planned

---

### 8. TrimComponent

**Purpose**: Decorative molding (casing, architrave).

**Properties**:
- `profile`: Trim profile
- `sides`: Which sides (`["top", "left", "right"]`, etc.)
- `offset`: Distance from frame

**Example**:
```json
{
  "kind": "trim",
  "id": "casing",
  "profile": "aperture:casing_classical_80",
  "sides": ["top", "left", "right"]
}
```

**Current Status**: ✅ Type exists, ⏸️ Generation not implemented

---

### 9. SillComponent

**Purpose**: Bottom ledge (window sill, door threshold).

**Properties**:
- `profile`: Sill profile
- `projection`: How far sill extends beyond wall

**Example**:
```json
{
  "kind": "sill",
  "id": "sill",
  "profile": "aperture:window_sill_sloped",
  "projection": 50
}
```

**Current Status**: ✅ Type exists, ⏸️ Generation not implemented

---

### 10. HeaderComponent

**Purpose**: Top trim or lintel.

**Current Status**: ✅ Type exists, ⏸️ Generation not implemented

---

### 11. DecorationComponent

**Purpose**: Generic decorative elements (rosettes, keystones, etc.).

**Current Status**: ⏸️ Planned for advanced use

---

## ComponentAssembly

A `ComponentAssembly` is an **ordered collection** of components that defines an opening type.

```java
public record ComponentAssembly(List<OpeningComponent> components) {
    public static ComponentAssembly of(OpeningComponent... components);
    public static ComponentAssembly of(List<OpeningComponent> components);
    
    public List<OpeningComponent> all();
    public Optional<OpeningComponent> get(ComponentRef ref);
    public List<OpeningComponent> byKind(ComponentKind kind);
}
```

**Example**:
```java
var assembly = ComponentAssembly.of(
    FrameComponent.builder()
        .id("frame")
        .profile("aperture:door_frame")
        .build(),
    PanelComponent.builder()
        .id("door_leaf")
        .kinematic(KinematicType.SWING)
        .build(),
    HandleComponent.builder()
        .id("handle")
        .style("lever")
        .build()
);
```

**JSON Representation**:
```json
{
  "components": [
    { "kind": "frame", "id": "frame", "profile": "aperture:door_frame" },
    { "kind": "panel", "id": "door_leaf", "kinematic": "swing" },
    { "kind": "handle", "id": "handle", "style": "lever" }
  ]
}
```

### Order Matters

Components are processed in order:
1. **Frame** (defines boundaries)
2. **Glass/Panel** (fill interior)
3. **Mullion/Divider** (subdivide glass)
4. **Hardware** (attach to panels/frame)
5. **Trim** (decorative, rendered last)

**Why**: Later components may depend on earlier ones (e.g., handle attaches to panel, which is inside frame).

---

## Component Generation Lifecycle

```
ComponentAssembly (from Definition)
    ↓
ComponentPlanBuilder (resolve parameters, compute layout)
    ↓
ComponentPlan (expanded, positioned components)
    ↓
GeometryBuilder (per-component geometry generation)
    ↓
GeometryAssembly (solids with material slots)
    ↓
MeshCompiler (triangulation, UV mapping)
    ↓
MeshAssembly (GPU-ready vertices)
```

### Step 1: ComponentAssembly → ComponentPlan

**Input**: Static assembly from definition + resolved parameters  
**Output**: Component plan with computed positions, sizes, counts

**Example**:
```java
// Definition has:
ComponentAssembly assembly = ...; // { frame, glass, mullions }
ParameterSet params = ...; // { width: 1800, mullions: 2 }

// Builder expands:
ComponentPlanBuilder builder = new ComponentPlanBuilder(assembly, params);
ComponentPlan plan = builder.build();

// Plan contains:
// - Frame: bounds = [0,0] to [1800, 1500]
// - Glass: bounds = [50,50] to [1750, 1450] (minus frame inset)
// - Mullion 1: position x=600, height=1400
// - Mullion 2: position x=1200, height=1400
```

**Responsibilities**:
- Resolve parameter references (`"source": "parameter:mullions"`)
- Compute positions (evenly spaced mullions, centered handles, etc.)
- Expand count-based components into individual instances
- Validate layout (e.g., too many mullions for width)

**Current Status**: ⏳ Partially exists in `ComponentPlanBuilder`, not fully separated from generation yet

---

### Step 2: ComponentPlan → GeometryAssembly

**Input**: Component plan with layout  
**Output**: Geometry solids with material slots

**Example**:
```java
GeometryBuilder geomBuilder = new GeometryBuilder(plan);

for (ComponentInstance comp : plan.instances()) {
    switch (comp.kind()) {
        case FRAME -> geomBuilder.addFrame(comp);
        case GLASS -> geomBuilder.addGlass(comp);
        case MULLION -> geomBuilder.addMullion(comp);
        // ...
    }
}

GeometryAssembly assembly = geomBuilder.build();
```

**GeometryAssembly** contains:
```java
public record GeometryAssembly(
    List<GeometrySolid> solids,
    BoundingBox bounds,
    CutVolume cutVolume,        // For host wall cutting
    CollisionProxy collision     // Simplified collision shape
)
```

**GeometrySolid**:
```java
public record GeometrySolid(
    ComponentRef componentRef,
    String materialSlot,
    SolidShape shape,           // BoundingBox, Extrusion, BRep, etc.
    Transform3d localTransform,
    RenderLayer layer           // OPAQUE, CUTOUT, TRANSLUCENT
)
```

**Responsibilities**:
- Generate geometric primitives (boxes, extrusions, sweeps)
- Assign material slots
- Compute collision volumes
- Determine render layers

**Current Status**: ⏳ `GeometryBuilder` stub exists, needs component-specific implementations

---

### Step 3: GeometryAssembly → MeshAssembly

**Input**: Geometry solids  
**Output**: Triangulated mesh with UVs

See [05-rendering.md](../05-rendering.md) for details.

**Current Status**: ✅ Box compiler works, ⏳ Extrusion compiler planned

---

## Component Presets

**Problem**: Every opening type must define a `ComponentAssembly`. This is verbose.

**Solution**: `ComponentAssemblyPresets` — common assemblies as reusable presets.

```java
public class ComponentAssemblyPresets {
    public static ComponentAssembly fixedWindow() {
        return ComponentAssembly.of(
            FrameComponent.standard("frame"),
            GlassComponent.standard("glazing"),
            MullionComponent.fromParameter("mullions")
        );
    }
    
    public static ComponentAssembly singleDoor() {
        return ComponentAssembly.of(
            FrameComponent.doorFrame("frame"),
            PanelComponent.swing("door_leaf", HingeSide.LEFT),
            HardwareComponent.hinges("hinges", 3),
            HandleComponent.lever("handle")
        );
    }
}
```

**Usage in Definition**:
```java
var definition = OpeningTypeDefinition.builder(id, category, generator)
    .components(ComponentAssemblyPresets.fixedWindow())
    .build();
```

**Current Status**: ✅ Presets exist for fixed_window and door

---

## Component Parameters

Components can reference parameters:

```json
{
  "kind": "mullion",
  "id": "mullions",
  "count": { "source": "parameter:mullions" }
}
```

**Resolution Flow**:
1. Parameter `mullions` is resolved to a value (e.g., 2)
2. `ComponentPlanBuilder` reads the parameter
3. Expands into 2 mullion instances

**Other Parameter References**:
- Material slots: `"material_slot": "frame"` → binds to parameter `frame_material`
- Kinematic state: Panel's `open_ratio` parameter drives motion
- Dimensions: Future (e.g., `"width": { "source": "parameter:panel_width" }`)

---

## Material Binding

Components declare material slots:

```java
FrameComponent.builder()
    .id("frame")
    .materialSlot("frame")  // Binds to parameter "frame_material"
    .build();
```

**Resolution**:
1. Parameter `frame_material` = `"aperture:oak_frame"`
2. `MaterialResolver` looks up `aperture:oak_frame` in catalog
3. Returns `MaterialInstance` with blocks, textures, properties
4. `GeometrySolid` for frame references this material

**See**: [platform/02-rendering-pipeline.md] for full material resolution flow

---

## Component Nesting (Future)

**Goal**: Components can contain other components.

**Example**: Panel with inset glass
```json
{
  "kind": "panel",
  "id": "door_leaf",
  "children": [
    {
      "kind": "glass",
      "id": "vision_panel",
      "bounds": { "inset": 100 }
    }
  ]
}
```

**Status**: ⏸️ Planned for advanced configurations (Phase 10+)

**Challenge**: Nested layout computation, material inheritance, collision hierarchy

---

## Component Validation

Components can be validated before generation:

```java
public interface ComponentValidator {
    ValidationResult validate(ComponentAssembly assembly, ParameterSet params);
}
```

**Validation Rules**:
- Required components present (e.g., every opening needs a frame)
- Component refs are unique
- Material slots reference valid parameters
- Kinematic panels have required hardware (e.g., swing panel needs hinges)
- Layout constraints (e.g., mullion count fits within width)

**Current Status**: ⏸️ Planned, not implemented

---

## Testing Strategy

### Unit Tests

**Component Construction**:
```java
@Test
void frameComponent_withProfile_setsProperties() {
    var frame = FrameComponent.builder()
        .id("frame")
        .profile("aperture:test_profile")
        .build();
    
    assertEquals(ComponentKind.FRAME, frame.kind());
    assertEquals("aperture:test_profile", frame.properties().getString("profile"));
}
```

**Component Assembly**:
```java
@Test
void componentAssembly_byKind_filtersCorrectly() {
    var assembly = ComponentAssembly.of(
        FrameComponent.standard("frame"),
        GlassComponent.standard("glass"),
        MullionComponent.fromParameter("mullions")
    );
    
    var mullions = assembly.byKind(ComponentKind.MULLION);
    assertEquals(1, mullions.size());
}
```

### Integration Tests

**Parameter Resolution**:
```java
@Test
void componentPlanBuilder_expandsMullionsFromParameter() {
    var assembly = ComponentAssembly.of(
        MullionComponent.fromParameter("mullions")
    );
    var params = ParameterSet.builder()
        .put("mullions", 3)
        .build();
    
    var plan = new ComponentPlanBuilder(assembly, params).build();
    var mullions = plan.instancesByKind(ComponentKind.MULLION);
    
    assertEquals(3, mullions.size());
}
```

**End-to-End**:
```java
@Test
void fixedWindow_withMullions_generatesCorrectGeometry() {
    var definition = loadDefinition("aperture:fixed_window");
    var params = ParameterSet.builder()
        .put("width", 1800.0)
        .put("mullions", 2)
        .build();
    
    var result = generator.generate(definition, params);
    
    // Should have: frame, glass, 2 mullions
    assertEquals(4, result.getSolids().size());
    
    var mullions = result.getSolids().stream()
        .filter(s -> s.componentRef().id().contains("mullion"))
        .toList();
    assertEquals(2, mullions.size());
}
```

---

## Current Status

| Component | Implementation | Generation | Notes |
|-----------|----------------|------------|-------|
| Frame | ✅ Complete | ⏳ Box only | Profile extrusion planned |
| Panel | ✅ Complete | ⏳ Flat only | Raised panels, kinematic motion planned |
| Glass | ✅ Complete | ✅ Complete | Single-layer works |
| Mullion | ✅ Complete | ✅ Count-based | Custom positioning future |
| Divider | ✅ Type exists | ❌ Not impl | Future |
| Handle | ✅ Type exists | ❌ Not impl | Future |
| Hardware | ✅ Type exists | ❌ Not impl | Future |
| Trim | ✅ Type exists | ❌ Not impl | Future |
| Sill | ✅ Type exists | ❌ Not impl | Future |
| Header | ✅ Type exists | ❌ Not impl | Future |

**Overall**: Component types are well-defined. Generation logic is ~40% complete.

---

## Acceptance Criteria

**For Kernel V1**:
- [x] All component types defined
- [x] ComponentAssembly API stable
- [x] Component presets for fixed_window and door
- [ ] **Component generation contract documented** (this document)
- [ ] ComponentPlanBuilder separated from generator
- [ ] Unit tests for all component types

**For Platform V1**:
- [ ] Frame profile extrusion works
- [ ] Panel kinematic motion (swing, slide)
- [ ] Hardware components (hinges, handles)
- [ ] All reference types (fixed_window, door, curtain_wall) use component assembly exclusively

**For Editor V1** (Phase C):
- [ ] Components can be selected individually
- [ ] Component properties editable in Inspector
- [ ] Add/remove components in design session

---

## Related Documents

- [kernel/02-parameter-engine.md](02-parameter-engine.md) — Components reference parameters
- [kernel/01-geometry-kernel.md](01-geometry-kernel.md) — Components generate geometry
- [platform/01-opening-pipeline.md](../platform/01-opening-pipeline.md) — Component → Geometry flow
- [05-rendering.md](../05-rendering.md) — Geometry → Mesh compilation

---

## Future Extensions

### Phase 4 (Opening Library)
- All door types share same ComponentAssembly structure
- All window types share same ComponentAssembly structure
- Differences are only in parameter presets and component choices

### Phase 11 (Curtain Wall)
- Grid layout components (rows × columns)
- Multi-panel assemblies
- Structural mullion/transom components

### Phase 12 (NodeCraft)
- Components as nodes in the graph
- Custom component types via plugins
- Procedural component generation (e.g., parametric handle shapes)

---

**Document Status**: ✅ Complete  
**Last Updated**: 2026-07-16  
**Implementation**: ~70% (types complete, generation partial)  
**Next Review**: After Platform V1 completion
