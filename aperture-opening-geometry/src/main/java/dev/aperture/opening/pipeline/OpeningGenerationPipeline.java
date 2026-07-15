package dev.aperture.opening.pipeline;

import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.geometry.pipeline.mesh.MeshAssembly;
import dev.aperture.geometry.recipe.GeometryRecipe;
import dev.aperture.opening.component.ComponentPlan;
import dev.aperture.opening.component.ComponentPlanBuilder;
import dev.aperture.opening.geometry.build.CompiledGeometry;
import dev.aperture.opening.geometry.build.GeometryBuilder;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.opening.geometry.pipeline.CompiledPipeline;
import dev.aperture.opening.mesh.MeshBuilder;
import dev.aperture.opening.resolve.OpeningParameterResolver;
import dev.aperture.opening.resolve.ResolvedOpening;

/**
 * Full opening generation stack:
 * <pre>
 * OpeningTypeDefinition (component recipe)
 *   → Parameter Resolver
 *   → Component Plan (one step per component instance)
 *   → Geometry Builder (ShapeRecipe IR)
 *   → Mesh Builder
 * </pre>
 * Renderer consumes {@link PipelineResult} on the client.
 */
public final class OpeningGenerationPipeline {
	private final OpeningParameterResolver parameterResolver;
	private final ComponentPlanBuilder componentPlanBuilder;
	private final GeometryBuilder geometryBuilder;
	private final MeshBuilder meshBuilder;

	public OpeningGenerationPipeline(
		OpeningParameterResolver parameterResolver,
		ComponentPlanBuilder componentPlanBuilder,
		GeometryBuilder geometryBuilder,
		MeshBuilder meshBuilder
	) {
		this.parameterResolver = parameterResolver;
		this.componentPlanBuilder = componentPlanBuilder;
		this.geometryBuilder = geometryBuilder;
		this.meshBuilder = meshBuilder;
	}

	public static OpeningGenerationPipeline standard() {
		return new OpeningGenerationPipeline(
			new OpeningParameterResolver(),
			new ComponentPlanBuilder(),
			new GeometryBuilder(),
			new MeshBuilder()
		);
	}

	public PipelineResult generate(GenerationContext context) {
		CompiledPipeline compiled = compile(context);
		return new PipelineResult(compiled.geometry(), compiled.meshes(), compiled.recipe());
	}

	public GeometryResult generateGeometry(GenerationContext context) {
		return compile(context).geometry();
	}

	public CompiledPipeline compile(GenerationContext context) {
		ResolvedOpening resolved = parameterResolver.resolve(context);
		ComponentPlan plan = componentPlanBuilder.build(context.definition().components());
		CompiledGeometry geometry = geometryBuilder.build(resolved, plan);
		MeshAssembly meshes = meshBuilder.build(geometry.result());
		return new CompiledPipeline(geometry.recipe(), geometry.result(), meshes);
	}

	public GeometryRecipe compileRecipe(GenerationContext context) {
		return compile(context).recipe();
	}

	public ComponentPlan planFor(GenerationContext context) {
		return componentPlanBuilder.build(context.definition().components());
	}
}
