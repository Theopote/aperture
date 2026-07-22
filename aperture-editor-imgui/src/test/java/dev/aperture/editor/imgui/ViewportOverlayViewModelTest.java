package dev.aperture.editor.imgui;

import dev.aperture.editor.interaction.ToolInteractionState;
import dev.aperture.editor.model.read.EditorDiagnostic;
import dev.aperture.editor.model.read.ObjectEditorView;
import dev.aperture.editor.model.read.SyncStatus;
import dev.aperture.math.Transform3d;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.object.*;
import dev.aperture.runtime.model.state.RuntimeState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViewportOverlayViewModelTest {
	@Test
	void localPreviewShowsTheChangedSemanticValue() {
		var operation = ViewportOverlayViewModel.operation(view(List.of(), SyncStatus.PREVIEW),
			Map.of("width", ParameterValue.length(1350)),
			new ViewportToolState.Snapshot(ToolInteractionState.DRAGGING, Optional.of("door.width.right")));
		assertEquals(ViewportOverlayViewModel.OperationKind.PREVIEW, operation.kind());
		assertEquals("Width 1350 mm", operation.title());
	}

	@Test
	void conflictOverridesPreviewAndExplainsResynchronization() {
		var diagnostic = new EditorDiagnostic(EditorDiagnostic.Severity.ERROR, "revision_conflict", "Stale revision",
			Optional.empty(), Optional.of("width"), "command", Instant.EPOCH, "Resync object", false);
		var operation = ViewportOverlayViewModel.operation(view(List.of(diagnostic), SyncStatus.RESYNC_REQUIRED),
			Map.of("width", ParameterValue.length(1350)),
			new ViewportToolState.Snapshot(ToolInteractionState.CONFLICT, Optional.of("door.width.right")));
		assertEquals(ViewportOverlayViewModel.OperationKind.CONFLICT, operation.kind());
		assertEquals("Revision conflict", operation.title());
		assertEquals("Resynchronizing...", operation.detail());
	}

	private static ObjectEditorView view(List<EditorDiagnostic> diagnostics, SyncStatus status) {
		return new ObjectEditorView(ArchitecturalObjectId.random(), ArchitecturalTypeId.parse("aperture:door_single"),
			new ArchitecturalFamilyId("aperture:opening"), "Door 001", Transform3d.identity(), List.of(),
			ParameterSet.builder().put("width", ParameterValue.length(1200)).put("height", ParameterValue.length(2100)).build(),
			RuntimeState.initial(dev.aperture.runtime.model.state.StateSchema.builder("door", 1).build()),
			8, 1, status, List.of(), diagnostics);
	}
}
