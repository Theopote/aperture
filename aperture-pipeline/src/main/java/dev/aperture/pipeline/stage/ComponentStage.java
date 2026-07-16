package dev.aperture.pipeline.stage;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.opening.component.ComponentPlan;
import dev.aperture.opening.component.ComponentPlanBuilder;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;

import java.util.Objects;

/** Builds exactly one component execution plan for a validated opening. */
public final class ComponentStage implements PipelineStage<ConstraintStage.ValidatedParameters, ComponentStage.PlannedOpening> {
	private final ComponentPlanBuilder planner;

	public ComponentStage(ComponentPlanBuilder planner) {
		this.planner = Objects.requireNonNull(planner, "planner cannot be null");
	}

	@Override
	public String name() {
		return "component";
	}

	@Override
	public dev.aperture.pipeline.StageId id() { return dev.aperture.pipeline.StageId.COMPONENT; }

	@Override
	public Class<?> inputType() { return ConstraintStage.ValidatedParameters.class; }

	@Override
	public Class<?> outputType() { return PlannedOpening.class; }

	@Override
	public StageResult<PlannedOpening> execute(ConstraintStage.ValidatedParameters input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");
		try {
			ComponentPlan plan = planner.build(input.typeDefinition().components());
			return new StageResult.Success<>(
				new PlannedOpening(input.typeDefinition(), input.parameters(), plan)
			);
		} catch (Exception exception) {
			return new StageResult.Failure<>("Failed to build component plan: " + exception.getMessage(), exception);
		}
	}

	public record PlannedOpening(
		OpeningTypeDefinition typeDefinition,
		ParameterSet parameters,
		ComponentPlan plan
	) {
		public PlannedOpening {
			Objects.requireNonNull(typeDefinition, "typeDefinition cannot be null");
			Objects.requireNonNull(parameters, "parameters cannot be null");
			Objects.requireNonNull(plan, "plan cannot be null");
		}
	}
}