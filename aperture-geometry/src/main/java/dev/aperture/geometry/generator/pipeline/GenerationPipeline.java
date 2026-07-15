package dev.aperture.geometry.generator.pipeline;

import dev.aperture.geometry.model.GeometryResult;

import java.util.Arrays;
import java.util.List;

/**
 * Executes ordered generation stages and returns the assembled geometry result.
 */
public final class GenerationPipeline {
	private final List<GenerationStage> stages;

	public GenerationPipeline(List<GenerationStage> stages) {
		if (stages.isEmpty()) {
			throw new IllegalArgumentException("pipeline requires at least one stage");
		}
		this.stages = List.copyOf(stages);
	}

	public static GenerationPipeline of(GenerationStage... stages) {
		return new GenerationPipeline(Arrays.asList(stages));
	}

	public GeometryResult execute(GenerationContext context) {
		GeometryAssemblyBuilder builder = new GeometryAssemblyBuilder();
		for (GenerationStage stage : stages) {
			stage.contribute(context, builder);
		}
		return builder.build();
	}

	public List<GenerationStage> stages() {
		return stages;
	}
}
