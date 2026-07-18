package dev.aperture.editor.imgui.workspace;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Versioned, dependency-free workspace persistence owned by the frontend. */
public final class WorkspaceLayoutStore {
	private final Path file;
	public WorkspaceLayoutStore(Path file) { this.file=Objects.requireNonNull(file).toAbsolutePath().normalize(); }
	public WorkspaceLayout load() {
		if (!Files.isRegularFile(file)) return WorkspaceLayout.defaults();
		Properties properties=new Properties();
		try (Reader reader=Files.newBufferedReader(file)) { properties.load(reader); }
		catch (IOException | IllegalArgumentException ignored) { return WorkspaceLayout.defaults(); }
		try {
			int version=Integer.parseInt(properties.getProperty("version","0"));
			if(version!=WorkspaceLayout.CURRENT_VERSION)return WorkspaceLayout.defaults();
			float scale=Float.parseFloat(properties.getProperty("uiScale","1"));
			Map<String,Boolean> windows=new LinkedHashMap<>(WorkspaceLayout.defaults().visibleWindows());
			windows.replaceAll((id,old)->Boolean.parseBoolean(properties.getProperty("window."+id,old.toString())));
			return new WorkspaceLayout(version,windows,scale);
		} catch (RuntimeException ignored) { return WorkspaceLayout.defaults(); }
	}
	public void save(WorkspaceLayout layout) throws IOException {
		Path parent=file.getParent(); if(parent!=null)Files.createDirectories(parent);
		Properties properties=new Properties(); properties.setProperty("version",Integer.toString(layout.version())); properties.setProperty("uiScale",Float.toString(layout.uiScale()));
		layout.visibleWindows().forEach((id,visible)->properties.setProperty("window."+id,visible.toString()));
		Path temp=Files.createTempFile(parent==null?Path.of("."):parent,file.getFileName().toString(),".tmp");
		try { try(Writer writer=Files.newBufferedWriter(temp)){properties.store(writer,"Aperture ImGui workspace");} try { Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); } catch(AtomicMoveNotSupportedException ex){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);} }
		finally { Files.deleteIfExists(temp); }
	}
	public void reset() throws IOException { save(WorkspaceLayout.defaults()); }
}
