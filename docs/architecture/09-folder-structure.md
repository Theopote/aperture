# 09 — Folder Structure

```
aperture/
├── build.gradle                     # Fabric Loom (root mod)
├── settings.gradle
├── gradle.properties
│
├── docs/
│   ├── architecture/                # Architecture docs (this folder)
│   └── schemas/                     # JSON Schema contracts
│
├── aperture-core/                   # Pure Java — NO Minecraft
│   └── src/main/java/dev/aperture/core/
│       ├── opening/
│       ├── definition/
│       ├── parameter/
│       ├── instance/
│       ├── geometry/
│       ├── validation/
│       └── serialization/
│
├── aperture-geometry/               # Pure Java procedural generators
│   └── src/main/java/dev/aperture/geometry/
│       ├── generators/
│       └── model/
│
├── aperture-api/                    # Public extension API
│   └── src/main/java/dev/aperture/api/
│       ├── ApertureApi.java
│       ├── Generator.java
│       └── registry/
│
├── src/                             # Fabric mod (single source set)
│   └── main/
│       ├── java/dev/aperture/
│       │   ├── Aperture.java
│       │   ├── bootstrap/
│       │   └── client/
│       └── resources/
│
├── aperture-data/                   # Pack-driven content
│   └── aperture/
│       ├── opening_types/
│       ├── profiles/
│       └── presets/
│
└── aperture-test/                   # (future)
    ├── core/
    └── golden/
```

## Current vs Target

Phase 0 establishes `aperture-core`, `aperture-geometry`, and `aperture-api` as Gradle subprojects. The root project is the Fabric mod with a single `src/main` source set (no split client environment).

Future phases may extract `aperture-common` and `aperture-client` into separate Gradle modules if the codebase warrants it.
