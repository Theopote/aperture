# K2.2 Final Acceptance Audit

Date: 2026-07-18

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
- Door render, collision, and picking use the session kinematic pose implemented in Phase 3.
- Command requests validate object and state revisions and are idempotent by command ID.
- Pure-Java two-replica loss/recovery tests prove delta-gap detection and resynchronization.
- Opening family runtime registration is inverted through `ArchitecturalFamilyPlugin`.
- Core runtime tests run without Minecraft startup.

## Honest remaining environment verification

The repository's automated JUnit suite, Fabric compilation, client compilation, and architecture checks pass. The existing Loom GameTest sources are excluded from ordinary JUnit execution and have not been executed in a launched Minecraft dedicated-server harness during this run. Consequently, in-game interaction, chunk reload, dynamic collision/picking feel, and two real connected clients still require a manual or Loom server GameTest pass before calling K2.2 production-verified.

The current Fabric interaction broadcast sends a full authoritative snapshot to all connected players after a successful interaction. This favors deterministic convergence over bandwidth efficiency. Tracking-range filtering and steady-state delta broadcasts are follow-up optimizations; the platform-neutral delta/resync protocol is already tested.

## Verification commands

```text
./gradlew test
./gradlew compileClientJava
./gradlew checkArchitecture
```

The next product phase remains K2.3 Editor Shell only after the Minecraft environment verification above is completed.
