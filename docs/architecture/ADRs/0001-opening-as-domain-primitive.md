# ADR 0001: Opening as the Domain Primitive for Opening Families

## Status

Superseded for platform-wide use by [ADR 0003](0003-architectural-object-as-runtime-primitive.md).

Accepted within the Opening family.

## Context

Minecraft mods typically implement doors, windows, and decorations as separate block classes. Aperture targets 5+ years of architectural opening development with BIM/parametric ambitions.

## Decision

Every door, window, curtain wall, and skylight is modeled as an **Opening**. Categories are metadata + behavior strategies, not separate class hierarchies. Unrelated architectural families use `ArchitecturalObject` and their own typed definitions; they are not modeled as openings.

## Consequences

- One generation strategy and shared contracts for the Opening family.
- New opening types are data + generators, not new block classes.
- Addon API stays stable.
- Requires procedural rendering (no per-variant block models).
- Does not imply that walls, stairs, rooms, equipment, or systems share the Opening generation pipeline.
