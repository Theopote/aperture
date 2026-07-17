# ADR 0003: ArchitecturalObject as the Universal Runtime Primitive

## Status

Accepted — 2026-07-17

Supersedes the platform-wide scope of [ADR 0001](0001-opening-as-domain-primitive.md). ADR 0001 remains valid only inside the Opening family.

## Context

The first Aperture implementation focused on doors, windows, curtain walls, and skylights. Treating these categories as one Opening family prevented block-per-variant designs and produced a coherent generation pipeline.

The long-term platform must also operate walls, slabs, stairs, furniture, equipment, rooms, routes, sensors, and building systems. These objects do not share one geometry pipeline and must not be forced into the Opening model. They do share runtime identity, definitions, parameters, state, behavior, capabilities, interactions, host dependencies, commands, persistence, replication, and simulation semantics.

## Decision

`ArchitecturalObject` is the universal runtime primitive.

- `Opening` is the first fully implemented architectural family.
- Each family owns its typed definition and generation strategy.
- Runtime services operate on common object identity and capability contracts, not Opening-specific classes.
- Editor actions, AI operations, player interactions, and network requests converge on the same Command and Transaction boundary.
- Platform adapters translate host-world APIs into pure runtime contracts; the core does not depend on Minecraft.
- A runtime definition may be hosted by multiple platform adapters when those adapters implement its required capabilities.

## Consequences

- The Opening generation kernel remains useful and typed without becoming the universal domain model.
- New architectural families can reuse runtime infrastructure without pretending to be openings.
- Dependency and system graphs can connect heterogeneous objects.
- Persistence and replication formats require family identifiers, schema versions, and capability-aware migration.
- A second non-Opening family is required to prove that the abstraction is genuinely general.
