package dev.aperture.opening.geometry.pipeline;

import dev.aperture.opening.geometry.pipeline.accessory.AccessoryGenerator;
import dev.aperture.geometry.pipeline.assembly.GeometryAssemblyBuilder;
import dev.aperture.opening.geometry.pipeline.frame.FrameGenerator;
import dev.aperture.opening.geometry.pipeline.glass.GlassGenerator;
import dev.aperture.geometry.pipeline.mesh.MeshAssembler;
import dev.aperture.opening.geometry.pipeline.panel.PanelGenerator;
import dev.aperture.opening.geometry.pipeline.profile.ProfileGenerator;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;

import java.util.Arrays;
import java.util.List;

/**
 * Opening assembly pipeline: component steps produce kernel solids, then mesh assembly.
 */
public final class OpeningPipeline {
	public static final List<String> STEP_ORDER = List.of(
		ProfileGenerator.STEP_ID,
		FrameGenerator.STEP_ID,
		PanelGenerator.STEP_ID,
		GlassGenerator.STEP_ID,
		AccessoryGenerator.STEP_ID
	);

	private final List<PipelineStep> steps;
	private final MeshAssembler meshAssembler;

	public OpeningPipeline(List<PipelineStep> steps, MeshAssembler meshAssembler) {
		if (steps.isEmpty()) {
			throw new IllegalArgumentException("pipeline requires at least one step");
		}
		this.steps = List.copyOf(steps);
		this.meshAssembler = meshAssembler;
	}

	public static OpeningPipeline standard() {
		return new OpeningPipeline(
			List.of(
				new ProfileGenerator(),
				new FrameGenerator(),
				new PanelGenerator(),
				new GlassGenerator(),
				new AccessoryGenerator()
			),
			new MeshAssembler()
		);
	}

	public static OpeningPipeline of(PipelineStep... steps) {
		return new OpeningPipeline(Arrays.asList(steps), new MeshAssembler());
	}

	public PipelineResult execute(GenerationContext input) {
		return execute(OpeningPipelineContext.from(input));
	}

	public PipelineResult execute(OpeningPipelineContext context) {
		GeometryAssemblyBuilder assembly = new GeometryAssemblyBuilder();
		for (PipelineStep step : steps) {
			step.execute(context, assembly);
		}
		var geometry = assembly.build();
		var meshes = meshAssembler.assemble(geometry);
		return new PipelineResult(geometry, meshes);
	}

	public List<PipelineStep> steps() {
		return steps;
	}

	public List<String> stepIds() {
		return steps.stream().map(PipelineStep::id).toList();
	}
}
