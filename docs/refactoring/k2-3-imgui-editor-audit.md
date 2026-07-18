# K2.3 ImGui editor audit

## Current state

The legacy `dev.aperture.core.editor` package contains selection, mutable editor objects, local commands/history, manipulators, snap and gizmo support. `src/client` owns `ClientRuntimeReplicas`; rendering is in `aperture-render`; Fabric owns Minecraft lifecycle. `aperture-editor-imgui` now contains platform-neutral window models and renderer seams, but no Dear ImGui implementation.

## Legacy mutation paths

The legacy `core.editor.session.EditorSession`, `EditorContext`, `EditorObject`, `SetParameterCommand` and `SetTransformCommand` retain `OpeningInstance` and execute/undo local mutations. They are retained temporarily for K2.2 compatibility and are not the K2.3 frontend API. Runtime state mutation remains transaction-owned. There is still no ImGui context, real docking, font atlas, native backend or OpenGL renderer. Workspace persistence and input policy contracts now exist, but they are not connected to Dear ImGui.

## Authoritative boundary

The client source set exposes `ClientRuntimeReplicas.store()` (`ClientReplicaStore`). K2.3 reads immutable `ReplicaObject` values through `ReplicaEditorReadModel`. Formal writes use `EditorCommandGateway` and an injected transport; widgets never use codecs. Preview values live in `LocalPreviewCoordinator`, never in the replica store.

## Migration table

| Capability | Current implementation | Target implementation | Action |
| --- | --- | --- | --- |
| Selection | `Selection<EditorObjectId>` | runtime `ArchitecturalObjectId` snapshots | added, migrate callers |
| Inspector | ad-hoc services | schema/value-driven descriptors | added first slice |
| Command | local `execute/undo` | gateway transport submission | added, legacy deprecated path remains |
| History | local mutable undo | compensating authoritative command | added contract |
| Preview | direct editor mutation | local overlay/edit session | added |
| UI frontend | renderer abstractions only | real Dear ImGui frontend | P0: implement binding, widgets, docking and native host |

## Remaining gaps

P0: `aperture-editor-imgui` has no `imgui-java` or native dependency and makes no Dear ImGui calls. Fabric still needs the concrete ImGui/GLFW/OpenGL host, real widgets/dockspace, world picking binding, native font setup and client smoke verification. Legacy editor callers must be migrated before its mutable package can be deleted.
