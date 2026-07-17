# Runtime Pipeline

**Status:** First vertical slice implemented

The Runtime Pipeline begins after an architectural object has been placed. It does not generate geometry and it does not add stages to the Generation Pipeline.

```text
Placed ArchitecturalObject
        ↓
Capability Resolution
        ↓
Behavior Evaluation
        ↓
Interaction
        ↓
State Transition
        ↓
World Effects
        ↓
Repository Commit
        ↓
Persistence / Replication Requests
```

## Ownership boundary

- `aperture-runtime` owns platform-neutral capability discovery, behavior evaluation, revisioned state transitions, and effect descriptions.
- Family behaviors retain strong types. The first implementation is `OpeningRuntimeBehavior`; unrelated families will register their own behavior rather than become openings.
- `aperture-fabric` consumes effects and performs Minecraft world mutation, block-entity persistence, and packet replication.
- `aperture-kernel` remains the authoritative Generation Pipeline entry point. Runtime state changes may invalidate generated geometry, but generation is not a Runtime Pipeline stage.

## First slice

Door instances expose `aperture:open`, `aperture:close`, and `aperture:toggle`. A successful transition:

1. creates a new immutable `OpeningInstance` snapshot;
2. advances its revision;
3. commits it through `RuntimeObjectRepository`;
4. emits geometry invalidation, persistence, and replication effects.

Fixed windows expose none of these door capabilities. Repeating an already-satisfied action is idempotent: it does not advance the revision, write the repository, or emit effects.

## Next slices

Actor authorization, redstone and sensor inputs, time-based transitions, effect consumers in Fabric, behavior definitions in the atomic Kernel resource snapshot, and event history/replay should extend this pipeline. Simulation remains a separate producer of runtime interactions and must not mutate objects directly.
