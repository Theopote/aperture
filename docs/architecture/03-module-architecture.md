---
status: active
implementation_version: kernel-pipeline-v1
last_verified: 2026-07-17
---

# Module Architecture

Aperture is split into small Java libraries and thin Minecraft/Fabric adapters. Dependency direction points from foundational data types toward the runtime; platform code must not leak back into the libraries.

```text
aperture-math
  -> aperture-parameter
  -> aperture-core
  -> aperture-geometry
  -> aperture-opening
  -> aperture-pipeline
  -> aperture-kernel
  -> aperture-runtime
       -> aperture-editor
       -> aperture-render
       -> aperture-fabric / root client adapters
```

This is a direction rule, not a requirement that every module depend on every predecessor.

## Responsibilities

| Module | Responsibility | Minecraft imports |
|---|---|---|
| `aperture-math` | Numeric and transform primitives | Forbidden |
| `aperture-parameter` | The single `ParameterSet` model | Forbidden |
| `aperture-core` | Opening definitions and domain contracts | Forbidden |
| `aperture-geometry` | Geometry/profile primitives | Forbidden |
| `aperture-opening` | Component planning and geometry/mesh compilers | Forbidden |
| `aperture-pipeline` | Typed stage orchestration and structural caching | Forbidden |
| `aperture-kernel` | Stable generation facade and composition root | Forbidden |
| `aperture-runtime` | World-facing generation service and instance mapping | Forbidden |
| `aperture-editor` | Headless editor state and interaction models | Forbidden |
| `aperture-render` | Platform-neutral render data and mesh services | Forbidden |
| `aperture-fabric`, root sources | Minecraft registration, input, rendering and world adapters | Allowed |

## Public surface

There is currently no compatibility-oriented `aperture-api` module. During development, consumers use the Kernel contracts directly. A separate API module should return only when real third-party SPI requirements exist; it must then depend inward on stable contracts rather than on Runtime or Editor implementations.

## Client boundary

Root `src/client` owns Minecraft-specific input, picking, buffer submission, and screen adapters. Platform-neutral editor state, interaction targets, and render caches belong in `aperture-editor` or `aperture-render`.