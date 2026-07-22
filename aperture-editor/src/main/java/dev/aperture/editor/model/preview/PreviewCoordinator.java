package dev.aperture.editor.model.preview;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import java.util.*;
public interface PreviewCoordinator {
	void put(ArchitecturalObjectId objectId, String parameterKey, ParameterValue value);
	Optional<ParameterValue> value(ArchitecturalObjectId objectId, String parameterKey);
	Map<String, ParameterValue> values(ArchitecturalObjectId objectId);
	void associate(java.util.UUID commandId, ArchitecturalObjectId objectId, String parameterKey);
	void transition(java.util.UUID commandId, PreviewState state);
	Optional<PreviewState> state(java.util.UUID commandId);
	void complete(java.util.UUID commandId);
	void dismiss(java.util.UUID commandId);
	void clear(ArchitecturalObjectId objectId, String parameterKey);
	void clearObject(ArchitecturalObjectId objectId);
}
