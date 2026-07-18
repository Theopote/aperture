package dev.aperture.gametest;

import dev.aperture.block.entity.OpeningBlockEntity;
import dev.aperture.math.Transform3d;
import dev.aperture.opening.runtime.DoorStateSchema;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.registry.ApertureBlocks;
import dev.aperture.runtime.model.object.*;
import dev.aperture.runtime.model.persistence.ArchitecturalObjectSnapshot;
import dev.aperture.runtime.model.event.ActorRef;
import dev.aperture.runtime.model.replication.CommandAcceptedMessage;
import dev.aperture.runtime.model.replication.CommandRequestMessage;
import dev.aperture.runtime.replication.AuthoritativeCommandGateway;
import dev.aperture.runtime.model.state.RuntimeState;
import dev.aperture.fabric.runtime.FabricRuntimeLifecycle;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.util.UUID;

/** Dedicated-server proof that a real BlockEntity activates the unified runtime session. */
public final class DoorRuntimeGameTests {
	@GameTest(maxTicks = 20)
	public void blockEntityActivatesAndSnapshotsDoorSession(GameTestHelper helper) {
		BlockPos relative = new BlockPos(1, 1, 1);
		helper.setBlock(relative, ApertureBlocks.OPENING);
		OpeningBlockEntity anchor = (OpeningBlockEntity) helper.getLevel()
			.getBlockEntity(helper.absolutePos(relative));
		helper.assertTrue(anchor != null, "OpeningBlockEntity must be created in the server world");

		ArchitecturalObjectInstance instance = new ArchitecturalObjectInstance(
			1, ArchitecturalObjectId.random(), ArchitecturalTypeId.parse("aperture:door"),
			new ArchitecturalFamilyId("aperture:opening"), ParameterSet.empty(), Transform3d.identity(),
			List.of(), Map.of(), 0, Map.of());
		RuntimeState state = RuntimeState.initial(DoorStateSchema.SCHEMA);
		anchor.setRuntimeSnapshot(ArchitecturalObjectSnapshot.capture(instance, state, List.of()));

		var session = FabricRuntimeLifecycle.find(instance.objectId()).orElse(null);
		helper.assertTrue(session != null, "BlockEntity must activate the authoritative runtime session");
		helper.assertTrue(session.objectRevision() == 0, "Initial object revision must survive activation");
		var outcome = FabricRuntimeLifecycle.submit(new CommandRequestMessage(
			AuthoritativeCommandGateway.PROTOCOL_VERSION, instance.objectId(), UUID.randomUUID(),
			"toggle_open", Map.of(), session.objectRevision(), session.stateRevision(),
			new ActorRef("gametest:player"), Instant.now()));
		helper.assertTrue(outcome.response() instanceof CommandAcceptedMessage,
			"Dedicated server must accept the Door interaction command");
		helper.assertTrue(FabricRuntimeLifecycle.find(instance.objectId()).orElseThrow().objectRevision() == 1,
			"Accepted command must commit exactly one authoritative revision");
		helper.assertTrue(anchor.resolveRuntimeSnapshot().orElseThrow().instance().objectId().equals(instance.objectId()),
			"BlockEntity snapshot and active session must retain one object identity");
		helper.succeed();
	}
}
