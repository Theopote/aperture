---
status: historical
superseded_by: docs/architecture/03-module-architecture.md
last_verified: 2026-07-17
---

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
├── aperture-render/                 # Pure Java render contracts
│   └── src/main/java/dev/aperture/render/
│       ├── data/
│       ├── mesh/
│       ├── material/
│       ├── pipeline/
│       └── collision/
│
├── aperture-api/                    # Public extension API
│   └── src/main/java/dev/aperture/api/
│       ├── ApertureApi.java
│       ├── Generator.java
│       └── registry/
│
├── src/                             # Fabric mod
│   ├── main/java/dev/aperture/
│   │   ├── Aperture.java
│   │   ├── bootstrap/
│   │   └── placement/fabric/        # Raycast → PlacementContext adapter
│   ├── client/java/dev/aperture/client/
│   │   ├── ApertureClient.java
│   │   ├── placement/               # Crosshair preview + commit key
│   │   └── render/placement/        # Gizmo wireframe overlay
│   └── main/resources/
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

Phase 0 uses Loom `splitEnvironmentSourceSets()` — common code in `src/main`, client-only code in `src/client`. Core subprojects remain pure Java.
