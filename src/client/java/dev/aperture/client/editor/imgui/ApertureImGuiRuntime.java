package dev.aperture.client.editor.imgui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aperture.Aperture;
import dev.aperture.editor.imgui.DearImGuiEditor;
import dev.aperture.editor.model.command.*;
import dev.aperture.editor.model.history.DefaultHistoryProjection;
import dev.aperture.editor.model.inspector.SchemaDrivenInspectorModel;
import dev.aperture.editor.model.preview.LocalPreviewCoordinator;
import dev.aperture.editor.model.read.*;
import dev.aperture.editor.model.selection.DefaultSelectionModel;
import dev.aperture.editor.model.session.*;
import dev.aperture.client.runtime.ClientRuntimeReplicas;
import dev.aperture.client.editor.ClientEditorPreviews;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

import java.nio.file.Files;
import java.io.IOException;

/** Minecraft-client owner of GLFW/OpenGL and Dear ImGui lifecycle. */
public final class ApertureImGuiRuntime implements AutoCloseable {
	private final ImGuiImplGlfw glfw = new ImGuiImplGlfw();
	private final ImGuiImplGl3 gl3 = new ImGuiImplGl3();
	private DearImGuiEditor editor;
	private boolean initialized;
	private boolean drawDataReady;
	private boolean drawSubmissionLogged;

	public void initialize() {
		if (initialized) return;
		Minecraft client = Minecraft.getInstance();
		long window = client.getWindow().handle();
		ImGui.createContext();
		ImGuiIO io = ImGui.getIO();
		io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard | ImGuiConfigFlags.DockingEnable);
		var configDirectory=client.gameDirectory.toPath().resolve("config/aperture"); try{Files.createDirectories(configDirectory);}catch(IOException error){throw new IllegalStateException("Unable to create Aperture UI config directory",error);} var iniPath=configDirectory.resolve("imgui.ini"); io.setIniFilename(iniPath.toString());
		io.getFonts().addFontDefault();
		io.getFonts().build();
		ImGui.styleColorsDark();
		if (!glfw.init(window, true)) throw new IllegalStateException("Unable to initialize ImGui GLFW backend");
		resetPixelStore();
		gl3.init("#version 150");
		editor = new DearImGuiEditor(createSession(), Files.notExists(iniPath));
		initialized = true;
		Aperture.LOGGER.info("Dear ImGui initialized for window {} (fontTexture={}, validTexture={})", window, io.getFonts().getTexID(), GL11.glIsTexture(io.getFonts().getTexID()));
	}

	public void buildFrame() {
		if (!initialized) initialize();
		RenderSystem.assertOnRenderThread();
		glfw.newFrame(); ImGui.newFrame(); editor.render(); ImGui.render();
		drawDataReady = true;
	}

	public void renderPendingDrawData() {
		if (!initialized || !drawDataReady) return;
		RenderSystem.assertOnRenderThread();
		int[] viewport = new int[4]; GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
		int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
		int previousDrawBuffer = GL11.glGetInteger(GL11.GL_DRAW_BUFFER);
		int previousSampler = GL30.glGetIntegeri(GL33.GL_SAMPLER_BINDING, 0);
		boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
		try {
			GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
			GL11.glDrawBuffer(GL11.GL_BACK);
			GL33.glBindSampler(0, 0);
			GL11.glDisable(GL11.GL_SCISSOR_TEST);
			gl3.renderDrawData(ImGui.getDrawData());
			if (!drawSubmissionLogged) { Aperture.LOGGER.info("Dear ImGui draw data submitted before frame swap (isolatedSampler={})", previousSampler); drawSubmissionLogged = true; }
		} finally {
			drawDataReady = false;
			GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
			GL11.glDrawBuffer(previousDrawBuffer);
			GL33.glBindSampler(0, previousSampler);
			GL11.glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);
			if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST); else GL11.glDisable(GL11.GL_SCISSOR_TEST);
		}
	}

	public boolean wantsMouse(){return initialized && ImGui.getIO().getWantCaptureMouse();}
	public boolean wantsKeyboard(){return initialized && ImGui.getIO().getWantCaptureKeyboard();}
	public boolean initialized(){return initialized;}

	private static EditorSession createSession() {
		var selection=new DefaultSelectionModel(); var previews=ClientEditorPreviews.get(); var diagnostics=new DiagnosticsModel();
		var read=new ReplicaEditorReadModel(ClientRuntimeReplicas.store(),previews,diagnostics,ClientRuntimeReplicas::runtimeActions);
		EditorCommandTransport transport=new ClientEditorCommandTransport(diagnostics,previews);
		var commands=new DefaultEditorCommandGateway(transport,diagnostics);
		return new DefaultEditorSession(selection,read,commands,new SchemaDrivenInspectorModel(read),previews,new DefaultHistoryProjection(),diagnostics,new DefaultWorkspaceModel(),()->{var selected=selection.snapshot().primaryObject();if(selected!=null)previews.clearObject(selected);});
	}

	private static void resetPixelStore() {
		GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
		GL11.glPixelStorei(org.lwjgl.opengl.GL12.GL_UNPACK_ROW_LENGTH, 0);
		GL11.glPixelStorei(org.lwjgl.opengl.GL12.GL_UNPACK_SKIP_PIXELS, 0);
		GL11.glPixelStorei(org.lwjgl.opengl.GL12.GL_UNPACK_SKIP_ROWS, 0);
		GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
		GL11.glPixelStorei(org.lwjgl.opengl.GL12.GL_PACK_ROW_LENGTH, 0);
		GL11.glPixelStorei(org.lwjgl.opengl.GL12.GL_PACK_SKIP_PIXELS, 0);
		GL11.glPixelStorei(org.lwjgl.opengl.GL12.GL_PACK_SKIP_ROWS, 0);
	}

	@Override public void close(){if(!initialized)return;gl3.dispose();glfw.dispose();ImGui.destroyContext();initialized=false;drawDataReady=false;editor=null;}
}
