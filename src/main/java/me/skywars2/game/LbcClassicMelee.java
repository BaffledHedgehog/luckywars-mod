package me.skywars2.game;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Source accepted-hit passives on ordinary vanilla sword stacks; all effect state stays native. */
public final class LbcClassicMelee {
    private static final TagKey<DamageType> ARROW_ONLY=TagKey.create(Registries.DAMAGE_TYPE,Identifier.parse("minecraft:arrow_only"));
    private final GameController game;
    public LbcClassicMelee(GameController game){this.game=game;}

    private boolean admitted(ServerPlayer owner){
        return owner!=null&&owner.isAlive()&&!owner.isRemoved()&&owner.level()==game.maps().level()
                &&game.canUseGameplayItems(owner)&&!game.isObserver(owner)&&game.stage()!=GameController.Stage.ENDING
                &&game.items().wands().validRoleTarget(owner,false)&&!game.items().actionsBlocked(owner)
                &&!game.items().wands().inputTimeStopped(owner)&&!game.items().timeStop().stopped(owner.level());
    }
    private boolean magic(ServerPlayer owner){
        return !owner.entityTags().contains("nomagic_active")&&!game.items().magic().suppressed(owner)
                &&!game.items().magic().suppressedAt(owner.level(),owner.getEyePosition());
    }

    /** Called after accepted positive, unblocked native damage, including fatal and attributed indirect hits. */
    public void damaged(LivingEntity victim,DamageSource source){
        if(!(source.getEntity() instanceof ServerPlayer owner)||!admitted(owner)||source.is(ARROW_ONLY)
                ||!game.items().wands().validRoleTarget(victim,true))return;
        var dimension=owner.level().dimension().identifier();
        if(dimension.equals(Identifier.parse("minecraft:nexus"))||dimension.equals(Identifier.parse("minecraft:imprinted")))return;
        var data=LbcFartus.data(owner.getMainHandItem());
        boolean feeding=data.getIntOr("feeding",0)==1,vampire=data.getIntOr("vampire",0)==1,guardian=data.getIntOr("seaguardian",0)==1;
        if(!feeding&&!vampire&&!guardian)return;
        // Source Feeding deliberately precedes and bypasses all magic suppression checks.
        if(feeding){
            // Native /effect gives instantaneous effects the supplied duration directly, so source "1" is one tick.
            owner.addEffect(ItemEffectLevels.create(MobEffects.SATURATION,1,127,false,false,false),owner);
            LbcOverload.sound(owner,"minecraft:entity.generic.eat",1,1,0);
            LbcOverload.sound(owner,"minecraft:entity.player.burp",1,1,0);
            game.items().mana().score(owner,"food_count").add(1);
        }
        if(!vampire&&!guardian||!magic(owner))return;
        if(vampire)
            owner.addEffect(ItemEffectLevels.create(MobEffects.REGENERATION,20,4,false,false,false),owner);
        if(guardian){
            // The actual swimming flag is authoritative; this path must not consume a random draw.
            if(owner.isSwimming())guardian(owner,true);
            else if(owner.getRandom().nextInt(4)==0)guardian(owner,false);
        }
    }
    private void guardian(ServerPlayer owner,boolean swimming){
        var level=owner.level();var origin=owner.position();
        var targets=level.getEntitiesOfClass(LivingEntity.class,owner.getBoundingBox().inflate(7),entity->{
            double distance=entity.position().distanceToSqr(origin);
            return distance>=.0001&&distance<=49&&game.items().wands().target(owner,entity);
        });
        for(var target:targets){
            // Native addEffect retains the source owner and shared signed amplifier/layer policy.
            target.addEffect(ItemEffectLevels.create(MobEffects.MINING_FATIGUE,swimming?1200:400,127),owner);
            if(swimming)target.addEffect(ItemEffectLevels.create(MobEffects.WEAKNESS,200,1),owner);
            else target.addEffect(ItemEffectLevels.create(MobEffects.WATER_BREATHING,40,1),owner);
        }
        if(swimming)owner.addEffect(ItemEffectLevels.create(MobEffects.WATER_BREATHING,200,2),owner);
        LbcOverload.sound(owner,"minecraft:entity.elder_guardian.curse",1,1,0);
        for(var target:targets)if(target instanceof ServerPlayer viewer)
            level.sendParticles(viewer,ParticleTypes.ELDER_GUARDIAN,true,false,origin.x,origin.y,origin.z,10,5,5,5,1);
    }
}
