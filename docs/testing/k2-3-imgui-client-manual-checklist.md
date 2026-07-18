# K2.3 ImGui client manual checklist

The native ImGui/GLFW/OpenGL adapter is not yet automated and must not be reported as passing. Once the binding can be resolved, verify:

- Start a client and join a world without loading ImGui classes on a dedicated server.
- Toggle the editor with F4; confirm all six dockspace regions open and close.
- Move, close, reopen, persist and reset windows at 100%, 150% and 200% UI scale.
- Type Chinese text and confirm the configured user/system font contains required glyphs.
- With an input field active, confirm movement, attack and global editor shortcuts are suppressed appropriately.
- Close the editor and confirm mouse capture, keyboard movement and cursor mode are fully restored.
- Render water, entities, translucent blocks and the editor consecutively; inspect for leaked blend, depth, scissor, viewport, shader and texture state.
- Edit door width continuously; confirm local preview updates and exactly one network command is emitted at gesture completion.
- Reject that command on a revision conflict; confirm preview rollback, diagnostic display and replica resync.
- Exercise Open, Close, Lock and Unlock with two clients and confirm both replicas and world geometry converge.
