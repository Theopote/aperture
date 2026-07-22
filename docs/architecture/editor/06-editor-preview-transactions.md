# Editor preview transactions

An activated widget creates one `ParameterEditSession`. Value changes update `LocalPreviewCoordinator`; they do not submit, mutate the replica, or advance a revision. Deactivation commits exactly one `SetParameterArchitecturalCommand`; the widget may then release its local session reference because the preview is associated with the returned `commandId`.

The overlay lifecycle is `EDITING -> PENDING -> ACCEPTED_WAITING_REPLICA -> COMPLETED`. The server broadcasts the authoritative Snapshot before its Accepted response, so completion cannot expose the old replica between preview removal and authority installation. Rejection transitions through `REJECTED` and removes the overlay. Revision conflict transitions through `CONFLICT -> RESYNCING`; the overlay remains until the requested Object Snapshot is applied, then completes and rolls back to authority.

Undo and redo are new authoritative operations. A design history entry supplies a compensation command and a repetition command; runtime events are kept in a separate projection and never enter the design undo stack.