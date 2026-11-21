package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/** Historical NPC policy and native damage graph cross the real full-world replay coordinator. */
public final class HistoricalNpcReplayChecks {
    private HistoricalNpcReplayChecks(){}
    private static Scene running;
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException("Historical NPC replay: "+detail);}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final int started;
        final Set<UUID> original=new HashSet<>();
        Cow npc,target,watcher;Projectile shot;Vec3 parking,pose;float hp;
        int phase,when;boolean restoring,closed;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,true);started=game.server().getTickCount();f.level.getAllEntities().forEach(e->original.add(e.getUUID()));}
        Cow body(Vec3 at){var cow=new Cow(EntityTypes.COW,f.level);cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(at);f.own(cow);return cow;}
        void next(int value){phase=value;when=game.server().getTickCount();}
        void begin(){
            f.prepareParticipants();require(game.items().terra().transientSnapshot().isEmpty()&&game.items().spellSources().current()==null,"exclusive ordinary source engine");
            parking=f.home.add(0,40,0);npc=body(parking.add(0,0,12));target=body(parking.add(0,0,6));target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);
            var root=new LbcSpellSource.Source(npc.getUUID(),f.owner.getUUID(),game.items().epoch(),game.items().indirectKills().wildCause(f.owner),Set.of(),true).hostileLoot();
            try(var scope=game.items().spellSources().scope(root,f.level)){require(f.level.addFreshEntity(npc),"real native loot NPC admitted through shared capture");}
            require(f.level.addFreshEntity(target),"native independent victim");
            watcher=body(parking.add(0,0,18));require(f.level.addFreshEntity(watcher),"native independent target observer");watcher.setTarget(npc);require(watcher.getTargetUnchecked()==npc,"real native target selected while emitter is alive");
            var policy=game.items().spellSources().source(npc);require(policy!=null&&policy.author()==null&&policy.unownedMobs()&&policy.nativeEmitter()==npc,"loot admission removes borrowed executor and retains actual NPC");
            game.items().sauvojen().emitHostile(f.level,LbcSauvojen.Kind.TERRA,parking,new Vec3(1,0,0),policy);
            shot=game.items().terra().transientSnapshot().getFirst();f.own(shot);
            require(npc.hurtServer(f.level,npc.damageSources().genericKill(),Float.MAX_VALUE)&&!npc.isAlive(),"real native emitter death");next(0);
        }
        void verify(){
            require(npc.isRemoved()&&f.level.getEntity(npc.getUUID())==null,"historical emitter never resurrects with the live attack");
            require(f.level.getEntity(watcher.getUUID())==watcher&&watcher.getTargetUnchecked()==npc&&watcher.getTarget()==null,"native stale raw target is preserved as historical while vanilla sanitization returns no live target");
            require(f.level.getEntity(shot.getUUID())==shot&&!shot.isRemoved()&&shot.position().equals(pose),"same live canonical charge and pose restored");
            var nativeOwner=((me.skywars2.mixin.BedwarsRoleGuardMixin.ProjectileOwner)shot).skywars2$owner();
            require(shot.getOwner()==null&&nativeOwner!=null&&nativeOwner.getUUID().equals(npc.getUUID()),"unresolved native Owner UUID preserved without promoting it to a live dependency");
            var policy=game.items().spellSources().source(shot);require(policy!=null&&policy.author()==null&&policy.nativeEmitter()==npc&&game.items().spellSources().nativeMobSource(shot)==npc,"typed policy keeps exact historical native identity");
            require(f.level.getEntity(target.getUUID())==target&&target.getHealth()==hp,"canonical native victim and HP restored");
            var source=target.getLastDamageSource();require(source!=null&&source.getDirectEntity()==shot&&source.getEntity()==npc&&LbcIndirectKills.npcSource(source)&&target.getKillCredit()==npc,"native accepted damage graph restores historical NPC instead of future player");
            require(game.items().timeStop().stopped(f.level),"captured TimeStop holds reconstructed authority");
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();require(!game.vanity().status().startsWith("FAILED"),"actual coordinator failed: "+game.vanity().status());require(game.playing()&&game.server().getTickCount()-started<900,"bounded scene phase="+phase);
            if(restoring&&game.vanity().busy())for(var player:game.server().getPlayerList().getPlayers())if(player.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            int elapsed=game.server().getTickCount()-when;
            if(phase==0){
                require(!shot.isRemoved(),"persistent native charge outlives emitter");shot.setPos(parking);
                if(f.level.getEntity(npc.getUUID())!=null){require(elapsed<30,"native corpse removal");return false;}
                shot.setPos(target.position().add(-.2,0,-.2));shot.setDeltaMovement(1.8,0,0);next(1);return false;
            }
            if(phase==1){
                if(target.getHealth()==1000){require(elapsed<3,"real delayed native contact");return false;}
                require(target.getLastDamageSource().getEntity()==npc&&target.getKillCredit()==npc,"positive historical cause before capture");
                hp=target.getHealth();pose=shot.position();f.pause();require(game.vanity().requestCapture(f.owner,false),"whole coordinator captures live shot with removed NPC source");next(2);return false;
            }
            if(phase==2){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;verify();shot.setPos(parking);f.thaw();next(3);return false;}
            if(phase==3){
                shot.setPos(parking);if(elapsed<3)return false;target.setInvulnerableTime(0);target.damageCooldownTime=0;
                require(target.hurtServer(f.level,target.damageSources().playerAttack(f.other),1)&&target.getKillCredit()==f.other,"actual future player supersedes old NPC");
                watcher.setTarget(f.other);require(watcher.getTargetUnchecked()==f.other,"actual abandoned-future target differs from the captured historical body");
                f.discard(shot);require(game.items().spellSources().source(shot)==null,"future removal actually forgets the active carrier");
                f.pause();require(game.vanity().requestRestoreForCheck(),"full restore after deleted carrier and changed real combat author");restoring=true;next(4);return false;
            }
            if(phase==4){if(game.vanity().busy())return false;verify();next(5);return false;}
            if(phase==5){verify();if(elapsed<3)return false;game.vanity().stop();shot.setPos(target.position().add(-.2,0,-.2));shot.setDeltaMovement(1.8,0,0);target.setInvulnerableTime(0);target.damageCooldownTime=0;f.thaw();next(6);return false;}
            if(target.getHealth()==hp){require(elapsed<3,"restored native flight really resumes");return false;}
            require(target.getHealth()==hp-7&&target.getLastDamageSource().getEntity()==npc&&target.getKillCredit()==npc,"new post-replay damage still belongs to historical NPC, not future player");return true;
        }
        @Override public void close(){if(closed)return;closed=true;try{game.vanity().stop();game.items().terra().stop();var fresh=new ArrayList<net.minecraft.world.entity.Entity>();for(var e:f.level.getAllEntities())if(!(e instanceof net.minecraft.server.level.ServerPlayer)&&!original.contains(e.getUUID()))fresh.add(e);fresh.forEach(f::discard);}finally{f.close();}}
    }
    public static void run(GameController game){require(running==null,"exclusive scene");running=new Scene(game);try{running.begin();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){var scene=running;if(scene==null)return;try{if(!scene.step())return;scene.close();running=null;SkyWars2.LOGGER.info("[check] PASS historical-npc-replay (real loot NPC, native corpse removal, actual accepted spell/combat graph, full canonical carrier and historical cause replay, newer player rollback, TimeStop hold and resumed native damage)");}catch(Throwable error){try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL historical-npc-replay",error);}}
}
