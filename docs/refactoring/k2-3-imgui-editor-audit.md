# K2.3 ImGui editor audit

## Current state

The legacy `dev.aperture.core.editor` package still contains mutable local editor objects and history. The K2.3 path is separate: `aperture-editor` owns the headless session/read/command/preview models, `aperture-editor-imgui` owns real Dear ImGui windows and docking, and the root Fabric mod's `src/client` source set owns Minecraft, GLFW and OpenGL lifecycle integration.

The client classpath explicitly includes `clientImplementation project(':aperture-editor-imgui')`, `imgui-java-lwjgl3`, and platform native runtime artifacts. `ApertureImGuiClient` is a Fabric client entrypoint; F4 opens `ApertureImGuiScreen`, initializes ImGui/GLFW/GL3, builds the Dockspace, and submits draw data before `RenderSystem.flipFrame`.

## Legacy mutation paths

The legacy `core.editor.session.EditorSession`, `EditorContext`, `EditorObject`, `SetParameterCommand` and `SetTransformCommand` retain the old local mutation design. They are not used by the K2.3 ImGui frontend and remain only as migration debt. The active frontend selects `ArchitecturalObjectId`, reads `ClientReplicaStore` through `ReplicaEditorReadModel`, previews locally, and submits durable changes through `EditorCommandGateway`.

## Authoritative boundary

`ClientEditorCommandTransport` encodes allow-listed command requests and sends them through Fabric C2S networking. `AuthoritativeCommandGateway` validates protocol and revisions, decodes typed commands, and commits through `RuntimeTransaction`. Accepted parameter edits broadcast an authoritative Object Snapshot; runtime state changes broadcast State Delta messages. Rejections and revision conflicts are correlated by command ID and exposed through Diagnostics.

## Migration table

| Capability | Current implementation | Remaining action |
| --- | --- | --- |
| Selection | `ArchitecturalObjectId` snapshots with single/multi selection | world picking synchronization |
| Inspector | schema/value-driven property descriptors and real ImGui controls | broaden widget types and units |
| Command | async Fabric transport to server-authoritative gateway | history correlation and automatic resync request |
| History | projection and compensating-command contracts | populate accepted design history from responses |
| Preview | local overlay/edit session; one commit on gesture end | world-render preview proof |
| UI frontend | real Dear ImGui Dockspace, windows, GL3 backend and persistent ini layout | Chinese font, DPI and formal client smoke checklist |

## Remaining gaps

P0 gaps are world picking synchronization, complete response-driven History/Undo/Redo, automatic resync after revision conflict, and in-world proof of the full Width edit loop. Capability-driven Open/Close/Lock actions and bidirectional Fabric command transport are integrated. The legacy mutable editor package must still be migrated or removed.