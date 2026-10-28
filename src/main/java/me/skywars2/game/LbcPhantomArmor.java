package me.skywars2.game;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

/** Phantom's reversible self buffs and real melee flash, using shared native effect ownership. */
public final class LbcPhantomArmor {
    public static final String EFFECT_OWNER="skywars2:phantom_armor";
    private static final String SHOT="skywars2.phantom_shot_seen";
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final DustParticleOptions DUST=new DustParticleOptions(0x000000,1);
    public interface ShotState { boolean skywars2$hasBeenShot(); }
    private record Lock(int remaining,int tick) { }
    private final GameController game;
    private final Map<ServerPlayer,Lock> locks=new IdentityHashMap<>();
    private int phase;
    private String epoch;
    LbcPhantomArmor(GameController game){this.game=game;}
    public static void used(ServerPlayer player,net.minecraft.world.InteractionResult result){
        var game=me.skywars2.SkyWars2.game();if(game!=null&&result.consumesAction())game.items().armorSets().phantom().activity(player);
    }
    private void ensureEpoch(){if(!game.items().currentEpoch(epoch)){stop();epoch=game.items().epoch();}}

    private boolean eligible(ServerPlayer owner){
        return owner.isAlive()&&!owner.isRemoved()&&owner.level()==game.maps().level()
                &&game.stage()!=GameController.Stage.ENDING&&game.canUseGameplayItems(owner)
                &&!game.isObserver(owner)&&game.items().wands().validRoleTarget(owner,false)
                &&LbcDimensionItems.ordinary(owner);
    }
    private boolean paused(ServerPlayer owner){return game.items().wands().timeStopped()
            ||game.items().timeStop().stopped(owner.level())||game.items().wands().inputTimeStopped(owner);}
    private static boolean full(ServerPlayer owner){
        for(var slot:SLOTS)if(LbcFartus.data(owner.getItemBySlot(slot)).getIntOr("phantom",0)!=1)return false;
        return true;
    }
    private boolean enemy(ServerPlayer owner,ServerPlayer other){
        double distance=owner.position().distanceToSqr(other.position());
        return other!=owner&&other.level()==owner.level()&&distance>=.0001&&distance<=64
                &&!owner.isAlliedTo(other)&&game.canUseGameplayItems(other)&&!game.isObserver(other)
                &&game.items().wands().validRoleTarget(other,false);
    }
    void pulse(ServerPlayer owner){
        ensureEpoch();
        if(!eligible(owner)||!full(owner)){game.items().ownedEffects().withdraw(owner,EFFECT_OWNER);return;}
        if(paused(owner))return;
        if(game.items().magic().beaconSuppressed(owner.position())){
            game.items().ownedEffects().withdraw(owner,EFFECT_OWNER);return;
        }
        if(locks.containsKey(owner))return;
        // Enemy approach only stops refresh; already granted source bonuses retain their five-minute clock.
        if(game.server().getPlayerList().getPlayers().stream().anyMatch(other->enemy(owner,other)))return;
        game.items().ownedEffects().grant(owner,EFFECT_OWNER,ItemEffectLevels.create(MobEffects.INVISIBILITY,6000,1,false,false),owner);
        game.items().ownedEffects().grant(owner,EFFECT_OWNER,ItemEffectLevels.create(MobEffects.STRENGTH,6000,1,false,false),owner);
    }
    public void activity(ServerPlayer owner){
        if(!eligible(owner))return;
        ensureEpoch();
        game.items().ownedEffects().withdraw(owner,EFFECT_OWNER);
        locks.put(owner,new Lock(20,game.server().getTickCount()));
    }
    /** A native shot is observed once, including queued shots; save/reload is not a new launch. */
    public void projectile(Projectile shot){
        if(!(shot.getOwner() instanceof ServerPlayer owner)||shot.entityTags().contains(SHOT)
                ||shot instanceof ShotState state&&state.skywars2$hasBeenShot())return;
        shot.entityTags().add(SHOT);activity(owner);
    }
    public void damaged(LivingEntity victim,DamageSource source){
        if(!(source.getEntity() instanceof ServerPlayer owner)||!LbcDamage.isMeleeAttack(source,owner)
                ||!eligible(owner)||paused(owner)||!full(owner)||!game.items().wands().validRoleTarget(victim,true))return;
        // The ordinary action cooldown applies even if suppression prevents the magical flash.
        activity(owner);
        if(game.items().magic().suppressed(owner)||owner.entityTags().contains("nomagic_active"))return;
        for(var viewer:owner.level().players()){
            if(enemy(owner,viewer)&&!game.items().magic().beaconSuppressed(viewer.position()))
                viewer.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS,100,0),owner);
            if(viewer!=owner&&viewer.distanceToSqr(owner)>=.0001)
                owner.level().sendParticles(viewer,DUST,false,false,owner.getX(),owner.getY()+1,owner.getZ(),50,.3,.6,.3,.3);
        }
    }
    public void tick(){
        ensureEpoch();
        phase=(phase+1)%5;
        for(var owner:game.server().getPlayerList().getPlayers()){
            if(!eligible(owner)){forget(owner);continue;}
            if(paused(owner))continue;
            if(owner.isUsingItem())activity(owner);
            var lock=locks.get(owner);int now=game.server().getTickCount();
            if(lock!=null&&lock.tick()!=now){
                if(lock.remaining()<=1)locks.remove(owner);else locks.put(owner,new Lock(lock.remaining()-1,now));
            }
            if(phase==0)pulse(owner);
        }
    }
    public void inventoryChanged(ServerPlayer owner){
        if(!full(owner)||!eligible(owner))game.items().ownedEffects().withdraw(owner,EFFECT_OWNER);
        else pulse(owner);
    }
    public void forget(ServerPlayer owner){locks.remove(owner);game.items().ownedEffects().withdraw(owner,EFFECT_OWNER);}
    public void stop(){
        for(var owner:List.copyOf(game.server().getPlayerList().getPlayers()))game.items().ownedEffects().withdraw(owner,EFFECT_OWNER);
        locks.clear();phase=0;epoch=null;
    }
}
