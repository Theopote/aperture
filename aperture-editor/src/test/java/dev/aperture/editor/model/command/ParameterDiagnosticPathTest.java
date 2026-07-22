package dev.aperture.editor.model.command;

import dev.aperture.editor.model.read.DiagnosticsModel;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParameterDiagnosticPathTest {
	@Test
	void rejectedParameterEditPointsBackToItsInspectorRow() {
		DiagnosticsModel diagnostics = new DiagnosticsModel();
		var gateway = new DefaultEditorCommandGateway((commandId, command, revision) ->
			new EditorCommandSubmission(commandId, EditorCommandSubmission.Status.REVISION_CONFLICT,
				"Object changed on the server", 4, 2), diagnostics);
		var objectId = ArchitecturalObjectId.random();

		gateway.submitParameterEdit(objectId, "width", ParameterValue.length(900),
			ParameterValue.length(1000), new ExpectedRevision(3, 2));

		var diagnostic = diagnostics.forObject(objectId).getFirst();
		assertEquals("width", diagnostic.path().orElseThrow());
		assertEquals("revision_conflict", diagnostic.code());
		assertEquals("Resync object", diagnostic.suggestedAction());
	}
}
