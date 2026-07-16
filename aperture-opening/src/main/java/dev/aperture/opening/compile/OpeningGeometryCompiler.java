package dev.aperture.opening.compile;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.opening.component.ComponentPlan;
import dev.aperture.opening.geometry.build.CompiledGeometry;
import dev.aperture.opening.geometry.build.GeometryBuilder;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.opening.resolve.OpeningParameterResolver;
import dev.aperture.parameter.ParameterSet;

import java.util.Objects;

/** Compiles a previously planned opening into recipe IR and evaluated geometry. */
public final class OpeningGeometryCompiler {
	private final OpeningParameterResolver layoutResolver;
	private final GeometryBuilder geometryBuilder;

	public OpeningGeometryCompiler() {
		this(new OpeningParameterResolver(), new GeometryBuilder());
	}

	public OpeningGeometryCompiler(
		OpeningParameterResolver layoutResolver,
		GeometryBuilder geometryBuilder
	) {
		this.layoutResolver = Objects.requireNonNull(layoutResolver, "layoutResolver cannot be null");
		this.geometryBuilder = Objects.requireNonNull(geometryBuilder, "geometryBuilder cannot be null");
	}

	public CompiledGeometry compile(
		OpeningTypeDefinition definition,
		ParameterSet parameters,
		ComponentPlan plan,
		ProfileCatalogRegistry profiles
	) {
		Objects.requireNonNull(definition, "definition cannot be null");
		Objects.requireNonNull(parameters, "parameters cannot be null");
		Objects.requireNonNull(plan, "plan cannot be null");
		Objects.requireNonNull(profiles, "profiles cannot be null");
		GenerationContext context = new GenerationContext(definition, parameters, profiles);
		return geometryBuilder.build(layoutResolver.resolve(context), plan);
	}
}