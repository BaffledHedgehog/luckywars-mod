package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Actual authority scheduler and native conversion/damage; no private clock advancement. */
public final class WitchWrathChecks {
    private WitchWrathChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Witch Wrath: "+why);}
    private static void near(double a,double b,String why){require(Math.abs(a-b)<.0001,why+" actual="+a+" expected="+b);}
    private static final Identifier BONUS=Identifier.parse("skywars2:check.wrath_max");
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,beta;final ServerLevel level,nether;
        final net.minecraft.world.scores.PlayerTeam betaTeam;
        final List<ShishEncounterChecks.State> saved=new ArrayList<>();
        final SauvojenMeditationChecks.Attribution attribution;final List<Entity> created=new ArrayList<>();
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();final LbcIndirectKills.TerrainProvenance provenance;
        final List<ShishEncounterChecks.Client> clients=new ArrayList<>();
        final Vec3 origin;Cow mob,overlap,foreign,boss,strict;Entity jammer;net.minecraft.world.entity.projectile.arrow.Arrow child;
        boolean forced,stopped,closed;int phase,at,start;
        Fixture(GameController game){this.game=game;
            owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            betaTeam=beta.getTeam();
            level=owner.level();nether=Objects.requireNonNull(game.server().getLevel(Level.NETHER));origin=new Vec3(owner.getX(),272,owner.getZ());
            require(game.playing()&&!game.items().timeStop().stopped(level),"active real match without borrowed Time Stop");
            for(var p:game.server().getPlayerList().getPlayers()){
                require(p.entityTags().stream().noneMatch(t->t.startsWith("sin_")||t.equals("affected_by_wrath")),"no borrowed authority");saved.add(new ShishEncounterChecks.State(game,p));
            }
            for(var world:game.server().getAllLevels())for(var e:world.getAllEntities()){
                before.add(e);require(!(e instanceof LivingEntity)||e instanceof ServerPlayer||!e.isAlive(),"global source fixture has no borrowed live mobs: "+e);
            }
            attribution=new SauvojenMeditationChecks.Attribution(game,game.server().getPlayerList().getPlayers());
            for(var cell:BlockPos.betweenClosed(BlockPos.containing(origin).offset(-1,-1,-1),BlockPos.containing(origin).offset(10,3,1)))
                require(level.hasChunkAt(cell)&&level.getBlockState(cell).isAir(),"resident exclusive actor footprint");
            for(var cell:List.of(BlockPos.containing(origin),BlockPos.containing(origin).above()))terrain.put(cell,game.maps().snapshot(level,cell));
            provenance=game.items().indirectKills().captureTerrain(level,terrain.keySet());
        }
        void initialize(){
            for(var p:game.server().getPlayerList().getPlayers()){
                p.entityTags().add("strict_map_object");p.getInventory().clearContent();p.removeAllEffects();
                for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.OFFHAND))p.setItemSlot(slot,net.minecraft.world.item.ItemStack.EMPTY);
                var profile=game.players().get(p.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);
                game.items().magic().inventoryChanged(p);
            }
            for(var p:List.of(owner,beta)){
                p.removeAllEffects();p.getInventory().clearContent();p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=false;
                p.entityTags().removeAll(Set.of("spectator","time_stopped","sin_greed_active","nomagic_active","magic_academy","rcdenyalways"));
                p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);p.setAbsorptionAmount(0);
                p.getFoodData().setFoodLevel(10);p.getFoodData().setSaturation(0);
                p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(80);
                var profile=game.players().get(p.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);game.items().magic().inventoryChanged(p);
            }
            owner.entityTags().remove("strict_map_object");move(owner,origin);move(beta,origin.add(40,0,0));
            clients.add(new ShishEncounterChecks.Client(game,owner));clients.add(new ShishEncounterChecks.Client(game,beta));
            game.items().magic().flushCleansing();
            owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(60.9);
            owner.getAttribute(Attributes.MAX_HEALTH).addTransientModifier(new AttributeModifier(BONUS,7.3,AttributeModifier.Operation.ADD_VALUE));owner.setHealth(17.9f);
            game.items().mana().score(owner,"lbcID2").set(1234);game.items().mana().score(owner,"team_number").set(29);
            mob=cow(level,origin.add(4,0,0));overlap=cow(level,origin.add(4.7,0,.2));boss=cow(level,origin.add(7,0,0));strict=cow(level,origin.add(9,0,0));
            boss.entityTags().add("sauvojen_tuntija");strict.entityTags().add("strict_map_object");
            var cell=BlockPos.containing(origin);forced=nether.setChunkForced(cell.getX()>>4,cell.getZ()>>4,true);nether.getChunk(cell.getX()>>4,cell.getZ()>>4);
            foreign=cow(nether,origin);owner.entityTags().add("sin_wrath");start=at=game.server().getTickCount();
        }
        Cow cow(ServerLevel world,Vec3 position){var cow=new Cow(EntityTypes.COW,world);cow.setPos(position);cow.setNoGravity(true);cow.setNoAi(true);cow.setSilent(true);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(90);cow.setHealth(80);require(world.addFreshEntity(cow),"native actor publication");created.add(cow);return cow;}
        void move(ServerPlayer player,Vec3 pos){require(player.teleportTo(level,pos.x,pos.y,pos.z,Set.of(),0,0,true),"native participant position");}
        Entity marker(String tag,Vec3 pos){var e=EntityTypes.MARKER.create(level,EntitySpawnReason.COMMAND);require(e!=null,"native jammer");e.setPos(pos);e.entityTags().add(tag);created.add(e);require(level.addFreshEntity(e),"jammer publication");game.items().magic().loaded(e);return e;}
        void discard(Entity e){if(e!=null){game.items().entityUnloaded(e);e.discard();
            // The source TECH marker migrates into a real block. Removing only its old entity leaves a live jammer.
            if(e.entityTags().contains("stopper"))for(var entry:terrain.entrySet())
                if(level.getBlockState(entry.getKey()).is(LbcMagic.MECHANISM_BEACON_BLOCK))game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());
        }}
        boolean step(){int now=game.server().getTickCount();for(var client:clients)client.ack();require(game.playing()&&now-start<160,"bounded native phase "+phase);if(now-at<2)return false;
            switch(phase){
                case 0->{
                    if(nether.getEntity(foreign.getUUID())!=foreign)return false;
                    for(var e:List.of(mob,overlap,foreign)){
                        require(e.entityTags().contains("affected_by_wrath")&&e.getTeam()==owner.getTeam()&&game.items().summons().owner(e)==owner,"native global subjugation/current controller");
                        near(e.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),60,"mob copies integer BASE, not effective modifier");near(e.getHealth(),17,"mob copies integer HP");
                        require(game.items().mana().hudValue(e,"lbcID2")==1234&&game.items().mana().hudValue(e,"team_number")==29,"source counter payload");
                    }
                    near(boss.getHealth(),80,"Sauvojen health exception");near(boss.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),90,"Sauvojen maxHP exception");require(owner.entityTags().contains("friendly_tuntija"),"actual Sauvojen conversion bridge");
                    near(strict.getHealth(),80,"protected map entity remains untouched");require(!strict.entityTags().contains("affected_by_wrath"),"protected role not adopted");
                    owner.setHealth(17);beta.entityTags().remove("strict_map_object");game.items().mana().score(beta,"lbcID2").set(9876);
                    game.items().witchWrath().convertAt(owner,level,beta.position(),true);
                    require(beta.entityTags().contains("affected_by_wrath")&&beta.getTeam()==owner.getTeam(),"source positive-unit-box player conversion and actual team");
                    require(game.items().mana().hudValue(beta,"team_number")==29&&game.items().mana().hudValue(beta,"lbcID2")==9876,"player branch copies team counter but not mob ID");
                    beta.setHealth(40);phase=1;at=now;return false;
                }
                case 1->{
                    near(beta.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),68,"player receives integer EFFECTIVE maxHP");near(beta.getHealth(),17,"source integer outside-border equalization");require(beta.getKillCredit()==owner,"real health-control damage author");
                    beta.entityTags().add("strict_map_object");jammer=marker("stopper_magic",mob.position().add(19,0,0));
                    require(!game.items().magic().suppressed(owner)&&game.items().magic().suppressedAt(level,mob.position()),"target-only antimagic");
                    mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(41);mob.setHealth(35);phase=2;at=now;return false;
                }
                case 2->{near(mob.getHealth(),35,"target antimagic protects native health");near(mob.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),41,"target antimagic protects attribute");
                    discard(jammer);jammer=marker("stopper_magic",owner.position());owner.setHealth(13.8f);phase=3;at=now;return false;}
                case 3->{near(mob.getHealth(),35,"holder antimagic pauses power");discard(jammer);jammer=marker("stopper",owner.position());phase=4;at=now;return false;}
                case 4->{near(mob.getHealth(),13,"mechanism jammer permits authority");discard(jammer);jammer=null;require(!game.items().magic().mechanismSuppressed(owner),"migrated native TECH block also removed from fixture");
                    game.items().timeStop().begin(level,100);stopped=true;owner.setHealth(9.9f);phase=5;at=now;return false;}
                case 5->{near(mob.getHealth(),13,"real Time Stop freezes authority updates");if(now-at<5)return false;
                    game.items().timeStop().end(level);stopped=false;phase=6;at=now;return false;}
                case 6->{near(mob.getHealth(),9,"thaw resumes source health control");
                    child=new net.minecraft.world.entity.projectile.arrow.Arrow(EntityTypes.ARROW,level);child.setOwner(mob);child.setPos(origin.add(0,3,8));child.setNoGravity(true);created.add(child);
                    var source=game.items().spellSources().hostile(mob,owner,Set.of());game.items().spellSources().bind(mob,source);game.items().spellSources().inherit(mob,child);require(level.addFreshEntity(child),"actual pre-transfer native child");
                    require(game.items().spellSources().source(child)!=null,"source retained on native publication: removed="+child.isRemoved()+" reason="+child.getRemovalReason()+" owner="+child.getOwner()+" tags="+child.entityTags());
                    owner.entityTags().remove("sin_wrath");beta.entityTags().remove("strict_map_object");beta.entityTags().add("sin_wrath");
                    require(betaTeam!=null&&betaTeam!=owner.getTeam(),"distinct native future holder team");game.server().getScoreboard().addPlayerToTeam(beta.getScoreboardName(),betaTeam);
                    beta.getAttribute(Attributes.MAX_HEALTH).setBaseValue(70);beta.setHealth(22.8f);game.items().mana().score(beta,"lbcID2").set(4567);phase=7;at=now;return false;}
                case 7->{for(var e:List.of(mob,overlap,foreign))require(e.getTeam()==beta.getTeam()&&game.items().summons().owner(e)==beta,"authority handover changes actual team and controller");
                    var oldSource=game.items().spellSources().source(child);
                    require(oldSource!=null&&oldSource.author().equals(owner.getUUID()),"accepted child preserves its original causal author: removed="+child.isRemoved()+" reason="+child.getRemovalReason()+" owner="+child.getOwner()+" tags="+child.entityTags()+" source="+oldSource);near(mob.getHealth(),22,"new holder owns health synchronization");
                    var next=new net.minecraft.world.entity.projectile.arrow.Arrow(EntityTypes.ARROW,level);next.setOwner(mob);next.setPos(origin.add(0,3,9));next.setNoGravity(true);created.add(next);
                    game.items().spellSources().inherit(mob,next);require(level.addFreshEntity(next),"new native child after handover");
                    require(game.items().spellSources().source(next)!=null&&game.items().spellSources().source(next).author().equals(beta.getUUID()),"future native child belongs to the current authority holder");
                    int old=game.config().gameMode;owner.entityTags().add("strict_map_object");try{game.config().gameMode=3;owner.entityTags().add("cantleave");game.items().witchWrath().convertAt(beta,level,owner.position(),true);require(!owner.entityTags().contains("cantleave"),"approved GLOBAL cantleave cleanup including preexisting restriction");}finally{game.config().gameMode=old;owner.entityTags().remove("strict_map_object");}
                    // Exercise the production death boundary, while retaining actual core participants for neighbor suites.
                    game.items().timeStop().begin(level,100);stopped=true;game.items().witchWrath().died(beta);phase=8;at=now;return false;}
                case 8->{require(mob.isAlive()&&foreign.isAlive(),"death consequence waits during real Time Stop");game.items().timeStop().end(level);stopped=false;phase=9;at=now;return false;}
                case 9->{require(!mob.isAlive()&&!overlap.isAlive()&&!foreign.isAlive(),"native global affected-mob death consequence after thaw");require(mob.getKillCredit()==beta&&foreign.getKillCredit()==beta,"inherited death consequence author");require(strict.isAlive()&&boss.isAlive()&&owner.isAlive()&&beta.isAlive(),"strict roles, Sauvojen and current holder survive");return true;}
                default->throw new IllegalStateException("phase "+phase);
            }
        }
        public void close(){if(closed)return;closed=true;try{
            if(stopped){game.items().timeStop().end(level);stopped=false;}
            owner.entityTags().remove("sin_wrath");beta.entityTags().remove("sin_wrath");
            for(var e:created)discard(e);
            var fresh=new ArrayList<Entity>();for(var world:game.server().getAllLevels())for(var e:world.getAllEntities())
                if(e!=null&&!before.contains(e)&&!(e instanceof ServerPlayer))fresh.add(e);
            for(var e:fresh)discard(e);
            for(var e:created){game.server().getScoreboard().removePlayerFromTeam(e.getScoreboardName());game.server().getScoreboard().resetAllPlayerScores(e);}
            for(var entry:terrain.entrySet())game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());game.items().indirectKills().restoreTerrain(provenance);
            if(forced){var cell=BlockPos.containing(origin);nether.setChunkForced(cell.getX()>>4,cell.getZ()>>4,false);}
        }finally{try{for(var state:saved)state.restore(game);for(var c:clients)c.ack();attribution.close();}finally{for(var c:clients)c.close();}}}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.initialize();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}throw error;}SkyWars2.LOGGER.info("[check] START witch-wrath (native authority scheduler)");}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS witch-wrath (native global mob control, base/effective HP quantization, source counters/teams, current-holder handover versus accepted causal child, holder/target MAG and TECH, real Time Stop, global cantleave cleanup and death boundary; acquisition/full Witch still gated)");}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL witch-wrath",error);}}
}
