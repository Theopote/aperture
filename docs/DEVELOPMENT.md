# Aperture Development Process

**Version**: 1.0  
**Last Updated**: 2026-07-16

---

## Core Principle

**Architecture First, Code Second.**

Every feature in Aperture must be designed before it is implemented. This ensures long-term maintainability and prevents the codebase from becoming a collection of ad-hoc solutions.

---

## The Standard Process

Every feature, whether a new Kernel capability or an Application-level opening type, follows this eight-step process:

```
1. Architecture Design
   ↓
2. Written Specification
   ↓
3. Interface Definition
   ↓
4. Data Model
   ↓
5. Test Writing
   ↓
6. Implementation
   ↓
7. Examples
   ↓
8. Documentation
```

**You may not skip steps.** Each step has explicit deliverables and acceptance criteria.

---

## Step 1: Architecture Design

**Goal**: Understand where the feature fits in the system and what it depends on.

**Questions to Answer**:
- Which layer does this belong to? (Kernel / Platform / Editor / Applications)
- What existing systems does it depend on?
- Does it require new Kernel capabilities? If yes, design those first.
- What are the boundaries and responsibilities?
- How does it interact with other modules?

**Deliverable**: A design document in `docs/architecture/[layer]/[feature-name].md`

**Example**: Adding sliding doors
- Layer: Applications
- Dependencies: Component System (kinematic panels), Geometry Kernel (swept collision volumes)
- Kernel gaps: Panel motion constraints, swept volume generation
- Document: `docs/architecture/applications/sliding-door-design.md`

**Acceptance**: Architecture review (self-review or team review) confirms:
- No violations of layer boundaries
- Dependencies are explicit
- Kernel gaps are identified

---

## Step 2: Written Specification

**Goal**: Define the feature's behavior in plain language, with no code.

**Contents**:
- **Purpose**: Why does this exist?
- **Scope**: What's in and what's out?
- **User-facing behavior**: How does a user interact with it?
- **Data requirements**: What information does it need?
- **Constraints**: What are the rules and limitations?
- **Edge cases**: What happens when things go wrong?

**Deliverable**: A section in the design document or a separate `[feature-name]-spec.md`

**Example**: Sliding Door Specification
```markdown
## Purpose
Support sliding doors with 1-3 panels on horizontal tracks.

## Scope
- IN: Horizontal sliding (parallel to wall)
- IN: Pocket doors (panels slide into wall cavity)
- OUT: Vertical sliding (garage doors - different kinematic model)

## Parameters
- `panel_count`: 1-3
- `track_type`: "surface" | "pocket"
- `open_ratio`: 0.0-1.0 (state parameter)

## Constraints
- Pocket doors require `host_depth >= panel_thickness + 50mm`
- Panel width = opening_width / panel_count

## Edge Cases
- If insufficient pocket depth → validation error
- If panel_count > 3 → validation error
```

**Acceptance**: Specification is unambiguous and testable.

---

## Step 3: Interface Definition

**Goal**: Define the contracts (interfaces, abstract classes, record types) without implementation.

**Rules**:
- Use pure types: interfaces, abstract classes, records
- No implementation logic (method bodies are empty or throw `UnsupportedOperationException`)
- Document each method's contract (preconditions, postconditions, invariants)
- Define input/output types

**Deliverable**: Java interface/abstract class files with Javadoc

**Example**: Kinematic Panel Interface
```java
package dev.aperture.core.component;

/**
 * A panel that can move along a constrained path.
 * 
 * Implementors define the kinematic model (sliding, swinging, folding).
 */
public interface KinematicPanel extends OpeningComponent {
    /**
     * Returns the motion path this panel follows.
     * 
     * @return motion constraint (linear track, circular arc, etc.)
     */
    MotionConstraint getMotionPath();
    
    /**
     * Computes the panel's transform at a given open ratio.
     * 
     * @param openRatio 0.0 (closed) to 1.0 (fully open)
     * @return transform relative to opening anchor
     * @throws IllegalArgumentException if openRatio not in [0, 1]
     */
    Transform3d getTransformAt(double openRatio);
    
    /**
     * Returns the swept volume as the panel moves from 0 to 1.
     * Used for collision detection.
     */
    BoundingBox getSweptVolume();
}
```

**Acceptance**: Interfaces compile, are documented, and have no implementation.

---

## Step 4: Data Model

**Goal**: Define the data structures (records, schemas, JSON formats) that represent the feature.

**Rules**:
- Use immutable records where possible
- Define JSON schema if the feature is data-driven
- Include schema versioning (`schemaVersion` field)
- Document serialization format

**Deliverable**: 
- Java records in `aperture-core`
- JSON schema in `docs/schemas/`
- Example JSON files in `aperture-data/`

**Example**: Sliding Door Definition
```json
{
  "schemaVersion": 1,
  "id": "aperture:sliding_door",
  "category": "door",
  "parameters": {
    "width": { "type": "length", "default": 1800, "min": 600, "max": 4000 },
    "height": { "type": "length", "default": 2100, "min": 1800, "max": 3000 },
    "panel_count": { "type": "count", "default": 2, "min": 1, "max": 3 },
    "track_type": { "type": "choice", "default": "surface", "options": ["surface", "pocket"] },
    "open_ratio": { "type": "ratio", "default": 0.0, "min": 0.0, "max": 1.0 }
  },
  "constraints": [
    { "expr": "track_type != 'pocket' || host_depth >= 150", 
      "message": "Pocket doors require at least 150mm wall depth" }
  ],
  "generator": "aperture:sliding_door_v1",
  "components": [
    { "kind": "frame", "id": "frame" },
    { "kind": "panel", "id": "panels", "count": "parameter:panel_count", "kinematic": "linear_slide" },
    { "kind": "hardware", "id": "track" }
  ]
}
```

**Acceptance**: Data model is consistent with interfaces, schemas validate.

---

## Step 5: Test Writing

**Goal**: Write tests that verify the contracts, BEFORE implementing.

**Types**:
- **Unit tests**: Test individual classes/methods in isolation
- **Integration tests**: Test subsystems working together
- **Golden tests**: Snapshot-based geometry/output verification
- **Property tests**: Randomized input validation (optional)

**Deliverable**: Test files in `src/test/java/` that initially fail

**Example**: Sliding Door Tests
```java
@Test
void slidingDoor_withTwoPanels_generatesCorrectGeometry() {
    var definition = loadDefinition("aperture:sliding_door");
    var params = ParameterSet.builder()
        .put("width", 1800.0)
        .put("panel_count", 2)
        .build();
    
    var result = generator.generate(definition, params);
    
    assertEquals(2, result.getPanels().size());
    assertEquals(900.0, result.getPanels().get(0).getWidth(), 0.1);
    // Golden snapshot
    assertGeometryMatches(result, "golden/sliding_door_2panel.json");
}

@Test
void slidingDoor_pocketType_withInsufficientDepth_failsValidation() {
    var definition = loadDefinition("aperture:sliding_door");
    var params = ParameterSet.builder()
        .put("track_type", "pocket")
        .build();
    var context = PlacementContext.builder()
        .hostDepth(80.0) // Too shallow
        .build();
    
    var result = validator.validate(definition, params, context);
    
    assertTrue(result.hasErrors());
    assertThat(result.getErrors())
        .anyMatch(err -> err.getMessage().contains("150mm wall depth"));
}
```

**Acceptance**: Tests compile, express the specification clearly, and fail (because implementation doesn't exist yet).

---

## Step 6: Implementation

**Goal**: Write the minimum code to make tests pass.

**Rules**:
- Implement interfaces defined in Step 3
- Follow existing code style and patterns
- No "TODO" comments unless they reference a future Phase
- Keep implementation logic out of models (use services/builders)
- Maintain layer boundaries (Kernel never imports Platform)

**Deliverable**: Implementation files that make all tests pass

**Example**: Sliding Door Generator
```java
public class SlidingDoorGenerator implements OpeningGenerator {
    @Override
    public GeometryResult generate(OpeningTypeDefinition definition, ParameterSet params) {
        var resolved = definition.resolveParameters(params);
        var panelCount = resolved.getInt("panel_count");
        var trackType = resolved.getString("track_type");
        
        var builder = ComponentPlanBuilder.create();
        
        // Frame generation
        builder.addComponent(
            FrameComponent.rectangular(
                resolved.getDouble("width"),
                resolved.getDouble("height"),
                resolved.getDouble("frame_width")
            )
        );
        
        // Panel generation with kinematic motion
        var panelWidth = resolved.getDouble("width") / panelCount;
        for (int i = 0; i < panelCount; i++) {
            var panel = PanelComponent.builder()
                .width(panelWidth)
                .height(resolved.getDouble("height") - 2 * resolved.getDouble("frame_width"))
                .kinematic(LinearSlideMotion.horizontal(panelWidth * i))
                .build();
            builder.addComponent(panel);
        }
        
        // Track hardware
        if (trackType.equals("surface")) {
            builder.addComponent(HardwareComponent.surfaceTrack());
        } else {
            builder.addComponent(HardwareComponent.pocketTrack());
        }
        
        return builder.build();
    }
}
```

**Acceptance**: All tests pass, code review passes, CI passes.

---

## Step 7: Examples

**Goal**: Provide reference usage for developers and users.

**Deliverables**:
- Code example in `examples/` or test fixtures
- Data pack example in `aperture-data/`
- If user-facing: screenshot/video of the feature in action

**Example**: Sliding Door Preset
```json
// aperture-data/aperture/presets/sliding_door_glass_double.json
{
  "name": "Double Glass Sliding Door",
  "type": "aperture:sliding_door",
  "parameters": {
    "width": 1800,
    "height": 2100,
    "panel_count": 2,
    "track_type": "surface",
    "frame_material": "aperture:aluminum_silver",
    "panel_material": "aperture:glass_clear"
  }
}
```

**Acceptance**: Examples work out of the box, demonstrate key features.

---

## Step 8: Documentation

**Goal**: Help users and developers understand and use the feature.

**Types**:
- **Architecture docs**: How it works internally (for contributors)
- **API docs**: Javadoc on public interfaces (for addon developers)
- **User docs**: How to use in-game (for players)

**Deliverables**:
- Update relevant architecture document with implementation notes
- Ensure Javadoc is complete on public APIs
- If user-facing: add to user manual (future)

**Example**: Update Component System Doc
```markdown
## Component System

### Kinematic Panels

As of v0.2.0, panels can be kinematic (motion-enabled).

Supported motion types:
- `LinearSlideMotion`: Horizontal/vertical sliding
- `CircularSwingMotion`: Hinged rotation (doors, casement windows)
- `FoldMotion`: Multi-segment folding (bifold doors)

See `dev.aperture.core.component.KinematicPanel` for the interface contract.

Example: Sliding door with 2 panels (see `aperture:sliding_door` definition).
```

**Acceptance**: Documentation is accurate, up-to-date, and helpful.

---

## Process Checklist

Before marking a feature "complete", verify:

- [ ] Design document exists in `docs/architecture/`
- [ ] Written specification defines behavior
- [ ] Interfaces are defined with Javadoc
- [ ] Data model (records/schemas) is defined
- [ ] Tests exist and pass
- [ ] Implementation follows code style
- [ ] No layer boundary violations
- [ ] Examples/presets work
- [ ] Documentation is updated
- [ ] CI passes (build, tests, dependency check)

---

## Special Cases

### Adding a New Kernel Capability

If your feature requires a new Kernel capability (e.g., swept volumes, kinematic constraints):

1. **Stop** — do not implement the Application-level feature yet
2. Design the Kernel capability following this process
3. Implement and test the Kernel capability in isolation
4. Update Kernel documentation
5. **Then** return to your Application-level feature

**Example Flow**:
1. Want to add sliding doors
2. Realize Kernel lacks kinematic panel support
3. Design `KinematicPanel` interface (Kernel layer)
4. Implement `LinearSlideMotion`, `CircularSwingMotion`
5. Test kinematic system independently
6. Document in `kernel/03-component-system.md`
7. **Now** sliding doors become trivial to implement

### Modifying Existing Code

If modifying an existing feature:

1. Read the relevant architecture document
2. Update the specification if behavior changes
3. Update or add tests to cover new behavior
4. Make the implementation change
5. Update documentation

**Do not skip documentation updates.** Outdated docs are worse than no docs.

### Bug Fixes

Small bug fixes can skip some steps, but:

1. **Always** add a regression test
2. If the bug reveals a design flaw, treat it as a feature change (full process)
3. Update architecture docs if the bug was due to unclear contracts

---

## Example Workflow: Adding Skylight

Let's walk through adding a skylight opening type.

### Step 1: Architecture Design

**Document**: `docs/architecture/applications/skylight-design.md`

Key questions:
- Layer: Applications (it's an opening type)
- Dependencies: Component System (frame, glass), Placement System (roof host detection)
- **Kernel gap identified**: Placement System currently only detects vertical walls, not sloped roofs
- Decision: Design roof host detection first

**Pause here** — go design roof host detection in Platform layer before continuing.

### Step 2: Specification

```markdown
## Skylight Specification

### Purpose
Provide roof-mounted glazed openings for natural light.

### Parameters
- `width`, `height`: opening dimensions
- `pitch`: roof slope angle (degrees, derived from host surface)
- `frame_material`, `glass_material`: material slots
- `operable`: boolean (can it open for ventilation?)

### Placement
- Must be placed on sloped roof surface (pitch > 10°)
- Cannot be placed on walls (validation error)
- Snap to roof grid (rafter spacing if detected)

### Constraints
- `width <= 2000` (larger sizes need mullions)
- `height <= 3000`
- `pitch >= 10 && pitch <= 60` (too flat → drainage issues, too steep → impractical)
```

### Step 3: Interfaces

```java
package dev.aperture.core.placement;

/**
 * Detects and classifies roof surfaces for skylight placement.
 */
public interface RoofHostDetector {
    /**
     * Analyzes blocks in the given region to detect roof surfaces.
     * 
     * @return detected roof plane, or empty if no valid roof surface
     */
    Optional<HostPlane> detectRoofSurface(BlockPos center, int searchRadius);
    
    /**
     * Computes the roof pitch (angle from horizontal).
     * 
     * @return pitch in degrees, 0 = flat, 90 = vertical
     */
    double computePitch(HostPlane roof);
}
```

### Step 4: Data Model

```json
{
  "schemaVersion": 1,
  "id": "aperture:skylight",
  "category": "window",
  "parameters": {
    "width": { "type": "length", "default": 1200, "min": 600, "max": 2000 },
    "height": { "type": "length", "default": 1500, "min": 600, "max": 3000 },
    "operable": { "type": "boolean", "default": false }
  },
  "constraints": [
    { "expr": "host_pitch >= 10 && host_pitch <= 60", 
      "message": "Skylights require roof pitch between 10° and 60°" }
  ],
  "generator": "aperture:skylight_v1",
  "components": [
    { "kind": "frame", "id": "frame", "profile": "aperture:skylight_frame" },
    { "kind": "glass", "id": "glazing", "system": "aperture:double_glazed" },
    { "kind": "hardware", "id": "flashing" }
  ]
}
```

### Step 5: Tests

```java
@Test
void skylight_onSlopedRoof_placesCorrectly() {
    var roof = createSlopedRoof(30); // 30° pitch
    var definition = loadDefinition("aperture:skylight");
    var context = PlacementContext.builder()
        .hostSurface(roof)
        .build();
    
    var result = placementService.validate(definition, ParameterSet.empty(), context);
    
    assertTrue(result.isValid());
}

@Test
void skylight_onVerticalWall_failsValidation() {
    var wall = createVerticalWall();
    var definition = loadDefinition("aperture:skylight");
    var context = PlacementContext.builder()
        .hostSurface(wall)
        .build();
    
    var result = placementService.validate(definition, ParameterSet.empty(), context);
    
    assertTrue(result.hasErrors());
    assertThat(result.getErrors())
        .anyMatch(err -> err.getMessage().contains("roof pitch"));
}
```

### Step 6-8: Implementation, Examples, Documentation

(Follow standard process)

---

## Anti-Patterns to Avoid

### ❌ Code-First Development
**Wrong**:
```java
// I'll just start coding and see what happens
public class AwesomeNewFeature {
    public void doStuff() {
        // TODO: figure out what this should do
    }
}
```

**Right**: Write design doc first.

---

### ❌ Skipping Tests
**Wrong**: "I'll add tests later" → tests never get written

**Right**: Write tests in Step 5, before implementation.

---

### ❌ Bypassing Layers
**Wrong**:
```java
// In aperture-core (Kernel)
import net.minecraft.world.World; // ❌ Kernel importing Minecraft
```

**Right**: Keep Kernel pure. Put Minecraft integration in Platform layer.

---

### ❌ Hardcoding What Should Be Data
**Wrong**:
```java
if (doorType.equals("sliding")) {
    // 50 lines of sliding door logic
} else if (doorType.equals("french")) {
    // 50 lines of french door logic
}
```

**Right**: Make it data-driven. All door types use the same generator, configured by JSON components.

---

## Tools and Automation

### CI Checks (Automated)
- Module dependency validation (no Kernel → Minecraft imports)
- Test coverage (minimum 70% for Kernel, 50% for Platform)
- Code style (Checkstyle / SpotBugs)
- Schema validation (all JSON in `aperture-data/` validates against schemas)

### Documentation Linting (Future)
- Ensure every public interface has Javadoc
- Check for broken links in architecture docs
- Verify example code compiles

---

## Questions?

- **Q**: This seems like a lot of overhead for a small bug fix.  
  **A**: Small bug fixes can skip Steps 1-2. But if you find yourself thinking "this is getting complicated", switch to the full process.

- **Q**: What if I discover during implementation that my design was wrong?  
  **A**: Go back and update the design doc. Then update tests and implementation. The process is iterative, not waterfall.

- **Q**: Do I need design docs for every tiny helper class?  
  **A**: No. The process applies to *features* (user-visible or architecturally significant). Internal helpers can be implemented directly if they don't cross module boundaries.

- **Q**: What if I'm prototyping and don't know if the approach will work?  
  **A**: Create a `prototype/` branch. Prototype freely. Once you've validated the approach, **then** follow the process to implement it properly in main.

---

## Conclusion

This process may feel slow at first, but it pays off:

- **Clarity**: Everyone knows what they're building and why
- **Quality**: Tests and specs catch issues early
- **Maintainability**: Future developers (including future you) understand the code
- **Consistency**: The architecture stays coherent over years

Aperture is not a weekend hobby project. It's designed to last 5-10 years and support a growing ecosystem.

**Architecture first, code second.**
