# Aperture Architecture Index

**Version**: 2.0  
**Last Updated**: 2026-07-16  
**Purpose**: Master index of all architecture documentation

---

## Overview

Aperture's architecture is organized into four layers:

1. **Kernel**: Pure abstractions (geometry, parameters, components, constraints)
2. **Platform**: Runtime system connecting Kernel to Minecraft
3. **Editor**: CAD-quality manipulation and design tools
4. **Applications**: Concrete opening and building types

This index maps all architecture documents to these layers and tracks their completion status.

---

## Document Status Legend

- ✅ **Complete**: Fully written, reflects current implementation
- 🔄 **In Progress**: Partially written or being updated
- 📝 **Draft**: Outline exists, needs detail
- ⏸️ **Planned**: Identified but not yet started
- 🔒 **Frozen**: Deliberately not worked on yet (see roadmap)

---

## Foundation Documents

| Document | Status | Description |
|----------|--------|-------------|
| [APERTURE-REDEFINED.md](APERTURE-REDEFINED.md) | ✅ | Strategic framework: "Aperture is an Architectural Design Kernel" |
| [DEVELOPMENT.md](DEVELOPMENT.md) | ✅ | Standard 8-step development process |
| [01-vision.md](architecture/01-vision.md) | 🔄 | Project vision, mission, design tenets (needs update with new positioning) |
| [03-module-architecture.md](architecture/03-module-architecture.md) | ✅ | Gradle module structure and dependency rules |
| [09-folder-structure.md](architecture/09-folder-structure.md) | ✅ | Repository layout |
| **00-dependency-rules.md** | ⏸️ | Layer boundary enforcement (to be created) |

### Action Items:
- [ ] Update 01-vision.md with "Architectural Design Kernel" positioning
- [ ] Create 00-dependency-rules.md with CI enforcement rules
- [ ] Add "Iron Law" to README.md and CONTRIBUTING.md

---

## Layer 1: Kernel

**Purpose**: Pure abstractions, zero Minecraft dependencies, reusable across contexts.

**Modules**: `aperture-core`, `aperture-geometry`, `aperture-math`

### Core Systems

| Document | Status | Description |
|----------|--------|-------------|
| [02-domain-model.md](architecture/02-domain-model.md) | ✅ | Opening data model (Definition, Instance, Parameters) |
| [04-core-systems.md](architecture/04-core-systems.md) | ✅ | Overview of 9 cooperating systems |
| **kernel/01-geometry-kernel.md** | 📝 | Geometry primitives: Point, Curve, Profile, Mesh, Transform, Extrusion, Boolean |
| **kernel/02-parameter-engine.md** | 📝 | Parameter types, constraint expressions, resolution |
| **kernel/03-component-system.md** | 📝 | Component abstractions: Frame, Panel, Glass, Hardware; ComponentAssembly contract |
| **kernel/04-constraint-solver.md** | ⏸️ | Constraint evaluation, dependency propagation (basic version exists, advanced future) |

### ADRs (Architecture Decision Records)

| Document | Status | Description |
|----------|--------|-------------|
| [ADRs/0001-opening-as-domain-primitive.md](architecture/ADRs/0001-opening-as-domain-primitive.md) | ✅ | Why "Opening" is the only entity |
| [ADRs/0002-pure-java-core.md](architecture/ADRs/0002-pure-java-core.md) | ✅ | Why Kernel must be Minecraft-free |

### Completion Status: **~60%**

**Exists**:
- ✅ Basic geometry types (BoundingBox, Transform)
- ✅ 8 parameter types
- ✅ Constraint expression evaluator
- ✅ Component types (Frame, Glass, Panel, etc.)

**Missing**:
- ❌ Detailed Geometry Kernel documentation
- ❌ Parameter Engine unification across Editor/Preview/Generate
- ❌ Component System contract documentation
- ❌ Profile extrusion implementation (stub exists)
- ❌ Curve primitives (Bezier, Arc)

### Action Items:
- [ ] Write kernel/01-geometry-kernel.md (map existing types + planned extensions)
- [ ] Write kernel/02-parameter-engine.md (document ParametricSchema, resolver, constraints)
- [ ] Write kernel/03-component-system.md (ComponentAssembly contract, generation flow)
- [ ] Unify parameter resolution (all code paths use OpeningParameterResolver)
- [ ] Add CI check: fail if aperture-core or aperture-geometry imports `net.minecraft.*`

---

## Layer 2: Platform

**Purpose**: Runtime system connecting Kernel to Minecraft world, rendering, and persistence.

**Modules**: `aperture-runtime`, `aperture-fabric`, `aperture-opening-geometry`, `aperture-render`

### Core Systems

| Document | Status | Description |
|----------|--------|-------------|
| **platform/01-opening-pipeline.md** | 📝 | Definition → Parameter → Component → Geometry → Mesh → Render data flow |
| [05-rendering.md](architecture/05-rendering.md) | ✅ | Rendering pipeline: delta engine, mesh compiler, LOD, materials |
| [06-placement.md](architecture/06-placement.md) | ✅ | Placement workflow: targeting, preview, validation, commit |
| [07-serialization.md](architecture/07-serialization.md) | ✅ | Persistence, networking, schema migration |
| [10-fabric-placement-adapter.md](architecture/10-fabric-placement-adapter.md) | ✅ | Minecraft raycast → PlacementContext adapter |

### Completion Status: **~70%**

**Exists**:
- ✅ OpeningTypeDefinition loader (JSON data packs)
- ✅ OpeningInstance model
- ✅ Generation pipeline skeleton (OpeningGenerationPipeline)
- ✅ Rendering pipeline (aperture-render module, delta engine, ghost preview)
- ✅ Material catalog system
- ✅ Placement preview system
- ✅ Basic validation chain

**Missing**:
- ❌ Detailed pipeline documentation (Definition → Render flow)
- ❌ NBT persistence (OpeningInstance to Block Entity)
- ❌ Network synchronization (client parameter edits → server authority)
- ❌ Collision and Footprint as formal pipeline outputs
- ❌ Golden tests for pipeline stages

### Action Items:
- [ ] Write platform/01-opening-pipeline.md (document the full chain)
- [ ] Implement NBT persistence (place → save → reload works)
- [ ] Add Collision and Footprint to PipelineResult
- [ ] Create golden tests for fixed_window and door
- [ ] Verify adding new opening types requires zero pipeline code changes

---

## Layer 3: Editor

**Purpose**: CAD-quality in-game manipulation, history, constraints, and inspection.

**Modules**: `aperture-editor`, client-side GUI (in main mod)

### Core Systems

| Document | Status | Description |
|----------|--------|-------------|
| **editor/01-editor-kernel.md** | 📝 | Selection, Manipulator, Command, History, Snap (GUI-independent abstractions) |
| **editor/02-manipulation.md** | ⏸️ | Gizmo system, drag math, constraint projection |
| **editor/03-history.md** | ⏸️ | Undo/redo, edit commands, revision tracking |

### Completion Status: **~20%**

**Exists**:
- ✅ Editor module structure (aperture-editor)
- ✅ Basic selection model
- ✅ Gizmo rendering skeleton
- ✅ History and Command pattern interfaces
- ✅ Snap engine skeleton

**Missing**:
- ❌ Editor Kernel documentation
- ❌ Gizmo → Parameter binding (drag handle → update width)
- ❌ Inspector ↔ Gizmo bidirectional sync
- ❌ Live preview during resize (incremental pipeline invalidation)
- ❌ Undo/Redo implementation
- ❌ Dimension overlays

### Action Items:
- [ ] Write editor/01-editor-kernel.md (Selection, Manipulator, History abstractions)
- [ ] Write editor/02-manipulation.md (Gizmo math, parameter projection)
- [ ] Implement gizmo → parameter → preview → render flow
- [ ] Implement undo/redo for parameter edits
- [ ] Verify: drag handle updates width in < 100ms

---

## Layer 4: Applications

**Purpose**: Concrete opening types (Door, Window, Curtain Wall) and future building types (Roof, Stair, etc.).

**Status**: 🔒 **Frozen** until Kernel/Platform/Editor reach acceptance criteria (see [13-platform-roadmap-af.md](architecture/13-platform-roadmap-af.md))

### Documents

| Document | Status | Description |
|----------|--------|-------------|
| **applications/01-opening-library.md** | ⏸️ | Door, Window, Curtain Wall families |
| **applications/02-building-library.md** | ⏸️ | Roof, Stair, Column, Facade systems |

### Existing Reference Types (For Testing Only)

| Type | Purpose | Status |
|------|---------|--------|
| `aperture:fixed_window` | Reference window, pipeline validation | ✅ Keep |
| `aperture:door` | Reference door, component composition test | ✅ Keep |
| `aperture:curtain_wall` | Multi-panel layout test | ✅ Keep |

**No new opening types until Phase 4** (see Roadmap).

### Action Items:
- [ ] Do NOT create new door/window types yet
- [ ] Do NOT write applications/01-opening-library.md yet
- [ ] Focus on making the platform capable of trivially supporting new types

---

## Roadmaps

| Document | Status | Description |
|----------|--------|-------------|
| [08-expansion-plan.md](architecture/08-expansion-plan.md) | ✅ | Original Phase 0-5 timeline view |
| [12-phase-roadmap.md](architecture/12-phase-roadmap.md) | ✅ | Twelve-phase engineering breakdown |
| [13-platform-roadmap-af.md](architecture/13-platform-roadmap-af.md) | ✅ | **Phase A-F decision framework + Family Library Freeze** |

**Relationship**:
- **08**: Time-horizon view (original)
- **12**: Detailed task breakdown (engineering)
- **13**: Strategic priorities (product decisions)
- **APERTURE-REDEFINED**: Reframes A-F as Kernel/Platform/Editor/Applications

All roadmaps agree: **Platform before Content**.

---

## Schemas

| Schema | Status | Description |
|--------|--------|-------------|
| [opening-type-definition.schema.json](../schemas/opening-type-definition.schema.json) | ✅ | JSON schema for opening type definitions |
| [opening-instance.schema.json](../schemas/opening-instance.schema.json) | ✅ | JSON schema for placed instances |

### Action Items:
- [ ] Verify all JSON in `aperture-data/` validates against schemas
- [ ] Add CI schema validation step

---

## Current Priorities (Next 2 Weeks)

Based on [APERTURE-REDEFINED.md](APERTURE-REDEFINED.md), the immediate priorities are:

### Week 1: Architecture Bible Completion
1. **Update foundation docs**
   - [ ] Update 01-vision.md with new positioning
   - [ ] Create 00-dependency-rules.md
   - [ ] Add Iron Law to README and CONTRIBUTING

2. **Write missing Kernel docs**
   - [ ] kernel/01-geometry-kernel.md
   - [ ] kernel/02-parameter-engine.md
   - [ ] kernel/03-component-system.md

3. **Write missing Platform docs**
   - [ ] platform/01-opening-pipeline.md

4. **Write missing Editor docs**
   - [ ] editor/01-editor-kernel.md

5. **Architecture review**
   - [ ] Verify consistency across all docs
   - [ ] Check for contradictions or missing links

### Week 2: Kernel Completion & Validation
1. **Code**
   - [ ] Unify parameter resolution
   - [ ] Implement NBT persistence
   - [ ] Add Collision/Footprint to pipeline outputs

2. **Tests**
   - [ ] Create golden tests (fixed_window, door)
   - [ ] Write end-to-end integration test

3. **Milestone**
   - [ ] Create docs/milestones/KERNEL-V1.md
   - [ ] Declare Kernel V1 feature-complete

---

## Document Coverage by Layer

| Layer | Documents | Complete | In Progress | Planned |
|-------|-----------|----------|-------------|---------|
| **Foundation** | 6 | 4 | 1 | 1 |
| **Kernel** | 6 | 2 | 1 | 3 |
| **Platform** | 5 | 4 | 0 | 1 |
| **Editor** | 3 | 0 | 0 | 3 |
| **Applications** | 2 | 0 | 0 | 2 |
| **Roadmaps** | 4 | 4 | 0 | 0 |
| **Total** | 26 | 14 (54%) | 2 (8%) | 10 (38%) |

**Target**: 100% Foundation, Kernel, Platform docs complete before Phase 4.

---

## How to Use This Index

1. **Before starting a feature**: Check which layer it belongs to, read the relevant docs
2. **When creating a PR**: Ensure you've updated the relevant architecture doc
3. **When onboarding**: Read Foundation → Kernel → Platform → Editor in order
4. **When unsure**: If a doc is missing or unclear, create an issue or update it

---

## Contributing to Documentation

See [DEVELOPMENT.md](DEVELOPMENT.md) for the process.

Key points:
- Architecture docs are written in Markdown
- Place docs in the appropriate layer directory
- Update this index when adding new docs
- Link to related docs liberally
- Include diagrams (Mermaid preferred)

---

## Questions?

- **Q**: Why are some docs marked "Planned" but the feature exists?  
  **A**: The code exists, but the design documentation is missing or incomplete. This is technical debt we're paying down.

- **Q**: Do I need to read all 26 documents to contribute?  
  **A**: No. Read Foundation docs, then the layer you're working in. If you're adding a new door type (don't!), you'd read Foundation + Kernel + Platform + Applications.

- **Q**: A document says something different from the code. Which is right?  
  **A**: The code is the truth. Update the document and note the discrepancy in your PR.

---

## Last Updated

This index reflects the state as of **2026-07-16**, after the strategic reframing in [APERTURE-REDEFINED.md](APERTURE-REDEFINED.md).

**Next review**: After Week 1 sprint (Kernel docs completion).
