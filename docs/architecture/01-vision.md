# 01 — Project Vision

## Mission

Aperture is a **parametric architectural opening platform** inside Minecraft. Players and builders design, generate, edit, and place architectural openings — doors, windows, curtain walls, skylights, and façade elements — using a procedural, data-driven system comparable to Revit families, Grasshopper graphs, or SketchUp dynamic components.

## What Aperture Is

| Aperture Is | Aperture Is Not |
|---|---|
| A design system for openings | A furniture/decoration pack |
| Procedural geometry from definitions | Hand-authored block-per-variant content |
| Host-aware (cuts walls, respects structure) | Free-floating decorative blocks |
| Parametric, extensible, data-driven | Hardcoded door/window classes |
| Editor + placement + serialization pipeline | A catalog of static models |

## Core Philosophy

**Everything is an Opening.**

- Door is an Opening.
- Window is an Opening.
- Curtain Wall is an Opening.
- Garage Door is an Opening.
- French Window is an Opening.

Nothing is implemented as an isolated block. Every architectural element is generated from a procedural Opening definition.

## Long-Term North Star

```
Definition  →  Validation  →  Generation  →  Placement  →  Instance  →  Render/Sync
     ↑              ↑              ↑              ↑              ↑
  Catalog      Constraints    Geometry      Host Context    Persistence
```

Future surfaces plug into the same pipeline:

- **Node editor** → edits `OpeningTypeDefinition` graphs
- **AI assistant** → proposes/modifies definitions and parameters
- **BIM export** → serializes instances + host relationships
- **Multiplayer** → syncs instances and collaborative edits

## Design Principles

The project must be:

- **Modular** — strict module boundaries, pure-Java core
- **Data-driven** — opening types defined in JSON/YAML packs
- **Procedural** — geometry generated at runtime from parameters
- **Future AI-compatible** — schema-first, deterministic generation
- **Future node-based** — parameter engine doubles as graph evaluator
- **Future BIM-compatible** — host bindings map to IFC concepts
- **Future parametric** — expressions and constraints from day one
- **Future multiplayer-compatible** — server-authoritative instances

## Architectural Tenets

1. **Opening is the only first-class entity** — no special-case door block logic.
2. **Core is Minecraft-agnostic** — geometry, parameters, definitions live in pure Java modules.
3. **Adapters at the edges** — Minecraft blocks, rendering, networking are infrastructure.
4. **Schemas over code** — new opening types are data + small generators, not new mod forks.
5. **Immutable definitions, mutable instances** — Revit-like family vs. placed element split.
6. **Version everything** — migrations are first-class; long-term survival depends on this.

## Reference Systems

Think like:

- **Autodesk Revit** — families (definitions) vs. instances, host relationships
- **Rhino/Grasshopper** — parametric graphs, procedural geometry
- **Blender** — modifier stack, non-destructive editing
- **Minecraft Fabric** — split client/server, data packs, mixin adapters

Do **not** think like a typical Minecraft decoration mod.
