package dev.aperture.client.editor.imgui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** Transparent Minecraft screen hosting the Dear ImGui editor over the world viewport. */
public final class ApertureImGuiScreen extends Screen {
	private final ApertureImGuiRuntime runtime;
	public ApertureImGuiScreen(ApertureImGuiRuntime runtime){super(Component.literal("Aperture Editor"));this.runtime=runtime;}
	@Override public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick){runtime.buildFrame();}
	@Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick){}
	@Override public boolean mouseClicked(MouseButtonEvent click,boolean doubled){if(click.button()==0&&!runtime.wantsMouse()){runtime.selectAtCrosshair(click.hasControlDown());return true;}return super.mouseClicked(click,doubled);}
	@Override public boolean isInGameUi(){return true;}
	@Override public boolean isPauseScreen(){return false;}
}
