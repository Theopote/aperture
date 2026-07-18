package dev.aperture.editor.imgui.platform;

import dev.aperture.editor.imgui.input.EditorInputPolicy;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import java.util.Objects;

/** Owns the Dear ImGui context; GLFW/OpenGL rendering stays behind injected client callbacks. */
public final class DearImGuiBackend implements ImGuiBackend {
	private final Runnable platformNewFrame; private final Runnable drawDataRenderer;
	private boolean initialized;
	public DearImGuiBackend(Runnable platformNewFrame,Runnable drawDataRenderer){this.platformNewFrame=Objects.requireNonNull(platformNewFrame);this.drawDataRenderer=Objects.requireNonNull(drawDataRenderer);}
	@Override public void initialize(){if(initialized)return;ImGui.createContext();ImGuiIO io=ImGui.getIO();io.addConfigFlags(ImGuiConfigFlags.DockingEnable);io.setIniFilename(null);initialized=true;}
	@Override public void beginFrame(float framebufferScale){ensureInitialized();ImGuiIO io=ImGui.getIO();io.setFontGlobalScale(Math.max(0.5f,framebufferScale));platformNewFrame.run();ImGui.newFrame();}
	@Override public void renderDrawData(){ensureInitialized();ImGui.render();drawDataRenderer.run();}
	@Override public EditorInputPolicy inputPolicy(){if(!initialized)return new EditorInputPolicy(false,false,false);ImGuiIO io=ImGui.getIO();return new EditorInputPolicy(io.getWantCaptureMouse(),io.getWantCaptureKeyboard(),io.getWantTextInput());}
	@Override public void close(){if(initialized){ImGui.destroyContext();initialized=false;}}
	private void ensureInitialized(){if(!initialized)throw new IllegalStateException("Dear ImGui context is not initialized");}
}
