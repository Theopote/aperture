package dev.aperture.editor.model.selection;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class DefaultSelectionModelTest {
	@Test void storesOnlyIdsSupportsMultiSelectAndNotifies(){var model=new DefaultSelectionModel();var calls=new AtomicInteger();model.addListener(x->calls.incrementAndGet());var a=ArchitecturalObjectId.random();var b=ArchitecturalObjectId.random();model.selectObject(a);model.addObject(b);assertEquals(2,model.snapshot().objectIds().size());assertEquals(a,model.snapshot().primaryObject());assertEquals(2,calls.get());model.removeObject(a);assertEquals(b,model.snapshot().primaryObject());}
	@Test void componentSelectionHasStablePath(){var model=new DefaultSelectionModel();var id=ArchitecturalObjectId.random();model.selectComponent(id,new ComponentPath("panel/leaf"));assertEquals("panel/leaf",model.snapshot().primaryComponent().orElseThrow().path().value());}
}
