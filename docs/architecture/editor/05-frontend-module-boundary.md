# Editor Frontend Module Boundary

Date: 2026-07-18

The Editor UI is split into three one-way layers:

```text
aperture-editor
  pure editor model, selection, projections, intents, command gateway ports
        ^
        |
aperture-editor-imgui
  ImGui windows, widgets, layout, and frame composition
        ^
        |
aperture-fabric client source set
  Minecraft lifecycle, GLFW, input, render-loop and OpenGL bridge
```

## Dependency rules

- `aperture-editor` must not import or depend on ImGui, Minecraft, GLFW, LWJGL, or OpenGL.
- `aperture-editor-imgui` depends only on `aperture-editor` among Aperture modules. It may own ImGui bindings and native declarations, but must not import Minecraft or GLFW.
- `aperture-fabric` is the only module allowed to integrate `aperture-editor-imgui`; that integration belongs in its client-only source set.
- The Fabric dedicated server and GameTest classpaths must remain able to start without loading any ImGui, GLFW, or OpenGL class.

The initial module contains only the frontend frame boundary. Concrete ImGui dependencies and windows are added there when UI implementation begins; they are never added to `aperture-editor`.
