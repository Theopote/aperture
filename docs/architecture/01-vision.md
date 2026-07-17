# 01 — Project Vision

## Mission

**Aperture is an Architectural Runtime Kernel with Minecraft as its first platform adapter.**

It provides pure, reusable contracts for defining, generating, placing, operating, editing, persisting, replicating, and simulating architectural objects. Doors and windows are the first complete family, not the boundary of the platform.

Aperture is **not a content mod**. It is a **platform for architectural design and operation**.

## What Aperture Is

| Aperture Is | Aperture Is Not |
|---|---|
| An Architectural Runtime Kernel | A furniture/decoration pack |
| A platform for parametric design and operation | A catalog of static models |
| Procedural geometry from definitions | Hand-authored block-per-variant content |
| Host-aware (cuts walls, respects structure) | Free-floating decorative blocks |
| Stateful, behavioral, extensible, data-driven | Hardcoded door/window classes |
| Definitions + generation + runtime + simulation | A collection of door types |

## Core Identity

**"Aperture is an Architectural Runtime Kernel; Minecraft is one adapter."**

This means:

1. **Kernel-first**: Definition, geometry, state, behavior, commands, and simulation contracts are Minecraft-free and reusable
2. **Platform, not content**: We build shared object lifecycle infrastructure, not a catalog of families
3. **Schema-driven**: Object families declare parameters, state, capabilities, interactions, and migrations
4. **CAD-quality and transactional**: Precise geometry, constraints, commands, undo/redo, revision checks, and deterministic replay
5. **Adapter-based runtime**: The same runtime definition can operate on multiple platforms that provide its required capabilities

**The Iron Law**: "aperture-core, aperture-geometry, and aperture-runtime SHALL NOT import net.minecraft.*"

## Core Philosophy

**Everything placed and operated by Aperture is an Architectural Object.**

`ArchitecturalObject` is the universal runtime primitive. `Opening` is the first implemented architectural family, so doors, windows, curtain walls, garage doors, and French windows continue to share the Opening pipeline. Future structural, circulation, equipment, spatial, and system families must not pretend to be openings.

Nothing is implemented as an isolated Minecraft block. Each architectural family owns its definitions and generation strategy while sharing runtime identity, persistence, synchronization, editing, interaction, and simulation infrastructure.

## Six Domain Layers

| Layer | Domain | Current and planned modules | Responsibility |
|---:|---|---|---|
| 1 | Foundation | `aperture-math`, future `aperture-data`, `aperture-parameter` | Math, IDs, schemas, values, serialization foundations, diagnostics, versions |
| 2 | Design Kernel | `aperture-core`, `aperture-geometry`, `aperture-pipeline`, `aperture-kernel` | Definitions, components, constraints, geometry, mesh, assets, generation |
| 3 | Runtime Kernel | planned `aperture-runtime-model` | Objects, state, capabilities, behavior, events, commands, transactions |
| 4 | Simulation Kernel | future `aperture-simulation` | Snapshots, solver API, time stepping, results, visualization data |
| 5 | Platform Adapters | `aperture-runtime`, `aperture-render`, `aperture-editor`, `aperture-fabric` | World, render, editor, persistence, network and Minecraft integration |
| 6 | Applications | `aperture-opening`, future building/spatial families | Concrete object definitions and family-specific strategies |

The lifecycle boundary is explicit:

```text
ArchitecturalObjectDefinition
        -> instantiate
ArchitecturalObjectInstance
        -> activate in world
RuntimeArchitecturalObject
```

### Historical four-layer grouping

```
┌─────────────────────────────────────────────────────┐
│  Layer 4: APPLICATIONS                              │
│  (Content: Door, Window, Curtain Wall families)     │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  Layer 3: EDITOR                                    │
│  (CAD-quality manipulation: gizmos, undo, snap)     │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  Layer 2: PLATFORM                                  │
│  (Objects, state, behavior, events, commands, sync) │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  Layer 1: KERNEL (Pure Abstractions)                │
│  (Geometry, Parameters, Components, Constraints)    │
│  NO MINECRAFT IMPORTS                               │
└─────────────────────────────────────────────────────┘
```

**Development Priority**: Platform before Content.

We do not need 50 door types. We need one complete Opening reference implementation and a second non-Opening family that proves the runtime abstractions are genuinely general.

## Long-Term North Star

Aperture is organized around four related pipelines with separate responsibilities:

```text
Definition Pipeline
Asset / JSON / Node Graph / AI Output → Validation → Resolution → Compiled Definition

Generation Pipeline
Compiled Definition → Parameters → Constraints → Components → Geometry → Mesh → Placement

Runtime Pipeline
Placed Object → State → Capabilities → Behavior → Interaction → Transition → Effects → Persistence/Replication

Simulation Pipeline
World Snapshot → Spatial Semantics → Domain Solvers → Results → Visualization/Behavior Commands
```

Generation is one processor in the platform, not the complete runtime.

All mutation producers converge on the same command boundary:

- **Editor** → emits validated Commands and Transactions
- **AI assistant** → proposes the same Commands; it does not bypass validation
- **Player interaction** → becomes an Interaction and then a Command or state transition
- **Multiplayer** → replicates authoritative Commands, revisions, and results
- **Simulation** → reads snapshots and emits results or behavior commands
- **External CAD/BIM** → imports and exports definitions, instances, host relations, and spatial semantics

## Design Principles

The project must be:

- **Modular** — strict module boundaries, dependency rules enforced by CI
- **Minecraft-agnostic domain** — Kernel and Runtime contracts are pure Java
- **Schema-driven** — architectural families declare definitions, parameters, state, capabilities, and interactions
- **Procedural** — family-specific geometry is generated at runtime from parameters
- **Immutable definitions** — compiled runtime definitions never change after publication
- **Transactional instances** — object state changes through commands, revisions, and validated transitions
- **Cache-friendly** — pipeline results cached by (type, parameters) tuple
- **Incrementally updateable** — parameter change → partial pipeline recompute
- **Future AI-compatible** — schema-first, deterministic generation
- **Future node-based** — parameter engine doubles as graph evaluator
- **Future BIM-compatible** — host bindings map to IFC concepts
- **Future multiplayer-compatible** — server-authoritative instances

## Architectural Tenets

1. **ArchitecturalObject is the universal runtime primitive** — Opening is the first implemented family; no special-case door block logic and no forcing unrelated families into the Opening model.
2. **Domain logic is Minecraft-agnostic** — definitions, geometry, state, behavior, commands, and simulation contracts live in pure Java modules.
3. **The Iron Law** — aperture-core, aperture-geometry, and aperture-runtime SHALL NOT import net.minecraft.*
4. **Adapters at the edges** — Minecraft blocks, rendering, networking are Platform layer concerns.
5. **Schemas over code** — new family definitions are data plus typed strategies, not platform forks.
6. **Immutable definitions, mutable instances** — Revit-like family vs. placed element split.
7. **Version everything** — migrations are first-class; long-term survival depends on this.
8. **Platform before content** — reusable lifecycle infrastructure is worth more than 50 hardcoded families.
9. **Explicit lifecycle pipelines** — Definition, Generation, Runtime, and Simulation retain separate responsibilities and typed boundaries.
10. **Components are graph nodes** — topological evaluation, incremental updates, dependency tracking.

## Reference Systems

Think like:

- **Autodesk Revit** — families (definitions) vs. instances, host relationships, parameter-driven geometry
- **Rhino/Grasshopper** — parametric graphs, procedural geometry, data flow paradigm
- **Blender Geometry Nodes** — node-based procedural modeling, non-destructive workflow
- **SketchUp Dynamic Components** — parametric definitions with user-editable parameters
- **Unreal Engine Blueprint** — visual scripting, graph-based execution

Do **not** think like:

- A typical Minecraft decoration mod (hardcoded blocks)
- A furniture catalog (static models)
- A texture pack (visual-only changes)

## Platform Success Criteria

Aperture succeeds as an Architectural Runtime Kernel when:

1. **Unified object model** — any supported `ArchitecturalObject` can be created through a Definition/Instance contract without pretending to be an Opening.
2. **Declarative runtime semantics** — definitions can declare parameters, persistent and transient state, capabilities, behaviors, interactions, host requirements, and migrations.
3. **Heterogeneous graphs** — dependency and system graphs connect openings, structure, equipment, spatial objects, and building systems.
4. **Event-driven operation** — world, time, weather, player, sensor, and object events can trigger deterministic behavior evaluation and state transitions.
5. **One mutation boundary** — Editor actions, AI proposals, player interactions, automation, and simulation effects become the same validated Commands and Transactions.
6. **Authoritative collaboration** — concurrent multiplayer edits use revisions, conflict policy, server authority, replication, and replayable command history.
7. **Simulation access** — solvers can read immutable world snapshots, object capabilities, host relations, rooms, zones, routes, and system topology.
8. **Portable runtime definitions** — a definition can run through different platform adapters when they provide the required world-query and effect capabilities.
9. **Minecraft-free core** — Kernel and Runtime domain logic do not import Minecraft APIs; platform dependencies remain at adapter edges.
10. **Versioned durability** — definitions, instances, state, host dependencies, commands, and simulation inputs have explicit schemas and migrations.
11. **Observable performance** — generation, behavior evaluation, transactions, persistence, and simulation publish measurable budgets and diagnostics.
12. **Generalization proof** — Opening is the first complete Runtime Object case, and at least one non-Opening family works without an Opening-shaped compatibility layer.

## Opening Reference Acceptance

Opening remains the first end-to-end proof, not the platform's final success definition:

- A new Opening type requires no generation-pipeline code changes.
- Door and window definitions declare parameters, state, capabilities, behavior, and interactions.
- Placement preserves structured host identity, feature, local frame, attachment policy, and dependency revision.
- JSON/NBT persistence round-trips persistent state and host bindings; transient state is reconstructed.
- Real-time editing meets measured preview and cached-generation budgets.

## Current Strategic Milestones

1. Complete the Opening runtime path from interaction through persistence and replication.
2. Make Commands and Transactions the only mutation boundary for Editor, AI, player, and network operations.
3. Add dependency and system graphs with revision-driven invalidation.
4. Prove portability with a platform-neutral runtime test adapter.
5. Implement one non-Opening family to validate `ArchitecturalObject` generalization.
6. Establish immutable world snapshots and the first simulation-domain contract.

---

**Document Status**: ✅ Updated with new positioning  
**Last Updated**: 2026-07-17
**Next Review**: After the first non-Opening runtime object milestone
