package dev.aperture.opening.component;

import dev.aperture.core.component.ComponentAssemblyPresets;
import dev.aperture.core.component.MullionComponent;
import dev.aperture.opening.geometry.pipeline.ComponentPipelineStep;
import org.junit.jupiter.api.Test;

import java.util.List;

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
		long mullionSteps = plan.steps().stream()
			.filter(ComponentPipelineStep.class::isInstance)
			.map(ComponentPipelineStep.class::cast)
			.filter(step -> step.component() instanceof MullionComponent)
			.count();
		assertEquals(2, mullionSteps);
	}

	@Test
	void stepIdsMatchComponentInstanceIdsInGeometryOrder() {
		var assembly = ComponentAssemblyPresets.door(
			"aperture:frame_standard_50",
			"aperture:frame_standard_50",
			"aperture:single_glazed",
			"left"
		);
		var plan = ComponentPlanBuilder.buildPlan(assembly);

		assertEquals(
			List.of("door_frame", "door_leaf", "door_glass", "threshold", "hinges", "handle"),
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
