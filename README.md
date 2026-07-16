# Aperture

**An Architectural Design Kernel running inside Minecraft.**

Aperture is not a door mod or a furniture collection. It is a parametric design platform — a kernel for procedural architectural modeling that happens to run in Minecraft.

Doors, windows, curtain walls, roofs, stairs, and future building components are all **applications** of the kernel, not the kernel itself.

## The Iron Law

> **Every new feature must improve the kernel before it improves a specific building component.**

This ensures Aperture remains an extensible architectural design platform, not a collection of hard-coded Minecraft blocks.

## Philosophy

Everything is parametric. Everything is procedural. Everything is generated from data-driven definitions, never from fixed models.

## Architecture

Aperture is organized into four layers:

**Layer 1: Kernel** — Pure abstractions (geometry, parameters, components, constraints)  
**Layer 2: Platform** — Runtime system connecting Kernel to Minecraft  
**Layer 3: Editor** — CAD-quality manipulation and design tools  
**Layer 4: Applications** — Concrete opening and building types

Dependencies flow downward only. The kernel has zero Minecraft dependencies.

```
aperture-core/          Kernel: domain model, parameters, components
aperture-geometry/      Kernel: geometry primitives (Point, Curve, Mesh, Transform)
aperture-math/          Kernel: mathematical utilities
aperture-opening-geometry/  Platform: opening generation pipeline
aperture-render/        Platform: rendering engine (mesh compiler, delta updates)
aperture-runtime/       Platform: instance lifecycle, persistence
aperture-editor/        Editor: selection, manipulation, history
aperture-fabric/        Platform: Minecraft integration (Fabric mod)
aperture-api/           Platform: public extension API
src/                    Fabric mod entrypoints and client rendering
aperture-data/          Applications: JSON opening type definitions
docs/                   Architecture documentation (see below)
```

## Documentation

See [`docs/architecture/README.md`](docs/architecture/README.md) for the full architecture.

## Development Status

**Phase 0 — Foundation** (in progress)

- [x] Architecture documentation
- [x] Multi-module Gradle structure
- [x] Core domain model (`OpeningTypeDefinition`, `OpeningInstance`, parameters)
- [x] JSON data pack loader (`aperture-data/`)
- [x] Reference generator (`RectangularWindowGenerator`)
- [x] Instance store interface + in-memory implementation
- [x] Generation service (validate → generate pipeline)
- [ ] Placement system
- [ ] Rendering pipeline
- [ ] World persistence (NBT)

Requires **Java 21+** (set `java_version` in `gradle.properties`; bump to 25 when targeting MC 26.1 with JDK 25).

## Build

```bash
./gradlew build
```

Requires Java 21+ and Minecraft 26.1 (Fabric).

Set `java_version` in `gradle.properties` (default `21`).

## License

CC0-1.0
