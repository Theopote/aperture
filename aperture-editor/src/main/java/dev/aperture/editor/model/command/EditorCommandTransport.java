package dev.aperture.editor.model.command;
import dev.aperture.runtime.model.command.ArchitecturalCommand;
import java.util.UUID;
@FunctionalInterface public interface EditorCommandTransport { EditorCommandSubmission submit(UUID commandId, ArchitecturalCommand command, ExpectedRevision revision); default void addResultListener(EditorCommandResultListener listener) { } }
