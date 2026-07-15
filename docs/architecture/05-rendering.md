# 05 — Rendering Architecture

## Problem

A single opening may produce **hundreds of sub-elements** (mullions, gaskets, glass). Per-sub-element block models do not scale.

## Strategy: Generated Composite Rendering

```
OpeningInstance
    → GeometryResult
        → RenderBakeCache (per LOD)
            → BlockEntityRenderer OR baked multiblock model
```

## Render Pipeline

| Stage | Responsibility |
|---|---|
| `GeometryResult` | Logical solids + material slots |
| `MeshCompiler` | Convert to vertex buffers, sectioned by material |
| `RenderBakeService` | Cache keyed by `(typeId, params hash, revision)` |
| `OpeningBlockEntityRenderer` | Submit baked mesh at instance transform |
| `LODSelector` | Full detail < 16m, simplified 16–48m, bounds-only > 48m |
| `TransparencyPass` | Glass in separate render layer |

## Render Layers

```
OpeningRenderLayer
├── OPAQUE_FRAME
├── TRANSLUCENT_GLASS
├── CUTOUT_HARDWARE
└── DEBUG_OVERLAY (placement/editor)
```

## Caching & Invalidation

Invalidate on:

- Parameter change
- Type definition upgrade
- Material resolver change

Background bake on client thread pool; show proxy box until ready. Optional disk cache per instance hash for large curtain walls.

## Placement Preview Rendering (Phase 0)

Implemented in `PlacementPreviewRenderer` using Minecraft 26.1 **Gizmos API** (`Gizmos.cuboid`, `GizmoStyle.stroke`):

| Overlay | Color | Data source |
|---|---|---|
| Host available area | Cyan wireframe | `FabricPlacementTarget.hostBounds()` |
| Opening footprint | Green / red wireframe | `OpeningFootprint` + validation state |

Hook: `LevelRenderEvents.BEFORE_GIZMOS`. See [10-fabric-placement-adapter.md](10-fabric-placement-adapter.md).

Future: ghost mesh, host cut stencil, parameter handles.

## Minecraft Adapter

```
OpeningBlockEntity               // single BE per instance
├── holds OpeningInstance
├── delegates to InstanceManager
└── no per-panel BEs
```

All rendering code lives in `dev.aperture.client.render.*`.
