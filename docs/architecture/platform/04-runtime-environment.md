# Architectural Runtime Environment

**Status:** Foundation implemented; platform adapters in progress

`ApertureRuntime` is the operational entry point for live architectural objects. Generation, placement, and material services remain available, but they do not form the runtime loop.

```text
Actor / Automation / Simulation
              ↓
      RuntimeInteraction
              ↓
  ArchitecturalRuntimeEnvironment
              ↓
 Object Registry → Transaction Manager
              ↓
 Capability Resolution → Behavior Engine
              ↓
 State Transition → State Store
              ↓
 Events → Replication → Platform Effects
              ↑
 Tick Scheduler + Read-only World Query
```

## Implemented responsibilities

| Responsibility | Runtime component |
|---|---|
| Live object identity and lookup | `RuntimeObjectRegistry` |
| Persistence-facing state boundary | `RuntimeStateStore` |
| Family behavior dispatch | `RuntimeBehaviorEngine` |
| Capability discovery and enforcement | `RuntimePipeline.capabilities` and `RuntimeBehavior` |
| Actor-carrying interactions | `RuntimeInteraction` and `RuntimeActor` |
| Per-object atomic execution | `RuntimeTransactionManager` |
| Lifecycle notifications | `RuntimeEventBus` |
| Deterministic delayed work | `RuntimeTickScheduler` |
| Read-only light/weather/power/etc. access | `RuntimeWorldQuery` |
| Committed revision publication | `RuntimeReplicator` |
| Operational counters and last failure | `RuntimeDiagnostics` |

Behavior evaluation receives a `RuntimeEvaluationContext` containing the current runtime tick and the read-only world-query port. This keeps behavior deterministic in tests and prevents family code from importing Minecraft APIs.

## Transaction boundary

Interactions address an object by UUID rather than submitting an arbitrary stale snapshot. The environment resolves the latest registered revision inside an object-scoped transaction, evaluates behavior, validates identity and revision monotonicity, and commits to the state store. Unrelated objects may transition concurrently.

Events and replication happen only after state commit. A rejected capability or behavior transition leaves the registered snapshot unchanged and emits `InteractionRejected` diagnostics/event data.

## Platform adapter status

The Fabric bootstrap currently installs an empty world-query adapter and a no-op replicator. These are explicit incomplete adapters, not hidden Runtime behavior. The next platform slice must connect:

- `RuntimeWorldQuery` to server-safe Minecraft world snapshots;
- `RuntimeReplicator` to revision-aware network packets;
- runtime events/effects to Block Entity dirty marking and render invalidation;
- the server tick callback to `ApertureRuntime.tick()`.

Simulation remains a separate pipeline. A simulation solver reads immutable world snapshots and submits authorized runtime interactions; it never mutates `ArchitecturalObject` state directly.
