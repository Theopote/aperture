# Editor runtime binding

The authoritative client source is `ClientReplicaStore`. `ReplicaEditorReadModel` projects replicas and overlays local preview values without changing replica revisions. Intents cross `EditorCommandGateway`; its injected transport owns actor, wire request and server delivery. Rejections produce diagnostics and revision conflicts request a resync at the platform transport layer.

Implemented and unit-testable headlessly. Concrete Fabric transport, replica removal notification and world picking integration remain client gaps.
