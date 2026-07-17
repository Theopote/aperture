# Aperture Architecture

Aperture is an **Architectural Runtime Kernel** with a Minecraft platform adapter — not a furniture, decoration, or door-and-window catalog.

Every placed and operated element is an `ArchitecturalObject`. Openings are the first implemented family; other families retain typed definitions and generation strategies while sharing runtime identity, state, behavior, commands, persistence, replication, and simulation contracts.

Minecraft is one adapter. Pure Kernel and Runtime modules must remain usable by other hosts.

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
| [13-platform-roadmap-af.md](13-platform-roadmap-af.md) | **Platform roadmap A–F** — 决策框架 + **族库冻结策略** |
| [12-phase-roadmap.md](12-phase-roadmap.md) | Twelve-phase engineering breakdown (Architecture → AI/NodeCraft) |
| [09-folder-structure.md](09-folder-structure.md) | Repository layout |
| [10-fabric-placement-adapter.md](10-fabric-placement-adapter.md) | Fabric raycast → PlacementContext adapter |

## Schema Contracts

JSON schemas live in [`../schemas/`](../schemas/):

- `opening-type-definition.schema.json` — immutable opening family definitions
- `opening-instance.schema.json` — placed opening instances

## Architecture Decision Records

Major decisions are recorded in [`ADRs/`](ADRs/).

## Core Pipeline

```
Definition → Validation → Compiled Definition
                         ↓
              Generation → Placement
                         ↓
Instance → Runtime State → Behavior → Interaction → Effects → Persistence/Replication
                         ↓
World Snapshot → Simulation → Results → Visualization/Behavior Commands
```
