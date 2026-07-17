# Aperture Architecture Index

**Version**: 3.0
**Last Updated**: 2026-07-17
**Purpose**: Master index of all architecture documentation


---

## Overview

Aperture's target architecture is organized into six domain layers:

1. **Foundation**
2. **Design Kernel**
3. **Runtime Kernel**
4. **Simulation Kernel**
5. **Platform Adapters**
6. **Applications**

The older four-layer headings retained later in this index describe current document grouping, not the target domain boundary. [03-module-architecture.md](03-module-architecture.md) is authoritative for dependencies and [14-k2-architectural-runtime-object.md](14-k2-architectural-runtime-object.md) defines the next milestone.

1. **Kernel**: Pure abstractions (geometry, parameters, components, constraints)
2. **Platform**: Platform-neutral object runtime with Minecraft and future host adapters
3. **Editor**: CAD-quality manipulation and design tools
4. **Applications**: Concrete architectural object families and system definitions

This index maps architecture documents to these layers. Document currentness and implementation progress are tracked separately.

---

## Document Status Legend

- ✅ **Current**: Fully written and consistent with the intended architecture; this does not imply implementation completion
- 🔄 **In Progress**: Partially written or being updated
- 📝 **Draft**: Outline exists, needs detail
- ⏸️ **Planned**: Identified but not yet started
- 🔒 **Frozen**: Deliberately not worked on yet (see roadmap)

## Implementation Progress Matrix

Implementation and documentation progress are reported separately. The percentages below are directional planning estimates, not earned milestones and not an aggregate score. A dimension moves only when executable evidence—tests, platform integration, persistence round-trips, or working user flows—changes.

**Assessment date**: 2026-07-17

| Dimension | Estimate | Current evidence | Principal gap |
|---|---:|---|---|
| Architecture specification | 85% | Kernel, generation, runtime, editor, asset, and placement contracts documented | Cross-document cleanup and simulation/collaboration contracts |
| Generation Kernel implementation | 80% | Single kernel, staged geometry/mesh pipeline, structured caching and diagnostics | Golden coverage, broader geometry primitives, incremental graph execution |
| Minecraft placement integration | 40% | Raycast placement, preview, validation, structured host binding | Production lifecycle coverage and host revision invalidation |
| Runtime behavior system | 20% | Runtime environment, state schema, behavior/interaction interfaces, ticks and diagnostics | Rich behaviors, world effects, scheduling semantics, heterogeneous objects |
| Persistence | 35% | Versioned JSON and NBT codecs for Opening state and host binding | End-to-end place/save/reload tests, migrations, command history durability |
| Networking | 5% | Replication boundary and revision concepts | Protocol, server authority, conflict handling, reconnect/resync |
| Editor runtime integration | 30% | Selection, preview, parameter UI, gizmo and command abstractions | Unified command path, parameter-bound gizmos, undo/redo integration |
| Simulation | 2% | Direction and runtime query concepts only | World snapshots, spatial semantics, solver API, first domain solver |
| Collaboration | 2% | Transaction and revision concepts only | Multi-user commands, merge policy, presence, audit/replay |
| Architectural object generalization | 25% | `ArchitecturalObject`, generic runtime state, structured host relationships | Generic definitions and a second non-Opening family |

These dimensions are intentionally non-additive. Documentation coverage tables later in this index measure documents only and must not be read as implementation completion.

---

## Foundation Documents

| Document | Status | Description |
|----------|--------|-------------|
| [01-vision.md](01-vision.md) | ✅ | Project vision, mission, design tenets with new positioning |
| [03-module-architecture.md](03-module-architecture.md) | ✅ | Gradle module structure and dependency rules |
| [09-folder-structure.md](09-folder-structure.md) | ✅ | Repository layout |
| [00-dependency-rules.md](00-dependency-rules.md) | ✅ | Layer boundary enforcement with CI checks |

### Week 4 New Documents:
- [x] Command History design (editor/03-command-history.md) ✅
- [x] Preview Renderer design (editor/02-preview-renderer.md) ✅
- [x] Block Entity Integration (platform/02-block-entity-integration.md) ✅

---

## Layer 1: Kernel

**Purpose**: Pure abstractions, zero Minecraft dependencies, reusable across contexts.

**Modules**: `aperture-core`, `aperture-geometry`, `aperture-math`

### Core Systems

| Document | Status | Description |
|----------|--------|-------------|
| [02-domain-model.md](02-domain-model.md) | ✅ | Opening data model (Definition, Instance, Parameters) |
| [04-core-systems.md](04-core-systems.md) | ✅ | Overview of 9 cooperating systems |
| [kernel/01-geometry-kernel.md](kernel/01-geometry-kernel.md) | ✅ | Geometry primitives: Point, Curve, Profile, Mesh, Transform, Extrusion, Boolean |
| [kernel/02-parameter-engine.md](kernel/02-parameter-engine.md) | ✅ | 8 parameter types, constraint expressions, validation |
| [kernel/03-component-system.md](kernel/03-component-system.md) | ✅ | Component types, ComponentAssembly contract, component lifecycle |
| [kernel/04-generation-pipeline.md](kernel/04-generation-pipeline.md) | ✅ | 8-stage pipeline: Definition → Parameter → Constraint → Component → Geometry → Mesh → Collision → Placement → Render |
| [kernel/05-component-graph.md](kernel/05-component-graph.md) | ✅ | Components as graph nodes with ports, topological evaluation, incremental updates |
| [kernel/06-constraint-solver.md](kernel/06-constraint-solver.md) | ✅ | Expression parser, evaluator, dependency tracking, incremental validation |
| [kernel/07-command-system.md](kernel/07-command-system.md) | ✅ | Command pattern for Undo/Redo, networking, AI integration |
| [kernel/08-asset-system.md](kernel/08-asset-system.md) | ✅ | Asset catalog: profiles, materials, opening types, presets, hot reload |

### ADRs (Architecture Decision Records)

| Document | Status | Description |
|----------|--------|-------------|
| [ADRs/0001-opening-as-domain-primitive.md](ADRs/0001-opening-as-domain-primitive.md) | Superseded | Opening remains the common primitive within the Opening family |
| [ADRs/0002-pure-java-core.md](ADRs/0002-pure-java-core.md) | ✅ | Why Kernel must be Minecraft-free |
| [ADRs/0003-architectural-object-runtime-primitive.md](ADRs/0003-architectural-object-runtime-primitive.md) | ✅ | ArchitecturalObject is the universal runtime primitive |

### Implementation View: **See the capability matrix above**

**Exists**:
- ✅ Basic geometry types (BoundingBox, Transform)
- ✅ 8 parameter types with full specification
- ✅ Constraint expression evaluator with dependency tracking
- ✅ Component types (Frame, Glass, Panel, etc.)
- ✅ Generation Pipeline specification and staged implementation
- ✅ Component Graph specification; partial implementation
- ✅ Command System specification; runtime integration remains incomplete
- ✅ Asset catalog and hot-reload foundations
- ✅ Comprehensive documentation coverage

**Missing**:
- ❌ Detailed Geometry Kernel documentation
- ❌ Parameter Engine unification across Editor/Preview/Generate (implementation)
- ❌ Profile extrusion implementation (stub exists)
- ❌ Curve primitives (Bezier, Arc)

### Action Items:
- [ ] Write kernel/01-geometry-kernel.md (map existing types + planned extensions)
- [x] Write kernel/02-parameter-engine.md ✅
- [x] Write kernel/03-component-system.md ✅
- [x] Write kernel/04-generation-pipeline.md ✅
- [x] Write kernel/05-component-graph.md ✅
- [x] Write kernel/06-constraint-solver.md ✅
- [x] Write kernel/07-command-system.md ✅
- [x] Write kernel/08-asset-system.md ✅
- [ ] Unify parameter resolution (all code paths use OpeningParameterResolver)
- [ ] Add CI check: fail if aperture-core or aperture-geometry imports `net.minecraft.*`

---

## Layer 2: Platform

**Purpose**: Platform-neutral object runtime plus adapters for Minecraft world access, rendering, persistence, and replication.

**Modules**: `aperture-runtime`, `aperture-fabric`, `aperture-opening`, `aperture-render`

### Core Systems

| Document | Status | Description |
|----------|--------|-------------|
| [platform/01-opening-pipeline.md](platform/01-opening-pipeline.md) | ✅ | Definition → Parameter → Component → Geometry → Mesh → Render data flow |
| [platform/03-runtime-pipeline.md](platform/03-runtime-pipeline.md) | In Progress | Capabilities, behavior evaluation, state transitions, effects, persistence and replication |
| [platform/04-runtime-environment.md](platform/04-runtime-environment.md) | In Progress | Object registry, state, behavior, interactions, events, ticks, world queries, replication and diagnostics |
| [05-rendering.md](05-rendering.md) | ✅ | Rendering pipeline: delta engine, mesh compiler, LOD, materials |
| [06-placement.md](06-placement.md) | ✅ | Placement workflow: targeting, preview, validation, commit |
| [07-serialization.md](07-serialization.md) | ✅ | Persistence, networking, schema migration |
| [10-fabric-placement-adapter.md](10-fabric-placement-adapter.md) | ✅ | Minecraft raycast → PlacementContext adapter |

### Implementation View: **See the capability matrix above**

**Implemented foundations**:
- ✅ Opening Definition/Instance generation path through `ApertureKernel`
- ✅ Runtime object registry, schema-backed state, behavior and interaction boundaries
- ✅ Rendering, material, placement preview, validation, collision and placement metadata
- ✅ Structured JSON/NBT codecs for persistent state and host bindings
- ✅ Runtime diagnostics, event, tick, world-query, transaction and replication interfaces

**Major gaps**:
- ❌ End-to-end place → save → reload → resume golden test
- ❌ Server-authoritative network protocol and conflict handling
- ❌ Command unification across Editor, AI, player interaction and networking
- ❌ Dependency/system graphs and revision-driven host invalidation
- ❌ Simulation snapshots, spatial semantics and domain solvers
- ❌ Second non-Opening family proving runtime generalization

### Action Items:
- [x] Implement structured Opening JSON/NBT codecs
- [ ] Prove world lifecycle persistence with an end-to-end reload test
- [ ] Implement authoritative Command/Transaction replication
- [ ] Create generation and runtime golden tests for fixed_window and door
- [ ] Add one non-Opening ArchitecturalObject reference family

---

## Layer 3: Editor

**Purpose**: CAD-quality in-game manipulation, history, constraints, and inspection.

**Modules**: `aperture-editor`, client-side GUI (in main mod)

### Core Systems

| Document | Status | Description |
|----------|--------|-------------|
| [editor/01-editor-kernel.md](editor/01-editor-kernel.md) | ✅ | Selection, Manipulator, Command, History, Snap (GUI-independent abstractions) |
| **editor/02-manipulation.md** | ⏸️ | Gizmo system, drag math, constraint projection |
| **editor/03-history.md** | ⏸️ | Undo/redo, edit commands, revision tracking |

### Implementation View: **See Editor runtime integration in the capability matrix**

**Exists**:
- ✅ Editor module structure (aperture-editor)
- ✅ Basic selection model
- ✅ Gizmo rendering skeleton
- ✅ History and Command pattern interfaces
- ✅ Snap engine skeleton
- ✅ Complete editor kernel documentation

**Missing**:
- ❌ Gizmo → Parameter binding (drag handle → update width)
- ❌ Inspector ↔ Gizmo bidirectional sync
- ❌ Live preview during resize (incremental pipeline invalidation)
- ❌ Undo/Redo implementation
- ❌ Dimension overlays

### Action Items:
- [x] Write editor/01-editor-kernel.md ✅
- [ ] Write editor/02-manipulation.md (Gizmo math, parameter projection)
- [ ] Implement gizmo → parameter → preview → render flow
- [ ] Implement undo/redo for parameter edits
- [ ] Verify: drag handle updates width in < 100ms

---

## Layer 4: Applications

**Purpose**: Concrete opening types (Door, Window, Curtain Wall) and future building types (Roof, Stair, etc.).

**Status**: 🔒 **Frozen** until Kernel/Platform/Editor reach acceptance criteria (see [13-platform-roadmap-af.md](13-platform-roadmap-af.md))

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
| [08-expansion-plan.md](08-expansion-plan.md) | ✅ | Original Phase 0-5 timeline view |
| [12-phase-roadmap.md](12-phase-roadmap.md) | ✅ | Twelve-phase engineering breakdown |
| [13-platform-roadmap-af.md](13-platform-roadmap-af.md) | ✅ | **Phase A-F decision framework + Family Library Freeze** |
| [14-k2-architectural-runtime-object.md](14-k2-architectural-runtime-object.md) | ✅ | K2 Runtime Object and Command/Event/Behavior delivery sequence |

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

## Current Strategic Priorities

1. Complete the Opening runtime path through persistence, replication, and deterministic resume.
2. Make Commands and Transactions the only mutation boundary for Editor, AI, player, automation, and network input.
3. Implement dependency and system graphs with revision-driven invalidation.
4. Add end-to-end golden tests for generation, placement, save/reload, interaction, and replay.
5. Introduce immutable world snapshots and spatial semantics for simulation consumers.
6. Implement a second, non-Opening `ArchitecturalObject` family without Opening compatibility wrappers.
7. Demonstrate one Runtime Definition on a platform-neutral test adapter.

Progress must be updated from executable evidence in the matrix, not from document count or elapsed sprint time.

---

## Document Coverage by Layer

| Layer | Documents | Complete | In Progress | Planned |
|-------|-----------|----------|-------------|---------|
| **Foundation** | 6 | 6 | 0 | 0 |
| **Kernel** | 11 | 10 | 0 | 1 |
| **Platform** | 5 | 5 | 0 | 0 |
| **Editor** | 3 | 1 | 0 | 2 |
| **Applications** | 2 | 0 | 0 | 2 |
| **Roadmaps** | 5 | 5 | 0 | 0 |
| **Total** | 32 | 27 (84%) | 0 (0%) | 5 (16%) |

**Target**: 100% Foundation, Kernel, Platform docs complete before Phase 4.

---

## How to Use This Index

1. **Before starting a feature**: Check which layer it belongs to, read the relevant docs
2. **When creating a PR**: Ensure you've updated the relevant architecture doc
3. **When onboarding**: Read Foundation → Kernel → Platform → Editor in order
4. **When unsure**: If a doc is missing or unclear, create an issue or update it

---

## Contributing to Documentation

See [DEVELOPMENT.md](../DEVELOPMENT.md) for the process.

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

This index reflects the state as of **2026-07-17**, after the Architectural Runtime Kernel reframing.

**Next review**: After the first non-Opening runtime object or authoritative networking milestone.
