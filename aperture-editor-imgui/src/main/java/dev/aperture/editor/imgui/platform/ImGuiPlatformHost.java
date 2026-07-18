package dev.aperture.editor.imgui.platform;

import dev.aperture.editor.imgui.workspace.MainDockspace;
import java.util.Objects;

public final class ImGuiPlatformHost implements AutoCloseable {
	private final ImGuiBackend backend; private final RenderStateGuard stateGuard; private final MainDockspace dockspace;
	private boolean initialized; private boolean open;
	public ImGuiPlatformHost(ImGuiBackend backend,RenderStateGuard stateGuard,MainDockspace dockspace){this.backend=Objects.requireNonNull(backend);this.stateGuard=Objects.requireNonNull(stateGuard);this.dockspace=Objects.requireNonNull(dockspace);}
	public void initialize(){if(!initialized){backend.initialize();initialized=true;}}
	public void render(float framebufferScale){if(!initialized)throw new IllegalStateException("host is not initialized");if(!open)return;var snapshot=stateGuard.capture();try{backend.beginFrame(framebufferScale);dockspace.render();backend.renderDrawData();}finally{stateGuard.restore(snapshot);}}
	public void toggle(){open=!open;} public void open(){open=true;} public void closeEditor(){open=false;} public boolean isOpen(){return open;} public boolean initialized(){return initialized;}
	@Override public void close(){if(initialized){open=false;initialized=false;backend.close();}}
}
