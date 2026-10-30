package me.skywars2.game;

import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

/** Actual hand flight, native finger rebirth/payment, deferred Wrath death and managed Lust child. */
final class VanityAuthorityScene {
    static final String LEASE="vanity-authority-probe";
    final GameController game;final ChefChecks.Fixture base;
    final Skeleton finger;final Cow heart,victim,lustChild;final Entity hand;
    final LbcSpellSource.Source handPolicy,lustPolicy;final UUID future=UUID.randomUUID();
    net.minecraft.world.damagesource.DamageSource rawMelee,melee,wrappedMelee;
    final int age;int frame,pulse,lustPulse,bornAge;LbcIndirectKills.Owner wrathCause;
    static <T>T get(Object target,String name){return VanitySpellScene.get(target,name);}
    static void set(Object target,String name,Object value){VanitySpellScene.set(target,name,value);}
    static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity authorities: "+why);}
    static Object invoke(Object target,String name,Class<?>[] types,Object... args){try{var method=target.getClass().getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(target,args);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    VanityAuthorityScene(GameController game,ChefChecks.Fixture base){
        this.game=game;this.base=base;var hands=game.items().witchIndolence();var fingers=game.items().indolenceFingers();
        require(hands.snapshot().isEmpty()&&fingers.snapshot().isEmpty()&&game.items().witchLust().snapshot().isEmpty(),"no borrowed authority actors");
        base.owner.entityTags().add("sin_indolence");base.other.entityTags().add("sin_indolence");
        var original=base.owner.getMainHandItem().copy();var flags=new CompoundTag();flags.putInt("indolence",1);var claw=new ItemStack(Items.PAPER);claw.set(DataComponents.CUSTOM_DATA,CustomData.of(flags));base.owner.setItemInHand(InteractionHand.MAIN_HAND,claw);game.items().mana().score(base.owner,"indolence_cd").set(0);
        require(hands.use(base.owner,InteractionHand.MAIN_HAND),"real source claw creates hand formation");set(hands,"lastTick",Integer.MIN_VALUE);hands.tick();base.owner.setItemInHand(InteractionHand.MAIN_HAND,original);
        // State replay witnesses stay in open air; gameplay checks cover the random source formation.
        for(var flight:hands.snapshot()){flight.setPos(base.at.add(0,8,-8));flight.setYRot(0);flight.setXRot(0);}
        hand=hands.snapshot().getFirst();age=hands.age(hand);handPolicy=game.items().spellSources().source(hand);require(age==1&&handPolicy!=null,"one actual flight step before freeze");
        finger=(Skeleton)invoke(fingers,"spawn",new Class<?>[]{ServerPlayer.class,net.minecraft.server.level.ServerLevel.class,Vec3.class},base.other,base.other.level(),base.at.add(-12,1,-12));require(finger!=null,"real finger payload");finger.setNoAi(true);finger.setNoGravity(true);
        original=base.other.getMainHandItem().copy();require(fingers.prepareDeath(base.other),"native Beta rebirth payload");fingers.respawned(base.other);base.other.setItemInHand(InteractionHand.MAIN_HAND,original);
        var payment=VanityAuthorityScene.<List<Object>>get(fingers,"costs").getFirst();set(payment,"left",2); // Explicit partial countdown, not a natural three-tick elapsed claim.
        base.owner.addEffect(new MobEffectInstance(MobEffects.LUCK,1400,-3),base.other);game.items().ownedEffects().grant(base.owner,LEASE,new MobEffectInstance(MobEffects.LUCK,400,4),base.other);
        base.owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,900,0),base.other);base.owner.setAbsorptionAmount(3.5f);require(fingers.prepareDeath(base.owner),"independent suspended Alpha effects/leases/absorption payload");
        heart=cow(base.at.add(12,2,12));victim=cow(base.at.add(12,2,-12));victim.entityTags().add("affected_by_wrath");
        lustChild=new Cow(EntityTypes.COW,base.owner.level());lustChild.setNoAi(true);lustChild.setNoGravity(true);lustChild.setPos(base.at.add(10,2,-10));
        var producing=game.items().spellSources().hostile(lustChild,base.other,Set.of("strict_map_object"));var lust=game.items().witchLust();set(lust,"producing",producing);
        try(var scope=game.items().spellSources().scope(producing,base.owner.level())){require(base.owner.level().addFreshEntity(lustChild),"native Lust producer child event");}finally{set(lust,"producing",null);}
        lustPolicy=game.items().spellSources().source(lustChild);require(lust.snapshot().contains(lustChild)&&lustPolicy!=null,"managed Lust child has actual accepted policy");
    }
    private Cow cow(Vec3 at){var c=new Cow(EntityTypes.COW,base.owner.level());c.setNoAi(true);c.setNoGravity(true);c.setPos(at);require(base.owner.level().addFreshEntity(c),"native authority witness");return c;}
    void arm(ServerPlayer gamma){
        gamma.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));rawMelee=gamma.damageSources().playerAttack(gamma);melee=LbcWitchStatistics.nativeMelee(gamma,victim,rawMelee);wrappedMelee=game.items().indirectKills().captureDamage(melee,game.items().indirectKills().wildCause(gamma));
        var passives=game.items().witchPassives();set(passives,"phaseOffset",19-game.server().getTickCount());game.items().mana().score(base.owner,"lust_cd").set(37);invoke(passives,"heart",new Class<?>[]{Entity.class},gamma);invoke(passives,"heart",new Class<?>[]{Entity.class},heart);
        VanityAuthorityScene.<Set<ServerPlayer>>get(passives,"managed").add(gamma);VanityAuthorityScene.<Set<ServerPlayer>>get(game.items().witchLust(),"hidden").add(gamma);
        game.items().indolenceFingers().respawned(base.owner);require(VanityAuthorityScene.<Set<UUID>>get(game.items().indolenceFingers(),"ready").contains(base.owner.getUUID()),"real respawn handler defers prepared rebirth in Time Stop");
        base.owner.entityTags().add("sin_wrath");try{game.items().witchWrath().died(base.owner);}finally{base.owner.entityTags().remove("sin_wrath");}
        var deaths=VanityAuthorityScene.<List<Object>>get(game.items().witchWrath(),"deaths");require(deaths.size()==1,"real Wrath death consequence remains deferred");wrathCause=get(deaths.getFirst(),"cause");
        frame=game.server().getTickCount();pulse=Math.floorMod(frame+VanityAuthorityScene.<Integer>get(passives,"phaseOffset"),20);lustPulse=Math.floorMod(frame+VanityAuthorityScene.<Integer>get(game.items().witchLust(),"phaseOffset"),5);
        var payment=VanityAuthorityScene.<List<Object>>get(game.items().indolenceFingers(),"costs").getFirst();bornAge=frame-VanityAuthorityScene.<Integer>get(payment,"born");
    }
    void sources(ServerPlayer gamma,MatchDamageSources graph){
        graph.capture(wrappedMelee);
    }
    void verify(ServerPlayer gamma){
        require(rawMelee.getEntity()==gamma&&melee.getEntity()==gamma&&wrappedMelee.getEntity()==gamma&&melee.getWeaponItem().is(Items.DIAMOND_SWORD),"accepted melee wrapper AND its native weapon delegate follow the current body");
        var passives=game.items().witchPassives();require(VanityAuthorityScene.<Set<ServerPlayer>>get(passives,"managed").contains(gamma)&&VanityAuthorityScene.<Map<Entity,String>>get(passives,"hearted").containsKey(gamma),"passive membership and heart key follow CURRENT Gamma");
        require(VanityAuthorityScene.<Set<ServerPlayer>>get(game.items().witchLust(),"hidden").contains(gamma)&&game.items().witchLust().snapshot().contains(lustChild)&&game.items().spellSources().source(lustChild)==lustPolicy,"hidden-player links and immutable managed Lust child policy return together");
        require(game.items().witchIndolence().age(hand)==age&&game.items().spellSources().source(hand)==handPolicy,"actual hand lifetime and accepted policy restore without another cast");
        var fingers=game.items().indolenceFingers();var births=VanityAuthorityScene.<Map<UUID,Object>>get(fingers,"rebirths");require(births.size()==1&&births.containsKey(base.owner.getUUID()),"only saved prepared rebirth remains");
        var effects=VanityAuthorityScene.<List<MobEffectInstance>>get(births.get(base.owner.getUUID()),"effects");var luck=effects.stream().filter(e->e.is(MobEffects.LUCK)).findFirst().orElseThrow();require(luck.getAmplifier()==4&&luck.getDuration()==400&&((EffectLayers.Access)luck).skywars2$layerHidden().getDuration()==1400,"suspended active/hidden effect clocks are independent of future resume/mutation");
        require(VanityAuthorityScene.<Set<UUID>>get(fingers,"ready").equals(Set.of(base.owner.getUUID()))&&!VanityAuthorityScene.<Set<UUID>>get(fingers,"consumed").contains(future),"ready/spent ledgers restore exactly");
        var payment=VanityAuthorityScene.<List<Object>>get(fingers,"costs").getFirst();require(VanityAuthorityScene.<Integer>get(payment,"left")==2&&VanityAuthorityScene.<UUID>get(payment,"finger").equals(finger.getUUID())&&game.server().getTickCount()-VanityAuthorityScene.<Integer>get(payment,"born")==bornAge,"selected finger, partial payment and birth-frame age restore");
        var deaths=VanityAuthorityScene.<List<Object>>get(game.items().witchWrath(),"deaths");require(deaths.size()==1&&VanityAuthorityScene.<LbcIndirectKills.Owner>get(deaths.getFirst(),"cause")==wrathCause,"deferred Wrath retains accepted cause without fresh causal stamp");
        int now=game.server().getTickCount();require(Math.floorMod(now+VanityAuthorityScene.<Integer>get(passives,"phaseOffset"),20)==pulse&&Math.floorMod(now+VanityAuthorityScene.<Integer>get(game.items().witchLust(),"phaseOffset"),5)==lustPulse,"periodic authority phases survive monotonic server-frame advancement");
    }
    void thawed(){
        require(game.items().mana().hudValue(base.owner,"lust_cd")==36,"actual periodic pulse occurs on the next saved logical frame, once across two native frames");var fingers=game.items().indolenceFingers();require(finger.isRemoved()&&victim.isDeadOrDying()&&victim.getKillCredit()==base.owner,"real resumed payment removes selected finger; deferred Wrath kills its actual victim with original author");
        require(VanityAuthorityScene.<Map<UUID,Object>>get(fingers,"rebirths").isEmpty()&&VanityAuthorityScene.<Set<UUID>>get(fingers,"ready").isEmpty()&&game.items().ownedEffects().debugTargetCount()>0,"real deferred rebirth resumes owned lease on current Alpha");
        var luck=base.owner.getEffect(MobEffects.LUCK);require(luck!=null&&luck.getAmplifier()==4&&luck.getDuration()>=397&&base.owner.getAbsorptionAmount()==3.5f,"native rebirth restores the saved positive lease and absorption");
        game.items().ownedEffects().withdraw(base.owner,LEASE);luck=base.owner.getEffect(MobEffects.LUCK);require(luck!=null&&luck.getAmplifier()==-3&&luck.getDuration()>=1397,"withdrawing resumed ownership reveals the saved independent baseline");
        require(game.items().witchIndolence().age(hand)>=age+2,"resumed native frames advance saved hand lifetime");
        game.items().witchIndolence().stop();fingers.stop();game.items().witchWrath().stop();game.items().witchLust().stop();game.items().witchPassives().stop();
        for(var p:List.of(base.owner,base.other)){p.entityTags().remove("sin_indolence");p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcStand.CLOCKS));}
    }
    void mutate(){game.items().witchPassives().stop();game.items().witchWrath().stop();game.items().witchIndolence().stop();game.items().indolenceFingers().stop();game.items().witchLust().stop();VanityAuthorityScene.<Set<UUID>>get(game.items().indolenceFingers(),"ready").add(future);VanityAuthorityScene.<Set<UUID>>get(game.items().indolenceFingers(),"consumed").add(future);}
    void pending(){require(VanityAuthorityScene.<Set<UUID>>get(game.items().indolenceFingers(),"ready").equals(Set.of(future))&&game.items().witchPassives().count()==0,"missing body defers every authority owner without partial publication");}
}
