package dev.aperture.opening.geometry.pipeline;

import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.geometry.recipe.GeometryRecipe;
import dev.aperture.opening.component.ComponentPlan;
import dev.aperture.opening.component.ComponentPlanBuilder;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.opening.pipeline.OpeningGenerationPipeline;

import java.util.List;

/**
 * Legacy facade over {@link OpeningGenerationPipeline} geometry steps.
 *
 * @deprecated Prefer {@link OpeningGenerationPipeline} and explicit layer types.
 */
@Deprecated
public final class OpeningPipeline {
	private final ComponentPlan plan;

	public OpeningPipeline(ComponentPlan plan) {
		this.plan = plan;
	}

	public static OpeningPipeline forAssembly(dev.aperture.core.component.ComponentAssembly assembly) {
		return new OpeningPipeline(ComponentPlanBuilder.buildPlan(assembly));
	}

	public PipelineResult execute(GenerationContext input) {
		return OpeningGenerationPipeline.standard().generate(input);
	}

	public CompiledPipeline compile(OpeningPipelineContext context) {
		return OpeningGenerationPipeline.standard().compile(context.source());
	}

	public GeometryRecipe compileRecipe(OpeningPipelineContext context) {
		return OpeningGenerationPipeline.standard().compileRecipe(context.source());
	}

	public List<PipelineStep> steps() {
		return plan.steps();
	}

	public List<String> stepIds() {
		return plan.stepIds();
	}
}
