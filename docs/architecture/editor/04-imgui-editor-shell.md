# ImGui editor shell

## Status: concrete UI implemented, client smoke proof pending

Aperture now contains a concrete Dear ImGui implementation based on `imgui-java` 1.86.12. `DearImGuiEditor` calls `ImGui.begin()`, `ImGui.dockSpace()` and DockBuilder APIs for the first-run layout.

## Implemented

- `imgui-java-binding`, `imgui-java-lwjgl3` and Windows/Linux/macOS native declarations.
- Full-screen transparent docking root.
- Concrete Object Outliner, Inspector, Runtime State, Command History and Diagnostics windows.
- Default left/right/bottom dock layout.
- GLFW and OpenGL3 backend initialization in the Minecraft client source set.
- F4 client key binding and transparent non-pausing editor screen.
- Client shutdown disposal and basic viewport/scissor restoration.
- Headless Editor Model, replica projection and preview/command boundaries.

## Proven

- Editor and editor-imgui unit tests.
- Offline `compileClientJava`.
- Architecture checks.

## Not yet proven / remaining P0

- A real Minecraft client smoke run has not yet been completed.
- The Fabric editor command transport is intentionally not connected; UI writes are rejected rather than mutating replicas.
- Inspector properties are displayed through real ImGui widgets but are currently read-only.
- Chinese font atlas and broader OpenGL state restoration remain to be implemented.
- Packaging/remap verification for a distributable client jar remains.

## Target flow

`ArchitecturalObjectId -> ClientReplicaStore -> EditorReadModel -> Dear ImGui widgets -> EditorCommandGateway -> authoritative server runtime`

Client Integrated: **compiled**. World Proven: **No**. K2.3 full acceptance: **Not met**.
