package dev.aperture.editor.model.command;
import java.util.UUID;
public record EditorCommandSubmission(UUID commandId, Status status, String message, long objectRevision, long stateRevision) {
	public enum Status { PENDING, ACCEPTED, REJECTED, REVISION_CONFLICT }
	public boolean accepted(){return status==Status.ACCEPTED;}
}
