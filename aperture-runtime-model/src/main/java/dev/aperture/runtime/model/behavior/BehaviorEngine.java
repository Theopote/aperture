package dev.aperture.runtime.model.behavior;

import java.util.List;

public interface BehaviorEngine {
	BehaviorResult evaluate(List<? extends BehaviorInstance> behaviors, BehaviorContext context);
}
