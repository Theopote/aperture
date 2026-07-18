# 01 — Editor Kernel

**Layer:** Client Editor
**Status:** K2.3 normative architecture
**Dependencies:** runtime-model contracts, client replica store, editor projection, command gateway

## Authority rule

The Editor is not an object owner. It never mutates an `OpeningInstance`, `ArchitecturalObjectInstance`, `RuntimeState`, or server session. Its inputs are immutable client replicas and local, disposable preview projections. Every durable edit is submitted as an architectural command and becomes visible only after an authoritative replication message is applied.

```text
ImGui Widget
    ↓
Editor Intent
    ↓
EditorCommandGateway.submit(...)
    ↓
ArchitecturalCommand / CommandRequestMessage
    ↓
AuthoritativeCommandGateway
    ↓
Server Runtime Transaction
    ↓
Replication Message
    ↓
ClientReplicaStore
    ↓
EditorProjection / ViewModel refresh
```

The following design is forbidden:

```text
Widget → mutable local domain object → local history mutation
```

## Selection model

Selection contains stable references, never mutable domain objects.

```java
public record EditorSelection(
    Set<ArchitecturalObjectId> objectIds,
    Set<ComponentSelection> components
) {
    public record ComponentSelection(
        ArchitecturalObjectId objectId,
        ComponentPath componentPath
    ) { }
}
```

Rules:

- Object selection uses `ArchitecturalObjectId`.
- Sub-object selection uses `(ArchitecturalObjectId, ComponentPath)`.
- The selection service resolves IDs against `ClientReplicaStore` on demand.
- A missing replica clears or marks a selection stale; it does not retain an old mutable object.
- Picking returns IDs and component paths.

## Client replica and editor projection

`ClientReplicaStore` is the replicated source of truth on the client. The Editor derives a read-only projection for widgets and gizmos.

```java
public record EditorProjection(
    ArchitecturalObjectId objectId,
    long objectRevision,
    StateRevision stateRevision,
    ParameterSet parameters,
    Transform3d transform,
    Map<ComponentPath, Transform3d> componentPoses
) { }
```

`EditorProjection` is replaceable cache data. It may be rebuilt after every accepted snapshot or delta. It must not expose mutation methods or be persisted as authority.

## Editor intents

Widgets and manipulators emit UI-level intent. Intent describes the user's goal without changing replicated state.

```java
public sealed interface EditorIntent permits
    SetParameterIntent, SetTransformIntent, DeleteObjectIntent {
    ArchitecturalObjectId objectId();
}

public record SetParameterIntent(
    ArchitecturalObjectId objectId,
    String parameterName,
    ParameterValue value
) implements EditorIntent { }
```

Manipulators keep only transient drag state:

```java
public interface Manipulator {
    void begin(EditorProjection base, ComponentPath handle);
    PreviewPatch update(DragEvent event);
    EditorIntent finish();
    void cancel();
}
```

During drag, `PreviewPatch` may update a local ghost or gizmo. It is never inserted into `ClientReplicaStore` and never advances authoritative revisions.

### Continuous input policy

Inspector sliders and gizmos must not submit on every rendered frame. Widget activation captures the base projection; intermediate values update only PreviewPatch; mouse release/deactivation submits one final intent; cancellation discards the preview and submits nothing. See [04-interaction-transport.md](04-interaction-transport.md).

## Command gateway

The GUI depends on one gateway boundary.

```java
public interface EditorCommandGateway {
    CompletionStage<EditorSubmission> submit(EditorIntent intent);
    CompletionStage<EditorSubmission> undo(EditorHistoryEntry entry);
    CompletionStage<EditorSubmission> redo(EditorHistoryEntry entry);
}
```

The gateway:

1. resolves the latest client replica;
2. converts intent to an allow-listed `ArchitecturalCommand` request;
3. includes expected object and state revisions;
4. submits to the authoritative server boundary;
5. reports accepted, rejected, conflict, and resync-required outcomes;
6. never edits the local replica directly.

A revision conflict causes refresh/resync and requires the user action to be reconsidered against the new projection. The Editor must not silently overwrite newer server state.

## Inspector and gizmo flow

```text
Replica update
  → rebuild EditorProjection
  → refresh inspector and gizmo

Widget edit / gizmo release
  → EditorIntent
  → EditorCommandGateway
  → pending UI state
  → authoritative response and replication
  → rebuild EditorProjection
```

Pending UI state may display a spinner, optimistic ghost, or proposed value. The committed widget value always comes from the replica.

## Live preview

Geometry preview may execute locally from a copied parameter set because it is disposable visualization. Preview must satisfy all of these:

- derived from a known replica revision;
- stored outside the authoritative replica;
- visually marked pending when a command is in flight;
- discarded on rejection or revision conflict;
- replaced by the projection produced from the accepted replication message.

## Undo and redo

Undo is not memory rollback. The Editor history records accepted operations and the data needed to construct a compensating intent. `Ctrl+Z` submits that compensating command through `EditorCommandGateway`; `Ctrl+Shift+Z` submits a new forward command. Details are normative in [03-command-history.md](03-command-history.md).

## Threading

- Network receivers apply messages on the Minecraft client thread.
- Projection rebuilds observe an atomic replica revision.
- ImGui rendering reads immutable projections.
- Background preview generation receives immutable input snapshots and publishes results only if the base revision is still current.

## Acceptance criteria

- [ ] No Editor selection stores `OpeningInstance` or another mutable domain object.
- [ ] Object selection is `ArchitecturalObjectId`; component selection includes `ComponentPath`.
- [ ] Every durable widget/gizmo edit goes through `EditorCommandGateway`.
- [ ] Client replica updates only from replication messages.
- [ ] Rejection and revision conflict discard pending preview state.
- [ ] Undo and redo submit compensating/forward commands.
- [ ] Tests prove that Editor actions cannot directly advance server or replica revisions.
- [ ] One completed drag/slider gesture submits at most one authoritative command.
- [ ] A cancelled gesture submits no command and creates no history entry.

## Migration note

Legacy `DesignSession`, `EditCommand`, and local history implementations may remain temporarily for non-world generation drafts only. They must not be wired to placed runtime objects. Any adapter must terminate at `EditorIntent` and the authoritative gateway.
