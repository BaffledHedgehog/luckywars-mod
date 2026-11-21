package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Real shared101 explosions and actual TNT-block children; only the native scheduler advances fuse/world state. */
public final class SharedArrowExplosionChecks {
    private SharedArrowExplosionChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Shared101 explosions: "+why);}
    private static Object member(Object object,String name){for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(object);}catch(NoSuchFieldException ignored){}catch(IllegalAccessException error){throw new IllegalStateException(error);}throw new IllegalStateException("missing fixture field "+name);}
    @SuppressWarnings("unchecked") private static List<Packet<?>> packets(RegenerationBeltChecks.ScheduledClient client){return (List<Packet<?>>)member(member(client,"connection"),"packets");}
    private static boolean pushed(RegenerationBeltChecks.ScheduledClient client){return packets(client).stream().filter(ClientboundExplodePacket.class::isInstance).map(ClientboundExplodePacket.class::cast).anyMatch(packet->packet.playerKnockback().map(vector->vector.lengthSqr()>1e-10).orElse(false));}
    private static Trial running;
    private static final class Trial implements AutoCloseable {
        final GameController game;final ServerPlayer owner,ally;final ServerLevel level;final LbcBabylonEffects service;
        final RegenerationBeltChecks.Snapshot saved,allySaved;final RegenerationBeltChecks.ScheduledClient client,audience;
        final SauvojenMeditationChecks.Attribution attribution;final Vec3 at;final String epoch;final int protection;
        final boolean mobGrief,tntExplodes,mobDrops;final Set<String> tags,allyTags;final net.minecraft.world.scores.PlayerTeam oldTeam,oldAllyTeam,team;
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());final List<Cow> cows=new ArrayList<>();final List<PrimedTnt> children=new ArrayList<>();
        LbcIndirectKills.TerrainProvenance provenance;Cow target;Cow fangWitness;net.minecraft.world.entity.projectile.EvokerFangs observerFang;PrimedTnt child;LbcIndirectKills.Owner initialCause;int kind,updates,lastFuse,elapsed;float ownerHealth,allyHealth;boolean closed;
        Trial(GameController game){this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));ally=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&ally!=null&&game.playing()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(ally),"two actual native active participants");
            level=game.maps().level();service=game.items().babylonEffects();require(service.pendingCount()==0,"exclusive empty shared contribution service");
            for(var entity:level.getAllEntities()){require(!service.managedSecondary(entity),"no ambient shared secondary actor");before.add(entity);}
            saved=new RegenerationBeltChecks.Snapshot(game,owner);allySaved=new RegenerationBeltChecks.Snapshot(game,ally);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,ally));
            tags=new HashSet<>(owner.entityTags());allyTags=new HashSet<>(ally.entityTags());epoch=game.items().epoch();at=new Vec3(owner.getX(),Math.min(240,level.getMaxY()-24),owner.getZ());
            protection=game.config().tntProtection;mobGrief=level.getGameRules().get(GameRules.MOB_GRIEFING);tntExplodes=level.getGameRules().get(GameRules.TNT_EXPLODES);mobDrops=level.getGameRules().get(GameRules.MOB_DROPS);
            var board=game.server().getScoreboard();oldTeam=owner.getTeam();oldAllyTeam=ally.getTeam();require(board.getPlayerTeam("sw2_shared_101_guard")==null,"exclusive native friendship fixture team");team=board.addPlayerTeam("sw2_shared_101_guard");team.setAllowFriendlyFire(false);board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(ally.getScoreboardName(),team);
            client=new RegenerationBeltChecks.ScheduledClient(game,owner);audience=new RegenerationBeltChecks.ScheduledClient(game,ally);
        }
        void initialize(){
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(at.add(-9,-9,-9)),BlockPos.containing(at.add(9,9,9)))){
                require(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null&&level.getBlockState(pos).isAir(),"resident isolated native blast volume");terrain.put(pos.immutable(),game.maps().snapshot(level,pos));
            }
            provenance=game.items().indirectKills().captureTerrain(level,terrain.keySet());game.config().tntProtection=0;
            level.getGameRules().set(GameRules.MOB_GRIEFING,true,game.server());level.getGameRules().set(GameRules.TNT_EXPLODES,true,game.server());level.getGameRules().set(GameRules.MOB_DROPS,false,game.server());
            for(var player:List.of(owner,ally)){
                player.getInventory().clearContent();player.removeAllEffects();player.stopUsingItem();player.setGameMode(GameType.SURVIVAL);player.getAbilities().invulnerable=false;player.setNoGravity(true);player.setSprinting(false);player.setShiftKeyDown(false);
                for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))player.setItemSlot(slot,ItemStack.EMPTY);
                for(var type:List.of(Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS,Attributes.EXPLOSION_KNOCKBACK_RESISTANCE,Attributes.KNOCKBACK_RESISTANCE)){var attr=player.getAttribute(type);attr.removeModifiers();attr.setBaseValue(0);}
                player.getAttribute(Attributes.MAX_HEALTH).removeModifiers();player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
                for(var tag:List.of("sin_greed_active","nomagic_active","time_stopped","spectator","strict_map_object","magic_academy","rcdenyalways"))player.entityTags().remove(tag);
                var profile=game.players().get(player.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);profile.lbcChallenge=0;
                game.items().magic().inventoryChanged(player);game.items().magic().flushCleansing();
            }
            immediateControls();startChild(0);
        }
        LbcIndirectKills.Owner cause(boolean wild){var cause=game.items().indirectKills().coldCause(owner,true);return new LbcIndirectKills.Owner(cause.player(),cause.order(),wild,true);}
        void reset(ServerPlayer player,Vec3 point){player.removeAllEffects();player.getAttribute(Attributes.MAX_HEALTH).removeModifiers();player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);player.setHealth(1000);player.setAbsorptionAmount(0);player.setInvulnerableTime(0);player.damageCooldownTime=0;
            player.teleportTo(level,point.x,point.y,point.z,Set.of(),0,0,true);player.setDeltaMovement(Vec3.ZERO);player.setKnownMovement(Vec3.ZERO);player.setNoGravity(true);player.setOnGround(true);player.getFoodData().setFoodLevel(20);player.getFoodData().setSaturation(5);}
        void positions(Vec3 center){reset(owner,center.add(-.65,-.8,0));reset(ally,center.add(.65,-.8,0));client.acknowledge();audience.acknowledge();window();}
        void window(){packets(client).clear();packets(audience).clear();}
        Cow cow(Vec3 point,int health){var cow=new Cow(EntityTypes.COW,level);cow.setPos(point);cow.setNoAi(true);cow.setNoGravity(true);cow.noPhysics=true;cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(health);require(level.addFreshEntity(cow),"native resident enemy witness");cows.add(cow);return cow;}
        void blast(Vec3 point,LbcIndirectKills.Owner cause){service.quiverExplosionAt(level,point,cause);}
        void immediateControls(){
            var center=at.add(0,.8,0);positions(center);var cow=cow(center.add(0,-.8,.65),1000);blast(center,cause(false));
            require(owner.getHealth()<1000&&pushed(client),"real cold101 native self HP and impulse admitted on FF-disabled team");
            require(ally.getHealth()==1000&&!pushed(audience)&&ally.getDeltaMovement().lengthSqr()<1e-10,"real cold101 protects other allied player HP and impulse");
            require(cow.getHealth()<1000&&cow.getLastHurtByPlayer()==owner,"real cold101 enemy damage carries original author");cow.discard();
            positions(center);blast(center,cause(true));require(owner.getHealth()<1000&&ally.getHealth()<1000&&pushed(client)&&pushed(audience),"real WILD101 retains self and allied-player HP/impulse");
            positions(center);var offline=game.items().indirectKills().restoreColdCause(UUID.fromString("00000000-0000-4000-8000-000000000101"),0,true);require(game.server().getPlayerList().getPlayer(offline.player())==null,"actual source lookup is offline");
            blast(center,offline);require(owner.getHealth()==1000&&ally.getHealth()==1000&&!pushed(client)&&!pushed(audience),"actual offline cold101 producer fails closed for HP and impulse");
            positions(center);ally.entityTags().add("sin_greed_active");try{blast(center,cause(true));require(owner.getHealth()<1000&&ally.getHealth()==1000&&!pushed(audience),"WILD positive control retains absolute Greed protection");}finally{ally.entityTags().remove("sin_greed_active");}
            positions(center);var protectedCow=cow(center.add(0,-.8,.5),1000);protectedCow.entityTags().add("strict_map_object");blast(center,cause(true));require(protectedCow.getHealth()==1000&&protectedCow.getDeltaMovement().lengthSqr()==0,"native blast keeps protected technical role untouched");protectedCow.discard();
            // A current counter pulse has no direct Entity; its original online observer must still author enemy damage.
            positions(center);var observerCow=cow(center.add(0,-.8,.5),1000);game.spectators().enter(owner);
            try{blast(center,cause(false));require(observerCow.getHealth()<1000&&observerCow.getLastHurtByPlayer()==owner,"current trusted null-source producer preserves observer author's accepted damage");require(ally.getHealth()==1000&&!pushed(audience),"observer cold producer keeps other ally protected");}
            finally{game.spectators().remove(owner);owner.setGameMode(GameType.SURVIVAL);owner.setNoGravity(true);observerCow.discard();}
            for(var entity:level.getAllEntities())require(before.contains(entity)||entity.getType()!=EntityTypes.CREEPER,"shared producer publishes no creeper proxy actor");
        }
        List<PrimedTnt> liveSecondary(){var result=new ArrayList<PrimedTnt>();for(var entity:level.getAllEntities())if(entity instanceof PrimedTnt tnt&&service.managedSecondary(tnt)&&!tnt.isRemoved())result.add(tnt);return result;}
        void startChild(int mode){
            kind=mode;var base=BlockPos.containing(at);for(var pos:BlockPos.betweenClosed(base.offset(-3,-1,-3),base.offset(3,-1,3)))level.setBlock(pos,Blocks.BEDROCK.defaultBlockState(),818);
            var block=base;level.setBlock(block,Blocks.TNT.defaultBlockState(),818);positions(at.add(0,.8,0));initialCause=cause(mode==2);
            require(liveSecondary().isEmpty(),"previous native secondary completed");blast(Vec3.atCenterOf(block),initialCause);var produced=liveSecondary();require(produced.size()==1,"real radius1 explosion ignites one actual native TNT block");child=produced.getFirst();children.add(child);
            require(child.getClass()==PrimedTnt.class&&child.getOwner()==owner&&service.owner(child)==owner&&game.items().currentEpoch(child),"native child has native author and current epoch before publication");
            var savedCause=service.cause(child);require(savedCause.player().equals(owner.getUUID())&&savedCause.friendlyFire()==(mode==2),"native child preserves original cold/WILD cause");
            require(child.getFuse()>=10&&child.getFuse()<=29,"native short fuse, not synthetic immediate child");lastFuse=child.getFuse();elapsed=0;
            if(mode==3){service.stop();require(child.isRemoved()&&!service.managedSecondary(child)&&liveSecondary().isEmpty(),"shared stop discards native child and clears authority");return;}
            if(mode==0){game.items().electro().assignOwner(child,ally);require(service.owner(child)==ally&&service.cause(child).player().equals(ally.getUUID())&&!service.cause(child).friendlyFire(),"production native reflection authority supersedes original native owner while retaining cold policy");}
            positions(child.position().add(0,.8,0));ownerHealth=owner.getHealth();allyHealth=ally.getHealth();
            if(mode==1){game.spectators().enter(owner);owner.setNoGravity(true);client.acknowledge();}
            target=cow(child.position().add(0,0,.65),mode==2?1000:1);if(mode==1){fangWitness=cow(child.position().add(0,0,20),1000);observerFang=game.items().evokerBass().spawnQuiverOwned(owner,fangWitness.position());require(observerFang!=null&&game.isObserver(owner),"native deferred Quiver fang retains its observer author");}window();updates=client.updates();
        }
        boolean step(){client.acknowledge();audience.acknowledge();if(client.updates()==updates)return false;require(client.updates()-updates==1,"one actual registered player clock per native child observation");updates=client.updates();require(game.playing()&&game.items().currentEpoch(epoch),"active current fixture epoch");elapsed++;
            if(!child.isRemoved()){
                require(child.getFuse()==lastFuse-1,"native TNT fuse advances exactly once per real server tick");lastFuse=child.getFuse();require(elapsed<35,"native short-fuse child terminates without a manual tick");
                // Keep the isolated enemy inside the live moving child's blast; do not move/tick the TNT itself.
                target.setPos(child.position().add(0,0,.65));owner.setDeltaMovement(Vec3.ZERO);ally.setDeltaMovement(Vec3.ZERO);owner.setKnownMovement(Vec3.ZERO);ally.setKnownMovement(Vec3.ZERO);return false;
            }
            require(lastFuse==1&&liveSecondary().isEmpty(),"actual native zero fuse discards/explodes its child");
            var expectedOwner=kind==0?ally:owner;
            // Electro retires its removed-entity reflection at end tick. Read the saved terminal cause without re-resolving the original native TNT owner.
            var terminalCause=((Map<?,?>)member(service,"secondary")).get(child);
            require(terminalCause instanceof LbcIndirectKills.Owner savedCause&&savedCause.player().equals(expectedOwner.getUUID()),"discard-before-explode saved terminal cause mode="+kind+" expected="+expectedOwner.getUUID()+" actual="+terminalCause+" nativeOwner="+child.getOwner()+" reflected="+game.items().electro().reflectedOwner(child)+" ownerHP="+owner.getHealth()+" allyHP="+ally.getHealth()+" targetHP="+target.getHealth()+" targetCredit="+target.getKillCredit());
            if(kind==0){require(owner.getHealth()==ownerHealth&&!pushed(client),"reflected cold child protects original owner as other ally, HP+impulse");require(ally.getHealth()<allyHealth&&pushed(audience),"reflected current owner keeps self HP and native impulse on own FF-disabled team");}
            if(kind==1){require(ally.getHealth()==allyHealth&&!pushed(audience),"observer-author cold child still protects other ally HP+impulse");require(fangWitness.getHealth()==994&&fangWitness.getLastHurtByPlayer()==owner&&fangWitness.getLastDamageSource().getDirectEntity()==observerFang,"actual native observer-author deferred Quiver fang accepted exactly6HP and retained its source");game.spectators().remove(owner);owner.setGameMode(GameType.SURVIVAL);owner.setNoGravity(true);}
            if(kind==2)require(owner.getHealth()<ownerHealth&&ally.getHealth()<allyHealth&&pushed(client)&&pushed(audience),"WILD child keeps original self/other-allied HP+impulse");
            if(kind<2)require(target.isDeadOrDying()&&target.getLastHurtByPlayer()==expectedOwner&&target.getKillCredit()==expectedOwner,"actual native child explosion kills and credits its current/observer original author");
            target.discard();game.items().indirectKills().clear(target);
            if(kind<2){startChild(kind+1);return false;}
            startChild(3);return true;
        }
        @Override public void close(){if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(()->{game.spectators().remove(owner);owner.setGameMode(GameType.SURVIVAL);service.stop();if(observerFang!=null)game.items().evokerBass().unloaded(observerFang);},()->{for(var child:children){child.discard();game.items().electro().reflectedOwners.remove(child);}for(var cow:cows){game.items().indirectKills().clear(cow);cow.discard();}},
                ()->{for(var entry:terrain.entrySet())game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());if(provenance!=null)game.items().indirectKills().restoreTerrain(provenance);},
                ()->{for(var entity:level.getAllEntities())if(!before.contains(entity)&&entity instanceof ItemEntity&&entity.position().distanceToSqr(at)<900)entity.discard();},
                ()->{level.getGameRules().set(GameRules.MOB_GRIEFING,mobGrief,game.server());level.getGameRules().set(GameRules.TNT_EXPLODES,tntExplodes,game.server());level.getGameRules().set(GameRules.MOB_DROPS,mobDrops,game.server());game.config().tntProtection=protection;},
                ()->{var board=game.server().getScoreboard();if(owner.getTeam()!=null)board.removePlayerFromTeam(owner.getScoreboardName(),owner.getTeam());if(ally.getTeam()!=null)board.removePlayerFromTeam(ally.getScoreboardName(),ally.getTeam());if(oldTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldTeam);if(oldAllyTeam!=null)board.addPlayerToTeam(ally.getScoreboardName(),oldAllyTeam);board.removePlayerTeam(team);},
                ()->{client.close();audience.close();},()->saved.restore(game),()->allySaved.restore(game),()->{saved.restoreAttributes();allySaved.restoreAttributes();saved.restoreUse();allySaved.restoreUse();owner.entityTags().clear();owner.entityTags().addAll(tags);ally.entityTags().clear();ally.entityTags().addAll(allyTags);},attribution::close,()->game.players().save()))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("shared native explosion scoped restoration",failure);
        }
    }
    public static void run(GameController game){require(running==null,"exclusive shared native explosion trial");var trial=new Trial(game);try{trial.initialize();running=trial;SkyWars2.LOGGER.info("[check] shared-arrow-explosions running (native child fuse scheduler)");}catch(Throwable error){try{trial.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS shared-arrow-explosions (actual101 common native cold/WILD HP+impulse; self/other allies/Greed/roles/offline/observer; actual TNT-block native children/short fuse/reflection/epoch/killcredit/discard/stop; scoped restore)");}}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL shared-arrow-explosions",error);}}
}
