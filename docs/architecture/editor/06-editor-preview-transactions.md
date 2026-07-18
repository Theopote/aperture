# Editor preview transactions

An activated widget creates one `ParameterEditSession`. Value changes update `LocalPreviewCoordinator`; they do not submit or advance a revision. Deactivation commits exactly one `SetParameterArchitecturalCommand`. Escape cancels and removes the overlay. Accepted and rejected submissions both remove the local overlay so the read model returns to authoritative replica data.

Undo and redo are new authoritative operations. A design history entry supplies a compensation command and a repetition command; runtime events are kept in a separate projection and never enter the design undo stack.
