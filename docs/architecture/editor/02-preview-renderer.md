# 02 — Editor Preview Renderer

**Layer:** Client-only visualization
**Status:** K2.3 normative architecture

## Boundary

Preview is disposable projection, not authoritative state. It may render proposed parameters or transforms immediately, but it must never update `ClientReplicaStore`, a placed runtime session, BlockEntity state, or persistence.

```text
Client Replica
  → EditorProjection(base object/state revisions)
  → Editor Intent draft
  → PreviewPatch
  → asynchronous generation / kinematic evaluation
  → PreviewRenderModel
  → viewport
```

On commit:

```text
Editor Intent
  → EditorCommandGateway
  → server transaction
  → replication
  → new Client Replica
  → rebuild preview base
```

## Preview request

```java
public record PreviewRequest(
    ArchitecturalObjectId objectId,
    long baseObjectRevision,
    StateRevision baseStateRevision,
    ParameterSet proposedParameters,
    Transform3d proposedTransform,
    long requestSequence
) { }
```

For an unplaced catalog draft, `objectId` may be absent and the request is explicitly marked `DRAFT`. A draft can generate geometry but cannot masquerade as a placed runtime object.

## Preview manager

```java
public interface PreviewManager {
    void request(PreviewRequest request);
    void cancel(long requestSequence);
    Optional<PreviewRenderModel> latest();
    void invalidate(ArchitecturalObjectId objectId);
}
```

Rules:

- Inputs are immutable copies.
- New requests cancel or obsolete older work.
- Results carry the base revisions and request sequence.
- A result is published only if its sequence is latest and its base revision still matches the replica.
- Rejection, resync, selection change, or replica removal discards the result.

## Preview render model

```java
public record PreviewRenderModel(
    Optional<ArchitecturalObjectId> objectId,
    long requestSequence,
    MeshData mesh,
    Map<ComponentPath, Transform3d> componentPoses,
    Bounds3d bounds,
    PreviewStatus status
) { }
```

The renderer consumes only `PreviewRenderModel`. It does not resolve or modify domain instances.

## Placed-object editing

A placed object preview starts from `EditorProjection` resolved from `ClientReplicaStore`. Parameter and transform proposals are applied to a copied preview input. During drag, the viewport may show the proposal optimistically, while inspector committed values continue to reflect the replica.

Every drag/slider frame remains local. Only gesture completion submits one final intent; cancellation submits nothing. Preview generation may be debounced or coalesced independently from the network. See [04-interaction-transport.md](04-interaction-transport.md).

When the authoritative response arrives:

- accepted: discard the proposal when the corresponding replica revision is observed;
- rejected: discard it and display the structured error;
- revision conflict: discard it, request/await resync, and rebuild from the new projection;
- timeout: retain a visibly pending state or cancel; never assume commit.

## Dynamic components

Runtime Door motion is rendered from replicated state and kinematic component poses. Editor transform proposals are an additional preview layer and must not overwrite the runtime pose. Component selection uses `(ArchitecturalObjectId, ComponentPath)`.

## Threading

- Replica application and projection swap occur on the Minecraft client thread.
- Geometry generation may run on a worker with immutable inputs.
- GPU upload and viewport publication occur on the render/client thread.
- Stale worker results are dropped by sequence and revision checks.

## Cache key

A preview cache key includes definition/type revision, proposed parameters, proposed transform, relevant runtime pose inputs, material revision, and generation settings. It must not use a mutable object identity as its only invalidation key.

## Performance targets

- Widget/gizmo feedback: immediate local overlay.
- Lightweight preview pose update: within one frame when no mesh rebuild is needed.
- Cached geometry preview: target under 16 ms publication.
- Full regeneration: asynchronous, cancellable, and never blocks network replica application.

## Failure behavior

Preview failure produces diagnostics associated with the pending intent. It cannot corrupt the committed projection. The last authoritative render remains available while an invalid proposal is highlighted.

## Acceptance criteria

- [ ] Preview inputs and outputs are immutable.
- [ ] Preview state never writes to `ClientReplicaStore` or Runtime.
- [ ] Every placed-object preview is tagged with base revisions.
- [ ] Stale async results are rejected.
- [ ] Accepted edits refresh from replication rather than adopting the preview as truth.
- [ ] Conflict/rejection removes optimistic preview state.
- [ ] Dynamic component previews preserve `ComponentPath` identity.