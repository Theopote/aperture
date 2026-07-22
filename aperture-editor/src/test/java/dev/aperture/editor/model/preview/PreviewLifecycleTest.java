package dev.aperture.editor.model.preview;

import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PreviewLifecycleTest {
	@Test void pendingOverlayIsOwnedByCommandUntilAuthorityCompletesIt() {
		var previews=new LocalPreviewCoordinator();
		var objectId=ArchitecturalObjectId.random();
		var commandId=UUID.randomUUID();
		previews.put(objectId,"width",ParameterValue.length(1500));
		previews.associate(commandId,objectId,"width");
		assertEquals(PreviewState.PENDING,previews.state(commandId).orElseThrow());
		assertTrue(previews.value(objectId,"width").isPresent());
		previews.transition(commandId,PreviewState.ACCEPTED_WAITING_REPLICA);
		assertTrue(previews.value(objectId,"width").isPresent());
		previews.complete(commandId);
		assertTrue(previews.value(objectId,"width").isEmpty());
		assertEquals(PreviewState.ACCEPTED_WAITING_REPLICA, previews.state(commandId).orElseThrow());
		previews.dismiss(commandId);
		assertTrue(previews.state(commandId).isEmpty());
	}

	@Test void resyncingConflictKeepsOverlayUntilSnapshotCompletion() {
		var previews=new LocalPreviewCoordinator();
		var objectId=ArchitecturalObjectId.random();
		var commandId=UUID.randomUUID();
		previews.put(objectId,"width",ParameterValue.length(1600));
		previews.associate(commandId,objectId,"width");
		previews.transition(commandId,PreviewState.CONFLICT);
		previews.transition(commandId,PreviewState.RESYNCING);
		assertEquals(PreviewState.RESYNCING,previews.state(commandId).orElseThrow());
		assertTrue(previews.value(objectId,"width").isPresent());
		previews.complete(commandId);
		assertTrue(previews.value(objectId,"width").isEmpty());
	}
}
