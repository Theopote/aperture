# K2.2 Runtime Vertical Slice Audit

Date: 2026-07-18

## Scope and method

This audit records the repository as implemented before the K2.2 runtime unification. It inspects module dependencies, concrete types, tests, and the actual Fabric-to-runtime call paths. An interface or unit test is not treated as platform integration.

## Current modules

| Module | Current responsibility | K2.2 observation |
| --- | --- | --- |
| `aperture-runtime-model` | Minecraft-free object, state, capability, event, behavior, command, persistence, and replication contracts | Contains most new contracts, but no `ArchitecturalRuntime`, `RuntimeObjectSession`, `RuntimeMutation`, or command-request protocol |
| `aperture-runtime` | Generation services plus the older live-object environment and replication/persistence adapters | Depends directly on `aperture-opening`; contains a second runtime pipeline based on `core.object.ArchitecturalObject` and `RuntimeInteraction` |
| `aperture-opening` | Opening generation and the new Door runtime schema, capabilities, behaviors, handlers, and vertical-slice tests | New Door model exists but is not the path used by Fabric world interaction |
| `aperture-kernel` | Generation-kernel composition and pipeline access | Generates static artifacts; it does not activate authoritative runtime sessions |
| `aperture-render` | Mesh/material/collision proxy abstractions | Does not consume runtime kinematic transforms in the Minecraft renderer |
| `aperture-editor` | Editor models, commands, gizmos, and selection | Depends on `aperture-runtime`; no ImGui dependency was found in kernel modules |
| `aperture-fabric` | Minecraft bootstrap, placement, block entity, NBT, and replication payload transport | Persists two representations and has no real door command receiver or runtime activation lifecycle |

Current dependency issue:

```text
aperture-runtime -> aperture-opening
```

This makes the generic runtime know the concrete Opening family. Family-plugin inversion is deferred until the world lifecycle is proven, as required by the task order.

## Required type inventory

| Required type | Current implementation | Finding |
| --- | --- | --- |
| `ArchitecturalRuntime` | Missing | `ApertureRuntime` is a generation/service facade, not the required lifecycle interface |
| `RuntimeObjectSession` | Missing | No single authoritative session aggregates instance, state, capabilities, behaviors, kinematics, revisions, dirty flags, and snapshot |
| `RuntimeState` | `runtime-model.state.RuntimeState`; also `core.state.RuntimeState` | Duplicate state models exist |
| `StatePatch` | `runtime-model.state.StatePatch` | Implemented and schema-validated by `RuntimeState.apply`, but not used by the active Fabric path |
| `CommandBus` | `runtime-model.command.CommandBus` / `DefaultCommandBus` | One new command bus exists; Fabric does not submit player interaction to it |
| `CommandTransaction` | `runtime-model.command.CommandTransaction` | Implemented as a command batch, not the required atomic runtime-state transaction |
| `BehaviorEngine` | `runtime-model.behavior.BehaviorEngine` | Implemented, while the old path also has `runtime.behavior.RuntimeBehaviorEngine` |
| `RuntimeMutation` | Missing | Behavior results and old runtime transitions currently represent competing mutation paths |
| `KinematicEvaluator` | `geometry.kinematic.KinematicEvaluator` | Implemented and unit tested, but not connected to Fabric render, collision, or picking |
| `ArchitecturalObjectSnapshot` | `runtime-model.persistence.ArchitecturalObjectSnapshot` | Implemented with JSON and Fabric NBT codecs |
| `ReplicationMessage` | `runtime-model.replication.ReplicationMessage` | Snapshot, state delta, event delta, and removal variants exist |
| `CommandRequestMessage` | Missing | Client-to-server authoritative command protocol is absent |
| `ObjectSnapshotMessage` | Implemented | Codec and pure-Java model exist |
| `StateDeltaMessage` | Implemented | Contiguous revision checks exist |
| `EventDeltaMessage` | Implemented | Sequence checks exist |
| `OpeningBlockEntity` | Implemented | Stores both legacy `OpeningInstance` and a runtime snapshot; it does not activate/restore a session |

## Actual player-right-click call chain

There is currently no implemented right-click-to-runtime chain.

The concrete Fabric anchor block, `OpeningBlock`, implements render shape, selection shape, collision shape, and block-entity creation only. It does not override a use/interaction method. No Fabric client-to-server `CommandRequest` receiver exists. Therefore the real chain stops at Minecraft's default block handling:

```text
Player right-click
  -> Minecraft/Fabric block handling
  -> OpeningBlock (no interaction override)
  -> no CommandRequest
  -> no CommandBus
  -> no authoritative state transaction
  -> no replication broadcast caused by the interaction
```

The repository has two platform-neutral demonstrations, neither integrated with that Fabric entry:

```text
Legacy test/runtime path
ApertureRuntime.interact
  -> ArchitecturalRuntimeEnvironment.interact
  -> RuntimeTransactionManager (striped lock only)
  -> RuntimePipeline.process
  -> RuntimeBehaviorEngine
  -> OpeningRuntimeBehavior
  -> OpeningInstance.state().transition + withState
  -> RuntimeObjectRepository.save
  -> RuntimeReplicator
```

```text
New Door unit-test path
PlayerInteractEvent
  -> runtime-model BehaviorEngine
  -> ManualDoorInteractionBehavior
  -> RequestOpenCommand / RequestCloseCommand
  -> DefaultCommandBus
  -> Door command handler
  -> StatePatch
  -> test applies/ticks state
```

The new path proves contracts in pure Java, but no unified owner commits the patch and no Fabric adapter invokes it.

## Downstream world paths

### Render

`OpeningInstanceRenderer` reads the legacy `OpeningInstance` from `OpeningBlockEntity`, resolves a cached static asset, and submits one object transform. It does not read `ArchitecturalObjectSnapshot`, a runtime session, or `KinematicEvaluator` output. Door-panel motion is therefore not integrated.

### Collision and picking

`OpeningBlock.getCollisionShape` and `getShape` both return `Shapes.empty()`. Render-side collision proxies exist but are not used by the Fabric block. Existing raycasts cover placement and editor gizmos, not runtime door-panel picking. Dynamic collision and picking are absent.

### Persistence

`OpeningBlockEntity` independently stores a legacy `OpeningInstance` and an optional `ArchitecturalObjectSnapshot`. Load decodes fields into the block entity only; it does not call a runtime restore operation. Save serializes its cached fields rather than asking a live authoritative session for a fresh snapshot. Unload and remove semantics are not implemented.

### Replication

Fabric registers a client-bound payload and has a server-side `FabricReplicationSink`. JSON replication encoding exists. There is no command-request payload, server receiver/validator, client receiver applying messages, resync request, unload message, or duplicate-command ledger. The newly added pure-Java `ClientReplicaStore` demonstrates deterministic snapshot/delta application but is not yet Fabric-integrated.

## Duplicate and competing implementations

| Area | Competing paths | Risk |
| --- | --- | --- |
| Runtime state | `core.state.RuntimeState` and `runtime-model.state.RuntimeState` | Different semantics can remain authoritative in different paths |
| Runtime behavior | `runtime.behavior.RuntimeBehaviorEngine` / `OpeningRuntimeBehavior` and `runtime-model.behavior.BehaviorEngine` / Door behaviors | Player, editor, and tests can execute different logic |
| State mutation | Legacy `OpeningState.transition` + `OpeningInstance.withState`; new handlers return `StatePatch` | State does not have one transaction-only write chain |
| Runtime owner | `ApertureRuntime`, `ArchitecturalRuntimeEnvironment`, repositories, and `OpeningBlockEntity` | No unique authoritative `RuntimeObjectSession` |
| Persistence | Legacy `OpeningInstanceNbtCodec` plus snapshot NBT/JSON codecs | Block entity stores two representations that can diverge |
| Door state driver | `OpeningRuntimeBehavior` immediately changes `openRatio`; new Door handlers/behavior/tick use target and motion | Runtime motion can differ by entry point |
| Replication | Legacy `RuntimeReplicator` and new `ReplicationMessage`/`ReplicationSink` model | No single committed-change source drives network output |

Only one `runtime-model` `CommandBus` was found. JSON and NBT snapshot codecs are transport-specific encodings of the same snapshot contract, but the legacy Opening codec is a separate persistence path. No evidence was found that a client currently derives and commits authoritative Door state; the larger issue is that no client/server command path exists.

## Migration table

| Capability | Current implementation | Target implementation | Action |
| --- | --- | --- | --- |
| Runtime entry | `ApertureRuntime` + `ArchitecturalRuntimeEnvironment` | `ArchitecturalRuntime` / `DefaultArchitecturalRuntime` | Refactor and retain generation facade compatibility temporarily |
| Runtime session | Registry stores whole legacy objects | Unique `RuntimeObjectSession` per active object | Add repository and session; make it authoritative |
| State mutation | `OpeningState.transition`, `withState`, repository save, and isolated `StatePatch` tests | Command -> Behavior -> `RuntimeMutation` -> `RuntimeTransaction` | Migrate; prohibit direct production writes |
| Door interaction | No Fabric path; legacy `RuntimeInteraction` exists in tests/services | Client `CommandRequestMessage` -> server runtime submit | Implement after runtime/transaction phases |
| Door behavior | Two behavior engines and two Door paths | Opening plugin registers one Door state machine | Reuse new schema/handlers, remove legacy path after integration |
| Tick | Scheduler runs callbacks; Door tick exists only in pure-Java model | Runtime ticks active sessions transactionally | Add to unified runtime |
| Kinematics | Evaluator and metadata are isolated | Session pose consumed by render/collision/picking | Integrate after Door lifecycle is proven |
| Persistence | Block entity caches legacy instance and optional snapshot | Block entity anchors ID and saves/restores session snapshots | Migrate; keep legacy decode only as migration adapter |
| Replication | Server sink plus message/codec contracts | Committed runtime changes produce deltas; clients resync on gaps | Complete after authoritative runtime |
| Removal | Environment `unregister` only | Distinct `unload` and `remove` lifecycle | Add explicit operations |
| Family dependency | Runtime directly depends on Opening | `ArchitecturalFamilyPlugin` registration | Defer until world lifecycle tests pass |

## Phase 1 implementation decisions

1. Build `ArchitecturalRuntime` around the `runtime-model` identity/state/command contracts; do not extend the legacy `RuntimeInteraction` path.
2. Introduce `RuntimeObjectSession` and `RuntimeObjectRepository` as the only active authoritative object store.
3. Adapt or retire `ArchitecturalRuntimeEnvironment` behind compatibility boundaries instead of creating another public runtime facade.
4. Reuse the new Door schema, capabilities, handlers, behaviors, persistence snapshot, replication messages, and kinematic model.
5. Add `DoorRuntimeLifecycleTest` before connecting Fabric.
6. Keep the current pure-Java replica-store work unintegrated until Phase 5; it does not satisfy multiplayer or Fabric integration by itself.

## Audit conclusion

K2 contracts are substantially implemented and unit tested, but K2.2 is not an integration-completion exercise yet: the real Minecraft interaction chain is absent, the block entity is a second state holder, the old and new runtime paths compete, and dynamic pose consumers are disconnected. The next valid implementation step is Phase 1: establish the unified runtime/session/repository lifecycle and prove it with a Door lifecycle test.

## Phase 6 update

The deferred family dependency inversion is complete. perture-runtime no longer has a production dependency on perture-opening; Door runtime composition now lives in perture-opening-runtime and is registered through ArchitecturalFamilyPluginRegistry. See [K2.2 Phase 6: Family Plugin Decoupling](k2-2-phase-6-family-plugin.md).
