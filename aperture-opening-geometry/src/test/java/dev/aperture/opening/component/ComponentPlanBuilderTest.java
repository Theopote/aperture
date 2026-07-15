package dev.aperture.opening.component;

import dev.aperture.core.component.ComponentAssemblyPresets;
import dev.aperture.core.component.DividerComponent;
import dev.aperture.opening.geometry.pipeline.ComponentPipelineStep;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ComponentPlanBuilderTest {
	@Test
	void buildsOneBoundStepPerComponentInstance() {
		var plan = ComponentPlanBuilder.buildPlan(ComponentAssemblyPresets.curtainWall(
			"aperture:frame_l_50x80",
			"aperture:single_glazed"
		));

		assertEquals(6, plan.steps().size());
		long dividerSteps = plan.steps().stream()
			.filter(ComponentPipelineStep.class::isInstance)
			.map(ComponentPipelineStep.class::cast)
			.filter(step -> step.component() instanceof DividerComponent)
			.count();
		assertEquals(2, dividerSteps);
	}

	@Test
	void stepIdsMatchComponentInstanceIds() {
		var assembly = ComponentAssemblyPresets.door(
			"aperture:frame_standard_50",
			"aperture:frame_standard_50",
			"aperture:single_glazed",
			"left"
		);
		var plan = ComponentPlanBuilder.buildPlan(assembly);

		assertEquals(
			assembly.all().stream().map(component -> component.ref().id()).toList(),
			plan.stepIds().subList(0, assembly.size())
		);
	}

	@Test
	void implicitMullionStepUsesSyntheticDividerId() {
		var assembly = ComponentAssemblyPresets.door(
			"aperture:frame_standard_50",
			"aperture:frame_standard_50",
			"aperture:single_glazed",
			"left"
		);
		var plan = ComponentPlanBuilder.buildPlan(assembly);

		assertInstanceOf(ComponentPipelineStep.class, plan.steps().getLast());
		assertEquals("_mullions", plan.steps().getLast().id());
	}
}
