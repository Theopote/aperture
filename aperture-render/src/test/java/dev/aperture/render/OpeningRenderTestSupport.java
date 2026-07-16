package dev.aperture.render;

import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.opening.compile.OpeningGeometryCompiler;
import dev.aperture.opening.compile.OpeningMeshCompiler;
import dev.aperture.opening.component.ComponentPlanBuilder;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;

public final class OpeningRenderTestSupport {
	private static final ComponentPlanBuilder PLANNER = new ComponentPlanBuilder();
	private static final OpeningGeometryCompiler GEOMETRY = new OpeningGeometryCompiler();
	private static final OpeningMeshCompiler MESH = new OpeningMeshCompiler();

	private OpeningRenderTestSupport() {
	}

	public static PipelineResult compile(GenerationContext context) {
		var plan = PLANNER.build(context.definition().components());
		var geometry = GEOMETRY.compile(
			context.definition(), context.parameters(), plan, context.profiles()
		);
		return new PipelineResult(
			geometry.result(), MESH.compile(geometry.result()), geometry.recipe()
		);
	}
}