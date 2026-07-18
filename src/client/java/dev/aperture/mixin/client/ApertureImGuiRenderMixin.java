package dev.aperture.mixin.client;

import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.aperture.client.editor.imgui.ApertureImGuiClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public abstract class ApertureImGuiRenderMixin {
	@Inject(method = "flipFrame(Lcom/mojang/blaze3d/TracyFrameCapture;)V", at = @At("HEAD"))
	private static void aperture$renderDearImGui(TracyFrameCapture frameCapture, CallbackInfo ci) {
		ApertureImGuiClient.renderPendingDrawData();
	}
}