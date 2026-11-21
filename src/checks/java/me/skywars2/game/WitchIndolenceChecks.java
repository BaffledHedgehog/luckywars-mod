package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Raw claw dispatch, real native selector counts, native ticks and shared damage/terrain ownership. */
public final class WitchIndolenceChecks {
    private WitchIndolenceChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Indolence: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture base;final LbcWitchIndolence service;
        final ShishEncounterChecks.Client client,otherClient;
        final List<Entity> fingers=new ArrayList<>(),own=new ArrayList<>();
        final Map<ServerPlayer,Set<String>> tags=new IdentityHashMap<>();
        final Map<ServerPlayer,Integer> cooldowns=new IdentityHashMap<>();
        LbcIndirectKills.TerrainProvenance provenance;
        ItemStack claw;Entity flight,jammer;Cow ally,miss,strict;BlockPos wall;
        LbcSpellSource.Source accepted;Vec3 flightAt;int phase,at,start,age,cd,mana;boolean stopped,closed,foreignForced;
        net.minecraft.server.level.ServerLevel foreign;
        Fixture(GameController game){this.game=game;base=new ChefChecks.Fixture(game);service=game.items().witchIndolence();
            client=new ShishEncounterChecks.Client(game,base.owner);otherClient=new ShishEncounterChecks.Client(game,base.other);
            require(service.snapshot().isEmpty(),"no borrowed invisible hands");
            for(var world:game.server().getAllLevels())for(var e:world.getAllEntities())require(!e.entityTags().contains("indolence_finger"),"no borrowed global fingers");
            for(var p:List.of(base.owner,base.other)){tags.put(p,new HashSet<>(p.entityTags()));var objective=game.server().getScoreboard().getObjective("indolence_cd");var score=objective==null?null:game.server().getScoreboard().getPlayerScoreInfo(p,objective);cooldowns.put(p,score==null?null:score.value());}
            var cells=new ArrayList<BlockPos>();for(var cell:BlockPos.betweenClosed(base.center.offset(-16,-4,-16),base.center.offset(16,12,16)))cells.add(cell.immutable());
            provenance=game.items().indirectKills().captureTerrain(base.owner.level(),cells);
        }
        void init(){base.init();base.move(base.owner,base.at);base.move(base.other,base.at.add(12,0,0));game.config().offhandWands=true;
            for(var p:List.of(base.owner,base.other)){p.entityTags().removeAll(Set.of("nomagic_active","sin_greed_active","spectator","time_stopped"));p.setShiftKeyDown(false);}
            claw=game.lbc().rollTable(base.owner.level(),base.owner.position(),base.owner,"lbc:ported/indolence_claw").stream().filter(LbcWitchIndolence::matches).findFirst().orElseThrow();
            require(LbcFartus.data(claw).getIntOr("magicwand",0)==1&&LbcFartus.data(claw).getIntOr("spellwithcooldown",0)==1,"actual authored claw flags");
            mana=game.items().mana().get(base.owner);claw.setCount(2);base.owner.setItemInHand(InteractionHand.OFF_HAND,claw.copy());
            require(!callback(InteractionHand.OFF_HAND)&&service.snapshot().isEmpty(),"offhand claw cannot bypass missing authority");
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,claw.copy());require(!callback(InteractionHand.MAIN_HAND),"mainhand claw also requires authority");
            base.owner.entityTags().add("sin_indolence");game.items().mana().score(base.owner,"indolence_cd").set(0);
            require(callback(InteractionHand.OFF_HAND)&&service.snapshot().size()==16,"offhand raw carrier works with its real authority: tags="+base.owner.entityTags()+" gameplay="+game.canUseGameplayItems(base.owner)+" blocked="+game.items().actionsBlocked(base.owner)+" role="+game.items().wands().validRoleTarget(base.owner,false)+" magic="+game.items().magic().suppressed(base.owner)+" cd="+game.items().mana().score(base.owner,"indolence_cd").get()+" count="+service.snapshot().size());cast(16,40,false,40);service.stop();addFingers(1);phase=0;start=at=game.server().getTickCount();
        }
        void addFingers(int count){while(fingers.size()<count){boolean remote=fingers.isEmpty();var level=base.owner.level();
            if(remote){foreign=Objects.requireNonNull(game.server().getLevel(net.minecraft.world.level.Level.NETHER));foreignForced=foreign.setChunkForced(0,0,true);foreign.getChunk(0,0);level=foreign;}
            var mob=(Mob)EntityTypes.SKELETON.create(level,EntitySpawnReason.COMMAND);require(mob!=null,"native skeleton factory");mob.setPos(remote?new Vec3(.5,200,.5):base.at.add(-8+fingers.size(),6,-8));mob.setNoAi(true);mob.setNoGravity(true);mob.setPersistenceRequired();mob.entityTags().add("indolence_finger");require(level.addFreshEntity(mob),"native finger enters global lookup, including foreign dimension");fingers.add(mob);}}
        void cast(int count,int spread,boolean controlled,int cooldown){service.stop();game.items().mana().score(base.owner,"indolence_cd").set(0);mana=game.items().mana().get(base.owner);
            require(callback(InteractionHand.MAIN_HAND),"actual native UseItem callback accepts raw claw carrier through two-hand dispatch");
            require(service.snapshot().size()==count&&game.items().mana().score(base.owner,"indolence_cd").get()==cooldown,"global native finger pattern: expected="+count+" actual="+service.snapshot().size()+" cooldown="+game.items().mana().score(base.owner,"indolence_cd").get()+" phase="+phase);
            require(game.items().mana().get(base.owner)==mana&&base.owner.getMainHandItem().getCount()==2&&base.owner.getOffhandItem().getCount()==2,"claw consumes neither mana nor items");
            for(var e:service.snapshot()){
                require(e.getType()==EntityTypes.MARKER&&e.entityTags().containsAll(Set.of("slowraycast","magic","other",controlled?"controlled_invisible_hand":"invisible_hand")),"source marker policy/tags");
                var source=game.items().spellSources().source(e);require(source!=null&&source.cause().friendlyFire()&&source.persistent()&&source.author().equals(base.owner.getUUID()),"shared persistent WILD cause, not ordinary enemy-only projectile");
                if(spread>0){require(e.getYRot()>=-spread&&e.getYRot()<=spread&&e.getXRot()>=-spread&&e.getXRot()<=spread,"source integer angular spread");}
            }
        }
        boolean callback(InteractionHand hand){return hand==InteractionHand.OFF_HAND
                ?base.owner.gameMode.useItem(base.owner,base.owner.level(),base.owner.getItemInHand(hand),hand).consumesAction()
                :net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(base.owner,base.owner.level(),hand).consumesAction();}
        static long particles(ShishEncounterChecks.Client client){try{Object wire=client.wire;var f=wire.getClass().getDeclaredField("packets");f.setAccessible(true);return ((List<?>)f.get(wire)).stream().filter(p->p instanceof ClientboundLevelParticlesPacket packet&&packet.particle() instanceof DustParticleOptions).count();}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
        long ownerParticles,otherParticles;
        boolean fingersVisible(){try{var selector=new net.minecraft.commands.arguments.selector.EntitySelectorParser(new com.mojang.brigadier.StringReader("@e[type=minecraft:skeleton,tag=indolence_finger]"),true).parse();return selector.findEntities(game.server().createCommandSourceStack().withLevel(base.owner.level())).size()==fingers.size();}catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}}
        boolean step(){client.ack();otherClient.ack();int now=game.server().getTickCount();require(now-start<460,"bounded real-tick authority fixture");
            // Publication is not native selector visibility until the forced foreign chunk is entity-ready.
            if(phase<4&&!fingersVisible())return false;
            if(phase==0){cast(8,20,false,30);service.stop();addFingers(3);phase++;return false;}
            if(phase==1){cast(4,10,false,20);service.stop();addFingers(5);phase++;return false;}
            if(phase==2){cast(2,5,false,10);base.owner.setShiftKeyDown(true);cast(3,0,true,15);require(service.snapshot().stream().filter(e->e.entityTags().contains("knockb")).count()==2,"five/six fingers yield one precise and two jittered controlled hands");service.stop();addFingers(7);phase++;return false;}
            if(phase==3){base.owner.setShiftKeyDown(false);base.owner.setXRot(-90);cast(1,0,false,5);flight=service.snapshot().getFirst();flightAt=flight.position();ownerParticles=particles(client);otherParticles=particles(otherClient);at=now;phase++;return false;}
            if(phase==4){require(service.age(flight)==1&&flight.position().distanceTo(flightAt.add(0,1,0))<1e-5,"ordinary recursive samples end at exactly one block per real tick");
                require(particles(client)-ownerParticles==3&&particles(otherClient)==otherParticles,"three native source particles only reach authority viewers");
                age=service.age(flight);cd=game.items().mana().score(base.owner,"indolence_cd").get();flightAt=flight.position();game.items().timeStop().begin(base.owner.level(),100);stopped=true;at=now;phase++;return false;}
            if(phase==5){require(service.age(flight)==age&&flight.position().equals(flightAt)&&game.items().mana().score(base.owner,"indolence_cd").get()==cd,"real Time Stop pauses both projectile and scoreboard cooldown");if(now-at<5)return false;game.items().timeStop().end(base.owner.level());stopped=false;at=now;phase++;return false;}
            if(phase==6){if(!flight.isRemoved()){require(service.age(flight)<60,"ordinary hand retains exact source sixty-tick lifetime");return false;}
                require(now-at>=59&&game.items().mana().score(base.owner,"indolence_cd").get()==0,"ordinary hand naturally expires and source cooldown drains per tick");
                base.owner.setShiftKeyDown(true);base.owner.setXRot(-90);cast(1,0,true,20);flight=service.snapshot().getFirst();flightAt=flight.position();at=now;phase++;return false;}
            if(phase==7){require(service.age(flight)==1&&flight.position().distanceTo(flightAt.add(0,.3,0))<1e-5,"controlled hand advances exactly .3 per native tick");
                client.listener.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(90,0,false,false));flightAt=flight.position();phase++;return false;}
            if(phase==8){require(flight.position().distanceTo(flightAt.add(-.3,0,0))<1e-5,"controlled hand follows actual native look input");
                accepted=game.items().spellSources().source(flight);base.owner.entityTags().remove("sin_indolence");base.other.entityTags().add("sin_indolence");base.other.setXRot(-90);flightAt=flight.position();phase++;return false;}
            if(phase==9){var next=game.items().spellSources().source(flight);require(next.author().equals(base.other.getUUID())&&accepted.author().equals(base.owner.getUUID())&&flight.position().distanceTo(flightAt.add(0,.3,0))<1e-5,"authority transfer changes current control and future cause without mutating accepted old cause");phase++;return false;}
            if(phase==10){if(!flight.isRemoved()){require(service.age(flight)<200,"controlled hand survives until natural two-hundred-tick lifetime");return false;}
                require(now-at>=200,"controlled lifetime was not shortened");base.other.entityTags().remove("sin_indolence");base.owner.entityTags().add("sin_indolence");base.owner.setShiftKeyDown(false);base.owner.setXRot(0);base.owner.setYRot(0);
                var board=game.server().getScoreboard();if(base.owner.getTeam()!=null)board.removePlayerFromTeam(base.owner.getScoreboardName(),base.owner.getTeam());board.addPlayerToTeam(base.owner.getScoreboardName(),base.team);
                ally=base.cow(base.at.add(.3,1.4,2.4));board.addPlayerToTeam(ally.getScoreboardName(),base.team);miss=base.cow(base.at.add(3,1.4,2.4));strict=base.cow(base.at.add(.3,1.4,4.4));strict.entityTags().add("strict_map_object");phase++;at=now;return false;}
            if(phase==11){cast(1,0,false,5);phase++;at=now;return false;}
            if(phase==12){if(now-at<6)return false;require(ally.getHealth()<20&&miss.getHealth()==20&&strict.getHealth()==20,"native source contact hits allied mob but respects noncontact and protected-role targets");
                service.stop();base.remove(ally);base.remove(miss);base.remove(strict);wall=BlockPos.containing(base.owner.getEyePosition().add(0,0,3));base.block(wall,Blocks.STONE);cast(1,0,false,5);flight=service.snapshot().getFirst();phase++;at=now;return false;}
            if(phase==13){if(now-at<4)return false;require(flight.isRemoved()&&base.owner.level().getBlockState(wall).isAir(),"native destroy-mode impact removes contacted block and retires the hand");
                jammer=new Marker(EntityTypes.MARKER,base.owner.level());jammer.setPos(base.at);jammer.entityTags().addAll(Set.of("stopper","not_block"));own.add(jammer);base.owner.level().addFreshEntity(jammer);game.items().magic().loaded(jammer);phase++;return false;}
            if(phase==14){cast(1,0,false,5);require(!service.snapshot().isEmpty(),"mechanical jammer does not suppress invisible hands");service.stop();clearJammer();
                base.move(base.owner,base.at.add(-12,0,0));base.owner.setYRot(-90);cast(1,0,false,5);flight=service.snapshot().getFirst();
                jammer=new Marker(EntityTypes.MARKER,base.owner.level());jammer.setPos(base.owner.getEyePosition().add(21,0,0));jammer.entityTags().addAll(Set.of("stopper_magic","not_block"));own.add(jammer);base.owner.level().addFreshEntity(jammer);game.items().magic().loaded(jammer);phase++;at=now;return false;}
            if(phase==15){if(now-at<3)return false;require(flight.isRemoved()&&service.snapshot().isEmpty(),"already launched hand is erased on entering actual magical jammer");base.move(base.owner,base.at.add(9,3,0));game.items().mana().score(base.owner,"indolence_cd").set(0);require(!callback(InteractionHand.MAIN_HAND)&&service.snapshot().isEmpty(),"actual magical jammer blocks raw claw admission");return true;}
            return false;
        }
        void clearJammer(){if(jammer!=null){game.items().magic().unloaded(jammer);jammer.discard();base.block(jammer.blockPosition(),Blocks.AIR);game.items().magic().flushCleansing();jammer=null;}}
        public void close(){if(closed)return;closed=true;try{if(stopped)game.items().timeStop().end(base.owner.level());service.stop();clearJammer();for(var e:fingers)e.discard();if(foreignForced)foreign.setChunkForced(0,0,false);for(var e:own)e.discard();base.close();if(provenance!=null)game.items().indirectKills().restoreTerrain(provenance);
            for(var p:List.of(base.owner,base.other)){p.entityTags().clear();p.entityTags().addAll(tags.get(p));var objective=game.server().getScoreboard().getObjective("indolence_cd");if(cooldowns.get(p)!=null)game.items().mana().score(p,"indolence_cd").set(cooldowns.get(p));else if(objective!=null)game.server().getScoreboard().resetSinglePlayerScore(p,objective);}
        }finally{client.close();otherClient.close();}}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive native fixture");running=new Fixture(game);try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}SkyWars2.LOGGER.info("[check] START witch-indolence");}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS witch-indolence (raw both-hand authority gate, global native finger patterns, WILD contacts/roles, private particles, natural60/200tick flights, native look/authority transfer, TimeStop, shared terrain and jammer policy)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL witch-indolence",error);}}
}
