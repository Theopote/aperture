package dev.aperture.runtime.model.behavior;

import dev.aperture.math.Transform3d;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.runtime.model.capability.CapabilitySet;
import dev.aperture.runtime.model.event.EventContext;
import dev.aperture.runtime.model.event.EventEnvelope;
import dev.aperture.runtime.model.event.ObjectRef;
import dev.aperture.runtime.model.event.ObjectRemovedEvent;
import dev.aperture.runtime.model.event.PlayerInteractEvent;
import dev.aperture.runtime.model.event.SpatialRef;
import dev.aperture.runtime.model.event.StandardEventTypes;
import dev.aperture.runtime.model.event.WorldRef;
import dev.aperture.runtime.model.object.ArchitecturalFamilyId;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.object.ArchitecturalTypeId;
import dev.aperture.runtime.model.state.RuntimeState;
import dev.aperture.runtime.model.state.StateDistribution;
import dev.aperture.runtime.model.state.StatePatch;
import dev.aperture.runtime.model.state.StatePersistence;
import dev.aperture.runtime.model.state.StatePropertyDefinition;
import dev.aperture.runtime.model.state.StateRevision;
import dev.aperture.runtime.model.state.StateSchema;
import dev.aperture.runtime.model.state.StateValue;
import dev.aperture.runtime.model.world.WorldQuery;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BehaviorEngineTest {
	@Test
	void evaluatesMatchingBehaviorsInOrderAndReturnsStateIntent() {
		BehaviorContext context = context();
		List<String> calls = new ArrayList<>();
		BehaviorInstance first = behavior("first", ctx -> {
			calls.add("first");
			return new BehaviorResult(List.of(), List.of(new StatePatch(
				StateRevision.INITIAL, Map.of("targetOpenRatio", StateValue.number(1)), Instant.EPOCH)),
				List.of(), List.of());
		});
		BehaviorInstance ignored = new TestBehavior(
			new BehaviorDefinition(new BehaviorId("test:ignored"), 1,
				Set.of(StandardEventTypes.OBJECT_REMOVED), Map.of()),
			ctx -> { calls.add("ignored"); return BehaviorResult.empty(); });
		BehaviorInstance second = behavior("second", ctx -> {
			calls.add("second");
			return BehaviorResult.empty();
		});

		BehaviorResult result = new DeterministicBehaviorEngine().evaluate(List.of(first, ignored, second), context);

		assertEquals(List.of("first", "second"), calls);
		assertEquals(1, result.statePatches().size());
		assertEquals(0, result.diagnostics().size());
		assertEquals(StateRevision.INITIAL, context.state().revision());
	}

	@Test
	void isolatesBehaviorFailureAsDiagnostic() {
		BehaviorInstance failing = behavior("failing", ignored -> { throw new IllegalStateException("denied"); });
		BehaviorResult result = new DeterministicBehaviorEngine().evaluate(List.of(failing), context());
		assertEquals(1, result.diagnostics().size());
		assertEquals("behavior.evaluation_failed", result.diagnostics().getFirst().code());
	}

	private static BehaviorInstance behavior(String id, Evaluator evaluator) {
		return new TestBehavior(new BehaviorDefinition(
			new BehaviorId("test:" + id), 1, Set.of(StandardEventTypes.PLAYER_INTERACT), Map.of()), evaluator);
	}

	private static BehaviorContext context() {
		ArchitecturalObjectId id = ArchitecturalObjectId.random();
		ArchitecturalObjectInstance instance = new ArchitecturalObjectInstance(
			1, id, ArchitecturalTypeId.parse("aperture:door"), new ArchitecturalFamilyId("aperture:opening"),
			ParameterSet.empty(), Transform3d.identity(), List.of(), Map.of(), 0, Map.of());
		StateSchema schema = StateSchema.builder("aperture:door", 1)
			.property("targetOpenRatio", StatePropertyDefinition.number(
				0, 0.0, 1.0, StatePersistence.TRANSIENT, StateDistribution.REPLICATED))
			.build();
		WorldRef world = new WorldRef("test:world");
		ObjectRef target = new ObjectRef(id);
		PlayerInteractEvent interaction = new PlayerInteractEvent(
			target, "toggle_open", new SpatialRef(world, dev.aperture.math.Vec3d.ZERO, "panel"));
		return new BehaviorContext(
			instance,
			RuntimeState.initial(schema),
			ParameterSet.empty(),
			CapabilitySet.empty(),
			EventEnvelope.create(StandardEventTypes.PLAYER_INTERACT, interaction, EventContext.system(world), Instant.EPOCH, 0),
			WorldQuery.unavailable()
		);
	}

	@FunctionalInterface
	private interface Evaluator { BehaviorResult evaluate(BehaviorContext context); }

	private record TestBehavior(BehaviorDefinition definition, Evaluator evaluator) implements BehaviorInstance {
		@Override public BehaviorResult evaluate(BehaviorContext context) { return evaluator.evaluate(context); }
	}
}
