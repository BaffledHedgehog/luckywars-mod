package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Native formulas, rather than only copies of the helper expressions. */
public final class SignedEffectMathChecks {
    private SignedEffectMathChecks() { }
    private static void require(boolean value,String message) { if(!value)throw new IllegalStateException("Signed effect math: "+message); }
    private static void close(double actual,double expected,String message) { require(Math.abs(actual-expected)<.003,message+" actual="+actual+" expected="+expected); }
    public static void run(GameController game) {
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(owner!=null&&game.playing(),"native participant");
        var world=owner.level();var cow=new Cow(EntityTypes.COW,world);
        try {
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(50);
            var attacker=new net.minecraft.world.entity.monster.zombie.Zombie(EntityTypes.ZOMBIE,world);
            try {
                attacker.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2);
                attacker.addEffect(new MobEffectInstance(MobEffects.STRENGTH,100,Integer.MAX_VALUE));
                close(attacker.getAttributeValue(Attributes.ATTACK_DAMAGE),6442450944d+2,"attribute multiplication widens before addition");
                attacker.removeAllEffects();attacker.addEffect(new MobEffectInstance(MobEffects.STRENGTH,100,-2));
                close(attacker.getAttributeValue(Attributes.ATTACK_DAMAGE),-1,"negative strength signed attribute");
            } finally {attacker.discard();}
            MobEffects.INSTANT_HEALTH.value().applyEffectTick(world,cow,1);close(cow.getHealth(),58,"native instant health II");
            cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            MobEffects.INSTANT_HEALTH.value().applyEffectTick(world,cow,-2);close(cow.getHealth(),52,"negative instant health harms");
            MobEffects.INSTANT_DAMAGE.value().applyEffectTick(world,cow,-3);close(cow.getHealth(),60,"negative instant harm heals");
            MobEffects.INSTANT_HEALTH.value().applyEffectTick(world,cow,Integer.MAX_VALUE);close(cow.getHealth(),100,"huge instant heal finite saturation");
            MobEffects.REGENERATION.value().applyEffectTick(world,cow,0);close(cow.getHealth(),100,"full HP regeneration never harms");
            MobEffects.POISON.value().applyEffectTick(world,cow,-2);close(cow.getHealth(),100,"full HP negative poison never harms");
            cow.setHealth(50);MobEffects.REGENERATION.value().applyEffectTick(world,cow,11);close(cow.getHealth(),52,"regeneration XII grows HP per tick");
            cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            MobEffects.REGENERATION.value().applyEffectTick(world,cow,-7);close(cow.getHealth(),51,"negative regeneration harms at signed magnitude");
            MobEffects.POISON.value().applyEffectTick(world,cow,-6);close(cow.getHealth(),52,"negative poison heals");
            cow.setHealth(1);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            MobEffects.POISON.value().applyEffectTick(world,cow,Integer.MAX_VALUE);close(cow.getHealth(),1,"large positive poison never kills");
            cow.setHealth(50);cow.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,100,5));cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            cow.hurtServer(world,cow.damageSources().generic(),10);close(cow.getHealth(),52,"resistance VI converts incoming damage to healing");
            cow.removeAllEffects();cow.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,100,-2));cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            cow.hurtServer(world,cow.damageSources().generic(),10);close(cow.getHealth(),40,"negative resistance raises damage");cow.removeAllEffects();
            cow.addEffect(new MobEffectInstance(MobEffects.HASTE,100,-3));
            require(net.minecraft.world.effect.MobEffectUtil.getDigSpeedAmplification(cow)==-3,"absent conduit does not erase negative haste");cow.removeAllEffects();
            for(int amplifier:new int[]{Integer.MIN_VALUE,-2,-1,0,31,32,Integer.MAX_VALUE}) {
                require(Double.isFinite(SignedEffectMath.fatigue(amplifier)),"finite fatigue endpoint");
                require(Float.isFinite(SignedEffectMath.periodicAmount(amplifier,50)),"finite periodic endpoint");
                require(MobEffects.REGENERATION.value().shouldApplyEffectTickThisTick(100,amplifier)==SignedEffectMath.due(100,amplifier,50),"native interval endpoint");
            }
            for(double velocity:new double[]{.42,.52,1.42,10,1000}) {
                double y=0,v=velocity;int ticks=0;
                while(v>0&&ticks++<100000){y+=v;v=(v-.08)*.98;}
                close(SignedEffectMath.jumpHeight(velocity,.08,.98),y,"safe jump height follows actual discrete trajectory");
            }
            close(SignedEffectMath.jumpHeight(.42,.08,1),1.32,"no drag exact discrete jump height");
            close(SignedEffectMath.jumpHeight(.52,0,.98),26,"zero gravity finite drag height");
            cow.setPos(owner.position().add(0,8,0));cow.setNoGravity(false);
            for(int amp:new int[]{Integer.MIN_VALUE,-2,-1,0,256,Integer.MAX_VALUE}) {
                cow.removeEffect(MobEffects.LEVITATION);cow.addEffect(new MobEffectInstance(MobEffects.LEVITATION,100,amp));
                cow.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                cow.travel(net.minecraft.world.phys.Vec3.ZERO);
                close(cow.getDeltaMovement().y,.05*SignedEffectMath.level(amp)*.2*(double).98f,"actual native signed levitation travel");
                cow.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            }
            cow.removeAllEffects();cow.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,100,49));
            try {
                var fall=net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod("calculateFallPower",double.class);fall.setAccessible(true);
                double jumpHeight=SignedEffectMath.jumpHeight(cow.getAttributeValue(Attributes.JUMP_STRENGTH)+5,
                        cow.getAttributeValue(Attributes.GRAVITY),.98);
                require((double)fall.invoke(cow,jumpHeight+1)<0,"native fall calculation covers entire boosted jump and lower landing");
            } catch(ReflectiveOperationException error) {throw new IllegalStateException(error);}
            cow.removeAllEffects();
            cow.addEffect(new MobEffectInstance(MobEffects.REGENERATION,100,99));
            game.items().indirectKills().extendEffectDuration(cow,cow.getEffect(MobEffects.REGENERATION));
            require(cow.getEffect(MobEffects.REGENERATION).getAmplifier()==99 && cow.getEffect(MobEffects.REGENERATION).getDuration()==101,"duration-only item extension preserves explicitly applied amplifier");
            cow.removeAllEffects();
            require(ItemEffectLevels.create(MobEffects.REGENERATION,100,255).getAmplifier()==5,"item regen caps native max rate");
            require(ItemEffectLevels.create(MobEffects.POISON,100,255).getAmplifier()==4,"item poison caps native max rate");
            require(ItemEffectLevels.create(MobEffects.RESISTANCE,100,255).getAmplifier()==4,"item resistance caps V");
            require(ItemEffectLevels.create(MobEffects.BLINDNESS,100,255).getAmplifier()==2,"item blindness caps III");
            require(new MobEffectInstance(MobEffects.RESISTANCE,100,255).getAmplifier()==255,"direct effects stay uncapped");
            var potion=new net.minecraft.world.item.alchemy.PotionContents(java.util.Optional.empty(),java.util.Optional.empty(),
                    java.util.List.of(new MobEffectInstance(MobEffects.RESISTANCE,100,99)),java.util.Optional.empty());
            potion.applyToLivingEntity(cow,1);
            require(cow.getEffect(MobEffects.RESISTANCE).getAmplifier()==4,"actual native potion consumption caps item-issued resistance");
            var food=owner.getFoodData();
            var saved=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,world.registryAccess());
            food.addAdditionalSaveData(saved);
            try {
                var empty=new net.minecraft.nbt.CompoundTag();empty.putInt("foodLevel",20);empty.putFloat("foodSaturationLevel",0);empty.putFloat("foodExhaustionLevel",0);
                food.readAdditionalSaveData(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,world.registryAccess(),empty));
                MobEffects.HUNGER.value().applyEffectTick(world,owner,-2);close(food.getSaturationLevel(),.00125,"negative hunger restores saturation slowly");
                MobEffects.SATURATION.value().applyEffectTick(world,owner,Integer.MIN_VALUE);close(food.getFoodLevel(),0,"negative saturation removes food within bar bounds");
                MobEffects.SATURATION.value().applyEffectTick(world,owner,Integer.MAX_VALUE);close(food.getFoodLevel(),20,"positive saturation saturates bar without overflow");
                close(food.getSaturationLevel(),20,"positive saturation caps its bar");
            } finally {food.readAdditionalSaveData(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,world.registryAccess(),saved.buildResult()));}
        } finally { cow.discard(); }
        SkyWars2.LOGGER.info("[check] PASS signed-effects-math (native signed attributes, instant and periodic effects, resistance healing, poison floor, finite endpoints, item caps and ballistic safe fall distance)");
    }
}
