package me.skywars2.game;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** The source Devil Sword's independently evaluated, descending self-effect ladder. */
public final class LbcDevilSword {
    private static final TagKey<DamageType> ARROW_ONLY=TagKey.create(Registries.DAMAGE_TYPE,Identifier.parse("minecraft:arrow_only"));
    private final GameController game;
    public LbcDevilSword(GameController game){this.game=game;}
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
    private static boolean strength(ServerPlayer owner,int amplifier){
        var effect=owner.getEffect(MobEffects.STRENGTH);return effect!=null&&effect.getAmplifier()==amplifier;
    }
    /** Native accepted positive/unblocked damage, including attributable indirect and fatal hits. */
    public void damaged(LivingEntity victim,DamageSource source){
        if(!(source.getEntity() instanceof ServerPlayer owner)||!LbcDamage.isMeleeAttack(source,owner)||!admitted(owner)||source.is(ARROW_ONLY)
                ||!game.items().wands().validRoleTarget(victim,true)||LbcFartus.data(owner.getMainHandItem()).getIntOr("heal",0)!=1)return;
        var dimension=owner.level().dimension().identifier();
        if(dimension.equals(Identifier.parse("minecraft:nexus"))||dimension.equals(Identifier.parse("minecraft:imprinted"))||!magic(owner))return;
        // Each source command tests the then-active effect, never its hidden lower layers.
        if(strength(owner,5))helper(owner,6);
        if(strength(owner,4))helper(owner,6);
        if(strength(owner,3))helper(owner,5);
        if(strength(owner,2))helper(owner,4);
        if(strength(owner,1))helper(owner,3);
        if(owner.hasEffect(MobEffects.STRENGTH))helper(owner,2);
        if(LbcFartus.data(owner.getMainHandItem()).getIntOr("heal",0)==1&&!owner.hasEffect(MobEffects.STRENGTH))helper(owner,1);
    }
    private static void effect(ServerPlayer owner,Holder<MobEffect> effect,int amplifier){
        // The native scheduled instant tick has no player damage entity, preventing recursive sword activation.
        owner.addEffect(ItemEffectLevels.create(effect,effect.is(MobEffects.INSTANT_DAMAGE)?1:200,amplifier),owner);
    }
    private void helper(ServerPlayer owner,int branch){
        switch(branch){
            case 1->effect(owner,MobEffects.STRENGTH,0);
            case 2->{
                effect(owner,MobEffects.SPEED,1);effect(owner,MobEffects.NAUSEA,1);effect(owner,MobEffects.STRENGTH,1);
            }
            case 3->{
                effect(owner,MobEffects.NAUSEA,1);effect(owner,MobEffects.STRENGTH,2);effect(owner,MobEffects.SPEED,2);
            }
            case 4->{
                effect(owner,MobEffects.NAUSEA,1);effect(owner,MobEffects.INSTANT_DAMAGE,0);
                effect(owner,MobEffects.STRENGTH,3);effect(owner,MobEffects.SPEED,3);
            }
            case 5->{
                effect(owner,MobEffects.WITHER,3);effect(owner,MobEffects.NAUSEA,1);effect(owner,MobEffects.INSTANT_DAMAGE,1);
                effect(owner,MobEffects.STRENGTH,4);effect(owner,MobEffects.SPEED,4);
            }
            case 6->{
                game.items().mana().award(owner,"lbc:true_advancements/swords/devil");
                effect(owner,MobEffects.WITHER,5);effect(owner,MobEffects.NAUSEA,1);effect(owner,MobEffects.INSTANT_DAMAGE,2);
                effect(owner,MobEffects.STRENGTH,5);effect(owner,MobEffects.SPEED,5);
            }
            default->throw new IllegalArgumentException("Devil Sword helper "+branch);
        }
    }
}
