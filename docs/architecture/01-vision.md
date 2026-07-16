# 01 — Project Vision

## Mission

**Aperture is an Architectural Design Kernel running inside Minecraft.**

It provides a pure, reusable foundation for generating architectural openings — doors, windows, curtain walls, skylights, and façade elements — using a procedural, parametric, data-driven system comparable to Autodesk Revit families, Rhino Grasshopper, or SketchUp dynamic components.

Aperture is **not a content mod**. It is a **platform for architectural generation**.

## What Aperture Is

| Aperture Is | Aperture Is Not |
|---|---|
| An Architectural Design Kernel | A furniture/decoration pack |
| A platform for parametric generation | A catalog of static models |
| Procedural geometry from definitions | Hand-authored block-per-variant content |
| Host-aware (cuts walls, respects structure) | Free-floating decorative blocks |
| Parametric, extensible, data-driven | Hardcoded door/window classes |
| Editor + pipeline + runtime | A collection of door types |

## Core Identity

**"Aperture is an Architectural Design Kernel running inside Minecraft."**

This means:

1. **Kernel-first**: The core abstractions (geometry, parameters, components, pipeline) are Minecraft-free and reusable
2. **Platform, not content**: We build the generation system, not the 50 door types
3. **Data-driven**: New opening types are JSON + small generators, not code forks
4. **CAD-quality**: Precise geometry, constraints, undo/redo, dimension feedback
5. **Runtime generation**: Everything computed from parameters, nothing hardcoded

**The Iron Law**: "Aperture-core and aperture-geometry SHALL NOT import net.minecraft.*"

## Core Philosophy

**Everything is an Opening.**

- Door is an Opening.
- Window is an Opening.
- Curtain Wall is an Opening.
- Garage Door is an Opening.
- French Window is an Opening.

Nothing is implemented as an isolated block. Every architectural element is generated from a procedural Opening definition through the Generation Pipeline.

## Four-Layer Architecture

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
│  (Runtime: pipeline, rendering, placement, NBT)     │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  Layer 1: KERNEL (Pure Abstractions)                │
│  (Geometry, Parameters, Components, Constraints)    │
│  NO MINECRAFT IMPORTS                               │
└─────────────────────────────────────────────────────┘
```

**Development Priority**: Platform before Content.

We don't need 50 door types. We need a platform so robust that adding the 51st door type requires zero pipeline code changes — just JSON + assets.

## Long-Term North Star

```
Definition  →  Parameter  →  Constraint  →  Component  →  Geometry  →  Mesh  →  Blocks  →  Render
     ↑              ↑              ↑              ↑              ↑         ↑         ↑
  Catalog      Resolution    Validation       Graph        Kernel    Voxelize  Platform
```

The Generation Pipeline is Aperture's CPU.

Future extensions plug into the same pipeline:

- **Node editor** → edits `OpeningTypeDefinition` graphs
- **AI assistant** → proposes/modifies definitions and parameters
- **BIM export** → serializes instances + host relationships to IFC
- **Multiplayer** → syncs instances and collaborative edits
- **External CAD** → import/export geometry through the kernel

## Design Principles

The project must be:

- **Modular** — strict module boundaries, dependency rules enforced by CI
- **Minecraft-agnostic core** — geometry, parameters, components are pure Java
- **Data-driven** — opening types defined in JSON data packs
- **Procedural** — geometry generated at runtime from parameters
- **Immutable definitions** — OpeningTypeDefinition never changes after load
- **Mutable instances** — OpeningInstance parameters can be edited with undo/redo
- **Cache-friendly** — pipeline results cached by (type, parameters) tuple
- **Incrementally updateable** — parameter change → partial pipeline recompute
- **Future AI-compatible** — schema-first, deterministic generation
- **Future node-based** — parameter engine doubles as graph evaluator
- **Future BIM-compatible** — host bindings map to IFC concepts
- **Future multiplayer-compatible** — server-authoritative instances

## Architectural Tenets

1. **Opening is the only first-class entity** — no special-case door block logic.
2. **Core is Minecraft-agnostic** — geometry, parameters, definitions live in pure Java modules (aperture-core, aperture-geometry, aperture-math).
3. **The Iron Law** — aperture-core and aperture-geometry SHALL NOT import net.minecraft.*
4. **Adapters at the edges** — Minecraft blocks, rendering, networking are Platform layer concerns.
5. **Schemas over code** — new opening types are data + small generators, not new mod forks.
6. **Immutable definitions, mutable instances** — Revit-like family vs. placed element split.
7. **Version everything** — migrations are first-class; long-term survival depends on this.
8. **Platform before content** — A robust pipeline is worth more than 50 hardcoded door types.
9. **Data flows through the pipeline** — Definition → Parameters → Components → Geometry → Mesh → Blocks → Render.
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

## Success Criteria

Aperture succeeds when:

1. **Adding a new opening type requires zero pipeline code changes** — only JSON + assets
2. **Users can edit parameters in real-time** — drag handle, see preview, commit
3. **The kernel is reusable** — someone ports Aperture abstractions to another voxel game with minimal changes
4. **AI can generate openings** — LLM outputs valid OpeningTypeDefinition JSON that works first try
5. **Performance is acceptable** — < 150ms cold pipeline, < 5ms cached, < 100ms parameter edit
6. **Content creators thrive** — data pack authors create custom opening families without touching Java

## What We're Building (Week 1-4)

**Week 1**: Architecture Bible completion
- ✅ All core documentation (Kernel, Platform, Editor)
- 77% → 100% Foundation/Kernel/Platform docs

**Week 2**: Kernel V1 validation
- NBT persistence (place → save → reload)
- Golden tests (fixed_window, door)
- Profile extrusion working
- Performance: < 150ms cold pipeline

**Week 3**: First geometry through pipeline
- Frame geometry generator (L-profile extrusion)
- Glass geometry generator (planar surface)
- Full fixed_window test case

**Week 4**: First door through pipeline
- Door frame + panel + hardware
- Opening/closing state
- Full single_door test case

**Beyond**: Platform refinement → Editor implementation → Content library

---

**Document Status**: ✅ Updated with new positioning  
**Last Updated**: 2026-07-16  
**Next Review**: After Kernel V1 milestone
