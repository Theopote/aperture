---
status: active
implementation_version: kernel-pipeline-v1
last_verified: 2026-07-17
---

# Module Architecture

Aperture is split into pure Java domain libraries and thin platform adapters. The target architecture has Foundation, Design Kernel, Runtime Kernel, Simulation Kernel, Platform Adapter, and Application layers. The dependency listing below describes current physical packaging while K2 introduces the missing runtime-model boundary.

Pure runtime contracts must not depend on `aperture-opening` or Minecraft. K2 first validates object, state, capability, event, command, and behavior contracts in one cohesive `aperture-runtime-model`; it does not create every proposed Gradle module at once.

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
## K2 dependency direction

```text
aperture-runtime-model
        -> aperture-runtime
        -> aperture-fabric

aperture-opening
        -> aperture-runtime-model
```

`aperture-runtime-model` owns pure object, state, capability, event, command, and behavior contracts. `aperture-runtime` owns Minecraft-independent orchestration. `aperture-fabric` owns platform conversion. The reverse dependency `runtime-model -> opening` is forbidden.

The domain-layer model is a responsibility map, not a request to split modules prematurely. Further physical splits require concrete dependency or ownership pressure.


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