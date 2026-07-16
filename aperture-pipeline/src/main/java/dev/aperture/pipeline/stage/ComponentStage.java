package dev.aperture.pipeline.stage;

import dev.aperture.opening.component.ComponentPlan;
import dev.aperture.opening.component.ComponentPlanBuilder;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;

import java.util.Objects;

/**
 * Component assembly stage.
 * <p>
 * Builds the component plan (assembly blueprint) from validated parameters.
 * The component plan describes which components to instantiate and how to
 * assemble them into the final opening geometry.
 * <p>
 * Input: {@link ConstraintStage.ValidatedParameters} (validated parameters + type definition)
 * Output: {@link ComponentPlan} (component assembly blueprint)
 */
public final class ComponentStage implements PipelineStage<ConstraintStage.ValidatedParameters, ComponentPlan> {

	@Override
	public String name() {
		return "component";
	}

	@Override
	public StageResult<ComponentPlan> execute(ConstraintStage.ValidatedParameters input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");

		ctx.debug("Building component plan for " + input.typeDefinition().id());

		try {
			// Build component plan from type definition and parameters
			ComponentPlan plan = ComponentPlanBuilder.build(
				input.typeDefinition(),
				input.parameters()
			);

			ctx.debug("Component plan built with " + plan.steps().size() + " steps");

			return new StageResult.Success<>(plan);

		} catch (Exception e) {
			return new StageResult.Failure<>(
				"Failed to build component plan: " + e.getMessage(),
				e
			);
		}
	}
}
