package dev.aperture.editor.model.history;
import dev.aperture.editor.model.command.ExpectedRevision;
import dev.aperture.runtime.model.command.ArchitecturalCommand;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;
public record EditorHistoryEntry(UUID commandId, ArchitecturalObjectId objectId, String commandType, String summary,
	String before, String after, String author, Instant timestamp, Result result, long revision, boolean undoable,
	Supplier<ArchitecturalCommand> compensation, Supplier<ArchitecturalCommand> repetition, ExpectedRevision expectedRevision) {
	public enum Result { ACCEPTED, REJECTED }
}
