package dev.aperture.client.editor.imgui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Transparent Minecraft screen hosting the Dear ImGui editor over the world viewport. */
public final class ApertureImGuiScreen extends Screen {
	private final ApertureImGuiRuntime runtime;
	public ApertureImGuiScreen(ApertureImGuiRuntime runtime){super(Component.literal("Aperture Editor"));this.runtime=runtime;}
	@Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){runtime.renderFrame();}
	@Override public boolean isPauseScreen(){return false;}
}
