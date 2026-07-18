package dev.aperture.runtime.lifecycle;

import dev.aperture.runtime.model.behavior.BehaviorInstance;
import dev.aperture.runtime.model.capability.CapabilitySet;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.persistence.ArchitecturalObjectSnapshot;
import dev.aperture.runtime.model.state.RuntimeState;
import dev.aperture.runtime.model.state.StateRevision;

import java.util.List;

/** The sole authoritative in-memory representation of one active architectural object. */
public interface RuntimeObjectSession {
	ArchitecturalObjectInstance instance();
	RuntimeState state();
	CapabilitySet capabilities();
	List<BehaviorInstance> behaviors();
	KinematicModel kinematics();
	default long objectRevision() { return instance().revision(); }
	default StateRevision stateRevision() { return state().revision(); }
	long eventSequence();
	DirtyFlags dirtyFlags();
	ArchitecturalObjectSnapshot snapshot();
}
