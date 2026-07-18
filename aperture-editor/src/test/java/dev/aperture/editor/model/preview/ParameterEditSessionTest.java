package dev.aperture.editor.model.preview;
import dev.aperture.editor.model.command.*;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class ParameterEditSessionTest {
	@Test void previewDoesNotSubmitAndCommitSubmitsExactlyOnce(){var calls=new AtomicInteger();EditorCommandTransport transport=(id,cmd,rev)->{calls.incrementAndGet();return new EditorCommandSubmission(id,EditorCommandSubmission.Status.ACCEPTED,"ok",rev.objectRevision()+1,rev.stateRevision());};var previews=new LocalPreviewCoordinator();var object=ArchitecturalObjectId.random();var gateway=new DefaultEditorCommandGateway(transport,new dev.aperture.editor.model.read.DiagnosticsModel());var edit=new DefaultParameterEditSession(object,"width",ParameterValue.length(900),new ExpectedRevision(4,2),previews,gateway);edit.updatePreview(ParameterValue.length(1000));edit.updatePreview(ParameterValue.length(1100));assertEquals(0,calls.get());assertTrue(previews.value(object,"width").isPresent());assertTrue(edit.commit().accepted());assertEquals(1,calls.get());assertTrue(previews.value(object,"width").isEmpty());}
	@Test void cancelDropsOverlayWithoutSubmission(){var previews=new LocalPreviewCoordinator();var object=ArchitecturalObjectId.random();EditorCommandGateway gateway=new DefaultEditorCommandGateway((id,c,r)->new EditorCommandSubmission(UUID.randomUUID(),EditorCommandSubmission.Status.ACCEPTED,"",1,0),new dev.aperture.editor.model.read.DiagnosticsModel());var edit=new DefaultParameterEditSession(object,"width",ParameterValue.length(900),new ExpectedRevision(0,0),previews,gateway);edit.updatePreview(ParameterValue.length(950));edit.cancel();assertFalse(edit.active());assertTrue(previews.value(object,"width").isEmpty());}
}
