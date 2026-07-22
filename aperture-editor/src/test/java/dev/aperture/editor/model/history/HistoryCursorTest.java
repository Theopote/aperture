package dev.aperture.editor.model.history;

import dev.aperture.editor.model.command.ExpectedRevision;
import dev.aperture.editor.model.command.SetParameterArchitecturalCommand;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HistoryCursorTest {
	@Test void consecutiveUndoAndRedoWalkDifferentEntries() {
		var history=new DefaultHistoryProjection();var first=entry("width",900,1000);var second=entry("height",2000,2100);
		history.recordDesign(first);history.recordDesign(second);assertEquals(2,history.cursor());
		var undo2=UUID.randomUUID();assertEquals(second,history.beginUndo(undo2).orElseThrow());history.completeOperation(undo2,true);
		var undo1=UUID.randomUUID();assertEquals(first,history.beginUndo(undo1).orElseThrow());history.completeOperation(undo1,true);assertEquals(0,history.cursor());
		var redo1=UUID.randomUUID();assertEquals(first,history.beginRedo(redo1).orElseThrow());history.completeOperation(redo1,true);assertEquals(1,history.cursor());
	}

	@Test void newEditClearsRedoBranch() {
		var history=new DefaultHistoryProjection();history.recordDesign(entry("a",1,2));history.recordDesign(entry("b",2,3));
		var operation=UUID.randomUUID();history.beginUndo(operation);history.completeOperation(operation,true);
		history.recordDesign(entry("c",3,4));assertEquals(2,history.designCommands().size());assertTrue(history.beginRedo(UUID.randomUUID()).isEmpty());
	}

	@Test void rejectedUndoRestoresCursorAndAllowsRetry() {
		var history=new DefaultHistoryProjection();var entry=entry("width",900,1000);history.recordDesign(entry);
		var rejected=UUID.randomUUID();history.beginUndo(rejected);history.completeOperation(rejected,false);assertEquals(1,history.cursor());
		assertEquals(entry,history.beginUndo(UUID.randomUUID()).orElseThrow());
	}

	@Test void runtimeActionsNeverEnterDesignCursor() {
		var history=new DefaultHistoryProjection();history.recordRuntime(entry("runtime",0,1));
		assertEquals(0,history.cursor());assertTrue(history.beginUndo(UUID.randomUUID()).isEmpty());
	}

	private static EditorHistoryEntry entry(String key,double before,double after){var id=ArchitecturalObjectId.random();return new EditorHistoryEntry(UUID.randomUUID(),id,"set_parameter",key,Double.toString(before),Double.toString(after),"test",Instant.EPOCH,EditorHistoryEntry.Result.ACCEPTED,1,true,()->new SetParameterArchitecturalCommand(id,key,ParameterValue.number(before)),()->new SetParameterArchitecturalCommand(id,key,ParameterValue.number(after)),new ExpectedRevision(1,0));}
}
