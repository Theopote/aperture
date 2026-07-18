# 03 — Authoritative Editor Command History

**Layer:** Client Editor coordination
**Status:** K2.3 normative architecture
**Depends on:** `EditorCommandGateway`, authoritative runtime commands, replication revisions

## Principle

History is a journal of user intent and confirmed server outcomes. It is not a stack of closures that mutate local objects.

```text
Undo request
  → build compensating EditorIntent
  → EditorCommandGateway.submit
  → server validation and transaction
  → replication
  → client projection refresh
```

No history operation may restore an old object reference, replace `RuntimeState`, or directly change parameters/transforms in client memory.

## History entry

```java
public record EditorHistoryEntry(
    UUID originalCommandId,
    ArchitecturalObjectId objectId,
    EditorIntent forwardIntent,
    CompensationDescriptor compensation,
    long acceptedObjectRevision,
    StateRevision acceptedStateRevision,
    EntryStatus status
) { }
```

A history entry is added to the undo timeline only after `CommandAcceptedMessage` and the corresponding replication revision are observed. Rejected and cancelled intents may be shown in diagnostics but are not undoable committed history.

`CompensationDescriptor` contains semantic data needed to request the inverse operation, for example the previously confirmed parameter value or transform. It does not contain a mutable object snapshot to write back locally.

## Submit

```java
public CompletionStage<EditorSubmission> submit(EditorIntent intent) {
    return gateway.submit(intent).thenApply(result -> {
        if (result.accepted()) history.recordAccepted(intent, result);
        return result;
    });
}
```

The gateway derives expected revisions from the latest replica at submission time. Pending entries are correlated by command ID and remain visually distinct from committed history.

## Undo

```java
public CompletionStage<EditorSubmission> undo() {
    EditorHistoryEntry entry = history.peekUndo();
    return gateway.undo(entry).thenApply(result -> {
        if (result.accepted()) history.recordCompensation(entry, result);
        return result;
    });
}
```

Undo submits a new command with a new command ID. It can be rejected because permissions, constraints, host state, or object revisions changed after the original edit. A rejected undo leaves both server state and history cursor unchanged and refreshes/resyncs the projection when required.

Examples:

| Original accepted intent | Compensation intent |
|---|---|
| Set width from 900 to 1200 | Set width to the previously confirmed 900 |
| Move from transform A to B | Request transform A |
| Lock Door | Request unlock, subject to current permissions |
| Create object | Request deletion of that object |
| Delete object | Request recreation from an allowed durable definition/snapshot; may be unsupported |

Not every command is automatically reversible. Commands declare compensation support explicitly.

## Redo

Redo submits a new forward command based on the original semantic intent. It is not replay of the original envelope because command IDs, revisions, permissions, and validation context must be current.

```java
public CompletionStage<EditorSubmission> redo() {
    return gateway.redo(history.peekRedo());
}
```

## Revision and collaboration behavior

- History is per actor/editor session, while authority is shared.
- Accepted revisions come from the server response and replica stream.
- Other actors may edit the same object between an operation and its undo.
- The server decides whether compensation is valid at the current revision.
- On `REVISION_CONFLICT`, the Editor requests/resolves a fresh projection and does not force rollback.
- Command ID idempotency prevents a retried submission from creating two history commits.

## Merging continuous edits

Slider and drag samples may be coalesced before submission or represented by one preview session and one final intent. Once multiple commands have been accepted by the server, the client must not rewrite server history by locally merging them.

Recommended drag flow:

```text
mouse down  → capture base projection and revision
mouse move  → update disposable PreviewPatch only
mouse up    → submit one final EditorIntent
accepted    → add one history entry
```

## Composite operations

A multi-object edit is submitted as an authoritative transaction when atomicity is required. Its compensation is another transaction. The client must not iterate local objects and partially undo them.

```java
public record CompositeEditorIntent(
    List<EditorIntent> operations,
    boolean atomic
) implements EditorIntent { }
```

## Persistence and audit

Optional persisted history stores command IDs, actor, semantic intents, accepted revisions, timestamps, and outcomes. It is an audit/replay aid, not a state persistence mechanism. Runtime snapshots remain the durable state source.

## UI behavior

- Undo/Redo buttons show the next semantic operation.
- Pending operations cannot be undone until accepted or cancelled.
- Rejected compensation shows a structured reason.
- A resync indicator remains visible until the projection reaches the authoritative revision.
- The history panel distinguishes original operations from compensation commands.

## Tests

Required tests:

1. Accepted edit appears in history only after authoritative acceptance.
2. Rejected edit never advances the undo cursor.
3. Undo submits a new compensation command and does not mutate the replica directly.
4. Redo uses a new command ID and current expected revisions.
5. Revision conflict preserves state and requests projection refresh.
6. Duplicate accepted response does not create duplicate history entries.
7. Concurrent remote edit can cause compensation rejection without corrupting history.
8. Atomic composite compensation is all-or-nothing.

## Forbidden patterns

- A history entry holding a mutable placed object.
- Local `execute/undo` methods that change domain state.
- Restoring parameters or transforms directly from a client-side snapshot.
- Treating optimistic preview values as committed history.
- Redoing an old network envelope with its original revision or command ID.

## Acceptance criteria

- [ ] All world-object edits submit through `EditorCommandGateway`.
- [ ] Undo is a compensating authoritative command.
- [ ] Redo is a new authoritative forward command.
- [ ] History advances only on accepted server outcomes.
- [ ] Conflict and rejection paths are represented in UI and tests.
- [ ] Replica state changes only through replication application.
