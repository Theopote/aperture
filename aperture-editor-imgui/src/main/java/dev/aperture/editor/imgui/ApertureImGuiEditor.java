package dev.aperture.editor.imgui;

import dev.aperture.editor.imgui.windows.*;
import dev.aperture.editor.imgui.workspace.MainDockspace;
import dev.aperture.editor.model.session.EditorSession;
import java.util.Objects;

/** Composes all K2.3 windows while keeping drawing calls behind a frontend renderer. */
public final class ApertureImGuiEditor {
	private final EditorSession session; private final MainDockspace dockspace;
	public ApertureImGuiEditor(EditorSession session,WindowRenderer renderer){
		this.session=Objects.requireNonNull(session);Objects.requireNonNull(renderer);
		var outliner=new OutlinerWindow(session);var inspector=new InspectorWindow(session);var runtime=new RuntimeStateWindow(session);var history=new HistoryWindow(session);var diagnostics=new DiagnosticsWindow(session);
		this.dockspace=new MainDockspace().window("outliner",()->renderer.renderOutliner(outliner)).window("inspector",()->renderer.renderInspector(inspector)).window("runtime",()->renderer.renderRuntime(runtime)).window("history",()->renderer.renderHistory(history)).window("diagnostics",()->renderer.renderDiagnostics(diagnostics)).window("viewport",renderer::renderViewport);
	}
	public void render(){dockspace.render();}
	public MainDockspace dockspace(){return dockspace;}
	public EditorSession session(){return session;}
	public interface WindowRenderer {
		void renderOutliner(OutlinerWindow window); void renderInspector(InspectorWindow window); void renderRuntime(RuntimeStateWindow window);
		void renderHistory(HistoryWindow window); void renderDiagnostics(DiagnosticsWindow window); void renderViewport();
	}
}
