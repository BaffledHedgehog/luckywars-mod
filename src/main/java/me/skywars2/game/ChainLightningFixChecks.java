package me.skywars2.game;

import java.util.*;
import java.lang.reflect.Field;
import net.minecraft.world.entity.LivingEntity;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Uses real controller ticks: Thunder mob -> caster, then the distinct Raid124 status ingress. */
public final class ChainLightningFixChecks {
    private ChainLightningFixChecks(){}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Chain fixes: "+why);}
    private static final class Saved implements AutoCloseable {
        final GameController game;final ServerPlayer player;final GameChecks.WandThirdPlayerState state;
        final Map<Field,Object> profile=new LinkedHashMap<>(),combat=new LinkedHashMap<>(),food=new LinkedHashMap<>(),tracker=new LinkedHashMap<>();
        final java.util.List<net.minecraft.world.damagesource.CombatEntry> trackerEntries;
        final Map<net.minecraft.advancements.AdvancementHolder,java.util.Set<String>> advancements=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<net.minecraft.resources.Identifier>,Integer> stats=new LinkedHashMap<>();final int visual;
        Saved(GameController game,ServerPlayer player){
            this.game=game;this.player=player;state=new GameChecks.WandThirdPlayerState(player,game.items().mana());
            visual=game.items().mana().regeneration(player);
            try{
                for(var field:net.minecraft.world.food.FoodData.class.getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())){field.setAccessible(true);food.put(field,field.get(player.getFoodData()));}
                var p=game.players().get(player.getUUID());for(var field:p.getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())){
                    profile.put(field,snapshotValue(field.get(p)));
                }
                for(String name:new String[]{"hurtTime","hurtDuration","lastHurt","lastDamageSource","lastDamageStamp","lastHurtByMob","lastHurtByMobTimestamp","lastHurtByPlayer","lastHurtByPlayerMemoryTime","lastHurtMob","lastHurtMobTimestamp"}){
                    var field=LivingEntity.class.getDeclaredField(name);field.setAccessible(true);combat.put(field,field.get(player));
                }
                for(String name:new String[]{"lastDamageTime","combatStartTime","combatEndTime","inCombat","takingDamage"}){
                    var field=net.minecraft.world.damagesource.CombatTracker.class.getDeclaredField(name);field.setAccessible(true);tracker.put(field,field.get(player.getCombatTracker()));
                }
                trackerEntries=java.util.List.copyOf(combatEntries(player));
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            for(var advancement:game.server().getAdvancements().getAllAdvancements()){
                var completed=new java.util.HashSet<String>();player.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(completed::add);advancements.put(advancement,completed);
            }
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=net.minecraft.stats.Stats.CUSTOM.get(id);stats.put(stat,player.getStats().getValue(stat));}
        }
        @Override public void close(){
            player.entityTags().remove("sin_greed_active");game.items().bolts().clearEffects(player);game.items().indirectKills().clear(player);
            try{var p=game.players().get(player.getUUID());for(var entry:profile.entrySet())entry.getKey().set(p,entry.getValue());state.restore(game);
                for(var entry:combat.entrySet())entry.getKey().set(player,entry.getValue());
                for(var entry:food.entrySet())entry.getKey().set(player.getFoodData(),entry.getValue());
                for(var entry:tracker.entrySet())entry.getKey().set(player.getCombatTracker(),entry.getValue());
                var entries=combatEntries(player);entries.clear();entries.addAll(trackerEntries);
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            game.items().mana().score(player,"manaregen_visual").set(visual);
            stats.forEach((stat,value)->player.getStats().setValue(player,stat,value));game.items().magic().inventoryChanged(player);
            advancements.forEach((advancement,completed)->{
                for(var criterion:advancement.value().criteria().keySet())if(!completed.contains(criterion))player.getAdvancements().revoke(advancement,criterion);
            });
        }
    }
    private static Object snapshotValue(Object value){
        if(value instanceof int[] array)return array.clone();
        if(value instanceof java.util.Set<?> set)return new java.util.LinkedHashSet<>(set);
        if(value instanceof java.util.List<?> list)return new java.util.ArrayList<>(list);
        if(value instanceof Map<?,?> map)return new LinkedHashMap<>(map);
        return value;
    }
    @SuppressWarnings("unchecked")
    private static java.util.List<net.minecraft.world.damagesource.CombatEntry> combatEntries(ServerPlayer player)throws ReflectiveOperationException{
        var field=net.minecraft.world.damagesource.CombatTracker.class.getDeclaredField("entries");field.setAccessible(true);
        return (java.util.List<net.minecraft.world.damagesource.CombatEntry>)field.get(player.getCombatTracker());
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,ally;final Saved saved,allySaved;
        final net.minecraft.world.scores.PlayerTeam team,oldOwnerTeam,oldAllyTeam;
        final Vec3 at;final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());final Set<Long> forced=new HashSet<>();
        final Set<LbcWands.ElectroTrace> baselineTraces=Collections.newSetFromMap(new IdentityHashMap<>());
        final boolean offhand;int phase,ticks,warmTicks;long last=Long.MIN_VALUE;
        Cow mob,technical;ProbeCow genericVictim;LbcBolts.Chain thunder;float ownerHp,allyHp;
        static final class ProbeCow extends Cow {
            int arrowAttempts;boolean exactArrow;
            ProbeCow(net.minecraft.server.level.ServerLevel level){super(EntityTypes.COW,level);}
            @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){
                if(source.is(net.minecraft.world.damagesource.DamageTypes.ARROW)){arrowAttempts++;exactArrow|=amount==16;}
                return super.hurtServer(level,source,amount);
            }
        }
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));ally=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&owner!=null&&ally!=null,"two native match participants");requireCleanBaseline();saved=new Saved(game,owner);allySaved=new Saved(game,ally);baselineTraces.addAll(game.items().wands().electroSnapshot());
            oldOwnerTeam=owner.getTeam();oldAllyTeam=ally.getTeam();offhand=game.config().offhandWands;
            at=new Vec3(-240.5,260,-240.5);owner.level().getAllEntities().forEach(before::add);team=game.server().getScoreboard().addPlayerTeam("chain_fix_fixture");team.setAllowFriendlyFire(false);
        }
        void requireCleanBaseline(){
            require(!game.items().wands().timeStopped()&&!game.items().timeStop().stopped(owner.level()),"no preexisting dimension Time Stop to disturb");
            for(var player:List.of(owner,ally))require(!LbcGreed.active(player)&&!game.items().frost().isFrozen(player)&&game.canUseGameplayItems(player),"active unfrozen participant baseline");
            for(var player:List.of(owner,ally))require(!game.items().bolts().hasChain(player)&&game.items().bolts().statusCount(player)==0&&game.items().mana().hudValue(player,"effect_chain_lightning")==0,"participant baseline contains no Thunder/Raid124 status");
            var origin=new Vec3(-240.5,260,-240.5);
            require(game.items().lifetimes().loadedTargets().stream().noneMatch(target->target!=owner&&target!=ally&&game.items().wands().validTarget(target)&&target.position().distanceToSqr(origin)<14400),"no unrelated eligible recipient in fixture's connected chain region");
            try{for(var field:LbcIndirectKills.class.getDeclaredFields())if(Map.class.isAssignableFrom(field.getType())){
                field.setAccessible(true);var map=(Map<?,?>)field.get(game.items().indirectKills());
                for(var player:List.of(owner,ally))require(!map.containsKey(player),"clean participant attribution baseline: "+field.getName());
            }}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }
        @SuppressWarnings("unchecked")
        void retireNewTraces(){
            try{var field=LbcWands.class.getDeclaredField("electroTraces");field.setAccessible(true);
                ((Set<LbcWands.ElectroTrace>)field.get(game.items().wands())).removeIf(trace->!baselineTraces.contains(trace));
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }
        void init(){
            for(int x=-17;x<=-14;x++)for(int z=-17;z<=-14;z++){long key=net.minecraft.world.level.ChunkPos.pack(x,z);if(!owner.level().getForceLoadedChunks().contains(key)){forced.add(key);owner.level().setChunkForced(x,z,true);}owner.level().getChunk(x,z);}
            for(var player:List.of(owner,ally)){player.closeContainer();player.getInventory().clearContent();player.removeAllEffects();player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);player.entityTags().remove("sin_greed_active");player.entityTags().remove("time_stopped");player.setShiftKeyDown(false);player.getAbilities().invulnerable=false;player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);player.setHealth(1000);player.setAbsorptionAmount(0);game.items().mana().noMagic(player,0);game.items().mana().set(player,100000);game.server().getScoreboard().addPlayerToTeam(player.getScoreboardName(),team);}
            owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);ally.teleportTo(ally.level(),at.x+10,at.y,at.z+8,Set.of(),0,0,true);
            for(var player:List.of(owner,ally)){var profile=game.players().get(player.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);game.items().magic().inventoryChanged(player);game.items().mana().inventoryChanged(player);}game.items().magic().flushCleansing();game.config().offhandWands=false;
        }
        boolean warm(){require(++warmTicks<300,"resident fixture ticking");for(long key:forced)if(!owner.level().areEntitiesLoaded(key))return false;return true;}
        Cow cow(Vec3 pos){var cow=new Cow(EntityTypes.COW,owner.level());cow.setPos(pos);cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);owner.level().addFreshEntity(cow);return cow;}
        String damageDiagnostic(){
            var targets=game.items().lifetimes().loadedTargets();
            return " [ownerHP="+owner.getHealth()+"/before="+ownerHp+",allyHP="+ally.getHealth()+"/before="+allyHp
                    +",technicalHP="+technical.getHealth()+",genericHP="+genericVictim.getHealth()+",arrowAttempts="+genericVictim.arrowAttempts+",exact16="+genericVictim.exactArrow
                    +",status="+game.items().bolts().statusCount(mob)+",indexedOwner="+targets.contains(owner)+",indexedAlly="+targets.contains(ally)
                    +",ownerSource="+owner.getLastDamageSource()+",allySource="+ally.getLastDamageSource()+",technicalSource="+technical.getLastDamageSource()
                    +",ownerCredit="+owner.getKillCredit()+",allyCredit="+ally.getKillCredit()+",ownerInvuln="+owner.getInvulnerableTime()+",allyInvuln="+ally.getInvulnerableTime()
                    +",ownerAbsorption="+owner.getAbsorptionAmount()+",allyAbsorption="+ally.getAbsorptionAmount()+"]";
        }
        void managedCarrierBoundary(){
            require(game.items().bolts().count()==0,"isolated explicit pulse boundary has no unrelated bolt work to advance");
            var prior=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());prior.addAll(game.items().creatureDrops().snapshot());
            var pos=at.add(0,1,6);var token=new net.minecraft.world.entity.item.ItemEntity(owner.level(),pos.x,pos.y,pos.z,new ItemStack(LbcCreatureDropItem.Kind.OLD_VEX.item()));
            token.setThrower(ally);token.setNoGravity(true);token.setDeltaMovement(Vec3.ZERO);token.setPickUpDelay(1000);owner.level().addFreshEntity(token);
            require(game.items().specialDrops().spawn(token),"actual ally-thrown LowVex drop creates managed carriers");token.discard();
            var actors=game.items().creatureDrops().snapshot().stream().filter(actor->!prior.contains(actor)&&!actor.isRemoved()).toList();
            require(actors.size()==5&&actors.stream().allMatch(actor->actor instanceof net.minecraft.world.entity.monster.Vex&&game.items().creatureDrops().owner(actor)==ally),"actual five LowVex actors retain independent ally authorship");
            var carrier=(net.minecraft.world.entity.monster.Vex)actors.getFirst();for(var extra:actors)if(extra!=carrier)extra.discard();
            carrier.setNoAi(true);carrier.setNoGravity(true);carrier.setDeltaMovement(Vec3.ZERO);carrier.setPos(pos);
            require(!game.items().creatureDrops().canAffect(carrier,owner),"carrier's own attack policy would reject the allied spellcaster");
            for(var player:List.of(owner,ally)){player.setHealth(1000);player.setAbsorptionAmount(0);player.clearFire();player.setInvulnerableTime(0);player.damageCooldownTime=0;}
            float own=owner.getHealth(),other=ally.getHealth();
            require(carrier.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN,1,124),owner)&&game.items().bolts().statusCount(carrier)==1,"native Raid124 intake on actual managed LowVex");
            // One explicit service boundary supplements the earlier natural eighty-tick Thunder fixture.
            game.items().bolts().tick();
            require(owner.getHealth()<own&&ally.getHealth()<other,"managed carrier cannot impose its own friendship on WILD status: owner="+owner.getHealth()+"/"+own+",ally="+ally.getHealth()+"/"+other);
            for(var player:List.of(owner,ally)){var source=player.getLastDamageSource();require(source!=null&&source.is(net.minecraft.world.damagesource.DamageTypes.ARROW)&&source.getDirectEntity()==owner&&source.getEntity()==owner,"status native ARROW direct/cause are spellcaster, never the LowVex carrier: "+source);}
            game.items().bolts().clearEffects(carrier);game.items().indirectKills().clear(carrier);carrier.discard();
        }
        void cast(){
            mob=cow(at.add(0,1,8));game.server().getScoreboard().addPlayerToTeam(mob.getScoreboardName(),team);
            technical=cow(at.add(2,1,8));technical.entityTags().add("strict_map_object");
            var stack=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/academy_lightning_bolts_wand").getFirst();var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.putInt("mode",3);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
            require(owner.gameMode.useItem(owner,owner.level(),stack,InteractionHand.MAIN_HAND).consumesAction(),"real Thunder chain input");
            thunder=game.items().bolts().chainSnapshot().stream().filter(c->c.owner==owner&&c.current.contains(mob)).findFirst().orElseThrow();
            require(thunder.untilNext==40&&!thunder.current.contains(owner),"mob status seeds before natural40tick propagation");
        }
        boolean step(){
            long now=owner.level().getGameTime();if(now==last)return false;last=now;
            if(phase<=2){owner.setDeltaMovement(Vec3.ZERO);ally.setDeltaMovement(Vec3.ZERO);owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);ally.teleportTo(ally.level(),at.x+10,at.y,at.z+8,Set.of(),0,0,true);}
            if(phase==0){if(!warm())return false;cast();phase=1;return false;}
            if(phase==1){
                require(++ticks<=45,"Thunder natural tick transition deadline");
                if(thunder.current.contains(mob)){require(!thunder.current.contains(owner)&&thunder.untilNext>0,"no early Thunder jump");return false;}
                require(thunder.current.contains(owner)&&thunder.current.contains(ally)&&!thunder.current.contains(technical),"natural Thunder jump reaches caster/allied player and protects technical role");
                ticks=0;phase=2;return false;
            }
            if(phase==2){
                require(++ticks<=45,"Thunder caster native strike deadline");
                var bolts=owner.level().getEntitiesOfClass(LightningBolt.class,owner.getBoundingBox().inflate(1),bolt->bolt.getCause()==owner);
                if(bolts.isEmpty())return false;
                require(!thunder.current.contains(technical),"retained native bolt branch remains role guarded");
                game.items().bolts().clearEffects(owner);game.items().bolts().clearEffects(ally);game.items().bolts().clearEffects(mob);
                owner.level().getEntitiesOfClass(LightningBolt.class,owner.getBoundingBox().inflate(30),bolt->!before.contains(bolt)).forEach(Entity::discard);
                owner.setHealth(1000);ally.setHealth(1000);owner.clearFire();ally.clearFire();owner.setInvulnerableTime(0);owner.damageCooldownTime=0;ally.setInvulnerableTime(0);ally.damageCooldownTime=0;
                genericVictim=new ProbeCow(owner.level());genericVictim.setPos(at.add(5,1,5));genericVictim.setNoAi(true);genericVictim.setNoGravity(true);genericVictim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);genericVictim.setHealth(1000);owner.level().addFreshEntity(genericVictim);
                require(mob.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN,4,124),owner)&&game.items().bolts().statusCount(mob)==4&&!mob.hasEffect(MobEffects.RAID_OMEN),"native Raid124 intake transfers into real status counter");
                mob.entityTags().add("sin_greed_active");ticks=0;phase=3;return false;
            }
            if(phase==3){require(game.items().bolts().statusCount(mob)==4,"Greed pauses existing status counter");if(++ticks<2)return false;mob.entityTags().remove("sin_greed_active");game.items().timeStop().begin(owner.level(),200);ticks=0;phase=4;return false;}
            if(phase==4){require(game.items().bolts().statusCount(mob)==4,"Time Stop pauses actual status tick");if(++ticks<2)return false;game.items().timeStop().end(owner.level());ownerHp=owner.getHealth();allyHp=ally.getHealth();ticks=0;phase=5;return false;}
            if(phase==5){
                require(++ticks<=12,"generic counter drains on real active ticks");if(game.items().bolts().statusCount(mob)>0)return false;
                require(owner.getHealth()<ownerHp&&ally.getHealth()<allyHp&&technical.getHealth()==1000,"generic status reaches caster/allied player with source arrow damage and role guard"+damageDiagnostic());
                require(genericVictim.exactArrow&&genericVictim.arrowAttempts==4,"generic124 attempts native16 arrow damage once per active counter tick"+damageDiagnostic());
                var payload=new ItemStack(net.minecraft.world.item.Items.TIPPED_ARROW);payload.set(DataComponents.POTION_CONTENTS,new net.minecraft.world.item.alchemy.PotionContents(Optional.empty(),Optional.empty(),List.of(new MobEffectInstance(MobEffects.RAID_OMEN,16,124)),Optional.empty()));
                var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(owner.level(),owner,payload,null);arrow.setPos(mob.getX(),mob.getY()+.6,mob.getZ()-1.2);arrow.setDeltaMovement(0,0,2);arrow.setNoGravity(true);mob.setInvulnerableTime(0);mob.damageCooldownTime=0;owner.level().addFreshEntity(arrow);arrow.tick();
                require(game.items().bolts().statusCount(mob)>0,"real tipped-arrow impact ingests Raid124 transport after native duration scaling");arrow.discard();game.items().bolts().clearEffects(mob);
                require(owner.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN,3,124),owner)&&game.items().mana().appendHudManaStatuses(owner,net.minecraft.network.chat.Component.empty()).getString().contains("\uE156"),"actual player pill/status intake and source HUD glyph");
                game.items().bolts().clearEffects(owner);require(game.items().bolts().statusCount(owner)==0&&game.items().mana().hudValue(owner,"effect_chain_lightning")==0,"shared status cleanse clears native counter");managedCarrierBoundary();return true;
            }
            return false;
        }
        public void close(){
            game.items().timeStop().end(owner.level());if(mob!=null){mob.entityTags().remove("sin_greed_active");game.items().bolts().clearEffects(mob);}game.items().bolts().clearEffects(owner);game.items().bolts().clearEffects(ally);
            var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(entity->{if(!before.contains(entity)&&entity.position().distanceToSqr(at)<40000)fresh.add(entity);});for(var entity:fresh){game.server().getScoreboard().removePlayerFromTeam(entity.getScoreboardName());if(entity instanceof LivingEntity living){game.items().bolts().clearEffects(living);game.items().indirectKills().clear(living);}entity.discard();}
            retireNewTraces();
            var board=game.server().getScoreboard();board.removePlayerTeam(team);if(oldOwnerTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldOwnerTeam);if(oldAllyTeam!=null)board.addPlayerToTeam(ally.getScoreboardName(),oldAllyTeam);saved.close();allySaved.close();
            game.config().offhandWands=offhand;for(long key:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");var fixture=new Fixture(game);running=fixture;try{fixture.init();}catch(RuntimeException|Error error){running=null;fixture.close();throw error;}}
    public static void tick(GameController game){if(running==null)return;var fixture=running;try{if(fixture.step()){running=null;fixture.close();SkyWars2.LOGGER.info("[check] PASS chain-lightning-fixes");}}catch(Throwable error){running=null;try{fixture.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL chain-lightning-fixes",error);}}
}
