package dev.aperture.editor.model.read;
import dev.aperture.runtime.model.object.*;
public record ObjectSummary(ArchitecturalObjectId objectId, ArchitecturalTypeId typeId, String displayName, SyncStatus syncStatus, boolean warning) {}
