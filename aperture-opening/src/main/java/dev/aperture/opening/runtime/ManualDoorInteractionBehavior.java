package dev.aperture.opening.runtime;

import dev.aperture.runtime.model.behavior.BehaviorDefinition;
import dev.aperture.runtime.model.behavior.BehaviorId;
import dev.aperture.runtime.model.behavior.BehaviorInstance;
import dev.aperture.runtime.model.behavior.BehaviorContext;
import dev.aperture.runtime.model.behavior.BehaviorResult;
import dev.aperture.runtime.model.command.ArchitecturalCommand;
import dev.aperture.runtime.model.command.RequestCloseCommand;
import dev.aperture.runtime.model.command.RequestOpenCommand;
import dev.aperture.runtime.model.event.PlayerInteractEvent;
import dev.aperture.runtime.model.event.StandardEventTypes;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Converts the platform-neutral toggle interaction into the shared Command API. */
public final class ManualDoorInteractionBehavior implements BehaviorInstance {
	private static final BehaviorDefinition DEFINITION = new BehaviorDefinition(
		new BehaviorId("aperture:manual_door_interaction"), 1,
		Set.of(StandardEventTypes.PLAYER_INTERACT), Map.of());

	@Override public BehaviorDefinition definition() { return DEFINITION; }

	@Override
	public BehaviorResult evaluate(BehaviorContext context) {
		if (!(context.event().event() instanceof PlayerInteractEvent interaction)
			|| !interaction.interactionId().equals("toggle_open")) return BehaviorResult.empty();

		double current = DoorStateSchema.number(context.state(), DoorStateSchema.OPEN_RATIO);
		double target = DoorStateSchema.number(context.state(), DoorStateSchema.TARGET_OPEN_RATIO);
		ArchitecturalCommand command = target > 0 || current > 0
			? new RequestCloseCommand(interaction.target())
			: new RequestOpenCommand(interaction.target());
		return new BehaviorResult(List.of(command), List.of(), List.of(), List.of());
	}
}
