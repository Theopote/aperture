# K2.3 ImGui client manual checklist

The Dear ImGui/GLFW/OpenGL adapter is client-integrated and compile-verified. Visual state compatibility still requires manual testing on the render thread and must not be reported as passing until every item below is exercised.

## Editor and input

- Start a client and join a world without loading ImGui classes on a dedicated server.
- Toggle the editor with F4; confirm all dockspace regions open and close.
- Move, close, reopen, persist and reset windows at 100%, 150% and 200% UI scale.
- Toggle F1 while the editor is open and closed; confirm Minecraft HUD visibility remains correct.
- With an input field active, confirm movement, attack and global editor shortcuts are suppressed appropriately.
- Close the editor and confirm mouse capture, keyboard movement and cursor mode are fully restored.

## RenderStateGuard compatibility matrix

Perform each case while repeatedly opening and closing F4 and moving/resizing docked panels. Look for black frames, missing textures, incorrect transparency, clipping, depth errors, shader corruption and persistent artifacts after closing the editor.

- Water surface above and below water.
- Translucent and stained glass in front of entities and Aperture geometry.
- Living entities, block entities and entity shadows.
- Dense particles with additive and alpha blending.
- Enchanted-item glint in hand, inventory and world.
- Day, sunset, night sky, clouds and weather.
- Fullscreen/windowed transitions and window resize.
- Alt+Tab away from and back to the game.
- Resource-pack reload while the editor is open, then while it is closed.

The guard currently snapshots and restores draw/read framebuffer and buffer, viewport, scissor, blend state/functions/equations, depth test/mask, cull, color mask, shader program, active and unit-zero texture/sampler bindings, VAO, array/element buffers, pack/unpack state and polygon mode. Font texture initialization is guarded as well as every ImGui draw submission.

## Authority and preview

- Edit door width continuously; confirm local world preview updates and exactly one network command is emitted at gesture completion.
- Accept the command; confirm no old-geometry frame appears between release and the authoritative Snapshot.
- Reject the command; confirm preview rollback and diagnostic display.
- Force a revision conflict; confirm the overlay remains during resync and rolls back only after the Object Snapshot arrives.
- Exercise Open, Close, Lock and Unlock with two clients and confirm both replicas and world geometry converge.