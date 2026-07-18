# K2.2 Open Verification Backlog

Date: 2026-07-18

Priority: P0 until K2.2 full acceptance. K2.3 Editor Shell may proceed in parallel; it does not close or lower the priority of these items.

## World lifecycle — not fully proven

- [ ] Runtime Tick advances `openRatio` continuously in a Minecraft server world.
- [ ] Door panel render pose visibly follows the runtime kinematic transform.
- [ ] Collision shape follows the moving Door panel.
- [ ] Picking/selection follows the moving Door panel.
- [ ] A real chunk unload removes the active session without deleting durable state.
- [ ] A real world save writes the latest authoritative snapshot to BlockEntity NBT.
- [ ] A real BlockEntity reload reconstructs its snapshot from NBT.
- [ ] Runtime Session restores from reloaded NBT with identity, parameters, host bindings, state, and revisions preserved.

## Multiplayer — not proven in Fabric environment

- [ ] Two real connected clients observe the same Door pose and state.
- [ ] An intentionally skipped Fabric delta triggers a resync request.
- [ ] A server snapshot response repairs the stale Fabric client replica.
- [ ] Duplicate Fabric command delivery does not execute twice.
- [ ] Revision-conflict rejection reaches the Fabric client boundary.

## Required scenarios

1. Place Door, interact, tick through partial and fully open poses, and verify render, collision, and picking.
2. Save at a non-terminal ratio, unload chunk/world, reload, and verify snapshot/session equality.
3. Connect two clients, interact from client A, and verify client B converges.
4. Drop a state delta for client B, observe `RESYNC_REQUIRED`, request a snapshot, and verify convergence.
5. Replay one command ID and submit one stale revision through Fabric transport; verify idempotency and rejection.

## Exit rule

K2.2 is fully accepted only when these scenarios have repeatable automated coverage where feasible and any unavoidable visual checks have recorded manual evidence.
## P1 replication scalability carried into K2.3

- [ ] Filter Fabric replication recipients by object/chunk tracking range.
- [ ] Use `StateDeltaMessage` as the normal committed steady-state path.
- [ ] Reserve full snapshots for bootstrap, recovery, upgrade, and resync.
- [ ] Add bounded `EditBegin`, ephemeral `PreviewUpdate`, `EditCommit`, and `EditCancel` interaction sessions.
- [ ] Verify Inspector/gizmo gestures send one durable commit rather than one command per frame.
- [ ] Add load tests for many dynamic objects, multi-selection, and multiple editors.

These P1 items do not block the first Editor shell, but the initial UI must follow the local-preview/single-commit policy in [Editor Interaction Transport](../architecture/editor/04-interaction-transport.md).