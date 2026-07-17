# K2 — Architectural Runtime Object

## Outcome

Upgrade the reference Door from a parametric geometry result into a runnable architectural object, while establishing reusable runtime contracts for every future family.

K2 builds Aperture's second main spine:

```text
Event / User / AI / Simulation
        -> Command
        -> Behavior
        -> State Transition
        -> Kinematics / World Effects
        -> Persistence / Replication
```

The existing Generation Pipeline remains the first spine and answers what an object looks like. K2 answers how an object operates.

## Scope order

1. Adopt `ArchitecturalObject` terminology and lifecycle boundaries.
2. Add family-neutral IDs, Definition, Instance, and Runtime Object contracts without removing `OpeningInstance`.
3. Add State Schema and Runtime State with persistent, transient, derived, replicated, server-only, and client-predicted properties.
4. Add capability contracts such as Openable, Lockable, Interactable, HostAware, Renderable, Collidable, Persistable, and Replicable.
5. Add platform-neutral events and references.
6. Add Behavior contracts whose outputs are Commands, State Patches, World Effects, and diagnostics.
7. Implement Commands, a Command Bus, validation, transactions, revisions, and event emission.
8. Establish World Query and World Effect boundaries.
9. Build the Door vertical slice from interaction through state, tick, kinematics, collision, persistence, and replication.
10. Add kinematic metadata so dynamic parts update transforms without rebuilding mesh every frame.
11. Persist only durable instance and state data; reconstruct transient and derived state.
12. Add server-authoritative command validation and state/event delta replication.

## Module direction

Do not create all proposed modules at once. Validate the contracts in one pure-Java module first:

```text
aperture-runtime-model
        -> aperture-runtime
        -> aperture-fabric

aperture-runtime-model
        <- aperture-opening
```

`aperture-runtime-model` owns object, state, capability, event, command, and behavior contracts. It must not depend on `aperture-opening` or Minecraft.

## Family freeze

Door, Fixed Window, and Curtain Wall are sufficient reference families. Until K2 acceptance, do not add Sliding Window, French Door, Garage Door, Skylight, additional curtain-wall libraries, advanced Boolean content, AI generation, NodeCraft integration, IFC export, simulation solvers, large UI rewrites, collaboration editing, or a visual Behavior Graph.

## Acceptance criteria

- Door is an instance of the generic ArchitecturalObject model.
- Door state is declared by StateSchema.
- Player, Editor, and AI operations use the same Command API.
- Behavior processes Commands/Events without importing Minecraft APIs or directly mutating the world.
- Dynamic door parts use kinematic transforms rather than per-frame mesh regeneration.
- Collision follows kinematic parts.
- The server owns authoritative state and revision.
- State, parameters, host bindings, and revision survive world reload.
- Two clients converge on the same state.
- Core runtime tests run without Minecraft.

## Delivery sequence

1. `docs: redefine ArchitecturalObject as runtime primitive`
2. `feat(runtime-model): add object identity and generic instance schema`
3. `feat(runtime-model): add state schema and runtime state`
4. `feat(runtime-model): add capability contracts`
5. `feat(runtime-model): add architectural event model`
6. `feat(runtime-model): add command contracts and transactions`
7. `feat(runtime-model): add behavior contracts and engine skeleton`
8. `feat(opening): define door state, capabilities and behaviors`
9. `feat(geometry): add kinematic part metadata`
10. `feat(runtime): add command-event-state execution loop`
11. `feat(fabric): route player interaction through command bus`
12. `feat(fabric): persist runtime object state in block entity`
13. `feat(network): add server-authoritative state replication`
14. `test: add door runtime vertical-slice tests`
15. `docs: update roadmap and implementation status`
