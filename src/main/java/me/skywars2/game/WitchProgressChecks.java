package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.phys.Vec3;

/** Native award/reset/mining/melee paths, without polling stat totals or advancing private clocks. */
public final class WitchProgressChecks {
    private WitchProgressChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Witch progression: "+why);}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<1e-5,why+" actual="+actual+" expected="+expected);}
    private record Score(int value,boolean locked,Component display,net.minecraft.network.chat.numbers.NumberFormat format){}
    private static final class ObjectiveState {
        final Scoreboard board;final String name;final Objective original;
        final Map<String,Score> scores=new LinkedHashMap<>();final Set<DisplaySlot> slots=EnumSet.noneOf(DisplaySlot.class);
        ObjectiveState(Scoreboard board,String name){this.board=board;this.name=name;original=board.getObjective(name);
            if(original!=null){for(var holder:board.getTrackedPlayers()){
                var info=board.getPlayerScoreInfo(holder,original);if(info==null)continue;
                var access=board.getOrCreatePlayerScore(holder,original,true);
                scores.put(holder.getScoreboardName(),new Score(info.value(),info.isLocked(),access.display(),info.numberFormat()));
            }for(var slot:DisplaySlot.values())if(board.getDisplayObjective(slot)==original)slots.add(slot);}
        }
        void remove(){var current=board.getObjective(name);if(current!=null)board.removeObjective(current);}
        void replace(ObjectiveCriteria criterion){remove();board.addObjective(name,criterion,Component.literal(name),ObjectiveCriteria.RenderType.INTEGER,false,null);}
        void restore(){remove();if(original==null)return;
            var next=board.addObjective(name,original.getCriteria(),original.getDisplayName(),original.getRenderType(),original.displayAutoUpdate(),original.numberFormat());
            for(var entry:scores.entrySet()){var value=entry.getValue();var out=board.getOrCreatePlayerScore(ScoreHolder.forNameOnly(entry.getKey()),next,true);
                out.set(value.value);if(value.locked)out.lock();else out.unlock();out.display(value.display);out.numberFormatOverride(value.format);}
            for(var slot:slots)board.setDisplayObjective(slot,next);
        }
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner;final ShishEncounterChecks.State saved;
        final SauvojenMeditationChecks.Attribution attribution;ShishEncounterChecks.Client client;
        final net.minecraft.server.level.ServerLevel level;final Vec3 origin;final BlockPos block;
        final MapManager.BlockSnapshot terrain;final LbcIndirectKills.TerrainProvenance provenance;
        final ObjectiveState damage,mining;final Stat<?> mined=Stats.BLOCK_MINED.get(Blocks.MUSHROOM_STEM);
        final int oldMined;final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final List<Entity> owned=new ArrayList<>();Cow target,other;Entity beacon;int phase,at;boolean closed;
        Fixture(GameController game){this.game=game;owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            require(game.playing()&&game.canUseGameplayItems(owner)&&!game.items().timeStop().stopped(owner.level()),"active native participant without borrowed stop");
            require(owner.entityTags().stream().noneMatch(tag->tag.startsWith("sin_")),"no borrowed Witch authority");
            saved=new ShishEncounterChecks.State(game,owner);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner));
            level=owner.level();origin=new Vec3(owner.getX(),240,owner.getZ());block=BlockPos.containing(origin.add(0,0,3));
            require(level.hasChunkAt(block)&&level.getBlockState(block).isAir(),"resident exclusive mining cell");
            terrain=game.maps().snapshot(level,block);provenance=game.items().indirectKills().captureTerrain(level,List.of(block));
            damage=new ObjectiveState(game.server().getScoreboard(),"dmg_dealt");mining=new ObjectiveState(game.server().getScoreboard(),"lolidefecation");
            oldMined=owner.getStats().getValue(mined);for(var e:level.getAllEntities())before.add(e);
        }
        int score(String name){return game.items().mana().hudValue(owner,name);}
        void score(String name,int value){game.items().mana().score(owner,name).set(value);}
        void initialize(){
            owner.removeAllEffects();owner.getInventory().clearContent();owner.setGameMode(GameType.SURVIVAL);owner.getAbilities().invulnerable=false;
            owner.entityTags().removeAll(Set.of("spectator","time_stopped","sin_greed_active","nomagic_active","strict_map_object","magic_academy","rcdenyalways"));
            var profile=game.players().get(owner.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);
            owner.setNoGravity(true);owner.setDeltaMovement(Vec3.ZERO);owner.setKnownMovement(Vec3.ZERO);move(origin);
            game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();client=new ShishEncounterChecks.Client(game,owner);
            statisticMatrix();
            owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.75);owner.setHealth(10);owner.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2);
            target=cow(origin.add(0,0,3),30.75);other=cow(origin.add(3,0,2),40.75);other.hurtTime=10;
            owner.entityTags().add("sin_lust");float hp=owner.getHealth();strike(target);
            near(owner.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),22,"source integer attacker base +2");
            near(target.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),28,"source integer victim base -2");
            near(owner.getHealth(),hp,"max-health theft does not heal wearer");near(other.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),40.75,"unrelated recent HurtTime is not an actual melee target");
            double base=owner.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
            var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(EntityTypes.ARROW,level);
            for(var source:List.of(new DamageSource(owner.damageSources().playerAttack(owner).typeHolder(),other,owner),owner.damageSources().arrow(arrow,owner))){
                target.setInvulnerableTime(0);target.damageCooldownTime=0;target.hurtServer(level,source,.1f);
                near(owner.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),base,"attributed NPC/arrow damage does not trigger owner melee bonus");
            }
            beacon=marker("stopper_magic");at=game.server().getTickCount();
        }
        void statisticMatrix(){
            var dealt=Stats.CUSTOM.get(Stats.DAMAGE_DEALT);
            for(boolean nativeCriteria:new boolean[]{false,true}){
                damage.replace(nativeCriteria?dealt:ObjectiveCriteria.DUMMY);mining.replace(nativeCriteria?mined:ObjectiveCriteria.DUMMY);
                score("dmg_dealt",110);score("lolidefecation",63);int before=owner.getStats().getValue(dealt);
                owner.awardStat(dealt,27);owner.awardStat(mined,1);
                require(score("dmg_dealt")==137&&score("lolidefecation")==64&&owner.getStats().getValue(dealt)==before+27,"one award, no duplicate counting, criteria="+nativeCriteria);
                game.items().legacyEffects().resetWitch(owner);owner.awardStat(dealt,9);owner.awardStat(mined,1);
                require(score("dmg_dealt")==10&&score("lolidefecation")==1,"custom reset remains authoritative, lifetime totals do not return");
                owner.resetStat(dealt);owner.resetStat(mined);
                require(score("dmg_dealt")==0&&score("lolidefecation")==0&&owner.getStats().getValue(dealt)==0&&owner.getStats().getValue(mined)==0,"native reset works for both criteria types");
                owner.awardStat(Stats.CUSTOM.get(Stats.JUMP),3);require(score("dmg_dealt")==0&&score("lolidefecation")==0,"unrelated statistic does not alter either Witch objective");
            }
            damage.remove();mining.remove();
            level.setBlock(block,Blocks.MUSHROOM_STEM.defaultBlockState(),2|16|32);owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_AXE));
            int old=owner.getStats().getValue(mined);require(owner.gameMode.destroyBlock(block),"actual native block destruction");
            require(owner.getStats().getValue(mined)==old+1&&score("lolidefecation")==1
                    &&game.server().getScoreboard().getObjective("lolidefecation").getCriteria()==mined,"first native mined event creates correct criterion and counts exactly once");
            owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            damage.restore();mining.restore();
        }
        Cow cow(Vec3 v,double health){var mob=new Cow(EntityTypes.COW,level);mob.setPos(v);mob.setNoAi(true);mob.setNoGravity(true);mob.setSilent(true);
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);mob.setHealth((float)health);require(level.addFreshEntity(mob),"native cow publication");owned.add(mob);return mob;}
        Entity marker(String tag){var e=EntityTypes.MARKER.create(level,EntitySpawnReason.COMMAND);require(e!=null,"native beacon marker");e.setPos(owner.position());e.entityTags().add(tag);require(level.addFreshEntity(e),"native beacon publication");owned.add(e);return e;}
        void strike(Cow cow){cow.setInvulnerableTime(0);cow.damageCooldownTime=0;owner.attack(cow);}
        void discard(Entity e){game.items().entityUnloaded(e);e.discard();}
        void move(Vec3 v){require(owner.teleportTo(level,v.x,v.y,v.z,Set.of(),0,0,true),"native position change");if(client!=null)client.ack();}
        boolean step(){int now=game.server().getTickCount();client.ack();if(now==at)return false;require(now-at<12,"actual scheduler deadline phase="+phase);
            switch(phase){
                case 0->{require(game.items().magic().suppressed(owner),"real magic stopper active");double before=owner.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),victim=target.getAttribute(Attributes.MAX_HEALTH).getBaseValue();strike(target);
                    near(owner.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),before,"magic stopper suppresses Lust gain");near(target.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),victim,"magic stopper suppresses Lust loss");discard(beacon);beacon=marker("stopper");phase=1;at=now;return false;}
                case 1->{require(!game.items().magic().suppressed(owner)&&game.items().magic().mechanismSuppressed(owner),"only real mechanism stopper active");
                    double before=owner.getAttribute(Attributes.MAX_HEALTH).getBaseValue();strike(target);near(owner.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),before+2,"mechanism stopper permits Lust");discard(beacon);
                    owner.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(.1);var fragile=cow(origin.add(0,0,2.5),1.75);strike(fragile);
                    require(!fragile.isAlive()&&fragile.getKillCredit()==owner,"depleted max-health kill preserves melee author");
                    var protectedCow=cow(origin.add(0,0,4),1.75);game.items().mana().score(protectedCow,"void_resistance").set(1);strike(protectedCow);
                    require(protectedCow.isAlive()&&protectedCow.getAttribute(Attributes.MAX_HEALTH).getBaseValue()<=0,"source void resistance prevents depleted-base kill");
                    beacon=marker("stopper_magic");beacon.setPos(target.position().add(0,0,19));game.items().magic().loaded(beacon);
                    require(!game.items().magic().suppressed(owner)&&game.items().magic().suppressedAt(level,target.position()),"target-only antimagic geometry");
                    before=owner.getAttribute(Attributes.MAX_HEALTH).getBaseValue();double protectedBase=target.getAttribute(Attributes.MAX_HEALTH).getBaseValue();strike(target);
                    near(owner.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),before,"target antimagic blocks owner gain");near(target.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),protectedBase,"target antimagic blocks maxHP theft");discard(beacon);
                    owner.entityTags().remove("sin_lust");score("x",net.minecraft.util.Mth.floor(owner.getX()*100));score("y",net.minecraft.util.Mth.floor(owner.getY()*100));score("z",net.minecraft.util.Mth.floor(owner.getZ()*100));score("sleep_time",100);phase=2;at=now;return false;}
                case 2->{require(score("sleep_time")==101&&score("speed")==0,"existing movement owner advances sleep exactly once while stationary");move(origin.add(.1,0,0));phase=3;at=now;return false;}
                case 3->{require(score("sleep_time")==86&&score("speed")>0,"existing movement owner applies source moving drain, no second Witch clock");return true;}
                default->throw new IllegalStateException("phase "+phase);
            }
        }
        public void close(){if(closed)return;closed=true;try{
            owner.entityTags().remove("sin_lust");for(var e:List.copyOf(owned))if(!e.isRemoved())discard(e);
            var fresh=new ArrayList<Entity>();for(var e:level.getAllEntities())if(e!=null&&!before.contains(e)&&!(e instanceof ServerPlayer)&&e.position().distanceToSqr(origin)<144&&!owned.contains(e))fresh.add(e);fresh.forEach(this::discard);
            game.maps().restoreSnapshot(level,block,terrain);game.items().indirectKills().restoreTerrain(provenance);
        }finally{try{saved.restore(game);owner.getStats().setValue(owner,mined,oldMined);damage.restore();mining.restore();if(client!=null)client.ack();attribution.close();}
            finally{if(client!=null)client.close();}}}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.initialize();}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}throw error;}
        SkyWars2.LOGGER.info("[check] START witch-progress (native statistic/mining/melee callbacks and canonical movement scheduler)");}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS witch-progress (native/dummy criterion awards/reset and first mining event, exact mutable progress, native Lust melee versus NPC/arrow, integer HP transfer/no heal/jammers/kill/void protection, existing sleep movement owner; no complete acquisition qualification)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL witch-progress",error);}}
}
