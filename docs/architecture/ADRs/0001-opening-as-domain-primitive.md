# ADR 0001: Opening as the Sole Domain Primitive

## Status

Accepted

## Context

Minecraft mods typically implement doors, windows, and decorations as separate block classes. Aperture targets 5+ years of architectural opening development with BIM/parametric ambitions.

## Decision

Every architectural element is modeled as an **Opening**. Categories (door, window, curtain wall) are metadata + behavior strategies, not separate class hierarchies.

## Consequences

- One placement, rendering, and serialization pipeline.
- New opening types are data + generators, not new block classes.
- Addon API stays stable.
- Requires procedural rendering (no per-variant block models).
