package dev.aperture.editor.imgui.input;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class ShortcutDispatcherTest {
	@Test void suppressesGlobalShortcutsWhileEditingText(){var calls=new AtomicInteger();var dispatcher=new ShortcutDispatcher();dispatcher.bind("CTRL+Z",calls::incrementAndGet);assertFalse(dispatcher.dispatch("CTRL+Z",new EditorInputPolicy(true,true,true)));assertTrue(dispatcher.dispatch("CTRL+Z",new EditorInputPolicy(false,true,false)));assertEquals(1,calls.get());}
}
