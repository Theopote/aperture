package dev.aperture.editor.imgui.workspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class WorkspaceLayoutStoreTest {
	@Test void roundTripsAndResets(@TempDir Path directory) throws Exception {var store=new WorkspaceLayoutStore(directory.resolve("layout.properties"));assertEquals(WorkspaceLayout.defaults(),store.load());var custom=new WorkspaceLayout(1,Map.of("outliner",false,"inspector",true),1.5f);store.save(custom);var loaded=store.load();assertFalse(loaded.visibleWindows().get("outliner"));assertEquals(1.5f,loaded.uiScale());store.reset();assertEquals(WorkspaceLayout.defaults(),store.load());}
	@Test void corruptLayoutFallsBackToDefaults(@TempDir Path directory) throws Exception {java.nio.file.Files.writeString(directory.resolve("layout.properties"),"version=nope");assertEquals(WorkspaceLayout.defaults(),new WorkspaceLayoutStore(directory.resolve("layout.properties")).load());}
}
