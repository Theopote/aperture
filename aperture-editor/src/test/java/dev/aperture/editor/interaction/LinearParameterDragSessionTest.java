package dev.aperture.editor.interaction;

import dev.aperture.editor.model.command.*;
import dev.aperture.editor.model.inspector.InspectorModel;
import dev.aperture.editor.model.preview.LocalPreviewCoordinator;
import dev.aperture.editor.model.read.*;
import dev.aperture.editor.model.selection.DefaultSelectionModel;
import dev.aperture.editor.model.session.*;
import dev.aperture.math.Transform3d;
import dev.aperture.parameter.*;
import dev.aperture.runtime.model.object.*;
import dev.aperture.runtime.model.state.RuntimeState;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LinearParameterDragSessionTest {
	@Test
	void appliesSnapFineSnapAndConstraints() {
		Fixture fixture = fixture(EditorCommandSubmission.Status.ACCEPTED);
		var drag = fixture.drag();

		assertEquals(1010, drag.updateDelta(106, false, false));
		assertEquals(1007, drag.updateDelta(107, true, false));
		assertEquals(1007.5, drag.updateDelta(107.5, false, true));
		assertEquals(1200, drag.updateDelta(999, false, true));
	}

	@Test
	void finishSubmitsExactlyOnceAndMapsState() {
		Fixture fixture = fixture(EditorCommandSubmission.Status.PENDING);
		var drag = fixture.drag();
		drag.updateDelta(100, false, false);

		drag.finish();
		drag.finish();

		assertEquals(1, fixture.submissions().get());
		assertEquals(ToolInteractionState.PENDING, drag.state());
	}

	@Test
	void unchangedFinishAndCancelNeverSubmit() {
		Fixture unchanged = fixture(EditorCommandSubmission.Status.ACCEPTED);
		unchanged.drag().finish();
		Fixture cancelled = fixture(EditorCommandSubmission.Status.ACCEPTED);
		cancelled.drag().updateDelta(50, false, false);
		cancelled.drag().cancel();

		assertEquals(0, unchanged.submissions().get());
		assertEquals(0, cancelled.submissions().get());
		assertEquals(ToolInteractionState.CANCELLED, unchanged.drag().state());
		assertEquals(ToolInteractionState.CANCELLED, cancelled.drag().state());
	}

	private static Fixture fixture(EditorCommandSubmission.Status status) {
		var id = ArchitecturalObjectId.random();
		var view = new ObjectEditorView(id, ArchitecturalTypeId.parse("aperture:door"),
			new ArchitecturalFamilyId("aperture:opening"), "Door", Transform3d.identity(), List.of(),
			ParameterSet.builder().put("size", ParameterValue.length(900)).build(), RuntimeState.initial(dev.aperture.runtime.model.state.StateSchema.builder("test", 1).build()),
			2, 1, SyncStatus.SYNCHRONIZED, List.of(), List.of());
		AtomicInteger submissions = new AtomicInteger();
		EditorCommandGateway commands = new DefaultEditorCommandGateway((commandId, command, revision) -> {
			submissions.incrementAndGet();
			return new EditorCommandSubmission(commandId, status, "test", revision.objectRevision(), revision.stateRevision());
		}, new DiagnosticsModel());
		EditorReadModel read = new EditorReadModel() {
			@Override public List<ObjectSummary> visibleObjects() { return List.of(); }
			@Override public List<EditorDiagnostic> diagnostics(ArchitecturalObjectId objectId) { return List.of(); }
			@Override public Optional<ObjectEditorView> object(ArchitecturalObjectId objectId) { return Optional.of(view); }
		};
		InspectorModel inspector = objectId -> List.of();
		ToolController tools = new ToolController() { @Override public void cancelActiveTool() { } };
		var session = new DefaultEditorSession(new DefaultSelectionModel(), read, commands, inspector,
			new LocalPreviewCoordinator(), new dev.aperture.editor.model.history.DefaultHistoryProjection(),
			new DiagnosticsModel(), new DefaultWorkspaceModel(), tools);
		var descriptor = new ManipulatorDescriptor("test.size", ManipulatorDescriptor.Kind.LINEAR_PARAMETER,
			"Size", "size", ManipulatorDescriptor.Axis.LOCAL_X, ManipulatorDescriptor.Anchor.RIGHT_MIDPOINT,
			ManipulatorDescriptor.Anchor.LEFT_MIDPOINT, ManipulatorDescriptor.DirectionPolicy.POSITIVE,
			java.util.OptionalDouble.of(800), java.util.OptionalDouble.of(1200), 10, 1, ParameterType.LENGTH);
		return new Fixture(LinearParameterDragSession.begin(session, view, descriptor).orElseThrow(), submissions);
	}

	private record Fixture(LinearParameterDragSession drag, AtomicInteger submissions) { }
}
