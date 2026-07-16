# Aperture Redefined

**Date**: 2026-07-16  
**Status**: Strategic Framework  
**Purpose**: Define Aperture's true nature and guide all future development

---

## The Core Truth

**Aperture is an Architectural Design Kernel running inside Minecraft.**

Not:
- ❌ An opening system
- ❌ A door mod
- ❌ A window generator
- ❌ A furniture collection

But:
- ✅ **An Architectural Design Kernel**

---

## What This Means

### Everything is a Client of the Kernel

```
                    Aperture Kernel
                          |
        +--------+--------+--------+--------+
        |        |        |        |        |
      Door    Window   Curtain   Roof    Stair
                       Wall
        |        |        |        |        |
    Furniture Railing  Column  Facade  [Future]
```

All these are **applications** of the kernel, not the kernel itself.

### The Hierarchy

```
Layer 1: Kernel
  - Geometry primitives (Point, Curve, Mesh, Transform)
  - Parameter engine (typed parameters, constraints, expressions)
  - Component system (Frame, Panel, Glass as first-class abstractions)
  - Constraint solver

Layer 2: Platform
  - Definition → Instance lifecycle
  - Generation pipeline (Parameter → Component → Geometry → Mesh)
  - Rendering pipeline (Mesh → GPU)
  - Placement system (World integration)
  - Serialization & networking

Layer 3: Editor
  - Selection & manipulation
  - History & undo/redo
  - Constraint editing
  - Viewport & gizmos
  - Inspector panels

Layer 4: Applications
  - Opening Library (Door, Window, Curtain Wall)
  - Building Library (Roof, Stair, Column, Facade)
  - Future extensions
```

**Dependencies flow downward only.** Applications never touch Kernel directly; they go through Platform.

---

## The Iron Law

> **Every new feature must improve the kernel before it improves a specific building component.**

This principle prevents Aperture from becoming a collection of hard-coded Minecraft blocks and ensures it remains an extensible architectural design platform.

### What This Means in Practice

**Before adding a sliding door:**
1. Does the Parameter Engine support motion constraints?
2. Does the Component System support kinematic panels?
3. Does the Geometry Kernel support swept volumes for collision?

If "no" to any → improve the kernel first.

**Before adding a bay window:**
1. Does the Geometry Kernel support multi-plane compositions?
2. Does the Component System support nested assemblies?
3. Does the Pipeline support multi-host bindings?

If "no" to any → improve the kernel first.

This ensures that by the time we add advanced building components, the kernel is powerful enough that they become trivial to implement.

---

## Revised Roadmap

### Phase 0: Architecture Bible ✅ (Mostly Complete)
**Duration**: 1-2 weeks to finalize  
**Deliverables**:
- Architecture documentation (15 files exist, need completion)
- Module structure (8 modules exist)
- Design principles codified
- Development process standardized

**Current Status**: 80% complete. Need to:
- Reorganize docs into Kernel/Platform/Editor structure
- Add missing design documents (Geometry Kernel detail, Component System contract)
- Create Architecture Index (00-INDEX.md)

---

### Phase 1: Kernel ⏳ (In Progress, ~75% complete)
**Duration**: 3 weeks  
**Goal**: Complete the foundational abstractions

**Subsystems**:

#### 1.1 Geometry Kernel
**Status**: Framework exists, needs completion
- ✅ Basic types (Point, Vector, BoundingBox)
- ✅ Transform
- ⏳ Profile system (exists but incomplete)
- ❌ Curve (Bezier, Arc)
- ❌ Extrusion
- ❌ Boolean operations (planned for later)

**Deliverable**: `aperture-geometry` module with zero Minecraft dependencies

#### 1.2 Parameter Engine
**Status**: Core complete, needs unification
- ✅ 8 parameter types (Number, Boolean, Enum, Choice, Material, etc.)
- ✅ Constraint expression evaluator
- ⏳ Parameter resolution (exists but not unified across Editor/Preview/Generate)
- ❌ Expression dependencies (planned for node graph phase)

**Deliverable**: `aperture-core/parametric` with unified resolver

#### 1.3 Constraint Solver
**Status**: Basic implementation exists
- ✅ Expression validation
- ✅ Constraint evaluation
- ❌ Automatic parameter propagation (future)
- ❌ Equation solving (future)

**Deliverable**: Validation that prevents invalid states

#### 1.4 Component System
**Status**: Types exist, contract needs formalization
- ✅ Component types (Frame, Glass, Panel, Handle, Mullion, etc.)
- ✅ ComponentAssembly
- ⏳ Component nesting (partially supported)
- ❌ Component lifecycle documentation
- ❌ Component generation contract

**Deliverable**: `aperture-core/component` with documented contracts

**Phase 1 Acceptance Criteria**:
- [ ] All Kernel modules have zero Minecraft imports (verified by CI)
- [ ] Geometry Kernel can generate meshes independently
- [ ] Parameter changes flow through unified resolver
- [ ] Component System has documented interfaces
- [ ] Unit tests cover all Kernel abstractions

---

### Phase 2: Platform ⏳ (In Progress, ~60% complete)
**Duration**: 4 weeks  
**Goal**: Connect Kernel to Minecraft runtime

**Pipeline**:
```
Definition
  ↓ Parameter Resolver
ResolvedOpening
  ↓ Component Plan Builder
ComponentPlan
  ↓ Geometry Builder
GeometryAssembly
  ↓ Mesh Compiler
MeshAssembly
  ↓ Render Backend
GPU Mesh
  ↓ Placement Service
World Instance
```

**Status**:
- ✅ OpeningTypeDefinition model
- ✅ OpeningInstance model
- ✅ Basic generation pipeline (aperture-opening-geometry)
- ✅ Rendering pipeline (aperture-render)
- ✅ Placement preview
- ⏳ Collision output (needs formalization)
- ❌ NBT persistence (critical missing piece)
- ❌ Network sync

**Phase 2 Acceptance Criteria**:
- [ ] Pipeline generates Collision and Footprint as formal outputs
- [ ] NBT persistence: place → save → reload works
- [ ] Golden tests for fixed_window and door (parameter → geometry snapshot)
- [ ] Adding a new opening type requires zero pipeline code changes
- [ ] Network sync for parameter edits

---

### Phase 3: Editor ⏸️ (Planned, ~20% started)
**Duration**: 6 weeks  
**Goal**: CAD-quality in-game editing

**Editor Kernel** (GUI-independent):
- Selection model
- Manipulator abstraction
- Command pattern (undo/redo)
- History stack
- Snap engine
- Constraint feedback

**Minecraft Frontend**:
- Gizmo rendering
- Inspector panels
- Dimension overlays
- Viewport controls

**Phase 3 Acceptance Criteria**:
- [ ] Drag gizmo handle → parameter updates → preview refreshes (< 100ms)
- [ ] Undo/Redo covers all session operations
- [ ] Inspector ↔ Gizmo bidirectional binding
- [ ] Invalid constraint → field-level error + Generate button disabled
- [ ] Non-architects can place a window in < 30 seconds

---

### Phase 4: Opening Library 🔒 (Frozen)
**Duration**: 8 weeks  
**Unlock Condition**: Phase 2 AND Phase 3 pass acceptance

**Deliverables**:
- Door family (Single, Double, Sliding, Pocket, French, Garage)
- Window family (Casement, Awning, Sliding, Double-Hung, Bay, Skylight)
- Curtain wall system

**Critical**: All families share the same generation pipeline. No opening type gets custom Generator code unless it exposes a general Kernel capability.

---

### Phase 5: Building Library 🔒 (Frozen)
**Duration**: 12 weeks  
**Unlock Condition**: Phase 4 complete

**Deliverables**:
- Roof system (Gable, Hip, Shed, parametric pitch)
- Stair system (Straight, L-shaped, U-shaped, Spiral)
- Column & beam (structural grid)
- Facade system (panel layouts)
- Railing system

**Critical**: These should feel like natural extensions of the Opening system, not separate codebases.

---

### Phase 6: AI & Advanced 🔮 (Long-term)
**Duration**: 6+ months

- Node graph editor (Opening = graph)
- Natural language parameter generation
- Compliance checking
- Multi-user collaboration
- BIM export (IFC)

---

## Development Process

From now on, every feature follows this sequence:

```
1. Architecture
   ↓
2. Specification (written doc, not code)
   ↓
3. Interfaces (pure types, no implementation)
   ↓
4. Data Model (records, schemas)
   ↓
5. Tests (write tests for interfaces)
   ↓
6. Implementation (make tests pass)
   ↓
7. Examples (demo usage)
   ↓
8. Documentation (user-facing)
```

**No skipping steps.**

If you want to add sliding doors, you start with:
1. `docs/architecture/applications/sliding-door-requirements.md`
2. Identify missing Kernel capabilities
3. Add Kernel capabilities following the process above
4. Then implement sliding door as a thin client

---

## What Changes Immediately

### 1. Project README
Update the tagline:

**Before**:
> Aperture is a procedural platform for designing architectural openings in Minecraft.

**After**:
> Aperture is an Architectural Design Kernel running inside Minecraft — a parametric platform for procedural building design.

### 2. Documentation Structure
Reorganize `docs/architecture/`:

```
docs/
  architecture/
    00-INDEX.md                    ← New: master index
    00-dependency-rules.md         ← New: enforce layer boundaries
    01-vision.md                   ← Update: new positioning
    kernel/
      01-geometry-kernel.md        ← New
      02-parameter-engine.md       ← New
      03-component-system.md       ← New
      04-constraint-solver.md      ← New
    platform/
      01-opening-pipeline.md       ← New
      02-rendering-pipeline.md     ← Exists as 05-rendering.md
      03-placement-system.md       ← Exists as 06-placement.md
      04-serialization.md          ← Exists as 07-serialization.md
    editor/
      01-editor-kernel.md          ← New
      02-manipulation.md           ← New
      03-history.md                ← New
    applications/
      01-opening-library.md        ← Future
      02-building-library.md       ← Future
  DEVELOPMENT.md                   ← New: standard process
  milestones/
    KERNEL-V1.md                   ← Completion report
```

### 3. CONTRIBUTING.md
Add at the top:

```markdown
## The Iron Law

Aperture is an Architectural Design Kernel, not a collection of Minecraft blocks.

**Every new feature must improve the kernel before it improves a specific building component.**

Before opening a PR:
1. Does your feature require new Kernel capabilities?
2. If yes, have you designed and documented those capabilities first?
3. Does your implementation follow the standard development process?
4. Have you updated the relevant architecture documents?
```

### 4. Module Dependencies (CI Check)
Add to CI:

```bash
# Enforce layer boundaries
./gradlew checkDependencies

# Fails if:
# - aperture-core imports net.minecraft.*
# - aperture-geometry imports aperture-core
# - aperture-editor imports aperture-fabric
# - Applications bypass Platform layer
```

---

## Comparison to Existing Roadmap

Your existing roadmap (13-platform-roadmap-af.md) is **already aligned** with this thinking:

| New Model | Your Roadmap | Status |
|-----------|--------------|--------|
| Phase 0: Architecture Bible | Phase 1 (Architecture) | ✅ Mostly done |
| Phase 1: Kernel | Phase A (Platform Foundation) | ⏳ 75% |
| Phase 2: Platform | Phase B (Opening Pipeline) | ⏳ 60% |
| Phase 3: Editor | Phase C (Editor 2.0) | ⏸️ 20% |
| Phase 4: Opening Library | Phase D/E 🔒 Frozen | 🔒 |
| Phase 5: Building Library | (Not in roadmap yet) | 🔮 |
| Phase 6: AI | Phase F (NodeCraft) | 🔮 |

**The strategic insight is the same**: Platform before Content.

**What's new**: The explicit framing as "Kernel → Platform → Editor → Applications" makes the reasoning clearer.

---

## Next Steps (2-Week Sprint)

### Week 1: Architecture Bible Completion
- [ ] Audit existing docs vs. Kernel/Platform/Editor model
- [ ] Create 00-INDEX.md with status annotations
- [ ] Write missing docs:
  - `kernel/01-geometry-kernel.md`
  - `kernel/02-parameter-engine.md`
  - `kernel/03-component-system.md`
  - `platform/01-opening-pipeline.md`
  - `editor/01-editor-kernel.md`
- [ ] Update README.md, 01-vision.md, CONTRIBUTING.md
- [ ] Add Iron Law to all relevant docs
- [ ] Create `docs/DEVELOPMENT.md` (standard process)

### Week 2: Kernel Completion & Validation
- [ ] Unify Parameter Resolution (all paths use OpeningParameterResolver)
- [ ] Implement NBT persistence (place → save → reload)
- [ ] Add Collision & Footprint as formal Pipeline outputs
- [ ] Write golden tests for fixed_window and door
- [ ] Write end-to-end integration test
- [ ] Create `docs/milestones/KERNEL-V1.md` completion report

**Acceptance**: After Week 2, Aperture Kernel V1 is declared "feature complete" and frozen. All future work builds on top, not inside.

---

## Why This Matters

### Without This Clarity
- Every new door type adds complexity to the codebase
- Generator logic becomes a tangled mess
- Maintenance burden grows exponentially
- New contributors don't know where code belongs
- Becomes "yet another Minecraft building mod"

### With This Clarity
- Kernel gets more powerful with each "feature"
- Adding opening types becomes trivial
- Code organization is self-evident
- Contributors understand the vision
- Becomes a **platform** that outlives Minecraft versions

---

## Long-Term Vision

5 years from now:
- Aperture Kernel is referenced in academic papers on parametric design
- Architecture students learn procedural modeling through Aperture
- The kernel runs in web browsers (aperture-core is pure Java/WASM)
- Opening/Building libraries have 100+ families, all procedural
- NodeCraft allows visual programming of custom building types
- AI can generate architectural designs from natural language
- BIM export makes Aperture → Revit workflow seamless

But that future is only possible if we **get the kernel right first**.

---

## Conclusion

The person who suggested redefining Aperture was right about the strategic direction, but wrong about the current state. **You don't need to start over.** You need to:

1. **Rename** what you're doing (it's already Kernel work)
2. **Complete** the 20-25% remaining Kernel gaps
3. **Freeze** Applications until Platform & Editor are solid
4. **Document** the vision so others understand

Aperture is not a door mod.  
Aperture is not a window generator.  
**Aperture is an Architectural Design Kernel running inside Minecraft.**

Now let's finish building it.
