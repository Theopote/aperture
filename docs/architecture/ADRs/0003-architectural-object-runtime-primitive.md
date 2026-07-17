# ADR 0003: ArchitecturalObject as the Universal Runtime Primitive

## Status

Accepted — 2026-07-17

This decision supersedes the platform-wide scope of [ADR 0001](0001-opening-as-domain-primitive.md). ADR 0001 remains valid inside the Opening family and its design-time generation pipeline.

## Context

Aperture began with Door, Fixed Window, and Curtain Wall. Those three references already exercise dynamic intent, static construction, and repeated component layouts. More Opening families would increase content maintenance without proving a new platform capability.

The intended platform must also operate structural, circulation, equipment, spatial, and system objects. Forcing all of them into an Opening hierarchy would couple the runtime to its first application family.

Generation also does not equal operation. A generated and placed instance still needs identity, state, capabilities, behavior, commands, events, world boundaries, persistence, and replication.

## Decision

`ArchitecturalObject` is the universal runtime entity. `Opening` is the first fully implemented architectural family.

The lifecycle has three distinct identities:

```text
ArchitecturalObjectDefinition
        -> instantiate
ArchitecturalObjectInstance
        -> activate in world
RuntimeArchitecturalObject
```

- A Definition is immutable, versioned design intent and may be shared by many instances.
- An Instance is durable world data: identity, type, parameter overrides, transform, host bindings, persistent state, revision, and metadata.
- A Runtime Object is the activated execution view: current state, capabilities, behaviors, subscriptions, scheduling, and platform-neutral world access.
- `OpeningInstance` remains a compatibility model while the generic instance contract is introduced incrementally.
- Runtime code dispatches through capabilities and contracts, never `instanceof Door`.
- Editor, player, AI, network, behavior, and simulation mutations converge on one Command/Transaction boundary.
- Events, world queries, and world effects are pure Java. Platform adapters translate Minecraft values at the edge.
- The existing Door, Fixed Window, and Curtain Wall references are retained but not expanded during K2.

## Consequences

- The current Opening Generation Kernel remains typed and useful; it does not become the universal runtime model.
- K2 prioritizes the Architectural Runtime Object and Command/Event/Behavior loop over additional families.
- A pure-Java `aperture-runtime-model` may be introduced first; it must not depend on `aperture-opening`.
- `aperture-opening` may depend on runtime-model contracts to provide Door state, capabilities, and behavior definitions.
- Persistence and replication carry instance/state/host/revision deltas, not complete meshes.
- A later non-Opening family must prove the abstraction without an Opening-shaped adapter.

## K2 acceptance direction

K2 is not accepted merely because a door animates. The Door vertical slice must use schema-backed state, Commands, Behavior, events, kinematic transforms, authoritative revisions, persistence, replication, and Minecraft-free runtime tests.
