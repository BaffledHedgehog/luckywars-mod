package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.WandProjectileMixin.ArrowAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/** Scoped scheduled native-arrow qualification; production movement/collision is never simulated here. */
public final class HomingArrowChecks {
    private HomingArrowChecks(){}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("IMBA homing arrow: "+why);}
    public static void run(GameController game){start(game,false);}
    public static void runRewind(GameController game){start(game,true);}
    private static void start(GameController game,boolean rewind){require(running==null,"exclusive fixture");var f=new Fixture(game,rewind);running=f;
        try{f.begin();}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){if(running==null)return;var f=running;
        try{require(f.game==game,"same native controller");if(!f.step())return;running=null;f.close();
            SkyWars2.LOGGER.info("[check] PASS {} (native ammo/component conversion, scheduled nearest-eye steering, MAG/TECH, TimeStop, canonical x2*x4 and typed native replay)",f.rewind?"imba-homing-arrow-rewind":"imba-homing-arrow");
        }catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}
            SkyWars2.LOGGER.error("[check] FAIL "+(f.rewind?"imba-homing-arrow-rewind":"imba-homing-arrow"),error);}}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final boolean rewind;final ServerPlayer owner,other;final ServerLevel level;final LbcHomingArrows service;
        final MatchCoreCheckpoint core;final RegenerationBeltChecks.Snapshot saved,otherSaved;
        final RegenerationBeltChecks.ScheduledClient client,audience;final SauvojenMeditationChecks.Attribution attribution;
        final MatchRandomState random,otherRandom,worldRandom;final boolean frozen,regen;final Vec3 home;final int began;
        final Set<UUID> owned=new LinkedHashSet<>();final List<Marker> fields=new ArrayList<>();
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final Map<BlockPos,LbcIndirectKills.TerrainProvenance> provenance=new LinkedHashMap<>();
        ItemStack prototype,expected,weapon;Arrow arrow;Cow near,far,protectedMob;LbcHomingArrows.Flight held;
        Vec3 heldPosition,heldMotion,continuationPosition,continuationMotion;int heldTicks,phase,when,continuationAge;boolean stopped,restoring,closed;
        Fixture(GameController game,boolean rewind){this.game=game;this.rewind=rewind;
            owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            other=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));level=owner.level();service=game.items().homingArrows();
            require(game.playing()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other)&&game.allowDamage(other)&&other.level()==level&&!game.balanceLegacy(),"two active ordinary native participants");
            require(!game.maps().busy()&&!game.vanity().busy()&&!game.vanity().hasSnapshot()&&!game.items().timeStop().stopped(level)&&service.count()==0,"exclusive stable native projectile/coordinator scene");
            core=new MatchCoreCheckpoint(game);saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);
            attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,other));random=MatchRandomState.capture(owner.getRandom());otherRandom=MatchRandomState.capture(other.getRandom());worldRandom=MatchRandomState.capture(level.getRandom());
            frozen=game.server().tickRateManager().isFrozen();regen=level.getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION);home=owner.position().add(0,18,0);began=game.server().getTickCount();
            client=new RegenerationBeltChecks.ScheduledClient(game,owner);audience=new RegenerationBeltChecks.ScheduledClient(game,other);
        }
        void next(int p){phase=p;when=game.server().getTickCount();}
        void own(Entity e){owned.add(e.getUUID());}
        void discard(Entity e){game.items().entityUnloaded(e);e.discard();}
        void move(ServerPlayer p,Vec3 at){require(p.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true),"actual scoped native teleport");p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);p.setOnGround(true);(p==owner?client:audience).acknowledge();}
        void remember(BlockPos p){require(level.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)!=null,"resident fixture cell "+p);
            if(!terrain.containsKey(p)){terrain.put(p.immutable(),game.maps().snapshot(level,p));provenance.put(p.immutable(),game.items().indirectKills().captureTerrain(level,List.of(p.immutable())));}}
        void pause(){game.items().timeStop().begin(level,10000);stopped=true;}
        void thaw(){game.items().timeStop().end(level);stopped=false;}
        Cow cow(Vec3 at){var c=new Cow(EntityTypes.COW,level);c.setPos(at);c.setNoAi(true);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);own(c);require(level.addFreshEntity(c),"native living target publication");return c;}
        void setup(){game.server().tickRateManager().setFrozen(false);game.config().noBorders=true;game.config().lbcSkills=false;game.config().lbcLevelups=false;
            level.getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION,false,game.server());
            for(var p:List.of(owner,other)){p.stopUsingItem();p.removeAllEffects();p.getInventory().clearContent();p.getAbilities().invulnerable=false;
                p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);p.setInvulnerableTime(0);p.damageCooldownTime=0;
                p.entityTags().remove("strict_map_object");game.server().getScoreboard().removePlayerFromTeam(p.getScoreboardName());game.items().mana().noMagic(p,0);game.items().magic().inventoryChanged(p);}
            game.items().magic().flushCleansing();move(owner,home.add(0,0,-12));move(other,home.add(12,0,0));
            for(var p:BlockPos.betweenClosed(BlockPos.containing(home).offset(-3,-2,-3),BlockPos.containing(home).offset(3,4,3))){remember(p);level.setBlock(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2|16|32);}
            near=cow(home.add(-6,0,0));far=cow(home.add(9,0,0));protectedMob=cow(home.add(1,0,0));protectedMob.entityTags().add("strict_map_object");
        }
        void prototype(){var loot=game.lbc().rollTable(level,home,owner,"lbc:imba/alotofarrow");require(loot.stream().mapToInt(ItemStack::getCount).sum()==320,"authored five64 source stacks");
            require(loot.stream().allMatch(s->s.is(LbcImbaNativeItems.HOMING_ARROW)&&s.getCount()==64&&LbcHomingArrows.matches(s)
                    &&Identifier.parse("lbc:tier_ss").equals(s.get(DataComponents.TOOLTIP_STYLE))),"canonical native homing ammo/tierSS");
            prototype=loot.getFirst().copyWithCount(3);prototype.set(DataComponents.CUSTOM_NAME,Component.literal("homing metadata witness"));
            var data=prototype.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.putString("fixture","preserve-all-ammo-metadata");prototype.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            prototype.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.of(LbcHomingArrows.SOURCE_COLOR),List.of(new MobEffectInstance(MobEffects.POISON,91,2)),Optional.of("fixture potion name")));
            expected=prototype.copyWithCount(1);var old=expected.get(DataComponents.POTION_CONTENTS);expected.set(DataComponents.POTION_CONTENTS,new PotionContents(old.potion(),Optional.of(LbcHomingArrows.ACTIVE_COLOR),old.customEffects(),old.customName()));
            weapon=new ItemStack(Items.BOW);weapon.set(DataComponents.CUSTOM_NAME,Component.literal("original native bow snapshot"));
        }
        Arrow spawn(){var body=new Arrow(level,owner,prototype.copyWithCount(1),weapon.copy());body.setPos(home);body.setDeltaMovement(Vec3.ZERO);body.setBaseDamage(7.25);body.setCritArrow(true);
            ((ArrowAccess)body).skywars2$setPierceLevel((byte)3);body.entityTags().add("fixture_homing_metadata");own(body);require(level.addFreshEntity(body),"actual original native arrow publication");
            require(service.state(body)!=null&&service.state(body).age()==0&&body.isNoGravity(),"source color activates typed state/noGravity on native load");
            require(ItemStack.matches(body.getPickupItemStackOrigin(),expected)&&ItemStack.matches(body.getWeaponItem(),weapon)&&body.getOwner()==owner&&body.isCritArrow()&&((me.skywars2.mixin.ShulkerBowMixin.ArrowAccess)body).skywars2$baseDamage()==7.25&&body.getPierceLevel()==3&&body.entityTags().contains("fixture_homing_metadata"),"color-only conversion preserves exact ammo/effects/name/data/native owner/weapon/crit/pierce/tags");return body;}
        void field(boolean tech,Vec3 at){var marker=new Marker(EntityTypes.MARKER,level);marker.setPos(at);remember(marker.blockPosition());remember(marker.blockPosition().above());marker.entityTags().add(tech?"stopper":"stopper_magic");own(marker);fields.add(marker);require(level.addFreshEntity(marker),"native suppression control");game.items().magic().loaded(marker);}
        void removeFields(){for(var marker:fields){game.items().magic().unloaded(marker);marker.discard();for(var p:List.of(marker.blockPosition(),marker.blockPosition().above())){var state=terrain.get(p);if(state!=null){game.maps().restoreSnapshot(level,p,state,2);game.items().magic().blockChanged(p,level.getBlockState(p));}}}fields.clear();}
        void begin(){setup();prototype();field(false,home);require(game.items().magic().suppressedAt(level,home)&&!game.items().magic().mechanismSuppressedAt(level,home),"positive MAG-only control");arrow=spawn();next(0);}
        void hold(){held=service.state(arrow);heldPosition=arrow.position();heldTicks=arrow.tickCount;pause();heldMotion=arrow.getDeltaMovement();}
        void verifyHeld(){require(!arrow.isRemoved()&&service.state(arrow).equals(held)&&arrow.position().equals(heldPosition)&&arrow.getDeltaMovement().equals(heldMotion)&&arrow.tickCount==heldTicks,"TimeStop holds exact native pose/motion/tick and typed age/RNG");}
        void clones(){require(game.items().cloneSkills().copyNow(arrow,15),"canonical native x2 accepts homing arrow");var doubled=service.actors().stream().filter(e->e!=arrow).findFirst().orElseThrow();
            require(game.items().cloneSkills().copyNow(arrow,17)&&game.items().cloneSkills().copyNow(doubled,17)&&service.count()==8,"canonical x2*x4 produces eight original-arrow carriers");
            var streams=new HashSet<Long>();for(var body:service.actors()){own(body);var state=service.state(body);streams.add(state.rng());require(state.age()==held.age()&&body.getOwner()==owner&&body.isNoGravity()&&ItemStack.matches(body.getPickupItemStackOrigin(),expected),"native clones preserve partial typed age/owner/ammo/noGravity");
                require(!game.items().cloneSkills().copyNow(body,15)&&!game.items().cloneSkills().copyNow(body,17),"same-variant clone recursion closed");}
            require(streams.size()==8,"native cloned arrows have independent future bounce streams");for(var body:List.copyOf(service.actors()))if(body!=arrow)discard(body);
        }
        boolean step(){client.acknowledge();audience.acknowledge();require(game.server().getTickCount()-began<900,"bounded scheduled fixture phase="+phase);
            if(restoring&&game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            int elapsed=game.server().getTickCount()-when;
            if(phase==0){if(elapsed<3)return false;require(!arrow.isRemoved()&&service.state(arrow).age()>=3&&service.state(arrow).age()==arrow.tickCount&&arrow.position().x<home.x&&arrow.getDeltaMovement().x<0&&arrow.getDeltaMovement().y>0,"actual native ticks steer toward nearest unprotected mob eyes under MAG");
                require(ItemStack.matches(arrow.getPickupItemStackOrigin(),expected),"scheduled flight retains exact original ammo metadata");removeFields();arrow.setPos(home);arrow.setDeltaMovement(Vec3.ZERO);near.setPos(home.add(-9,0,0));far.setPos(home.add(4,0,0));next(3);return false;}
            if(phase==3){if(elapsed<2)return false;require(!arrow.isRemoved()&&arrow.getDeltaMovement().x>0&&arrow.position().x>home.x,"native nearest selection switches immediately when another living target becomes closer than cached target");hold();next(1);return false;}
            if(phase==1){verifyHeld();if(elapsed<3)return false;thaw();clones();if(rewind){hold();require(game.vanity().requestCapture(owner,false),"whole coordinator accepts partial frozen native homing flight");next(10);return false;}field(true,arrow.position());require(game.items().magic().mechanismSuppressedAt(level,arrow.position()),"positive actual TECH control");next(2);return false;}
            if(phase==2){if(elapsed<2)return false;require(arrow.isRemoved()&&service.state(arrow)==null,"TECH removes original homing physical arrow");removeFields();return true;}
            if(phase==10){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;verifyHeld();thaw();next(11);return false;}
            if(phase==11){if(elapsed<3)return false;require(!arrow.isRemoved()&&service.state(arrow).age()==held.age()+elapsed,"partial native homing cursor advances once per active tick");continuationAge=service.state(arrow).age();continuationPosition=arrow.position();continuationMotion=arrow.getDeltaMovement();pause();discard(arrow);require(game.vanity().requestRestoreForCheck(),"whole restore accepts discarded original native arrow");restoring=true;next(12);return false;}
            if(phase==12){if(game.vanity().busy())return false;require(level.getEntity(arrow.getUUID())==arrow&&!arrow.isRemoved(),"coordinator restores SAME native arrow instance/UUID");verifyHeld();require(ItemStack.matches(arrow.getPickupItemStackOrigin(),expected)&&arrow.getOwner()==owner&&game.items().timeStop().stopped(level),"whole replay restores original ammo/owner and native TimeStop");game.vanity().stop();thaw();next(13);return false;}
            if(phase==13){if(service.state(arrow).age()<continuationAge)return false;require(service.state(arrow).age()==continuationAge&&arrow.position().equals(continuationPosition)&&arrow.getDeltaMovement().equals(continuationMotion),"replayed partial native age reaches identical physical pose and velocity");field(true,arrow.position());next(2);return false;}
            return false;
        }
        public void close(){if(closed)return;closed=true;Throwable failure=null;game.server().tickRateManager().setFrozen(true);
            for(Runnable action:List.<Runnable>of(()->{if(rewind)game.vanity().stop();removeFields();if(stopped)thaw();owner.stopUsingItem();},
                    ()->{for(var id:List.copyOf(owned)){var e=level.getEntity(id);if(e!=null)discard(e);}},
                    ()->{terrain.forEach((p,s)->game.maps().restoreSnapshot(level,p,s,2|16|32));provenance.values().forEach(game.items().indirectKills()::restoreTerrain);},
                    ()->{saved.restoreAttributes();otherSaved.restoreAttributes();saved.restore(game);otherSaved.restore(game);saved.restoreUse();otherSaved.restoreUse();core.restoreFrozen();random.restore(owner.getRandom());otherRandom.restore(other.getRandom());worldRandom.restore(level.getRandom());level.getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION,regen,game.server());game.players().save();},
                    client::close,audience::close,attribution::close,()->game.server().tickRateManager().setFrozen(frozen)))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("IMBA homing scoped cleanup",failure);
        }
    }
}
