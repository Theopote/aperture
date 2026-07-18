# ImGui editor shell

## Status: architecture only

Current implementation is an **ImGui-compatible editor frontend architecture**. It is not a Dear ImGui implementation.

There is currently no `imgui-java` dependency, native library, ImGui context, `ImGui.begin()`, `ImGui.dockSpace()`, concrete widget rendering, GLFW backend, or OpenGL draw-data renderer. `ApertureImGuiEditor` only composes window models and delegates them to an injected `WindowRenderer`. `MainDockspace` is a logical window composition, not a Dear ImGui dockspace.

## Implemented

- Headless editor session contracts and stable-ID selection.
- Replica/read-model projection and local preview overlay.
- Command gateway, diagnostics and compensating-command contracts.
- Window-facing models for Outliner, Inspector, Runtime State, History and Diagnostics.
- Backend, input, render-state and workspace persistence interfaces.
- Unit tests for these platform-neutral contracts.

## Not implemented - P0

- Dear ImGui binding and native artifacts.
- Dear ImGui context and font atlas.
- Real docking root and window/widget calls.
- GLFW event backend and Minecraft input bridge.
- OpenGL draw-data rendering and concrete state restoration.
- Fabric render-loop integration and client smoke proof.

## Target flow

`ArchitecturalObjectId -> ClientReplicaStore -> EditorReadModel -> Dear ImGui widgets -> EditorCommandGateway -> authoritative server runtime`

Client Integrated: **No**. World Proven: **No**. K2.3 ImGui UI acceptance: **Not met**.
