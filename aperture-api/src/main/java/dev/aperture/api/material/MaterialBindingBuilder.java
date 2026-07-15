package dev.aperture.api.material;

import dev.aperture.api.registry.MaterialResolverRegistry;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.render.data.PartId;
import dev.aperture.render.material.BlendMode;
import dev.aperture.render.material.MaterialBinding;
import dev.aperture.render.material.MaterialBindingSet;
import dev.aperture.render.material.MaterialInstance;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds a {@link MaterialBindingSet} from generated geometry and a resolver registry.
 */
public final class MaterialBindingBuilder {
	private MaterialBindingBuilder() {
	}

	public static MaterialBindingSet build(
		OpeningTypeDefinition definition,
		OpeningInstance instance,
		GeometryResult geometry,
		MaterialResolverRegistry materials
	) {
		ParameterSet parameters = ParameterSet.mergeDefaults(definition.parameters(), instance.parameters());
		Map<PartId, MaterialBinding> bindings = new LinkedHashMap<>();

		for (GeometrySolid solid : geometry.solids()) {
			MaterialResolveContext context = new MaterialResolveContext(
				solid.materialSlot(),
				solid.layer(),
				instance.typeId(),
				definition,
				parameters
			);
			MaterialInstance material = materials.resolve(context);
			BlendMode blendMode = blendModeFor(solid.layer(), material.definition().blendMode());
			bindings.put(
				PartId.of(solid.componentPath()),
				new MaterialBinding(
					PartId.of(solid.componentPath()),
					solid.materialSlot(),
					solid.layer(),
					material,
					blendMode
				)
			);
		}

		return new MaterialBindingSet(bindings);
	}

	private static BlendMode blendModeFor(GeometryLayer layer, BlendMode resolved) {
		return switch (layer) {
			case OPAQUE -> BlendMode.OPAQUE;
			case CUTOUT -> BlendMode.CUTOUT;
			case TRANSLUCENT -> BlendMode.TRANSLUCENT;
		};
	}
}
