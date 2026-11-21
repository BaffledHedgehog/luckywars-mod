package me.skywars2.game.items.gadgets;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.UUID;

import me.skywars2.game.GameController;
import me.skywars2.game.GameController.Stage;
import me.skywars2.game.combat.LbcIndirectKills;
import me.skywars2.game.effects.ItemEffectLevels;
import me.skywars2.game.effects.magic.LbcOverload;
import me.skywars2.game.entities.LbcDimensionItems;
import me.skywars2.game.entities.drones.LbcFpvDrone;
import me.skywars2.game.items.ItemData;
import me.skywars2.game.items.wands.LbcFartus;
import me.skywars2.game.items.wands.LbcStonekivi;
import me.skywars2.game.match.checkpoints.MatchPlayerCheckpoint;
import me.skywars2.game.match.checkpoints.MatchQueuedWorkCheckpoint;
import me.skywars2.game.skills.witch.LbcGreed;
import me.skywars2.game.world.maps.MapManager;
import me.skywars2.game.world.terrain.EffectTerrain;
import me.skywars2.game.world.terrain.LbcDropBlocks;
import me.skywars2.game.world.terrain.TeleportMomentum;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import me.skywars2.game.effects.magic.LbcMagic;

/** Native teleport charges and accepted IMBA continuations share the existing map/work/checkpoint owners. */
public final class LbcTeleportGadgets {
    public static final Identifier CHARGE_SOUND=Identifier.parse("minecraft:lbcsounds.swap_charge");
    static final Identifier SUCCESS_SOUND=Identifier.parse("minecraft:lbcsounds.swap_go");
    static final String FAILURE_KEY="lbc.swapper.charge_failed";
    private final GameController game;
    private final Map<ServerPlayer,Charge> charges=new IdentityHashMap<>();
    /** Unconfigured means disabled. Root supplies the author's admission and canonical terrain policy. */
    public interface ImbaPolicy {
        boolean launch(ServerPlayer player);
        boolean write(ServerPlayer author,ServerLevel level,BlockPos at,MapManager.BlockSnapshot value,LbcIndirectKills.Owner cause);
    }
    private ImbaPolicy imbaPolicy;
    public void configureImba(ImbaPolicy policy){imbaPolicy=Objects.requireNonNull(policy);}
    public record ImbaDelay(UUID owner,ServerLevel level,String epoch,int remaining,int createdTick,LbcIndirectKills.Owner cause){}
    public record ImbaPad(UUID owner,ServerLevel level,BlockPos origin,int cursor,long writes){}
    public record Frozen(List<ImbaDelay> delays){public Frozen{delays=List.copyOf(delays);}}
    private final Map<UUID,ImbaDelay> imbaDelays=new LinkedHashMap<>();
    private final List<PadWork> pads=new ArrayList<>();
    private boolean restoring,advancing;private int callbacks;
    public int pendingImba(){return imbaDelays.size();}
    public List<ImbaDelay> imbaDelays(){return List.copyOf(imbaDelays.values());}
    public List<ImbaPad> imbaPads(){return pads.stream().map(p->new ImbaPad(p.author.getUUID(),p.level,p.origin,p.cursor,p.writes)).toList();}
    public Frozen frozen(){checkpointBoundary();return new Frozen(List.copyOf(imbaDelays.values()));}
    public void restoreFrozen(Frozen image){checkpointBoundary();imbaDelays.clear();for(var value:image.delays)imbaDelays.put(value.owner,value);}
    public static BlockPos imbaPadOrigin(Vec3 current){int y=net.minecraft.util.Mth.floor(current.y);if(y<=1||y>=321)y=320;
        return new BlockPos(net.minecraft.util.Mth.floor(current.x)-2,y-2,net.minecraft.util.Mth.floor(current.z)-2);}

    private static final class Charge {
        final InteractionHand hand;final ItemStack stack;final Vec3 origin;final ServerLevel level;final String epoch;
        final List<ServerPlayer> listeners=new ArrayList<>();
        final int createdTick;int remaining=10;
        Charge(ServerPlayer owner,InteractionHand hand,String epoch){this.hand=hand;stack=owner.getItemInHand(hand);origin=owner.position();level=owner.level();this.epoch=epoch;createdTick=level.getServer().getTickCount();}
    }
    public LbcTeleportGadgets(GameController game){this.game=game;}
    private static int kind(ItemStack stack){
        if(stack.isEmpty())return 0;
        if(stack.is(LbcGadgetItems.LEGACY_SWAPPER))return 5;
        if(ItemData.intValue(stack,"teleport1",0)==1&&ItemData.intValue(stack,"gadget",0)==1)return 3;
        var data=LbcFartus.data(stack);
        if(data.getIntOr("teleport1_imba",0)==1&&data.getIntOr("gadget",0)==1)return 4;
        return data.getIntOr("islandtp",0)==1?1:data.getIntOr("randtp",0)==1?2:0;
    }
    public static boolean matches(ItemStack stack){return kind(stack)!=0;}
    private boolean suppressed(ServerPlayer player){
        var magic=game.items().magic();
        return magic.mechanismSuppressed(player)||magic.mechanismSuppressedAt(player.level(),player.getEyePosition())
                ||magic.artifactSuppressedAt(player.level(),player.position())||magic.artifactSuppressedAt(player.level(),player.getEyePosition());
    }
    private boolean eligible(ServerPlayer player){
        return player.isAlive()&&!player.isRemoved()&&player.level()==game.maps().level()
                &&game.canUseGameplayItems(player)&&!game.isObserver(player)&&!game.awaitingRespawn(player.getUUID())
                &&game.stage()!=GameController.Stage.ENDING&&LbcDimensionItems.ordinary(player)
                &&game.items().wands().validRoleTarget(player,false);
    }
    private boolean paused(ServerPlayer player){
        return !game.server().tickRateManager().runsNormally()||game.items().wands().inputTimeStopped(player)
                ||game.items().wands().timeStopped()||game.items().timeStop().stopped(player.level())||game.items().timeStop().frozen(player);
    }
    public boolean use(ServerPlayer player,InteractionHand hand){
        var stack=player.getItemInHand(hand);int kind=kind(stack);
        if(restoring||kind==0||!eligible(player)||game.items().actionsBlocked(player)||paused(player))return false;
        if(kind==4)return imba(player,stack);
        if(kind==3){
            var from=player.position();if(game.items().magic().mechanismSuppressedAt(player.level(),from,24))return false;
            var destination=from.add(player.getLookAngle().scale(5));
            if(!move(player,destination,player.getYRot(),player.getXRot()))return false;
            player.level().sendParticles(ParticleTypes.REVERSE_PORTAL,from.x,from.y+1,from.z,200,.2,.4,.2,.3);
            LbcOverload.sound(player,from,"minecraft:entity.enderman.teleport",10,1,20);
            debit(player,stack);return true;
        }
        if(suppressed(player))return false;
        if(kind==5)return game.balanceLegacy()&&legacySwap(player,stack);
        if(kind==1){if(!island(player))return false;debit(player,stack);return true;}
        // Held use and the other hand cannot restart an existing charge.
        if(charges.containsKey(player))return false;
        var charge=new Charge(player,hand,game.items().epoch());
        charges.put(player,charge);
        startSound(player,charge);
        return true;
    }
    private void debit(ServerPlayer player,ItemStack stack){
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));stack.consume(1,player);
        player.getInventory().setChanged();game.items().magic().inventoryChanged(player);player.containerMenu.broadcastChanges();
    }
    private boolean island(ServerPlayer player){
        var spawns=game.maps().spawns();if(spawns.isEmpty())return false;
        var spawn=spawns.get(player.getRandom().nextInt(spawns.size()));var before=player.position();
        if(!move(player,spawn.position(),spawn.yaw(),spawn.pitch()))return false;
        player.addEffect(ItemEffectLevels.create(MobEffects.SLOW_FALLING,60,0,false,false),player);
        particles(player.level(),before,ParticleTypes.REVERSE_PORTAL);particles(player.level(),spawn.position(),ParticleTypes.PORTAL);
        return true;
    }
    public void tick(){
        if(restoring)return;
        tickImba();
        for(var entry:new ArrayList<>(charges.entrySet())){
            var player=entry.getKey();var charge=entry.getValue();
            if(!eligible(player)||player.level()!=charge.level||!game.items().currentEpoch(charge.epoch)
                    ||player.getItemInHand(charge.hand)!=charge.stack||kind(charge.stack)!=2
                    ||suppressed(player)
                    ||player.position().distanceToSqr(charge.origin)>1e-4){cancel(player,true);continue;}
            if(paused(player))continue;
            if(game.items().actionsBlocked(player)){cancel(player,true);continue;}
            if(game.server().getTickCount()==charge.createdTick)continue;
            if(--charge.remaining>0)continue;
            charges.remove(player);stopSound(charge);
            if(swap(player))debit(player,charge.stack);else failure(player);
        }
    }
    private boolean swap(ServerPlayer player){
        var candidates=swapCandidates(player);
        if(candidates.isEmpty())return false;
        var target=candidates.get(player.getRandom().nextInt(candidates.size()));
        var level=player.level();var origin=player.position();var destination=target.position();
        float yaw=player.getYRot(),pitch=player.getXRot();
        // Neither destination edits blocks. Each participant keeps their own view and momentum.
        if(!move(player,destination,yaw,pitch))return false;
        if(!move(target,origin,target.getYRot(),target.getXRot())){move(player,origin,yaw,pitch);return false;}
        sound(player,SUCCESS_SOUND);sound(target,SUCCESS_SOUND);
        particles(level,origin,ParticleTypes.PORTAL);particles(level,destination,ParticleTypes.PORTAL);
        game.items().indirectKills().teleported(target,player,true);
        return true;
    }
    private List<ServerPlayer> swapCandidates(ServerPlayer player){
        var candidates=new ArrayList<ServerPlayer>();
        for(var other:player.level().players())
            if(other!=player&&eligible(other)&&!suppressed(other)&&!paused(other)&&!LbcGreed.active(other))candidates.add(other);
        return candidates;
    }
    private static void writeLegacySwapBlock(ServerLevel level,BlockPos pos,BlockState state,int flags){
        if(level.getBlockState(pos)==state && level.getBlockEntity(pos)==null)return;
        LbcDropBlocks.setBlockPreservingInventory(level,pos,state,flags);
    }

    private boolean legacySwap(ServerPlayer player,ItemStack stack){
        var level=player.level();
        var origin=player.position();
        boolean low=origin.y<=2,high=origin.y>=319;
        var destination=low?new Vec3(origin.x,3,origin.z):high?new Vec3(origin.x,315,origin.z):origin;
        int platformY=low?1:high?314:net.minecraft.util.Mth.floor(origin.y-1.5);
        var platform=BlockPos.containing(origin.x,platformY,origin.z);
        var clearFrom=BlockPos.containing(destination.x,low?2:destination.y,destination.z);
        for(var corner:List.of(platform.offset(-1,0,-1),platform.offset(1,0,1),
                platform.offset(-1,0,1),platform.offset(1,0,-1))){
            if(!level.hasChunkAt(corner)||!level.getWorldBorder().isWithinBounds(corner))return false;
        }
        final int flags=net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                |net.minecraft.world.level.block.Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
        var cause=game.items().indirectKills().wildCause(player);
        game.items().indirectKills().capturedWorldChanges(level,cause,()->{
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
                writeLegacySwapBlock(level,platform.offset(x,0,z),Blocks.OBSIDIAN.defaultBlockState(),flags);
                if(!high)
                    for(int y=0;y<3;y++)
                        writeLegacySwapBlock(level,clearFrom.offset(x,y,z),Blocks.AIR.defaultBlockState(),flags);
            }
        });
        if(high)level.sendParticles(ParticleTypes.PORTAL,origin.x,platformY,origin.z,50,.2,.4,.2,.3);
        else particles(level,destination,ParticleTypes.PORTAL);
        float yaw=player.getYRot(),pitch=player.getXRot();
        if(low||high){
            var clamped=low?destination:new Vec3(origin.x,317,origin.z);
            move(player,clamped,yaw,pitch);
        }
        var returnPosition=player.position();
        var candidates=swapCandidates(player);
        if(!candidates.isEmpty()){
            var target=candidates.get(player.getRandom().nextInt(candidates.size()));
            var targetPosition=target.position();
            particles(level,targetPosition,ParticleTypes.PORTAL);
            if(move(player,targetPosition,target.getYRot(),target.getXRot())){
                if(move(target,destination,0,0))game.items().indirectKills().teleported(target,player,true);
                else move(player,returnPosition,yaw,pitch);
            }
        }
        debit(player,stack);
        return true;
    }
    private static boolean move(ServerPlayer player,Vec3 destination,float yaw,float pitch){
        return player.teleportTo(player.level(),destination.x,destination.y,destination.z,
                TeleportMomentum.positionFlags(Set.of()),yaw,pitch,true);
    }
    private static void particles(ServerLevel level,Vec3 at,net.minecraft.core.particles.ParticleOptions kind){
        level.sendParticles(kind,at.x,at.y+1,at.z,50,.2,.4,.2,.3);
    }
    private static void startSound(ServerPlayer owner,Charge charge){
        var packet=new ClientboundSoundPacket(Holder.direct(SoundEvent.createVariableRangeEvent(CHARGE_SOUND)),
                SoundSource.MASTER,charge.origin.x,charge.origin.y,charge.origin.z,1,1,owner.getRandom().nextLong());
        for(var listener:charge.level.players())if(!listener.hasDisconnected()
                &&LbcFpvDrone.viewpoint(listener).distanceToSqr(charge.origin)<=16*16){
            charge.listeners.add(listener);listener.connection.send(packet);
        }
    }
    private void stopSound(Charge charge){
        for(var listener:charge.listeners)if(!listener.hasDisconnected()
                &&charges.values().stream().noneMatch(other->other.listeners.contains(listener)))
            listener.connection.send(new ClientboundStopSoundPacket(CHARGE_SOUND,SoundSource.MASTER));
    }
    private static void sound(ServerPlayer player,Identifier id){player.connection.send(new ClientboundSoundPacket(Holder.direct(SoundEvent.createVariableRangeEvent(id)),SoundSource.MASTER,player.getX(),player.getY(),player.getZ(),1,1,player.getRandom().nextLong()));}
    private static void failure(ServerPlayer player){player.sendSystemMessage(Component.translatable(FAILURE_KEY).withStyle(ChatFormatting.RED));}
    private void cancel(ServerPlayer player,boolean report){var charge=charges.remove(player);if(charge!=null){stopSound(charge);if(report&&player.isAlive()&&!player.hasDisconnected()){player.containerMenu.broadcastFullState();failure(player);}}}
    public void forget(ServerPlayer player){cancel(player,false);if(restoring||MatchPlayerCheckpoint.restoring(player))return;
        imbaDelays.remove(player.getUUID());player.entityTags().remove("syn_shlyhi");}
    /** Native packet completion catches moving away and back between two item-service ticks. */
    public void moved(ServerPlayer player){var charge=charges.get(player);if(charge!=null&&player.position().distanceToSqr(charge.origin)>1e-4)cancel(player,true);}
    /** Existing ten-tick swap charges retain the authorized short-operation cancellation at rewind. */
    public void cancelChargesForRewind(){for(var player:new ArrayList<>(charges.keySet()))cancel(player,false);}
    public void stop(){cancelChargesForRewind();for(var id:List.copyOf(imbaDelays.keySet())){var player=game.server().getPlayerList().getPlayer(id);if(player!=null)player.entityTags().remove("syn_shlyhi");}
        imbaDelays.clear();for(var pad:List.copyOf(pads))pad.cancel();pads.clear();}

    private boolean imba(ServerPlayer player,ItemStack stack){
        if(imbaPolicy==null||!imbaPolicy.launch(player))return false;
        callbacks++;try{var from=player.position();var destination=from.add(player.getLookAngle().scale(1488));
            player.level().sendParticles(ParticleTypes.REVERSE_PORTAL,from.x,from.y+1,from.z,200,.2,.4,.2,.3);
            game.items().mana().score(player,"void_resistance").add(30);
            player.addEffect(ItemEffectLevels.create(MobEffects.SLOW_FALLING,600,0),player);
            LbcOverload.sound(player,from,"minecraft:entity.enderman.teleport",3,1,0);
            // Source continues to its tag/schedule even when the native teleport command cannot move.
            move(player,destination,player.getYRot(),player.getXRot());
            player.entityTags().add("syn_shlyhi");imbaDelays.put(player.getUUID(),new ImbaDelay(player.getUUID(),player.level(),game.items().epoch(),20,game.server().getTickCount(),game.items().indirectKills().coldCause(player,false)));
            debit(player,stack);return true;
        }finally{callbacks--;}
    }
    private void tickImba(){advancing=true;try{for(var delay:List.copyOf(imbaDelays.values())){
        var player=game.server().getPlayerList().getPlayer(delay.owner);
        if(player==null||!eligible(player)||player.level()!=delay.level||!game.items().currentEpoch(delay.epoch)){
            imbaDelays.remove(delay.owner);if(player!=null)player.entityTags().remove("syn_shlyhi");continue;}
        if(paused(player)||game.server().getTickCount()==delay.createdTick)continue;
        if(delay.remaining>1){imbaDelays.put(delay.owner,new ImbaDelay(delay.owner,delay.level,delay.epoch,delay.remaining-1,delay.createdTick,delay.cause));continue;}
        imbaDelays.remove(delay.owner);player.entityTags().remove("syn_shlyhi");
        var pad=new PadWork(player,delay.level,delay.epoch,imbaPadOrigin(player.position()),delay.cause);
        pads.add(pad);game.items().stonekivi().queuedWork(pad);
    }}finally{advancing=false;}}
    private final class PadWork implements LbcStonekivi.Work {
        ServerPlayer author;final ServerLevel level;final String epoch;final BlockPos origin;final LbcIndirectKills.Owner cause;
        int cursor;long writes;boolean closed;
        PadWork(ServerPlayer author,ServerLevel level,String epoch,BlockPos origin,LbcIndirectKills.Owner cause){this.author=author;this.level=level;this.epoch=epoch;this.origin=origin;this.cause=cause;}
        public boolean ready(){return !restoring&&(closed||!game.items().timeStop().stopped(level)&&!game.items().timeStop().frozen(author)&&!game.items().wands().timeStopped());}
        public boolean snapshotIdle(){return true;}
        public boolean postgame(){return true;}
        public boolean step(){if(restoring)return false;callbacks++;game.items().stonekivi().beginNativeWork();try{
            if(closed)return true;if(!game.items().currentEpoch(epoch)){cancel();return true;}if(!ready())return false;
            long deadline=Math.min(EffectTerrain.deadlineNanos(),System.nanoTime()+350_000);
            while(cursor<25&&System.nanoTime()<deadline){var at=origin.offset(cursor/5,0,cursor%5);
                if(!level.isInValidBounds(at)){cursor++;continue;}
                if(!level.hasChunkAt(at)){game.maps().prepareChunk(level,new ChunkPos(at.getX()>>4,at.getZ()>>4));return false;}
                if(imbaPolicy==null)return false;
                var value=new MapManager.BlockSnapshot(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState(),null);
                if(imbaPolicy.write(author,level,at,value,cause))writes++;cursor++;
            }
            if(cursor<25)return false;closed=true;pads.remove(this);return true;
        }finally{game.items().stonekivi().endNativeWork();callbacks--;}}
        public void cancel(){closed=true;pads.remove(this);}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            var owner=graph.links.key(author);int savedCursor=cursor;long savedWrites=writes;boolean savedClosed=closed;
            return links->{var restored=(ServerPlayer)links.get(owner);return ()->{author=restored;cursor=savedCursor;writes=savedWrites;closed=savedClosed;};};
        }
    }
    public void checkpointBoundary(){if(advancing||callbacks!=0)throw new IllegalStateException("Finish teleport gadget callbacks before checkpoint");}
    public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){checkpointBoundary();var saved=frozen();var accepted=List.copyOf(pads);accepted.forEach(graph::capture);
        return links->()->{restoreFrozen(saved);pads.clear();pads.addAll(accepted);};}
    public void prepareFrozen(){checkpointBoundary();restoring=true;cancelChargesForRewind();imbaDelays.clear();pads.clear();}
    public void cancelFrozen(){checkpointBoundary();restoring=false;}
    public void publishFrozen(){checkpointBoundary();restoring=false;for(var id:imbaDelays.keySet()){var player=game.server().getPlayerList().getPlayer(id);if(player!=null)player.entityTags().add("syn_shlyhi");}}
    public int chargingCount(){return charges.size();}
}
