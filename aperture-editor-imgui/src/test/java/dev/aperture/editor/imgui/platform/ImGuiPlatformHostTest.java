package dev.aperture.editor.imgui.platform;
import dev.aperture.editor.imgui.input.EditorInputPolicy;
import dev.aperture.editor.imgui.workspace.MainDockspace;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class ImGuiPlatformHostTest {
	@Test void rendersOnlyWhenOpenAndAlwaysRestoresState(){var backend=new Backend();var restores=new AtomicInteger();RenderStateGuard guard=new RenderStateGuard(){public Snapshot capture(){return new Snapshot(){};}public void restore(Snapshot value){restores.incrementAndGet();}};var windows=new AtomicInteger();var host=new ImGuiPlatformHost(backend,guard,new MainDockspace().window("outliner",windows::incrementAndGet));host.initialize();host.render(1);assertEquals(0,backend.frames);host.open();host.render(1);assertEquals(1,backend.frames);assertEquals(1,windows.get());assertEquals(1,restores.get());host.close();assertTrue(backend.closed);}
	private static final class Backend implements ImGuiBackend {int frames;boolean closed;public void initialize(){}public void beginFrame(float scale){frames++;}public void renderDrawData(){}public EditorInputPolicy inputPolicy(){return new EditorInputPolicy(false,false,false);}public void close(){closed=true;}}
}
