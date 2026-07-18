# Editor runtime binding

The authoritative client source is `ClientReplicaStore`. `ReplicaEditorReadModel` projects replicas, capability-derived runtime actions, diagnostics, and local preview values without changing replica revisions.

Durable intents cross `EditorCommandGateway` into `ClientEditorCommandTransport`. The transport adds actor identity and expected object/state revisions, encodes an allow-listed `CommandRequestMessage`, and sends it through Fabric C2S networking. The server `AuthoritativeCommandGateway` validates and commits through `RuntimeTransaction`; responses are correlated by command ID. Accepted changes become visible only after Snapshot or Delta replication updates the client store. Rejections produce editor diagnostics.

Implemented and client-integrated: typed parameter commit, Open/Close/Lock commands, Accepted/Rejected response routing, and replica-driven refresh. Remaining gaps are automatic resync submission on revision conflict, accepted-command History population, replica removal selection cleanup, and world picking integration.