package dev.aperture.client.render.material;

import dev.aperture.render.material.BlendMode;
import dev.aperture.render.material.MaterialInstance;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Maps platform-agnostic material instances to Fabric draw state.
 */
public final class FabricMaterialGraphics {
	private FabricMaterialGraphics() {
	}

	public static ResolvedMaterialDraw resolve(MaterialInstance material) {
		Identifier texture = Identifier.parse(material.definition().albedoTexture());
		RenderType renderType = renderTypeFor(texture, material.definition().blendMode());
		return new ResolvedMaterialDraw(renderType, material.tintArgb());
	}

	private static RenderType renderTypeFor(Identifier texture, BlendMode blendMode) {
		return switch (blendMode) {
			case OPAQUE -> RenderTypes.entitySolid(texture);
			case CUTOUT -> RenderTypes.entityCutout(texture);
			case TRANSLUCENT -> RenderTypes.entityTranslucent(texture);
		};
	}

	public record ResolvedMaterialDraw(RenderType renderType, int tintArgb) {
	}
}
