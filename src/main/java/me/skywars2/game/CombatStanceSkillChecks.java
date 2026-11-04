package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Actual native damage/criterion, scheduled shield warmup and instant potion qualification. */
public final class CombatStanceSkillChecks {
    private CombatStanceSkillChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Combat stances: "+why);}
    private static void near(double got,double expected,String why){require(Math.abs(got-expected)<1e-4,why+" got="+got+" expected="+expected);}
    private static Fixture running;
    public static void run(GameController game){
        require(running==null,"exclusive staged fixture");var f=new Fixture(game);running=f;
        try{f.initialize();f.hits();f.champion();f.begin();}
        catch(RuntimeException|Error error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    public static void tick(GameController game){
        if(running==null)return;var f=running;
        try{if(f.step()){running=null;f.close();SkyWars2.LOGGER.info("[check] PASS combat-stance-skills (actual native attacks/arrows, target-local magic, source modifiers, shield criterion, registered Heal I, TimeStop and player-modifier rewind)");}}
        catch(RuntimeException|Error error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL combat-stance-skills",error);}
    }
    private static final class Witness extends Cow {
        final List<Float> magical=new ArrayList<>();ServerPlayer credited;
        Witness(ServerLevel level){super(EntityTypes.COW,level);}
        @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
            if(source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)){
                magical.add(amount);if(source.getEntity() instanceof ServerPlayer p)credited=p;
            }
            return super.hurtServer(level,source,amount);
        }
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,other;final ServerLevel level;
        final RegenerationBeltChecks.Snapshot saved,otherSaved;
        final boolean skills,ranked,frozen;final int pvp,cooldown,otherCooldown;
        final List<Entity> actors=new ArrayList<>();
        final Map<java.lang.reflect.Field,Object> ownerCombat=new LinkedHashMap<>(),otherCombat=new LinkedHashMap<>();
        RegenerationBeltChecks.ScheduledClient client,audience;Vec3 at;boolean closed;
        int started,updates,stage;float healingBefore;
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&owner.level()==other.level()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two native participants");
            level=owner.level();saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);
            skills=game.config().lbcSkills;ranked=game.config().ranked;frozen=game.server().tickRateManager().isFrozen();pvp=game.config().pvpMode;
            cooldown=owner.damageCooldownTime;otherCooldown=other.damageCooldownTime;
            try{for(String name:List.of("lastHurtByMob","lastHurtByMobTimestamp","lastHurtByPlayer","lastHurtByPlayerMemoryTime","lastHurtMob","lastHurtMobTimestamp")){
                java.lang.reflect.Field found=null;
                for(Class<?> type=owner.getClass();type!=null&&found==null;type=type.getSuperclass())try{found=type.getDeclaredField(name);}catch(NoSuchFieldException ignored){}
                if(found==null)throw new IllegalStateException("Missing native combat state "+name);
                found.setAccessible(true);ownerCombat.put(found,found.get(owner));otherCombat.put(found,found.get(other));
            }}catch(IllegalAccessException error){throw new IllegalStateException(error);}
        }
        void initialize(){
            game.config().lbcSkills=true;game.config().ranked=true;game.config().pvpMode=0;game.server().tickRateManager().setFrozen(false);
            for(var p:List.of(owner,other)){
                p.closeContainer();p.stopUsingItem();p.getInventory().clearContent();p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);
                p.getAbilities().invulnerable=false;p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);
                p.entityTags().removeAll(Set.of("time_stopped","spectator","strict_map_object","magic_academy","rcdenyalways","sin_greed_active","nomagic_active"));
                var profile=game.players().get(p.getUUID());profile.lbcSkill=0;profile.lbcKit=0;profile.lbcChallengeKit=0;profile.lbcChallenge=0;profile.lbcTrinket=0;
                Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);
                var defaults=DefaultAttributes.getSupplier(EntityTypes.PLAYER);
                for(var h:BuiltInRegistries.ATTRIBUTE.listElements().toList()){
                    var attr=p.getAttribute(h);if(attr!=null&&defaults.hasAttribute(h)){attr.removeModifiers();attr.setBaseValue(h.equals(Attributes.MAX_HEALTH)?100:defaults.getBaseValue(h));}
                }
                ready(p);game.items().magic().inventoryChanged(p);game.items().armorSets().inventoryChanged(p);
            }
            game.items().magic().flushCleansing();at=owner.position().add(0,8,0);
            move(owner,at);move(other,at.add(3,0,0));
            client=new RegenerationBeltChecks.ScheduledClient(game,owner);audience=new RegenerationBeltChecks.ScheduledClient(game,other);
        }
        void move(ServerPlayer p,Vec3 pos){p.teleportTo(level,pos.x,pos.y,pos.z,Set.of(),-90,0,true);if(p==owner&&client!=null)client.acknowledge();if(p==other&&audience!=null)audience.acknowledge();}
        void ready(ServerPlayer p){p.setHealth(100);p.setAbsorptionAmount(0);p.setInvulnerableTime(0);p.damageCooldownTime=0;p.hurtTime=0;p.setDeltaMovement(Vec3.ZERO);}
        void select(int id){game.items().championSkill().clear(owner);game.players().get(owner.getUUID()).lbcSkill=id;require(game.lbc().hasSkill(owner,id),"shared admission"+id);}
        void hit(ServerPlayer victim,DamageSource source){ready(victim);require(victim.hurtServer(level,source,1),"native accepted hit");}
        Arrow arrow(){var a=new Arrow(EntityTypes.ARROW,level);a.setOwner(owner);a.setPos(at.add(0,3,0));a.setNoGravity(true);actors.add(a);return a;}
        Witness body(boolean magical,Vec3 pos){var c=new Witness(level);c.setPos(pos);c.setNoAi(true);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);c.setHealth(100);if(magical)c.entityTags().add("summactivated");require(level.addFreshEntity(c),"native target body");actors.add(c);return c;}
        void hits(){
            select(29);var mana=game.items().mana();mana.maximum(owner,10000);mana.set(owner,14999);mana.set(other,0);
            hit(other,other.damageSources().playerAttack(owner));near(mana.get(owner),15999,"pre-hit threshold overshoot retained");near(mana.get(other),-1000,"victim debit has no floor");
            mana.set(owner,0);mana.set(other,500);hit(other,other.damageSources().arrow(arrow(),owner));near(mana.get(owner),1000,"native owned arrow steals");near(mana.get(other),-500,"native arrow victim debit");
            mana.set(owner,0);hit(other,new DamageSource(owner.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(LbcKfcRewards.CULTUS_ARROW),arrow(),owner));
            near(mana.get(owner),1000,"native Cultus arrow retains acceptable tag");
            mana.maximum(owner,Integer.MAX_VALUE);mana.set(owner,Integer.MAX_VALUE);mana.set(other,Integer.MIN_VALUE);hit(other,other.damageSources().playerAttack(owner));
            near(mana.get(owner),Integer.MAX_VALUE,"native award does not wrap into negative mana");near(mana.get(other),Integer.MIN_VALUE,"native debit does not wrap into positive mana");mana.maximum(owner,10000);
            mana.set(owner,0);hit(other,other.damageSources().indirectMagic(owner,owner));near(mana.get(owner),0,"plain attributed magic cannot steal");
            var actual=body(true,at.add(0,0,3));var collateral=body(true,at.add(0,0,5));collateral.hurtTime=10;
            require(game.items().magic().magical(actual),"shared magical summon positive control");
            require(actual.hurtServer(level,actual.damageSources().playerAttack(owner),1),"native magical mob contact");
            require(actual.magical.equals(List.of(15f))&&actual.credited==owner,"one native15magic attempt with actual owner");
            require(collateral.magical.isEmpty()&&collateral.getHealth()==100,"unrelated HurtTime mob untouched");
            var ordinary=body(false,at.add(0,0,-3));ordinary.hurtServer(level,ordinary.damageSources().playerAttack(owner),1);
            require(ordinary.magical.isEmpty(),"ordinary cow receives no invented undead/mob bonus");
            mana.set(owner,0);owner.entityTags().add("time_stopped");try{hit(other,other.damageSources().playerAttack(owner));near(mana.get(owner),0,"TimeStop closes event grant");}finally{owner.entityTags().remove("time_stopped");}
        }
        void magicPolicy(){
            var marker=new Marker(EntityTypes.MARKER,level);marker.setPos(owner.position());marker.entityTags().add("stopper_magic");
            require(level.addFreshEntity(marker),"actual MAG marker publication");game.items().magic().loaded(marker);
            try{
                var mana=game.items().mana();mana.set(owner,0);mana.set(other,500);
                require(game.items().magic().suppressed(owner),"positive MAG at vampire owner");hit(other,other.damageSources().playerAttack(owner));
                near(mana.get(owner),0,"MAG suppresses vampire reward");near(mana.get(other),500,"MAG suppresses vampire debit");
                marker.setPos(other.position().add(19,0,0));
                require(!game.items().magic().suppressed(owner)&&game.items().magic().suppressed(other),"separate target-only MAG field");hit(other,other.damageSources().playerAttack(owner));
                near(mana.get(owner),0,"target MAG prevents reward");near(mana.get(other),500,"target MAG protects its mana");
            }finally{game.items().magic().unloaded(marker);marker.discard();}
        }
        void defense(){
            near(owner.getAttribute(Attributes.ARMOR).getModifier(LbcChampionSkill.ARMOR).amount(),20,"exact source armor modifier");
            near(owner.getAttribute(Attributes.ARMOR_TOUGHNESS).getModifier(LbcChampionSkill.TOUGHNESS).amount(),5,"exact source toughness modifier");
            require(owner.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(LbcChampionSkill.ATTACK)==null,"defense replaces attack");
        }
        void offense(){
            near(owner.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(LbcChampionSkill.ATTACK).amount(),6,"exact source attack modifier");
            require(owner.getAttribute(Attributes.ARMOR).getModifier(LbcChampionSkill.ARMOR)==null&&owner.getAttribute(Attributes.ARMOR_TOUGHNESS).getModifier(LbcChampionSkill.TOUGHNESS)==null,"offense replaces both defenses");
        }
        void champion(){
            select(29);magicPolicy();select(33);hit(other,other.damageSources().playerAttack(owner));defense();hit(other,other.damageSources().playerAttack(owner));defense();
            hit(owner,owner.damageSources().playerAttack(other));offense();hit(other,other.damageSources().arrow(arrow(),owner));offense();
            hit(other,other.damageSources().indirectMagic(owner,owner));defense();
            owner.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,200,4),owner);hit(owner,owner.damageSources().playerAttack(other));offense();owner.removeAllEffects();
            hit(other,other.damageSources().playerAttack(owner));defense();
            var jammer=new Marker(EntityTypes.MARKER,level);jammer.setPos(owner.position());jammer.entityTags().addAll(Set.of("stopper","stopper_magic"));require(level.addFreshEntity(jammer),"actual ordinary jammer actor");game.items().magic().loaded(jammer);
            try{require(game.items().magic().suppressed(owner)&&game.items().magic().mechanismSuppressed(owner),"Champion positive MAG and TECH");hit(owner,owner.damageSources().playerAttack(other));offense();hit(other,other.damageSources().playerAttack(owner));defense();}
            finally{game.items().magic().unloaded(jammer);jammer.discard();}
            var image=new MatchPlayerCheckpoint(game,owner);game.items().championSkill().clear(owner);
            game.server().tickRateManager().setFrozen(true);
            try{require(image.restoreFrozen()==owner,"same native player body rewind");}finally{game.server().tickRateManager().setFrozen(false);}
            client.acknowledge();defense();
        }
        void begin(){
            started=game.server().getTickCount();stage=0;ready(owner);move(owner,at);move(other,at.add(3,0,0));
            owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHIELD));
            require(owner.gameMode.useItem(owner,level,owner.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction(),"actual native shield use");updates=client.updates();
        }
        boolean step(){
            client.acknowledge();audience.acknowledge();require(game.server().getTickCount()-started<90,"bounded native fixture");
            int elapsed=client.updates()-updates;
            if(stage==0){
                if(elapsed<6)return false;require(owner.isBlocking(),"scheduled native shield warmup");
                owner.setInvulnerableTime(0);owner.damageCooldownTime=0;float hp=owner.getHealth();
                owner.hurtServer(level,owner.damageSources().playerAttack(other),4);near(owner.getHealth(),hp,"actual native shield has no HP loss");offense();
                owner.stopUsingItem();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);select(29);
                var mana=game.items().mana();mana.maximum(owner,10000);mana.set(owner,15000);mana.set(other,0);
                owner.setHealth(50);healingBefore=owner.getHealth();hit(other,other.damageSources().playerAttack(owner));
                near(mana.get(owner),15000,"Heal branch adds no mana");near(mana.get(other),-1000,"Heal branch independently debits victim");
                require(owner.hasEffect(MobEffects.INSTANT_HEALTH),"native Heal I potion queued");updates=client.updates();stage=1;return false;
            }
            if(elapsed<2)return false;near(owner.getHealth(),healingBefore+4,"scheduled native Heal I applies4HP");return true;
        }
        @Override public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(()->{owner.entityTags().remove("time_stopped");owner.stopUsingItem();game.items().championSkill().clear(owner);},
                    ()->{for(var actor:actors){if(actor instanceof LivingEntity living)game.items().indirectKills().clear(living);actor.discard();}},
                    ()->{if(client!=null)client.close();if(audience!=null)audience.close();},
                    ()->{game.config().lbcSkills=skills;game.config().ranked=ranked;game.config().pvpMode=pvp;saved.restore(game);otherSaved.restore(game);},
                    ()->{saved.restoreAttributes();saved.restoreUse();otherSaved.restoreAttributes();otherSaved.restoreUse();owner.damageCooldownTime=cooldown;other.damageCooldownTime=otherCooldown;},
                    ()->{try{for(var e:ownerCombat.entrySet())e.getKey().set(owner,e.getValue());for(var e:otherCombat.entrySet())e.getKey().set(other,e.getValue());}catch(IllegalAccessException error){throw new IllegalStateException(error);}},
                    ()->{game.items().magic().inventoryChanged(owner);game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();game.items().armorSets().inventoryChanged(owner);game.items().armorSets().inventoryChanged(other);game.server().tickRateManager().setFrozen(frozen);game.players().save();}))
                try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Combat stance fixture restoration",failure);
        }
    }
}
