# 02 — Data Model

## Entity Relationship

```
┌─────────────────────────────────────────────────────────────┐
│                    OpeningTypeDefinition                     │
│  (immutable, data-driven, versioned)                        │
└──────────────────────────┬──────────────────────────────────┘
                           │ instantiates
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                      OpeningInstance                         │
│  (mutable, world-persisted, sync'd)                         │
└──────────┬───────────────────────────────┬──────────────────┘
           │                               │
           ▼                               ▼
┌─────────────────────┐         ┌─────────────────────┐
│     HostBinding      │         │   ParameterSet       │
│  (world attachment)  │         │  (override values)   │
└─────────────────────┘         └──────────┬──────────┘
                                           │ feeds
                                           ▼
                                ┌─────────────────────┐
                                │   GeometryResult     │
                                │  (generated output)  │
                                └─────────────────────┘
```

## Core Entities

### ArchitecturalObject

The family-neutral runtime contract for anything placed and operated by Aperture. It exposes only stable cross-family concerns: schema version, instance identity, sparse parameters, transform, and revision. Concrete families retain typed definition IDs, hosting rules, operational state, and generation outputs.

```text
ArchitecturalObject
├── OpeningInstance               (implemented)
├── StructuralObjectInstance      (future)
├── CirculationObjectInstance     (future)
├── EquipmentObjectInstance       (future)
├── SpatialObjectInstance         (future)
└── SystemObjectInstance          (future)
```

This is a runtime polymorphism boundary, not a single universal geometry pipeline. Opening generation remains typed around `OpeningTypeDefinition`, `OpeningRequest`, and `OpeningResult`.

### OpeningTypeDefinition

Describes *what* an opening type is — the procedural recipe (Revit "family").

| Field | Type | Description |
|---|---|---|
| `schemaVersion` | int | Migration version |
| `id` | OpeningId | `aperture:fixed_window` |
| `category` | OpeningCategory | door, window, curtain_wall, skylight, facade |
| `parameters` | Map<String, ParameterDefinition> | Parametric schema |
| `constraints` | List<ConstraintRule> | Validation expressions |
| `generator` | GeneratorId | Procedural generator binding |
| `components` | ComponentGraph | Logical assembly tree |
| `materialSlots` | List<String> | Resolvable material slots |

### OpeningInstance

A placed opening in the world (Revit "instance").

| Field | Type | Description |
|---|---|---|
| `instanceId` | UUID | Unique world identity |
| `typeId` | OpeningId | References definition |
| `parameters` | ParameterSet | Override values |
| `transform` | Transform3d | Position, facing, local frame |
| `host` | HostBinding | Wall/roof attachment |
| `state` | OpeningState | Operational state (open ratio, etc.) |
| `revision` | long | Sync conflict resolution |

## Value Objects

| Type | Purpose |
|---|---|
| `OpeningId` | Namespaced identifier (`aperture:fixed_window`) |
| `ParameterValue` | Typed parameter value (length, angle, count, enum, bool) |
| `Transform3d` | Origin + basis vectors (right-handed, Y-up) |
| `CutSpec` | Host boolean cut volume |
| `MaterialSlot` | Named slot → resolved material id |
| `ComponentRef` | Path in assembly tree (`frame.left_jamb`) |
| `Revision` | Monotonic counter for network sync |

## Object Hierarchy

### Principle: Composition over inheritance

Avoid `class OakDoorBlock extends DoorBlock`. Use one Opening pipeline with category metadata and generator selection.

```
OpeningCategory (enum)
├── DOOR
├── WINDOW
├── CURTAIN_WALL
├── SKYLIGHT
└── FACADE_ELEMENT

OpeningTypeDefinition     // data, not deep inheritance
ArchitecturalObject       // all architectural families share the runtime contract
OpeningInstance           // all Opening categories share this concrete type

Generator (interface)
├── RectangularFrameGenerator
├── CurtainWallGridGenerator
├── SkylightRoofCutGenerator
└── ParametricGraphGenerator   // future

ComponentNode (assembly tree)
├── FrameComponent
├── SashComponent
├── GlazingComponent
├── MullionComponent
├── HardwareComponent
└── PanelComponent
```

### Category Behavior (strategy, not subclass)

```java
interface OpeningBehavior {
    PlacementRules placementRules();
    StateMachine  stateMachine();
    HostRequirements hostRequirements();
}
```

| Category | Behavioral differences |
|---|---|
| Door | Swing/slide state machine, collision when open |
| Window | Sash operation, glazing layers |
| Curtain Wall | Multi-panel grid, spans multiple blocks |
| Skylight | Roof host plane, pitch dependency |
| Façade element | Non-operable, decorative sub-framing |

## Example Definition (JSON)

```json
{
  "schemaVersion": 1,
  "id": "aperture:fixed_window",
  "category": "window",
  "parameters": {
    "width":  { "type": "length", "default": 1200, "min": 300, "max": 6000 },
    "height": { "type": "length", "default": 1500, "min": 300, "max": 4000 },
    "mullions": { "type": "count", "default": 0, "min": 0, "max": 10 }
  },
  "constraints": [
    { "expr": "width % mullion_spacing == 0", "message": "Mullion grid mismatch" }
  ],
  "generator": "aperture:rectangular_window_v1",
  "components": {
    "frame": { "profile": "aperture:frame_standard_50" },
    "glazing": { "system": "aperture:double_glazed" }
  },
  "materialSlots": ["frame", "glazing", "hardware"]
}
```

## Example Instance (JSON)

```json
{
  "schemaVersion": 1,
  "instanceId": "550e8400-e29b-41d4-a716-446655440000",
  "typeId": "aperture:fixed_window",
  "parameters": { "width": 1800, "height": 2100, "mullions": 2 },
  "transform": { "x": 0, "y": 64, "z": 0, "facing": "north" },
  "host": { "type": "wall", "anchor": "block-pos-or-uuid" },
  "state": { "openRatio": 0.0 },
  "revision": 42
}
```

## Naming Conventions

| Concept | Canonical Name | Never Use |
|---|---|---|
| Type recipe | `OpeningTypeDefinition` | `DoorType`, `WindowBlock` |
| Placed element | `OpeningInstance` | `PlacedDoor` |
| Procedural recipe | `Generator` | `DoorModel` |
| Category | `OpeningCategory` | `BlockType` |
