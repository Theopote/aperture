# ADR 0002: Pure-Java Core Module

## Status

Accepted

## Context

Minecraft APIs change every version. Business logic tied to `net.minecraft.*` becomes expensive to maintain and impossible to unit test in isolation.

## Decision

All domain logic, geometry, parameters, and validation live in `aperture-core` and `aperture-geometry` with **zero** Minecraft imports. Minecraft adapters exist only in the Fabric root module.

## Consequences

- Core can be unit tested without launching Minecraft.
- Version ports touch adapter layer first.
- Slight overhead: mapping between domain types and NBT/block entities.
- CI can enforce the import boundary.
