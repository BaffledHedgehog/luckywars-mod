package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Real native START/tick/STOP/ABORT paths exercise signed progress and independent BE copies. */
public final class NegativeMiningChecks {
    private NegativeMiningChecks() { }
    private static void require(boolean ok,String text) {if(!ok)throw new IllegalStateException("Negative mining: "+text);}
    private static void aim(net.minecraft.server.level.ServerPlayer player,Vec3 point) {
        Vec3 delta=point.subtract(player.getEyePosition());
        player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));
        player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z))));
    }
    private static final class Recording extends Connection {
        final ServerPlayer player;final List<Packet<?>> packets=new ArrayList<>();
        Recording(ServerPlayer player){super(PacketFlow.SERVERBOUND);this.player=player;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public boolean isConnected(){return true;}
        @Override public PacketListener getPacketListener(){return player.connection;}
        @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){collect(packet);}
        @Override public void flushChannel(){ }
        private void collect(Packet<?> packet) {
            if(packet instanceof ClientboundBundlePacket bundle)for(var child:bundle.subPackets())collect(child);
            else packets.add(packet);
        }
        <T> T latest(Class<T> type) {
            for(int i=packets.size()-1;i>=0;i--)if(type.isInstance(packets.get(i)))return type.cast(packets.get(i));
            throw new IllegalStateException("Negative mining: missing outgoing "+type.getSimpleName());
        }
    }
    private static final class Swap implements AutoCloseable {
        final ServerPlayer player;final ServerGamePacketListenerImpl previous;final Recording recording;
        Swap(GameController game,ServerPlayer player) {
            this.player=player;previous=player.connection;recording=new Recording(player);
            player.connection=new ServerGamePacketListenerImpl(game.server(),recording,player,
                    new CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        }
        public void close(){player.connection=previous;previous.resetPosition();}
    }
    public static void run(GameController game) {
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(game.playing() && player!=null && game.allowDamage(player),"actual participant");
        var level=game.maps().level();
        var prior=new GameChecks.WandThirdPlayerState(player,game.items().mana());
        var speed=player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        var water=player.getAttribute(Attributes.SUBMERGED_MINING_SPEED);
        var efficiency=player.getAttribute(Attributes.MINING_EFFICIENCY);
        double oldSpeed=speed.getBaseValue(),oldWater=water.getBaseValue(),oldEfficiency=efficiency.getBaseValue();
        var source=new BlockPos((int)Math.floor(player.getX()/16)*16+8,Math.min(270,level.getMaxY()-10),(int)Math.floor(player.getZ()/16)*16+8);
        var target=source.north();
        var blocks=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
        var initial=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(initial::add);
        int sequence=741;
        try {
            for(var mutable:BlockPos.betweenClosed(source.offset(-2,-3,-5),source.offset(2,3,2))) {
                var pos=mutable.immutable();
                require(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null,"resident fixture");
                blocks.put(pos,game.maps().snapshot(level,pos));
                if(level.getBlockEntity(pos) instanceof net.minecraft.world.Container c)c.clearContent();
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),818);
            }
            player.closeContainer();player.getInventory().clearContent();player.removeAllEffects();
            player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);
            player.teleportTo(level,source.getX()+.5,source.getY()-1,source.getZ()-3.5,Set.of(),0,0,true);
            player.setDeltaMovement(Vec3.ZERO);player.setOnGround(true);
            var log=Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X);
            level.setBlock(source,log,818);speed.setBaseValue(-1);water.setBaseValue(1);efficiency.setBaseValue(0);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_AXE));
            double rate=-log.getDestroyProgress(player,level,source);
            require(rate>0 && rate<1,"negative unbounded native effective progress");
            int period=(int)Math.ceil(1/rate);
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            for(int i=1;i<period;i++)player.gameMode.tick();
            require(level.getBlockState(target).isAir() && level.getBlockState(source)==log,"fractional progress waits without source damage");
            player.gameMode.tick();
            require(level.getBlockState(target)==log && level.getBlockState(source)==log,"exact threshold creates adjacent toward-player oriented copy");
            level.setBlock(target,Blocks.GLASS.defaultBlockState(),818);
            for(int i=0;i<period*2;i++)player.gameMode.tick();
            require(level.getBlockState(target).is(Blocks.GLASS) && level.getBlockState(target.north()).isAir(),"occupied neighbor never overwritten or extended into a line");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            level.setBlock(target,Blocks.AIR.defaultBlockState(),818);for(int i=0;i<period*2;i++)player.gameMode.tick();
            require(level.getBlockState(target).isAir() && level.getBlockState(source)==log,"STOP cancels both custom hold and native delayed destruction");
            speed.setBaseValue(-1000);
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            player.gameMode.tick();require(level.getBlockState(target)==log,"greater magnitude copies on first held tick");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            level.setBlock(target,Blocks.AIR.defaultBlockState(),818);player.gameMode.tick();
            require(level.getBlockState(target).isAir(),"ABORT stops repeat copies");
            // Effective negative efficiency uses the same native route without a second formula.
            speed.setBaseValue(1);efficiency.setBaseValue(-1000);
            require(log.getDestroyProgress(player,level,source)<0,"negative efficiency makes native progress negative with an effective tool");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            player.gameMode.tick();require(level.getBlockState(target)==log,"negative efficiency clones through the shared effective-rate path");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            level.setBlock(target,Blocks.AIR.defaultBlockState(),818);efficiency.setBaseValue(0);water.setBaseValue(-1000);
            var eye=BlockPos.containing(player.getEyePosition());level.setBlock(eye,Blocks.WATER.defaultBlockState(),818);player.baseTick();player.setOnGround(true);
            require(player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER) && log.getDestroyProgress(player,level,source)<0,"native eye-water multiplier produces negative progress");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            player.gameMode.tick();require(level.getBlockState(target)==log,"negative submerged mining clones while eyes are in real source water");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            level.setBlock(eye,Blocks.AIR.defaultBlockState(),818);player.baseTick();player.setOnGround(true);water.setBaseValue(1);
            level.setBlock(target,Blocks.AIR.defaultBlockState(),818);speed.setBaseValue(-1000);
            // Each guard ends the held action; it cannot silently resume when protection ends.
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            game.items().timeStop().begin(level,200);player.gameMode.tick();game.items().timeStop().end(level);player.gameMode.tick();
            require(level.getBlockState(target).isAir(),"Time Stop cancels hold without delayed replay");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            player.entityTags().add("sin_greed_active");player.gameMode.tick();player.entityTags().remove("sin_greed_active");player.gameMode.tick();
            require(level.getBlockState(target).isAir(),"active Greed rejects and clears held cloning");
            player.setGameMode(GameType.CREATIVE);
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            require(level.getBlockState(source)==log,"negative creative START preserves source");player.gameMode.tick();
            require(level.getBlockState(target)==log,"creative negative progress uses same held clone");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            level.setBlock(target,Blocks.AIR.defaultBlockState(),818);
            level.setBlock(source,Blocks.BEDROCK.defaultBlockState(),818);
            require(Blocks.BEDROCK.defaultBlockState().getDestroyProgress(player,level,source)==0
                    && NegativeMiningClient.blocked(player),"unbreakable hardness cannot bypass signed client prohibition");
            require(!player.gameMode.destroyBlock(source),"negative creative direct destruction protects unbreakable blocks too");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            require(level.getBlockState(source).is(Blocks.BEDROCK),"negative creative START cannot bypass zero-progress unbreakable guard");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            level.setBlock(source,log,818);
            player.setGameMode(GameType.SURVIVAL);level.setBlock(target,Blocks.AIR.defaultBlockState(),818);
            require(!player.gameMode.destroyBlock(source) && level.getBlockState(source)==log,
                    "direct native destruction cannot bypass effective negative rate");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            speed.setBaseValue(1000);for(int i=0;i<3;i++)player.gameMode.tick();
            require(level.getBlockState(source)==log,"stray STOP cannot leave delayed work to destroy after sign restoration");
            speed.setBaseValue(-1000);
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            player.setYRot(180);player.gameMode.tick();player.setYRot(0);player.gameMode.tick();
            require(level.getBlockState(target).isAir(),"turning away cancels hold without replay on return");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));player.gameMode.tick();
            require(level.getBlockState(target).isAir(),"changing held source tool cancels hold");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_AXE));
            // Eye-to-center chooses NORTH here, while the actual outlined hit enters the WEST face.
            player.teleportTo(level,source.getX()-1.5,source.getY()-1,source.getZ()-3.1,Set.of(),0,0,true);
            aim(player,new Vec3(source.getX()+.1,source.getY()+.5,source.getZ()+.99));
            var oblique=NegativeMining.aimedBlock(player);
            require(oblique!=null && oblique.getBlockPos().equals(source) && oblique.getDirection()==Direction.WEST,
                    "oblique native outline hit selects WEST despite nearest center direction NORTH");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            player.gameMode.tick();
            require(level.getBlockState(source.west())==log && level.getBlockState(target).isAir(),"copy follows exact aimed WEST surface, ignoring inaccurate packet face");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,Direction.WEST,level.getMaxY(),sequence++);
            level.setBlock(source.west(),Blocks.AIR.defaultBlockState(),818);
            // A bottom slab's top surface is at half height, not the full block boundary.
            level.setBlock(source,Blocks.OAK_SLAB.defaultBlockState(),818);
            player.teleportTo(level,source.getX()+.5,source.getY(),source.getZ()-1.5,Set.of(),0,0,true);
            aim(player,new Vec3(source.getX()+.5,source.getY()+.5,source.getZ()+.5));
            var slabHit=NegativeMining.aimedBlock(player);
            require(slabHit!=null && slabHit.getBlockPos().equals(source) && slabHit.getDirection()==Direction.UP
                    && Math.abs(slabHit.getLocation().y-source.getY()-.5)<1e-6,"native slab top outline face at half height");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            player.gameMode.tick();
            require(level.getBlockState(source.above()).is(Blocks.OAK_SLAB),"slab copy grows from its actually aimed top");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,Direction.UP,level.getMaxY(),sequence++);
            level.setBlock(source.above(),Blocks.AIR.defaultBlockState(),818);
            player.teleportTo(level,source.getX()+.5,source.getY()-1,source.getZ()-3.5,Set.of(),0,0,true);
            level.setBlock(source,log,818);
            try(var swap=new Swap(game,player)) {
                player.setGameMode(GameType.CREATIVE);speed.setBaseValue(-1000);
                player.connection.send(new ClientboundUpdateAttributesPacket(player.getId(),List.of(efficiency)));
                var projected=swap.recording.latest(ClientboundUpdateAttributesPacket.class);
                require(projected.getValues().stream().anyMatch(value->value.attribute().equals(Attributes.BLOCK_BREAK_SPEED)
                        && value.base()==0 && value.modifiers().isEmpty()),"native outgoing efficiency update includes zero owner break speed");
                player.onUpdateAbilities();
                require(!swap.recording.latest(ClientboundPlayerAbilitiesPacket.class).canInstabuild()
                        && player.getAbilities().instabuild,"native creative ability resend disables only client instant break");
                // Incoming real packet listener path, including native sequence acknowledgement admission.
                player.connection.handlePlayerAction(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,source,Direction.NORTH,sequence++));
                player.gameMode.tick();
                require(level.getBlockState(source)==log && level.getBlockState(target)==log,
                        "real connection START preserves negative source and grows a copy");
                player.connection.handlePlayerAction(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,source,Direction.NORTH,sequence++));
                level.setBlock(target,Blocks.AIR.defaultBlockState(),818);speed.setBaseValue(1);player.gameMode.tick();
                require(swap.recording.latest(ClientboundPlayerAbilitiesPacket.class).canInstabuild(),"positive transition restores client creative instant-build flag");
                var restored=swap.recording.latest(ClientboundUpdateAttributesPacket.class);
                require(restored.getValues().stream().anyMatch(value->value.attribute().equals(Attributes.BLOCK_BREAK_SPEED)
                        && value.base()==speed.getBaseValue()),"positive transition restores actual native speed snapshot");
                player.setGameMode(GameType.SURVIVAL);water.setBaseValue(-1000);
                require(log.getDestroyProgress(player,level,source)>0 && !NegativeMiningClient.blocked(player),
                        "negative submerged attribute on dry eyes does not prohibit positive mining");
                water.setBaseValue(1);speed.setBaseValue(-1000);
            }
            // Copy complete independently owned native block-entity state, including intentional inventory duplicates.
            level.setBlock(source,Blocks.CHEST.defaultBlockState(),818);
            var chest=(ChestBlockEntity)level.getBlockEntity(source);require(chest!=null,"native source chest");
            var diamonds=new ItemStack(Items.DIAMOND,13);chest.setItem(0,diamonds);chest.setChanged();
            var beforeCopy=game.maps().snapshot(level,source);
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            player.gameMode.tick();
            var copy=(ChestBlockEntity)level.getBlockEntity(target);
            require(copy!=null && copy!=chest && copy.getBlockPos().equals(target)
                    && ItemStack.isSameItemSameComponents(copy.getItem(0),diamonds) && copy.getItem(0).getCount()==13
                    && chest.getItem(0).getCount()==13,"independent native chest copy intentionally retains complete contents and destination position");
            var sourceData=beforeCopy.entity().copy();var destinationData=game.maps().snapshot(level,target).entity().copy();
            for(var key:List.of("x","y","z")){sourceData.remove(key);destinationData.remove(key);}
            require(sourceData.equals(destinationData),"full block entity metadata/components copied except destination coordinates");
            copy.getItem(0).shrink(5);require(chest.getItem(0).getCount()==13,"copied inventory is deeply independent from source");
            require(level.getBlockState(source)==beforeCopy.state(),"container source state remains unchanged");
            for(var e:level.getAllEntities())if(!initial.contains(e) && e instanceof ItemEntity)require(e.isRemoved(),"cloning emits no block or inventory drops");
            // The positive control intentionally produces ordinary loot through CreativeBlockDropsMixin.
            // Run it after the unchanged no-drops assertion covering all negative cloning operations.
            chest.clearContent();level.setBlock(source,log,818);
            speed.setBaseValue(1);player.setGameMode(GameType.CREATIVE);
            require(player.gameMode.destroyBlock(source) && level.getBlockState(source).isAir(),
                    "positive native creative direct destruction remains functional");
        } finally {
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence);
            game.items().timeStop().end(level);player.entityTags().remove("sin_greed_active");
            for(var entry:blocks.entrySet()) {
                if(level.getBlockEntity(entry.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();
                game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());
            }
            speed.setBaseValue(oldSpeed);water.setBaseValue(oldWater);efficiency.setBaseValue(oldEfficiency);
            prior.restore(game);player.baseTick();
            var cleanup=new ArrayList<Entity>();for(var actor:level.getAllEntities())if(!initial.contains(actor))cleanup.add(actor);
            cleanup.forEach(Entity::discard);
        }
        SkyWars2.LOGGER.info("[check] PASS negative-mining");
    }
}
