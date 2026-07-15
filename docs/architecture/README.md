# Aperture Architecture

Aperture is a **parametric architectural opening platform** for Minecraft — not a furniture or decoration mod.

Every door, window, curtain wall, skylight, and façade element is a single kind of thing: an **Opening**, defined procedurally, placed in host geometry, edited parametrically, and rendered as generated structure.

## Document Index

| Document | Description |
|---|---|
| [01-vision.md](01-vision.md) | Project vision, mission, and design tenets |
| [02-domain-model.md](02-domain-model.md) | Data model, entities, and schemas |
| [03-module-architecture.md](03-module-architecture.md) | Gradle modules and dependency rules |
| [04-core-systems.md](04-core-systems.md) | Nine cooperating core systems |
| [05-rendering.md](05-rendering.md) | Client rendering pipeline |
| [06-placement.md](06-placement.md) | Placement workflow and validation |
| [07-serialization.md](07-serialization.md) | Persistence, networking, and migration |
| [08-expansion-plan.md](08-expansion-plan.md) | Phased roadmap (Phase 0–5) |
| [09-folder-structure.md](09-folder-structure.md) | Repository layout |

## Schema Contracts

JSON schemas live in [`../schemas/`](../schemas/):

- `opening-type-definition.schema.json` — immutable opening family definitions
- `opening-instance.schema.json` — placed opening instances

## Architecture Decision Records

Major decisions are recorded in [`ADRs/`](ADRs/).

## Core Pipeline

```
Definition → Validation → Generation → Placement → Instance → Render/Sync
```
