# Kernel Integration Inventory

Status: active  
Milestone: K1 — Single Authoritative Kernel  
Last verified: 2026-07-17

This inventory describes the current repository after the legacy pipeline removal. The historical old/new parity point has passed; deleted orchestration code must not be restored merely to create a comparison harness. Production-output stability is protected by Kernel determinism and golden tests instead.

| Capability | Current production entry | Target entry | Migration |
|---|---|---|---|
| Type resolution | `DefinitionStage(OpeningTypeRegistry)` | Kernel Pipeline | Complete |
| Parameter resolution | `ParameterStage`; layout derivation in `OpeningParameterResolver` | `ParameterStage` for effective values | Complete; resolver should eventually be renamed |
| Constraints | `ConstraintStage` | `ConstraintStage` | Complete |
| Component planning | `ComponentStage` | `ComponentStage` | Complete |
| Geometry | `GeometryStage` → `OpeningGeometryCompiler` | `GeometryStage` | Complete |
| Mesh | `MeshStage` → `OpeningMeshCompiler` | `MeshStage` | Complete |
| Runtime generation | `OpeningGenerationService` → `ApertureKernel` | `ApertureKernel` | Complete |
| Collision | `BoundingBoxCollisionStage` | Future collision compiler | Explicitly basic |
| Placement | `BasicPlacementMetadataStage` | Future host-aware placement compiler | Explicitly basic |
| Resource reload | Static Kernel-owned registries | Versioned reload coordinator | Pending |
| K1 family acceptance | Door-focused Kernel tests | Door, fixed window, curtain wall Kernel baselines | In progress |
| Dependency enforcement | Gradle module boundaries | Automated architecture check | In progress |

## Freeze

Until K1 acceptance, do not add opening families, accessories, editor panels, material systems, collision algorithms, partial execution, or incremental regeneration. Work is limited to architecture convergence, regression coverage, documentation, and dependency enforcement.

## K1 exit conditions still open

- All Kernel tests pass.
- Door, fixed window, and curtain wall deterministic production baselines pass through `ApertureKernel`.
- Resource reload owns definition/profile revision increments, Pipeline cache clearing, render invalidation, and affected-instance regeneration.
- Architecture checks run as part of normal verification.