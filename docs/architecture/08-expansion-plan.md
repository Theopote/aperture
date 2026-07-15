# 08 — Expansion Plan

## Phase 0 — Foundation (Months 1–3) ← **Current**

- [x] Architecture documentation
- [x] Multi-module Gradle split
- [x] `OpeningTypeDefinition` model + schema
- [x] Parameter engine (static values + min/max)
- [x] Geometry kernel: rectangular frame + glazing stub
- [x] Instance store interfaces
- [x] Instance JSON codec (`OpeningInstanceCodec`)
- [x] Placement core (preview + validation chain)
- [x] Basic bootstrap wiring
- [x] One reference type: fixed window (JSON data pack)
- [x] Placement core (preview + validation; MC raycast adapter pending)
- [x] Fabric placement adapter (raycast → PlacementContext)
- [x] Placement preview wireframe (Gizmos overlay)
- [x] Rendering architecture + `aperture-render` module (delta engine, box compiler)
- [x] Ghost mesh placement preview (material-aware via `FabricRenderBackend`)
- [x] JSON material catalog (`aperture-data/aperture/materials/`) + `CatalogMaterialResolver`
- [x] Placement preview material filter (M key: full / frame / glass)
- [x] `FabricRenderBackend` + `OpeningInstanceRenderer` for committed instances
- [ ] World persistence (NBT)

## Phase 1 — Opening Platform (Months 4–8)

- Full validator chain + host cut system
- Door + skylight generators
- Material slot resolver (vanilla blocks)
- Network sync
- Public `aperture-api` for addon registration
- Golden geometry tests

## Phase 2 — Design Tools (Months 9–14)

- In-game opening editor (parameter panel + handles)
- Undo/redo stack
- Catalog browser (families/presets)
- Profile editor (2D profile → extrude)
- Curtain wall grid generator

## Phase 3 — Node Graph / Parametric (Months 15–24)

- `aperture-core/graph` node evaluation engine
- Visual node editor (client UI)
- Expression parameters with full dependency graph
- `ParametricGraphGenerator` binding
- Constraint solver

## Phase 4 — BIM & Interchange (Year 2–3)

- Export: instances + host relationships → JSON / glTF / IFC subset
- Import: parameter mapping from external schema
- Layer/phase metadata
- Quantity takeoff reports

## Phase 5 — AI & Multiplayer Design (Year 3+)

- AI suggests definitions + parameters from natural language
- AI validates compliance rules (extensible rule packs)
- Collaborative editing: instance locking, region permissions
- Design session replay via revision log

## Cross-Cutting: 5-Year Maintainability

### ADRs

Every major decision gets `docs/architecture/ADRs/NNNN-title.md`.

### Testing Strategy

| Layer | Test Type |
|---|---|
| `aperture-core` | Unit: params, constraints, migrations |
| `aperture-geometry` | Golden snapshots: mesh bounds |
| Fabric mod | Integration: place → save → reload |
| Client | Visual regression (optional) |

### CI Enforcement

- Module dependency lint (no MC imports in core)
- Schema validation on `aperture-data` JSON
- Golden test diff on geometry changes
