# 07 — Serialization Architecture

## Three Serialization Domains

| Domain | Format | Purpose |
|---|---|---|
| **Definitions** | JSON in data pack | Opening types, profiles |
| **Instances** | NBT (chunk/world save) | Placed openings |
| **Network** | Binary packets | Real-time sync |
| **Interchange** (future) | JSON / glTF / IFC | BIM, external tools |

## Codec Design

Platform-neutral contracts in `aperture-core`; Minecraft adapters in the Fabric mod.

```java
interface JsonCodec<T> {
    String toJson(T value);
    T fromJson(String json, MigrationContext ctx);
}

interface NbtAdapter<T> {
    // Implemented in Fabric module only
}
```

## Versioning & Migration

Every persisted object carries:

```json
{ "schemaVersion": 3, "apertureVersion": "1.4.0" }
```

```
MigrationPipeline
├── v1 → v2: rename parameter "mullion_count" → "mullions"
├── v2 → v3: hostRef format change
└── failsafe: quarantine invalid instances, never corrupt chunk
```

## Chunk Storage Model

Section-scoped instance index:

```
ChunkSectionData
└── openingInstances: Map<BlockPos, InstanceRef>

GlobalInstanceStore (per world)
└── instances: Map<UUID, OpeningInstance>
```

Anchor block at `BlockPos` → lookup UUID → full instance in world store. Supports large openings spanning multiple block spaces.

## Network Sync Protocol

| Packet | Direction | Payload |
|---|---|---|
| `OpeningPlaced` | S→C | full instance |
| `OpeningUpdated` | S→C | delta (params/state/revision) |
| `OpeningRemoved` | S→C | instanceId |
| `PlacementPreview` | C→S | validation request (optional) |
| `CatalogSync` | S→C | type hashes (on join) |

- Server authoritative; client predicts preview only.
- Revision monotonic — stale packets ignored.
