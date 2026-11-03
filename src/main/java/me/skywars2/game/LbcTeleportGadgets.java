package me.skywars2.game;

import java.util.ArrayList;
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

/** Native map/player state stays authoritative; only a short, cancellable charge lives here. */
public final class LbcTeleportGadgets {
    static final Identifier CHARGE_SOUND=Identifier.parse("minecraft:lbcsounds.swap_charge");
    static final Identifier SUCCESS_SOUND=Identifier.parse("minecraft:lbcsounds.swap_go");
    static final String FAILURE_KEY="lbc.swapper.charge_failed";
    private final GameController game;
    private final Map<ServerPlayer,Charge> charges=new IdentityHashMap<>();
    private static final class Charge {
        final InteractionHand hand;final ItemStack stack;final Vec3 origin;final ServerLevel level;final String epoch;
        final int createdTick;int remaining=10;
        Charge(ServerPlayer owner,InteractionHand hand,String epoch){this.hand=hand;stack=owner.getItemInHand(hand);origin=owner.position();level=owner.level();this.epoch=epoch;createdTick=level.getServer().getTickCount();}
    }
    public LbcTeleportGadgets(GameController game){this.game=game;}
    private static int kind(ItemStack stack){
        if(stack.isEmpty())return 0;
        if(ItemData.intValue(stack,"teleport1",0)==1&&ItemData.intValue(stack,"gadget",0)==1)return 3;
        var data=LbcFartus.data(stack);
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
        if(kind==0||!eligible(player)||game.items().actionsBlocked(player)||paused(player))return false;
        if(kind==3){
            var from=player.position();if(game.items().magic().mechanismSuppressedAt(player.level(),from,24))return false;
            var destination=from.add(player.getLookAngle().scale(5));
            if(!move(player,destination,player.getYRot(),player.getXRot()))return false;
            player.level().sendParticles(ParticleTypes.REVERSE_PORTAL,from.x,from.y+1,from.z,200,.2,.4,.2,.3);
            LbcOverload.sound(player,from,"minecraft:entity.enderman.teleport",10,1,20);
            debit(player,stack);return true;
        }
        if(suppressed(player))return false;
        if(kind==1){if(!island(player))return false;debit(player,stack);return true;}
        // Held use and the other hand cannot restart an existing charge.
        if(charges.containsKey(player))return false;
        charges.put(player,new Charge(player,hand,game.items().epoch()));
        // The client carrier predicts a snowball debit. The server retains the item until completion.
        player.containerMenu.broadcastFullState();
        // Owner-local sound avoids one cancellation stopping a neighbour's identical sound.
        sound(player,CHARGE_SOUND);
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
            charges.remove(player);stopSound(player);
            if(swap(player))debit(player,charge.stack);else failure(player);
        }
    }
    private boolean swap(ServerPlayer player){
        var candidates=new ArrayList<ServerPlayer>();
        for(var other:player.level().players())if(other!=player&&eligible(other)&&!suppressed(other)&&!paused(other)&&!LbcGreed.active(other))candidates.add(other);
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
    private static boolean move(ServerPlayer player,Vec3 destination,float yaw,float pitch){
        return player.teleportTo(player.level(),destination.x,destination.y,destination.z,
                TeleportMomentum.positionFlags(Set.of()),yaw,pitch,true);
    }
    private static void particles(ServerLevel level,Vec3 at,net.minecraft.core.particles.ParticleOptions kind){
        level.sendParticles(kind,at.x,at.y+1,at.z,50,.2,.4,.2,.3);
    }
    private static void stopSound(ServerPlayer player){player.connection.send(new ClientboundStopSoundPacket(CHARGE_SOUND,SoundSource.MASTER));}
    private static void sound(ServerPlayer player,Identifier id){player.connection.send(new ClientboundSoundPacket(Holder.direct(SoundEvent.createVariableRangeEvent(id)),SoundSource.MASTER,player.getX(),player.getY(),player.getZ(),1,1,player.getRandom().nextLong()));}
    private static void failure(ServerPlayer player){player.sendSystemMessage(Component.translatable(FAILURE_KEY).withStyle(ChatFormatting.RED));}
    private void cancel(ServerPlayer player,boolean report){if(charges.remove(player)!=null){stopSound(player);if(report&&player.isAlive()&&!player.hasDisconnected()){player.containerMenu.broadcastFullState();failure(player);}}}
    public void forget(ServerPlayer player){cancel(player,false);}
    /** Native packet completion catches moving away and back between two item-service ticks. */
    public void moved(ServerPlayer player){var charge=charges.get(player);if(charge!=null&&player.position().distanceToSqr(charge.origin)>1e-4)cancel(player,true);}
    public void stop(){for(var player:new ArrayList<>(charges.keySet()))cancel(player,false);}
    public int chargingCount(){return charges.size();}
}
