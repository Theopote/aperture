package dev.aperture.client.render.material;

import dev.aperture.core.material.BlendMode;
import dev.aperture.geometry.material.MaterialBinding;
import dev.aperture.core.material.MaterialInstance;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Maps platform-agnostic material instances to Fabric draw state.
 */
public final class FabricMaterialGraphics {
	private static final float GHOST_ALPHA = 0.45f;
	private static final int INVALID_TINT = 0xFFFF6666;

	private FabricMaterialGraphics() {
	}

	public static ResolvedMaterialDraw resolveCommitted(MaterialInstance material) {
		Identifier texture = Identifier.parse(material.definition().albedoTexture());
		RenderType renderType = renderTypeFor(texture, material.definition().blendMode());
		return new ResolvedMaterialDraw(renderType, material.tintArgb());
	}

	public static ResolvedMaterialDraw resolveGhost(MaterialBinding binding, boolean valid) {
		ResolvedMaterialDraw base = resolveCommitted(binding.material());
		Identifier texture = Identifier.parse(binding.material().definition().albedoTexture());
		RenderType ghostType = RenderTypes.entityTranslucent(texture);
		int tint = valid ? applyAlpha(base.tintArgb(), GHOST_ALPHA) : applyAlpha(INVALID_TINT, GHOST_ALPHA);
		return new ResolvedMaterialDraw(ghostType, tint);
	}

	private static RenderType renderTypeFor(Identifier texture, BlendMode blendMode) {
		return switch (blendMode) {
			case OPAQUE -> RenderTypes.entitySolid(texture);
			case CUTOUT -> RenderTypes.entityCutout(texture);
			case TRANSLUCENT -> RenderTypes.entityTranslucent(texture);
		};
	}

	private static int applyAlpha(int argb, float alphaFactor) {
		int alpha = Math.clamp((int) (((argb >>> 24) & 0xFF) * alphaFactor), 0, 255);
		return (alpha << 24) | (argb & 0x00FFFFFF);
	}

	public record ResolvedMaterialDraw(RenderType renderType, int tintArgb) {
	}
}
