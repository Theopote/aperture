# K2.2 Phase 6: Family Plugin Decoupling

Date: 2026-07-18

## Outcome

The generic architectural runtime no longer has a production dependency on the Opening family. Runtime configuration is contributed through `ArchitecturalFamilyPlugin` and composed by `ArchitecturalFamilyPluginRegistry`.

```text
aperture-runtime-model
        ^
        |
aperture-opening       aperture-runtime
        ^                    ^
        |                    |
        +-- aperture-opening-runtime
                       ^
                       |
                 aperture-fabric
```

`aperture-opening-runtime` owns the Door registration for state schema, capabilities, interaction behavior, command handlers, tick evaluation, and kinematics. `aperture-runtime` owns only the family-neutral plugin contract and registry.

## Registration boundary

Fabric creates one plugin registry, passes it to `DefaultArchitecturalRuntime` as the configuration resolver, and builds the command bus from the same registry. The registry rejects duplicate plugin IDs and ambiguous type ownership.

## Architecture enforcement

`checkArchitecture` now rejects a production Runtime dependency on Opening or its composition module, and rejects Opening-family imports under `aperture-runtime/src/main`. Opening remains only a test fixture dependency for generic transaction and replication tests.

## Adapter cleanup decision

`OpeningRuntimeSnapshotAdapter` is still called by `OpeningWorldPlacement` to migrate legacy placed Door instances into authoritative K2 snapshots, so it is not dead code. It can be removed when placement creates the architectural instance and initial snapshot directly and the legacy persistence path is retired. The adapter remains one-way and is not an authoritative state owner.

## Verification

- Opening plugin registration test covers Door configuration and handler contribution.
- Fabric compilation covers bootstrap composition through the registry.
- `checkArchitecture` enforces the inverted dependency boundary.
- Full tests and client compilation form the final Phase 6 gate.
