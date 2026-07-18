# 04 — Editor Interaction Transport

**Priority:** P1 replication/editor scalability

## Initial K2.3 policy

Continuous UI input is local preview, not a stream of authoritative commands.

```text
mouse down / slider activation
  → capture replica revision and preview base

drag / slider frames
  → update local PreviewPatch only
  → no CommandRequestMessage
  → no replication snapshot

mouse release / slider deactivation-after-edit
  → create one final EditorIntent
  → submit one authoritative command
  → wait for replication

Escape / cancelled widget
  → discard PreviewPatch
  → submit nothing
```

This rule applies to Inspector sliders, numeric drags, gizmo translation/rotation/resize, multi-object transforms, and any high-frequency widget. Debouncing may reduce preview generation work, but it does not turn intermediate values into committed network edits.

## Why this is mandatory

The current Fabric steady path broadcasts a complete authoritative snapshot to every connected player after a successful interaction. Sending a command for every ImGui frame would multiply command validation, runtime transactions, snapshot encoding, network traffic, replica rebuilds, geometry invalidation, and history entries.

K2.3 must remain usable before replication optimization by committing once per completed gesture.

## Interaction session model

The future transport protocol should support:

```text
EditBegin
PreviewUpdate
EditCommit
EditCancel
```

Suggested semantics:

| Message | Authority effect | Replication/persistence |
|---|---|---|
| `EditBegin` | validates actor, target, base revisions, and optional edit lease | no durable state change |
| `PreviewUpdate` | optional ephemeral collaborator preview; rate-limited and replaceable | never persisted; never advances object/state revision |
| `EditCommit` | submits one final architectural command/transaction | authoritative revision and normal replication |
| `EditCancel` | releases ephemeral session state | no durable state change |

An `editSessionId` correlates messages. The server may reject or expire sessions. `PreviewUpdate` must not use `StateDeltaMessage`, because it is not committed authoritative state.

## Replication evolution

P1 follow-up work:

- broadcast only to players tracking the relevant object/chunk;
- use contiguous `StateDeltaMessage` as the normal steady-state path;
- reserve `ObjectSnapshotMessage` for initial sync, recovery, protocol upgrade, and explicit resync;
- batch/coalesce tick and editor deltas where safe;
- apply rate and payload limits to ephemeral preview updates;
- test many objects, multiple editors, high-frequency widgets, dropped deltas, and resync;
- collect snapshot/delta byte counts and messages per gesture.

## Multi-object editing

A multi-object drag maintains one local preview group. Mouse release submits one atomic composite command when all-or-nothing behavior is required. It must not emit one full snapshot per object per frame.

## Acceptance criteria

- [ ] No Inspector or gizmo frame emits a durable command.
- [ ] One completed gesture creates at most one history entry and one authoritative transaction.
- [ ] Cancelled gestures create neither command nor history.
- [ ] Intermediate preview values cannot enter `ClientReplicaStore`.
- [ ] Tracking-range filtering is covered by Fabric integration tests.
- [ ] Normal committed edits use deltas; snapshots are recovery/bootstrap messages.
- [ ] Edit-session preview traffic is ephemeral, bounded, and revision-aware.
