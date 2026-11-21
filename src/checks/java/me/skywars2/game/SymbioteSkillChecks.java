package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Actual native damage/index/admission checks; execute only in the isolated mode1 fixture. */
public final class SymbioteSkillChecks {
    private SymbioteSkillChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Symbiote: "+why);}
    private static void near(float actual,float expected,String why){require(Math.abs(actual-expected)<1e-4,why+": "+actual+" != "+expected);}
    private static boolean feedbackRegistered;
    private static Fixture feedbackFixture;
    private static void installFeedbackProbe(){
        if(feedbackRegistered)return;
        feedbackRegistered=true;
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((target,source,base,taken,blocked)->{
            var fixture=feedbackFixture;
            if(fixture==null)return;
            if(fixture.transferTargets.contains(target)){
                fixture.transferCallbacks++;
                require(source.getEntity()==fixture.other&&source.getDirectEntity()==null,"indirect pet share retains causal attacker only");
                require(source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)&&!source.isDirect()
                        &&!LbcDamage.isMeleeAttack(source,fixture.other),"retained PLAYER_ATTACK type is not another melee impact");
                require(fixture.game.items().indirectKills().babylonCause(source)==fixture.transferCause,"captured historical cause retained");
                require(LbcIndirectKills.originalDamage(source)==fixture.transferOriginal,"original source-specific behavior unwraps");
                require(target.getKillCredit()==fixture.other,"actual native mob attacker credit retained");
            }
            if(target!=fixture.feedbackTarget)return;
            require(++fixture.feedbackCallbacks==1,"no recursive pet feedback event");
            fixture.feedbackAccepted=fixture.owner.hurtServer(fixture.owner.level(),fixture.owner.damageSources().generic(),1);
        });
    }
    public static void run(GameController game){
        try(var f=new Fixture(game)){f.prepare();f.shares();f.reverse();f.nativeFeedback();f.indirectSource();f.index();f.clones();f.timeStop();f.acquisitionAndPause();f.bypass();}
        SkyWars2.LOGGER.info("[check] PASS symbiote-skills (actual native damage and raw bridge; exact fractional equal shares, terminal protection, fallback, reverse direct/self/raw subordinate hits, dead-owner and disabled-skill controls, source-score distant membership, source changes/removal, cross-dimension exclusion, native reactions, paused acquisition and unjammed MAG/TECH)");
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,other;
        final RegenerationBeltChecks.Snapshot saved,otherSaved;
        final boolean skills,ranked,frozen;final int pvp,ownerCooldown,otherCooldown;
        final Set<Entity> actors=Collections.newSetFromMap(new IdentityHashMap<>());
        final Vec3 at;
        LivingEntity feedbackTarget;int feedbackCallbacks;boolean feedbackAccepted;
        final Set<LivingEntity> transferTargets=Collections.newSetFromMap(new IdentityHashMap<>());
        int transferCallbacks;DamageSource transferOriginal;LbcIndirectKills.Owner transferCause;
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.canUseGameplayItems(owner)&&owner.level()==other.level(),"two actual mode1 participants");
            saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);
            skills=game.config().lbcSkills;ranked=game.config().ranked;pvp=game.config().pvpMode;
            ownerCooldown=owner.damageCooldownTime;otherCooldown=other.damageCooldownTime;
            frozen=game.server().tickRateManager().isFrozen();at=owner.position();
        }
        void prepare(){
            require(!frozen&&!game.items().timeStop().stopped(owner.level())&&game.items().timeStop().pendingDamageCount()==0,"active native fixture without preexisting TimeStop work");game.config().lbcSkills=true;game.config().ranked=true;game.config().pvpMode=0;
            for(var player:List.of(owner,other)){
                player.stopUsingItem();player.getInventory().clearContent();player.removeAllEffects();player.setGameMode(GameType.SURVIVAL);
                player.getAbilities().invulnerable=false;player.entityTags().removeAll(Set.of("time_stopped","spectator","strict_map_object","rcdenyalways","magic_academy","sin_greed_active","nomagic_active"));
                var defaults=DefaultAttributes.getSupplier(EntityTypes.PLAYER);
                for(var holder:net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.listElements().toList()){
                    var value=player.getAttribute(holder);if(value!=null&&defaults.hasAttribute(holder)){value.removeModifiers();value.setBaseValue(holder.equals(Attributes.MAX_HEALTH)?100:defaults.getBaseValue(holder));}
                }
                for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))player.setItemSlot(slot,ItemStack.EMPTY);
                var profile=game.players().get(player.getUUID());profile.lbcSkill=0;profile.lbcKit=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);
                player.setHealth(100);player.setAbsorptionAmount(0);ready(player);game.items().magic().inventoryChanged(player);
            }
            game.items().magic().flushCleansing();game.players().get(owner.getUUID()).lbcSkill=20;
            game.items().mana().score(owner,"lbcID2").set(712345);game.items().mana().score(other,"lbcID2").set(712346);
            require(game.lbc().hasSkill(owner,20),"actual catalog/live selection");
        }
        void ready(LivingEntity target){target.damageCooldownTime=0;target.setInvulnerableTime(0);}
        Cow cow(Vec3 position){var mob=new Cow(EntityTypes.COW,owner.level());mob.setPos(position);mob.setNoAi(true);mob.setNoGravity(true);require(owner.level().addFreshEntity(mob),"native publication");actors.add(mob);return mob;}
        void clearMobs(){for(var actor:List.copyOf(actors)){if(actor instanceof Marker)game.items().magic().unloaded(actor);game.items().entityUnloaded(actor);actor.discard();}actors.clear();}
        DamageSource hit(){return owner.damageSources().playerAttack(other);}
        void hitOwner(float damage){ready(owner);require(owner.hurtServer(owner.level(),hit(),damage),"actual owner hit admitted");}
        void bind(Cow mob){game.items().communism().subjugate(owner,mob,true,true);}
        void shares(){
            var a=cow(at.add(5,0,0));var b=cow(at.add(-5,0,0));bind(a);bind(b);
            owner.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(4);owner.setAbsorptionAmount(4);
            near(owner.getAbsorptionAmount(),4,"native absorption positive control before damage");float hp=owner.getHealth();hitOwner(3);
            near(owner.getHealth(),hp,"owner zero HP");near(owner.getAbsorptionAmount(),4,"owner absorption intact");
            near(a.getHealth(),8.5f,"first exact 1.5 share");near(b.getHealth(),8.5f,"second exact 1.5 share");
            require(owner.hurtTime>0&&owner.damageCooldownTime>0,"owner native hit reactions");
            require(a.hurtTime>0&&b.hurtTime>0,"pets use native hurt presentation");
            hitOwner(.5f);near(a.getHealth(),8.25f,"no minimum1");near(b.getHealth(),8.25f,"fractional equal shares");
            ready(a);require(a.hurtServer(owner.level(),a.damageSources().playerAttack(other),2),"direct pet hit accepted");
            near(a.getHealth(),8.25f,"direct pet hit leaves subordinate HP intact");near(b.getHealth(),8.25f,"direct hit not redistributed");near(owner.getHealth(),hp-2,"direct pet hit transfers accepted debit to owner");
            require(BlackHoleDamage.apply(game,owner,hit(),1),"actual raw bridge admitted");near(a.getHealth(),7.75f,"raw bridge half1");near(b.getHealth(),7.75f,"raw bridge half2");
            clearMobs();var last=cow(at.add(5,0,0));bind(last);last.setHealth(.25f);hitOwner(20);
            require(last.isDeadOrDying(),"real pet death");near(owner.getHealth(),hp-2,"terminal share still protects entire hit");
            clearMobs();owner.setAbsorptionAmount(0);hitOwner(3);near(owner.getHealth(),hp-5,"no pets fallback");
        }
        void reverse(){
            var pet=cow(at.add(5,0,0));var sibling=cow(at.add(-5,0,0));bind(pet);bind(sibling);float hp=owner.getHealth();
            ready(pet);require(pet.hurtServer(owner.level(),hit(),2),"actual direct subordinate hit");
            near(pet.getHealth(),10,"native subordinate damage redirected before HP debit");near(sibling.getHealth(),10,"reverse hit does not bounce into another pet");near(owner.getHealth(),hp-2,"owner pays entire accepted subordinate hit once");
            require(owner.getLastDamageSource()!=null&&owner.getLastDamageSource().getEntity()==other,"reverse transfer retains actual attacker credit");
            ready(pet);require(pet.hurtServer(owner.level(),pet.damageSources().playerAttack(owner),3),"owner can actually strike own subordinate");
            near(pet.getHealth(),10,"own attack cannot kill protected subordinate");near(owner.getHealth(),hp-5,"own attack still costs owner real HP");
            require(BlackHoleDamage.apply(game,pet,hit(),1.5f),"actual raw-HP subordinate damage admitted");near(pet.getHealth(),10,"raw bridge also protects subordinate");near(owner.getHealth(),hp-6.5f,"raw bridge inverse transfer paid once");
            float remaining=owner.getHealth();owner.setHealth(0);
            try{ready(pet);require(pet.hurtServer(owner.level(),hit(),2),"native hit with dead owner admitted");near(pet.getHealth(),8,"dead owner cannot protect subordinate");}finally{owner.setHealth(remaining);}
            game.players().get(owner.getUUID()).lbcSkill=0;
            try{ready(pet);require(pet.hurtServer(owner.level(),hit(),1),"native hit after skill removal admitted");near(pet.getHealth(),7,"disabled skill restores ordinary subordinate damage");near(owner.getHealth(),remaining,"no reverse protection after skill removal");}finally{game.players().get(owner.getUUID()).lbcSkill=20;}
            clearMobs();
        }
        void nativeFeedback(){
            var a=cow(at.add(5,0,0));var b=cow(at.add(-5,0,0));bind(a);bind(b);
            // Native generic is cooldown-bypassing in this pack: no fixture cooldown reset
            // inside the callback, so this is a genuinely accepted nested native hit.
            require(owner.damageSources().generic().is(net.minecraft.tags.DamageTypeTags.BYPASSES_COOLDOWN),"feedback source bypasses native cooldown");
            installFeedbackProbe();require(feedbackFixture==null,"exclusive feedback fixture");
            feedbackTarget=a;feedbackCallbacks=0;feedbackAccepted=false;feedbackFixture=this;
            float hp=owner.getHealth();
            try{hitOwner(4);}finally{feedbackFixture=null;feedbackTarget=null;}
            require(feedbackCallbacks==1&&feedbackAccepted,"pet AFTER_DAMAGE invoked one accepted nested native owner hit");
            near(owner.getHealth(),hp,"native feedback keeps protected owner at zero HP debit");
            near(a.getHealth(),8,"first original share paid once");near(b.getHealth(),8,"second original share paid once");
            clearMobs();
        }
        void indirectSource(){
            var a=cow(at.add(5,0,0));var b=cow(at.add(-5,0,0));bind(a);bind(b);
            transferOriginal=hit();transferCause=game.items().indirectKills().wildCause(other);
            var captured=game.items().indirectKills().captureDamage(transferOriginal,transferCause);
            installFeedbackProbe();require(feedbackFixture==null,"exclusive transfer source fixture");
            transferTargets.add(a);transferTargets.add(b);transferCallbacks=0;feedbackFixture=this;
            try{ready(owner);require(owner.hurtServer(owner.level(),captured,4),"actual captured melee owner hit");}
            finally{feedbackFixture=null;transferTargets.clear();}
            require(transferCallbacks==2,"both real native pet AFTER_DAMAGE callbacks checked");
            near(a.getHealth(),8,"indirect first equal share");near(b.getHealth(),8,"indirect second equal share");
            var accepted=a.getLastDamageSource();require(accepted==b.getLastDamageSource(),"one transferred source per distribution");
            var sources=new MatchDamageSources(game,List.of(a,b,owner,other));var image=sources.capture(accepted);
            require(image.wrapped()!=null&&image.wrapped().wrapped()!=null&&image.wrapped().wrapped().original()==captured,"captured/transfer/original source graph includes parent");
            ((me.skywars2.mixin.CombatCheckpointAccess.Source)accepted).skywars2$cause(null);
            ((me.skywars2.mixin.CombatCheckpointAccess.Source)image.wrapped().original()).skywars2$cause(null);
            var pending=new HashSet<UUID>();sources.restore(id->id.equals(owner.getUUID())?owner:id.equals(other.getUUID())?other:id.equals(a.getUUID())?a:id.equals(b.getUUID())?b:null,pending);
            require(pending.isEmpty()&&accepted.getEntity()==other&&accepted.getDirectEntity()==null
                    &&image.wrapped().original().getEntity()==other&&image.wrapped().original().getDirectEntity()==null,"canonical parent graph restores indirect identities");
            require(transferOriginal.getEntity()==other&&transferOriginal.getDirectEntity()==other,"original native impact remains intact");
            clearMobs();
        }
        void index(){
            var far=cow(at.add(30,0,0));game.items().mana().score(far,"lbcID2").set(712345);
            float hp=owner.getHealth();hitOwner(3);near(far.getHealth(),7,"preowned source-score mob outside acquisition range participates");near(owner.getHealth(),hp,"preowned remote protection");
            game.items().mana().score(far,"lbcID2").set(712346);hitOwner(1);near(owner.getHealth(),hp-1,"score reassignment immediate");
            game.items().mana().score(far,"lbcID2").set(712345);var objective=game.server().getScoreboard().getObjective("lbcID2");
            game.server().getScoreboard().resetSinglePlayerScore(far,objective);hitOwner(1);near(owner.getHealth(),hp-2,"score removal immediate");
            clearMobs();
            var summon=game.items().summons().spawnPreset(owner,"vex",at.add(10,0,0));
            require(summon!=null,"existing native summons ready");actors.add(summon);
            float summonHp=summon.getHealth();hp=owner.getHealth();hitOwner(1);
            near(summon.getHealth(),summonHp-1,"existing native owned summon participates without capture");near(owner.getHealth(),hp,"summon protects owner");clearMobs();
            // No chunk creation: a foreign-dimension native body can be indexed without publication.
            var foreignLevel=game.server().getAllLevels().iterator();net.minecraft.server.level.ServerLevel foreign=null;
            while(foreignLevel.hasNext()){var next=foreignLevel.next();if(next!=owner.level()){foreign=next;break;}}
            require(foreign!=null,"fixture has another native dimension");var foreignMob=new Cow(EntityTypes.COW,foreign);foreignMob.setPos(at);actors.add(foreignMob);
            game.items().communism().loaded(foreignMob);game.items().mana().score(foreignMob,"lbcID2").set(712345);
            hp=owner.getHealth();hitOwner(1);near(owner.getHealth(),hp-1,"different native dimension excluded");near(foreignMob.getHealth(),10,"foreign mob untouched");clearMobs();
            // Same real native type and score index as the living FPV hitbox; publication is
            // deliberately unnecessary for testing cohort admission and creates no pilot session.
            var drone=new LbcFpvDrone.Drone(LbcFpvDrone.TYPE,owner.level());drone.setPos(at.add(30,0,0));actors.add(drone);
            require(drone.getType().builtInRegistryHolder().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,net.minecraft.resources.Identifier.parse("minecraft:mobs"))),"FPV native tag is a positive classification control");
            game.items().communism().loaded(drone);game.items().mana().score(drone,"lbcID2").set(712345);
            float droneHp=drone.getHealth();hp=owner.getHealth();hitOwner(1);
            near(owner.getHealth(),hp-1,"living projectile hitbox never becomes a subordinate");near(drone.getHealth(),droneHp,"FPV projectile never pays a pet share");clearMobs();
        }
        void acquisitionAndPause(){
            var near=cow(at.add(1,0,0));near.entityTags().add("rcdenyalways");
            var service=cow(at.add(-1,0,0));service.entityTags().add("strict_map_object");
            game.server().tickRateManager().setFrozen(true);game.items().symbioteSkill().tick();
            require(game.items().communism().owner(near)==null,"native freeze pauses capture");game.server().tickRateManager().setFrozen(false);
            for(String tag:List.of("nomagic_active","sin_greed_active"))owner.entityTags().add(tag);
            for(String tag:List.of("stopper_magic","stopper")){
                var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(at);marker.entityTags().add(tag);require(owner.level().addFreshEntity(marker),"jammer publication");actors.add(marker);game.items().magic().loaded(marker);
            }
            require(game.items().magic().suppressed(owner)&&game.items().magic().mechanismSuppressed(owner),"real MAG/TECH fields active");
            game.items().symbioteSkill().tick();require(game.items().communism().owner(near)==owner,"MAG/TECH/tags do not suppress capture");
            require(game.items().communism().owner(service)==null,"service map object excluded");
            // Greed rejects ordinary owner admission, so test the accepted reducer boundary directly.
            near.setHealth(10);near(game.items().symbioteSkill().absorb(owner,hit(),3),0,"unjammed accepted reducer");near(near.getHealth(),7,"unjammed actual native pet debit");
            owner.entityTags().removeAll(Set.of("nomagic_active","sin_greed_active"));clearMobs();
        }
        void clones(){
            var original=cow(at.add(5,0,0));bind(original);original.entityTags().add("symbiote_check_family");
            require(game.items().cloneSkills().copyNow(original,17),"actual x4 native clone path");
            var family=owner.level().getEntitiesOfClass(Cow.class,original.getBoundingBox().inflate(4),e->e.entityTags().contains("symbiote_check_family"));
            require(family.size()==4,"original plus three fresh native bodies");actors.addAll(family);
            for(var mob:family)require(game.items().communism().owner(mob)==owner,"canonical cloned controller");
            float hp=owner.getHealth();hitOwner(4);for(var mob:family)near(mob.getHealth(),9,"all four native bodies take equal shares");near(owner.getHealth(),hp,"no owner recursion via source-score player");
            var bodies=MatchActorCheckpoint.currentBodies(game).values();game.items().communism().reindexOwnership(bodies);
            hitOwner(2);for(var mob:family)near(mob.getHealth(),8.5f,"canonical derived index rebuild keeps family");clearMobs();
        }
        void timeStop(){
            var pet=cow(at.add(5,0,0));bind(pet);var near=cow(at.add(1,0,0));float hp=owner.getHealth();
            game.items().timeStop().begin(owner.level(),1);require(game.items().timeStop().frozen(owner),"native victim actually frozen");
            game.items().symbioteSkill().tick();require(game.items().communism().owner(near)==null,"TimeStop freezes acquisition");
            ready(owner);owner.hurtServer(owner.level(),hit(),3);
            require(game.items().timeStop().pendingDamageCount()==1,"one actual accepted native hit queued");near(owner.getHealth(),hp,"queued owner unchanged");near(pet.getHealth(),10,"queued pet unchanged");
            game.items().timeStop().end(owner.level());game.items().timeStop().tick();
            require(game.items().timeStop().pendingDamageCount()==0,"queued hit replayed once");near(owner.getHealth(),hp,"replayed owner0");near(pet.getHealth(),7,"one exact replayed share");clearMobs();
        }
        void bypass(){
            var pet=cow(at.add(5,0,0));bind(pet);float hp=owner.getHealth();ready(owner);
            require(owner.hurtServer(owner.level(),owner.damageSources().fellOutOfWorld(),2),"actual bypass-source hit");near(owner.getHealth(),hp,"void still owner0");near(pet.getHealth(),8,"void exact raw share");clearMobs();
            var killPet=cow(at.add(5,0,0));bind(killPet);ready(owner);owner.kill(owner.level());
            near(owner.getHealth(),hp,"native kill genericKill damage still owner0");require(killPet.isDeadOrDying(),"native kill Float.MAX_VALUE diverted into real pet HP");clearMobs();
        }
        public void close(){
            game.server().tickRateManager().setFrozen(frozen);game.items().timeStop().end(owner.level());clearMobs();game.items().symbioteSkill().clear(owner);
            owner.entityTags().removeAll(Set.of("nomagic_active","sin_greed_active"));
            game.config().lbcSkills=skills;game.config().ranked=ranked;game.config().pvpMode=pvp;
            saved.restore(game);otherSaved.restore(game);saved.restoreAttributes();otherSaved.restoreAttributes();saved.restoreUse();otherSaved.restoreUse();
            owner.damageCooldownTime=ownerCooldown;other.damageCooldownTime=otherCooldown;
        }
    }
}
