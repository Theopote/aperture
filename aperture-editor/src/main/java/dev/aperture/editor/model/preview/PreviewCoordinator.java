package dev.aperture.editor.model.preview;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import java.util.*;
public interface PreviewCoordinator {
	void put(ArchitecturalObjectId objectId, String parameterKey, ParameterValue value);
	Optional<ParameterValue> value(ArchitecturalObjectId objectId, String parameterKey);
	void clear(ArchitecturalObjectId objectId, String parameterKey);
	void clearObject(ArchitecturalObjectId objectId);
}
