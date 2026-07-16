# Architecture Audit Report

**Date**: 2026-07-16  
**Purpose**: Assess current documentation against Kernel/Platform/Editor/Applications model

---

## Executive Summary

**Overall Coverage**: 54% complete (14 of 26 planned documents exist)

**Key Finding**: Aperture's existing architecture is **already aligned** with the Kernel/Platform/Editor model. The code and documentation follow the "platform before content" principle. What's needed is:
1. **Reorganization** of docs into the new structure
2. **Completion** of missing detailed design docs (Geometry Kernel, Parameter Engine, Component System)
3. **Unification** of some implementation gaps (parameter resolution, NBT persistence)

**Recommendation**: Do NOT start over. Complete the remaining 25% of Kernel/Platform layer docs, then proceed with implementation.

---

## Existing Documents Mapped to New Model

### Foundation Layer ✅ (Well Covered)

| Existing Document | Layer | Status | Notes |
|-------------------|-------|--------|-------|
| 01-vision.md | Foundation | 🔄 | Exists but needs update with "Architectural Design Kernel" positioning |
| 03-module-architecture.md | Foundation | ✅ | Already describes layer separation |
| 09-folder-structure.md | Foundation | ✅ | Complete |
| ADRs/0001-opening-as-domain-primitive.md | Foundation | ✅ | Documents why "Opening" is the core abstraction |
| ADRs/0002-pure-java-core.md | Foundation | ✅ | Documents Kernel independence from Minecraft |

**Missing**:
- ❌ 00-dependency-rules.md (CI enforcement)
- ❌ CONTRIBUTING.md update with Iron Law

**Assessment**: Strong foundation. Just needs the new positioning articulated clearly.

---

### Kernel Layer 📝 (Partially Covered)

| Existing Document | Maps To | Status | Notes |
|-------------------|---------|--------|-------|
| 02-domain-model.md | kernel/data-model | ✅ | Documents OpeningTypeDefinition, OpeningInstance, Parameters |
| 04-core-systems.md | kernel/overview | ✅ | High-level overview of 9 systems |

**What Exists in Code but Lacks Documentation**:

**Geometry System** (aperture-geometry module):
- ✅ Code: BoundingBox, Transform, basic mesh types
- ✅ Code: Profile system (skeletal)
- ❌ Doc: No detailed geometry kernel design doc
- ❌ Doc: No curve/extrusion/boolean operation specs

**Parameter System** (aperture-core/parametric):
- ✅ Code: 8 parameter types (Number, Boolean, Range, Choice, Enum, Material, etc.)
- ✅ Code: ParametricSchema, Parameter, ParameterMetadata
- ✅ Code: ConstraintEvaluator, ExpressionParser
- ❌ Doc: No unified parameter engine design doc
- ⚠️ Code: Parameter resolution paths not unified (Editor vs Preview vs Generate)

**Component System** (aperture-core/component):
- ✅ Code: ComponentAssembly, OpeningComponent interface
- ✅ Code: Frame, Glass, Panel, Handle, Mullion, Trim, Sill, Hardware components
- ✅ Code: ComponentAssemblyPresets
- ❌ Doc: No component system contract documentation
- ❌ Doc: Component generation lifecycle not documented

**Constraint System** (aperture-core/constraint):
- ✅ Code: ConstraintExpression, ConstraintEvaluator
- ✅ Code: ExpressionParser, ExpressionLexer
- ✅ Code: ParametricValidator
- ❌ Doc: No constraint solver design doc (basic validation exists, advanced solving is future)

**Missing Documents**:
- ❌ kernel/01-geometry-kernel.md
- ❌ kernel/02-parameter-engine.md
- ❌ kernel/03-component-system.md
- ⏸️ kernel/04-constraint-solver.md (can defer, current implementation is sufficient)

**Assessment**: Kernel implementation is ~60% complete. Documentation is ~30% complete. The gap is documentation, not code architecture.

---

### Platform Layer 🔄 (Well Covered, Some Gaps)

| Existing Document | Maps To | Status | Notes |
|-------------------|---------|--------|-------|
| 05-rendering.md | platform/rendering-pipeline | ✅ | Complete: delta engine, mesh compiler, LOD, materials |
| 06-placement.md | platform/placement-system | ✅ | Complete: targeting, preview, validation, commit |
| 07-serialization.md | platform/serialization | ✅ | Complete: JSON codec, NBT (spec only, impl missing), networking |
| 10-fabric-placement-adapter.md | platform/minecraft-integration | ✅ | Complete: raycast → PlacementContext |

**What Exists in Code but Lacks Documentation**:

**Opening Pipeline** (aperture-opening-geometry):
- ✅ Code: OpeningGenerationPipeline (skeletal)
- ✅ Code: GeometryBuilder, MeshBuilder concepts
- ⚠️ Code: Pipeline stages not fully separated (still monolithic in some generators)
- ❌ Doc: No detailed pipeline data flow documentation

**Rendering Pipeline** (aperture-render):
- ✅ Code: RenderDocument, RenderDeltaEngine, MeshCompiler
- ✅ Code: MaterialBindingSet, CatalogMaterialResolver
- ✅ Code: FabricRenderBackend, OpeningInstanceRenderer
- ✅ Code: Ghost preview rendering
- ✅ Doc: 05-rendering.md is comprehensive

**Placement System** (aperture-core/placement, aperture-fabric/placement):
- ✅ Code: PlacementSession, PlacementContext
- ✅ Code: PlacementValidator chain
- ✅ Code: FabricPlacementRaycast, HostClassifier, HostPlaneScanner
- ✅ Code: Ghost preview mesh
- ⚠️ Code: Collision and Footprint not formal pipeline outputs yet
- ✅ Doc: 06-placement.md is good

**Serialization** (aperture-core/serialization):
- ✅ Code: OpeningTypeDefinitionReader, ComponentAssemblyReader
- ✅ Code: OpeningInstanceCodec (JSON)
- ✅ Code: SchemaVersion, MigrationContext (skeletal)
- ❌ Code: NBT persistence not implemented yet
- ✅ Doc: 07-serialization.md documents the plan

**Missing Documents**:
- ❌ platform/01-opening-pipeline.md (CRITICAL — this ties everything together)

**Assessment**: Platform layer is ~70% complete in code, ~80% documented. Main gap: Opening Pipeline overview and NBT implementation.

---

### Editor Layer ⏸️ (Planned, ~20% Code Exists)

| Existing Document | Status | Notes |
|-------------------|--------|-------|
| (None yet) | ⏸️ | No editor design docs exist |

**What Exists in Code**:

**Editor Module** (aperture-editor):
- ✅ Code: Selection, EditorObject, EditorObjectId
- ✅ Code: EditHistory, EditCommand, EditResult
- ✅ Code: ManipulatorKind, ResizeAxis, MirrorAxis
- ✅ Code: SnapPolicy, SnapKind
- ✅ Code: EditorConstraint, EditorConstraintKind
- ❌ Code: No gizmo → parameter binding yet
- ❌ Code: No undo/redo implementation yet
- ❌ Code: No inspector ↔ gizmo sync yet

**Client Editor** (src/client/editor):
- ✅ Code: GizmoPickTarget, GizmoDragMath, GizmoDragController
- ✅ Code: GizmoPickService
- ✅ Code: EditorGizmoRenderer (skeletal)
- ❌ Code: No live preview during drag yet

**Missing Documents**:
- ❌ editor/01-editor-kernel.md
- ❌ editor/02-manipulation.md
- ❌ editor/03-history.md

**Assessment**: Editor layer is ~20% complete. This is intentional — it's Phase C in the roadmap. Should not be prioritized yet.

---

### Applications Layer 🔒 (Frozen, Reference Types Only)

| Existing Document | Status | Notes |
|-------------------|--------|-------|
| (None) | 🔒 | Correctly frozen per 13-platform-roadmap-af.md |

**What Exists** (Reference Types for Testing):
- ✅ Data: aperture-data/aperture/opening_types/fixed_window.json
- ✅ Data: aperture-data/aperture/opening_types/door.json
- ✅ Data: aperture-data/aperture/opening_types/curtain_wall.json
- ✅ Code: BuiltinOpeningTypes (presets, not published)

**Assessment**: Correctly frozen. These reference types are sufficient for platform validation.

---

## Roadmap Documents

| Document | Status | Coverage |
|----------|--------|----------|
| 08-expansion-plan.md | ✅ | Original Phase 0-5 timeline |
| 12-phase-roadmap.md | ✅ | Detailed 12-phase engineering breakdown |
| 13-platform-roadmap-af.md | ✅ | Strategic Phase A-F + Family Library Freeze |

**Assessment**: Excellent. All three roadmaps are aligned and comprehensive. The new APERTURE-REDEFINED.md complements (not replaces) these by providing strategic framing.

---

## Code Implementation Status by Layer

### Kernel Layer (~60% Complete)

**Geometry** (aperture-geometry):
- ✅ BoundingBox, Transform3d
- ✅ Vec3d, Direction
- ⏳ ProfileDefinition (exists but incomplete)
- ⏳ MeshAssembler (basic implementation)
- ❌ Curve, Bezier, Arc (planned)
- ❌ Extrusion (stub exists)
- ❌ Boolean operations (future)

**Parameters** (aperture-core/parametric):
- ✅ 8 parameter types fully implemented
- ✅ ParametricSchema, ParameterMetadata
- ✅ ConstraintEvaluator, ExpressionParser
- ✅ ParametricEditor (interface)
- ⚠️ Parameter resolution not unified across code paths

**Components** (aperture-core/component):
- ✅ All component types defined (Frame, Glass, Panel, Handle, etc.)
- ✅ ComponentAssembly, ComponentRef
- ✅ ComponentProperties
- ⚠️ Component generation contract not formalized

**Constraints** (aperture-core/constraint):
- ✅ Expression parsing and evaluation
- ✅ Basic validation
- ❌ Dependency propagation (future)
- ❌ Equation solving (future)

**Assessment**: Kernel has strong foundations. Needs:
1. Unified parameter resolution
2. Component contract documentation
3. Profile system completion

### Platform Layer (~70% Complete)

**Pipeline** (aperture-opening-geometry):
- ✅ OpeningGenerationPipeline (skeletal)
- ⏳ ComponentPlanBuilder (exists but not used consistently)
- ⏳ GeometryBuilder (partially implemented)
- ✅ MeshCompiler (basic box compiler works)
- ⚠️ Collision/Footprint not formal outputs

**Rendering** (aperture-render):
- ✅ RenderDocument, delta engine
- ✅ MeshCompiler (box strategy)
- ✅ MaterialBindingSet, resolver
- ✅ FabricRenderBackend
- ✅ Ghost preview
- ✅ Committed instance rendering
- ❌ LOD implementation (documented but not impl)
- ❌ Async baking (documented but not impl)

**Placement** (aperture-fabric/placement, aperture-core/placement):
- ✅ PlacementSession
- ✅ PlacementContext
- ✅ Validator chain (FitsWithin, NoOverlap, etc.)
- ✅ Ghost preview mesh
- ✅ P key commit (basic)
- ❌ Host cutting (planned)
- ❌ Snap to adjacent openings (planned)

**Serialization**:
- ✅ JSON codec (OpeningInstanceCodec)
- ✅ OpeningTypeDefinitionReader
- ❌ NBT persistence (critical missing piece)
- ❌ Network sync (planned)

**Assessment**: Platform is in good shape. Critical gap: NBT persistence.

### Editor Layer (~20% Complete)

**Editor Kernel** (aperture-editor):
- ✅ Interfaces exist
- ❌ Implementation mostly missing

**Client** (src/client/editor):
- ✅ Gizmo rendering skeleton
- ❌ Parameter binding missing
- ❌ Live preview missing
- ❌ Undo/redo missing

**Assessment**: As expected for Phase C. Not a priority yet.

---

## Documentation Gaps Prioritized

### Priority 1 (Critical for Kernel V1)
1. **kernel/02-parameter-engine.md** — Document ParametricSchema, resolution flow, constraints
2. **kernel/03-component-system.md** — Document ComponentAssembly contract, generation lifecycle
3. **platform/01-opening-pipeline.md** — Document Definition → Render data flow

### Priority 2 (Important for Platform V1)
4. **kernel/01-geometry-kernel.md** — Document existing types, planned extensions
5. **00-dependency-rules.md** — Document and enforce layer boundaries

### Priority 3 (Can Defer to Phase C)
6. **editor/01-editor-kernel.md**
7. **editor/02-manipulation.md**
8. **editor/03-history.md**

### Priority 4 (Phase D+, Frozen)
9. **applications/01-opening-library.md** (after Platform + Editor complete)
10. **applications/02-building-library.md** (Phase 5)

---

## Code Gaps Prioritized

### Priority 1 (Blocking Kernel V1)
1. **Unify parameter resolution** — All code paths use OpeningParameterResolver
2. **Implement NBT persistence** — OpeningInstance to Block Entity, save/load works
3. **Formalize pipeline outputs** — Add Collision and Footprint to PipelineResult

### Priority 2 (Quality/Validation)
4. **Golden tests** — Snapshot tests for fixed_window and door
5. **End-to-end test** — Place → Edit → Save → Reload
6. **CI dependency check** — Fail if Kernel imports Minecraft

### Priority 3 (Platform V1 Features)
7. **Host cutting** — CutVolume actually modifies wall blocks
8. **Network sync** — Client edits propagate to server
9. **Snap to adjacent** — Openings align to each other

### Priority 4 (Editor V1, Phase C)
10. **Gizmo → parameter binding**
11. **Live preview during resize**
12. **Undo/redo implementation**

---

## Recommendations

### Immediate (Week 1)
1. ✅ Create APERTURE-REDEFINED.md (done)
2. ✅ Create DEVELOPMENT.md (done)
3. ✅ Create 00-INDEX.md (done)
4. ✅ Update README.md (done)
5. ⏳ Write kernel/02-parameter-engine.md
6. ⏳ Write kernel/03-component-system.md
7. ⏳ Write platform/01-opening-pipeline.md
8. ⏳ Update 01-vision.md with new positioning
9. ⏳ Create 00-dependency-rules.md
10. ⏳ Update CONTRIBUTING.md with Iron Law

### Week 2
1. Unify parameter resolution
2. Implement NBT persistence
3. Add Collision/Footprint outputs
4. Write golden tests
5. Write end-to-end test

### After Week 2
- Declare Kernel V1 complete
- Begin Platform V1 completion (host cutting, network sync)
- When Platform V1 done → begin Editor Phase

---

## Conclusion

**The architecture is sound.** The existing code and documents already follow the Kernel → Platform → Editor → Applications model. What's needed is:

1. **Articulation** of the vision (done via APERTURE-REDEFINED.md)
2. **Completion** of ~10 missing design documents
3. **Unification** of a few implementation gaps
4. **Testing** to validate the architecture

**No major refactoring needed.** The project is on the right track.

---

**Audit Completed**: 2026-07-16  
**Next Step**: Write the missing Kernel layer design documents (Week 1 sprint)
