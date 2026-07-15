# Aperture

**Architectural Opening Design System** for Minecraft (Fabric).

Aperture is not a furniture or decoration mod. It is a procedural platform for designing, generating, editing, and placing architectural openings — doors, windows, curtain walls, skylights, and façade elements.

## Philosophy

Everything is an Opening. Every architectural element is generated from a data-driven, procedural opening definition.

## Project Structure

```
aperture-core/       Pure Java domain model (no Minecraft imports)
aperture-geometry/   Procedural geometry generators
aperture-api/        Public extension API for addon mods
src/                 Fabric mod entrypoints and adapters
aperture-data/       JSON opening type definitions
docs/                Architecture documentation
```

## Documentation

See [`docs/architecture/README.md`](docs/architecture/README.md) for the full architecture.

## Development Status

**Phase 0 — Foundation** (in progress)

- [x] Architecture documentation
- [x] Multi-module Gradle structure
- [x] Core domain model (`OpeningTypeDefinition`, `OpeningInstance`, parameters)
- [x] Reference generator (`RectangularWindowGenerator`)
- [ ] Placement system
- [ ] Rendering pipeline
- [ ] World persistence (NBT)

## Build

```bash
./gradlew build
```

Requires Java 25 and Minecraft 26.1 (Fabric).

## License

CC0-1.0
