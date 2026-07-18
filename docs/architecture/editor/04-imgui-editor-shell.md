# ImGui editor shell

Target flow: `ArchitecturalObjectId -> ClientReplicaStore -> EditorReadModel -> ImGui frontend -> EditorCommandGateway -> authoritative server runtime`.

Implemented: headless session contracts, ID selection, read projection, preview overlay, command boundary and standalone frontend module. Unit proven: selection and preview/commit contracts. Client integrated/world proven: not yet; Fabric host remains a gap. ImGui is a frontend only and may not retain mutable domain objects or use network codecs.
