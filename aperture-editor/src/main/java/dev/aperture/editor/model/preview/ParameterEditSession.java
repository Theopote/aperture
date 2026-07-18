package dev.aperture.editor.model.preview;
import dev.aperture.editor.model.command.EditorCommandSubmission;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
public interface ParameterEditSession {
	ArchitecturalObjectId objectId(); String parameterKey(); ParameterValue authoritativeValue(); ParameterValue previewValue();
	void updatePreview(ParameterValue value); EditorCommandSubmission commit(); void cancel(); boolean active();
}
