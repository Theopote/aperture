package dev.aperture.editor.model.read;

import dev.aperture.math.Transform3d;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.runtime.model.object.*;
import dev.aperture.runtime.model.state.RuntimeState;
import java.util.List;

public record ObjectEditorView(ArchitecturalObjectId objectId, ArchitecturalTypeId typeId,
	ArchitecturalFamilyId familyId, String displayName, Transform3d transform,
	List<HostBinding> hostBindings, ParameterSet parameters, RuntimeState runtimeState,
	long objectRevision, long stateRevision, SyncStatus syncStatus, List<RuntimeActionDescriptor> runtimeActions, List<EditorDiagnostic> diagnostics) {}
