package dev.aperture.runtime.model.behavior;

import java.util.List;

/** Evaluates matching behaviors in caller-defined order and isolates failures as diagnostics. */
public final class DeterministicBehaviorEngine implements BehaviorEngine {
	@Override
	public BehaviorResult evaluate(List<? extends BehaviorInstance> behaviors, BehaviorContext context) {
		BehaviorResult result = BehaviorResult.empty();
		for (BehaviorInstance behavior : List.copyOf(behaviors)) {
			if (!behavior.definition().accepts(context.event().type())) continue;
			try {
				BehaviorResult evaluated = behavior.evaluate(context);
				if (evaluated == null) throw new IllegalStateException("Behavior returned null");
				result = result.combine(evaluated);
			} catch (RuntimeException failure) {
				result = result.combine(new BehaviorResult(
					List.of(), List.of(), List.of(),
					List.of(BehaviorDiagnostic.error(
						"behavior.evaluation_failed",
						behavior.definition().id().value() + ": " + failure.getMessage()
					))
				));
			}
		}
		return result;
	}
}
