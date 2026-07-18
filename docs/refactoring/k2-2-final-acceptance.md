# K2.2 Final Acceptance Audit

Date: 2026-07-18

## Acceptance status

| Scope | Status |
|---|---|
| K2.2 Core Runtime | Passed |
| K2.2 Dedicated Server Integration | Partially Passed |
| K2.2 World Lifecycle | Not Fully Proven |
| K2.2 Multiplayer Visual Verification | Not Proven |

This document does not declare K2.2 fully accepted. The dedicated-server test proves activation and one authoritative command commit, not the complete lifecycle in the original acceptance criteria.


## Implemented call chain

```text
Minecraft server OpeningBlock.useWithoutItem
  -> CommandRequestMessage(toggle_open, objectRevision, stateRevision, actor)
  -> FabricRuntimeLifecycle.submit
  -> AuthoritativeCommandGateway
  -> generic CommandBus + Opening family handler
  -> runtime transaction commit
  -> authoritative ObjectSnapshotMessage broadcast
  -> ClientPlayNetworking receiver
  -> ClientReplicaStore
```

The client replica is a projection only. There is no client API that can commit a `RuntimeState` to the server session.

## Acceptance evidence

- World placement creates a runtime snapshot and the BlockEntity activates or restores its session.
- Runtime sessions are the sole live authority; BlockEntity persists snapshots.
- Commands and ticks commit state through runtime transactions.
- Door render, collision, and picking have code-level kinematic integration from Phase 3, but their behavior in a running Minecraft world is not covered by the current GameTest.
- Command requests validate object and state revisions and are idempotent by command ID.
- Pure-Java two-replica loss/recovery tests prove delta-gap detection and resynchronization.
- Opening family runtime registration is inverted through `ArchitecturalFamilyPlugin`.
- Core runtime tests run without Minecraft startup.

## Minecraft environment verification

The dedicated-server Loom GameTest source set is enabled and `DoorRuntimeGameTests.blockEntityActivatesAndSnapshotsDoorSession` passes under Minecraft 26.1. It proves only that a real server world creates the Opening BlockEntity, activates one authoritative Door session from its snapshot, accepts a Door command through the authority gateway, and commits exactly one object revision.

The repository's automated JUnit suite, Fabric compilation, client compilation, architecture checks, and dedicated-server GameTest pass. Two simultaneously connected graphical clients have not been launched in this headless run, so visual multiplayer observation and dynamic collision/picking feel remain manual verification items.
The current Fabric interaction broadcast sends a full authoritative snapshot to all connected players after a successful interaction. This favors deterministic convergence over bandwidth efficiency and is not the target Editor steady-state transport. K2.3 continuous controls must keep drag/slider updates in local Preview state and submit one command only on gesture completion. Tracking-range filtering, steady-state deltas, and bounded EditBegin/PreviewUpdate/EditCommit/EditCancel sessions remain explicit P1 work in [Editor Interaction Transport](../architecture/editor/04-interaction-transport.md).

## Verification commands

```text
./gradlew test
./gradlew compileClientJava
./gradlew checkArchitecture
./gradlew :aperture-fabric:runGameTest
```

K2.3 Editor Shell may start in parallel, but the unresolved K2.2 verification backlog remains P0 and must not disappear from the roadmap or be treated as implicitly accepted by UI progress. See [K2.2 Open Verification Backlog](k2-2-open-verification.md).
