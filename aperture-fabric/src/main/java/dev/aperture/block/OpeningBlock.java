package dev.aperture.block;

import com.mojang.serialization.MapCodec;
import dev.aperture.block.entity.OpeningBlockEntity;
import dev.aperture.fabric.network.FabricReplicationSink;
import dev.aperture.fabric.runtime.FabricRuntimeLifecycle;
import dev.aperture.runtime.model.event.ActorRef;
import dev.aperture.runtime.model.replication.CommandRejectedMessage;
import dev.aperture.runtime.model.replication.CommandRequestMessage;
import dev.aperture.runtime.model.replication.ObjectSnapshotMessage;
import dev.aperture.runtime.model.replication.ReplicaSnapshot;
import dev.aperture.runtime.replication.AuthoritativeCommandGateway;
import dev.aperture.runtime.replication.JsonReplicationMessageCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Invisible Minecraft anchor; interactions are submitted to the authoritative runtime. */
public final class OpeningBlock extends BaseEntityBlock {
 public static final MapCodec<OpeningBlock> CODEC=simpleCodec(OpeningBlock::new);
 public OpeningBlock(BlockBehaviour.Properties properties){super(properties);}
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
 @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
 @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
 @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
  if(!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
  if(!(level.getBlockEntity(pos) instanceof OpeningBlockEntity anchor)) return InteractionResult.PASS;
  var snapshot=anchor.resolveRuntimeSnapshot().orElse(null); if(snapshot==null) return InteractionResult.PASS;
  var session=FabricRuntimeLifecycle.find(snapshot.instance().objectId()).orElse(null); if(session==null) return InteractionResult.PASS;
  var request=new CommandRequestMessage(AuthoritativeCommandGateway.PROTOCOL_VERSION,snapshot.instance().objectId(),UUID.randomUUID(),"toggle_open",Map.of(),session.objectRevision(),session.stateRevision(),new ActorRef("minecraft:"+player.getUUID()),Instant.now());
  var outcome=FabricRuntimeLifecycle.submit(request); if(outcome.response() instanceof CommandRejectedMessage) return InteractionResult.FAIL;
  var committed=FabricRuntimeLifecycle.find(snapshot.instance().objectId()).orElseThrow();
  var message=new ObjectSnapshotMessage(AuthoritativeCommandGateway.PROTOCOL_VERSION,ReplicaSnapshot.capture(committed.instance(),committed.state()));
  var codec=new JsonReplicationMessageCodec();
  for(var recipient:serverLevel.getServer().getPlayerList().getPlayers()) new FabricReplicationSink(recipient,codec).publish(message);
  return InteractionResult.SUCCESS_SERVER;
 }
 @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new OpeningBlockEntity(pos,state);}
}