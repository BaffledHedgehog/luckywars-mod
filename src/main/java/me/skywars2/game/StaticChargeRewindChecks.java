package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

/** Whole native replay of a cloud, a partially lived visual, and an accepted pending discharge. */
public final class StaticChargeRewindChecks {
    private StaticChargeRewindChecks(){}
    private static Scene running;
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException("Static charge replay: "+detail);}
    private static final class Scene implements AutoCloseable {
        final GameController game;
        final WoolBucketChecks.Fixture f;
        final LbcStaticCharge service;
        final int started;
        AreaEffectCloud cloud;
        Display.ItemDisplay beam;
        ItemStack picture;
        int phase,when,ttl,cloudAge;
        float ownerHp,otherHp;
        boolean restoring,closed;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,true);service=game.items().legacyEffects().staticCharge();started=game.server().getTickCount();}
        LbcStaticCharge.Checkpoint view(){
            var actors=new ArrayList<Entity>();f.level.getAllEntities().forEach(actors::add);
            return service.checkpoint(new MatchSpellCheckpoint.Links(new MatchDamageSources(game,actors)));
        }
        boolean waiting(Entity body){return view().waiting.stream().anyMatch(ref->ref.id().equals(body.getUUID()));}
        int remaining(Display.ItemDisplay display){return view().rays.entrySet().stream().filter(e->e.getKey().id().equals(display.getUUID())).mapToInt(Map.Entry::getValue).findFirst().orElse(0);}
        int charge(Entity body){return game.items().mana().hudValue(body,LbcStaticCharge.COUNTER);}
        void next(int value){phase=value;when=game.server().getTickCount();}
        void hit(net.minecraft.server.level.ServerPlayer victim,net.minecraft.server.level.ServerPlayer attacker){
            victim.setInvulnerableTime(0);victim.damageCooldownTime=0;
            require(victim.hurtServer(f.level,victim.damageSources().playerAttack(attacker),1),"real native accepted hit queues discharge");
            victim.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
        void begin(){
            f.prepareParticipants();var before=view();require(before.live.isEmpty()&&before.waiting.isEmpty()&&before.fields.isEmpty()&&before.rays.isEmpty(),"exclusive original status authority");
            f.move(f.owner,f.home.add(0,40,0));f.move(f.other,f.owner.position().add(12,0,0));
            cloud=new AreaEffectCloud(EntityTypes.AREA_EFFECT_CLOUD,f.level);cloud.setPos(f.owner.position());
            cloud.setPotionContents(PotionContents.EMPTY.withEffectAdded(new MobEffectInstance(MobEffects.RAID_OMEN,200,126)));f.own(cloud);
            require(f.level.addFreshEntity(cloud)&&cloud.getDuration()==300&&cloud.getRadius()==5,"real registered marker126 cloud");next(0);
        }
        void saved(){
            var image=view();require(charge(f.owner)==0&&charge(f.other)==9&&waiting(f.other),"pending accepted charge before full capture");
            require(image.live.stream().anyMatch(ref->ref.id().equals(f.other.getUUID()))&&image.fields.stream().anyMatch(ref->ref.id().equals(cloud.getUUID())),"live charged body and actual cloud tracked");
            require(image.rays.size()==1,"one live original discharge visual");var entry=image.rays.entrySet().iterator().next();
            beam=(Display.ItemDisplay)f.level.getEntity(entry.getKey().id());ttl=entry.getValue();require(beam!=null&&ttl>0&&ttl<8,"original arc has a real partial active lifetime");
            picture=beam.getItemStack().copy();cloudAge=cloud.tickCount;ownerHp=f.owner.getHealth();otherHp=f.other.getHealth();
        }
        void verify(){
            require(f.level.getEntity(cloud.getUUID())==cloud&&!cloud.isRemoved()&&cloud.tickCount==cloudAge&&cloud.getDuration()==300,"canonical native cloud identity and partial age restored");
            require(f.level.getEntity(beam.getUUID())==beam&&!beam.isRemoved()&&remaining(beam)==ttl&&ItemStack.matches(picture,beam.getItemStack()),"same native arc body/model and exact remaining lifetime restored");
            DisplayTransformChecks.pose(beam,0,0,0,3,3,(float)-f.owner.getEyePosition().distanceTo(f.other.getEyePosition()));
            require(charge(f.owner)==0&&charge(f.other)==9&&waiting(f.other),"charged and waiting collections rebound to restored bodies");
            require(f.owner.getHealth()==ownerHp&&f.other.getHealth()==otherHp&&game.items().timeStop().stopped(f.level),"original HP and TimeStop restored without an early discharge");
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-started<900&&game.playing(),"bounded scene phase="+phase);
            if(restoring&&game.vanity().busy())for(var player:game.server().getPlayerList().getPlayers())if(player.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            int elapsed=game.server().getTickCount()-when;
            if(phase==0){
                if(charge(f.owner)==0){require(elapsed<20,"native cloud applies on its own scheduled tick");return false;}
                require(charge(f.owner)==10&&charge(f.other)==0&&!f.owner.hasEffect(MobEffects.RAID_OMEN),"real cloud gives ten charge only to its contact");
                cloud.setPos(f.owner.position().add(0,16,0));f.move(f.other,f.owner.position().add(4,0,0));hit(f.owner,f.other);require(waiting(f.owner),"native damage callback queued the first charge");next(1);return false;
            }
            if(phase==1){
                if(view().rays.isEmpty()){require(elapsed<5,"native shared scheduler produces the first actual arc");return false;}
                require(charge(f.other)==9&&f.other.getHealth()==994,"actual first discharge deals six and transfers nine");
                hit(f.other,f.owner);f.pause();saved();require(game.vanity().requestCapture(f.owner,false),"full coordinator captures pending charge, cloud and arc");next(2);return false;
            }
            if(phase==2){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;verify();f.thaw();next(3);return false;}
            if(phase==3){
                if(elapsed<12)return false;
                require(beam.isRemoved()&&cloud.tickCount>cloudAge&&charge(f.other)==0&&charge(f.owner)==8&&!waiting(f.other)&&!waiting(f.owner),"future timeline actually expired the arc, advanced cloud, and settled pending charge: beamRemoved="+beam.isRemoved()+" ttl="+remaining(beam)+" cloud="+cloud.tickCount+" savedCloud="+cloudAge+" charges="+charge(f.owner)+","+charge(f.other)+" waiting="+waiting(f.other)+" nativeRunning="+game.server().tickRateManager().runsNormally()+" timeStop="+game.items().timeStop().stopped(f.level));
                service.clear(f.owner);service.clear(f.other);f.discard(cloud);f.pause();require(game.vanity().requestRestoreForCheck(),"full restore after future native removal and changed counters");restoring=true;next(4);return false;
            }
            if(phase==4){if(game.vanity().busy())return false;verify();next(5);return false;}
            if(phase==5){verify();if(elapsed<3)return false;game.vanity().stop();f.thaw();next(6);return false;}
            if(phase==6){
                require(charge(f.owner)==8&&charge(f.other)==0,"one externally queued discharge transfers eight without self-triggered echoes");
                require(!waiting(f.owner)&&!waiting(f.other),"chain damage never enqueues its own recipients");
                require(f.owner.getHealth()==ownerHp-6&&f.other.getHealth()==otherHp,"exactly one resumed six-damage discharge without replay duplicates");
                require(remaining(beam)==Math.max(0,ttl-elapsed)&&beam.isRemoved()==(elapsed>=ttl),"original arc retires at its restored active deadline");
                if(elapsed<Math.max(ttl+2,10))return false;
                f.move(f.other,f.owner.position().add(16,0,0));cloud.setPos(f.owner.position());next(7);return false;
            }
            if(charge(f.owner)!=10){require(elapsed<15,"restored native cloud contact resumes its own reapplication");return false;}
            require(!cloud.isRemoved()&&!f.owner.hasEffect(MobEffects.RAID_OMEN),"restored cloud still delivers the custom marker instead of vanilla omen");return true;
        }
        @Override public void close(){if(closed)return;closed=true;try{game.vanity().stop();service.stop();if(cloud!=null&&!cloud.isRemoved())f.discard(cloud);}finally{f.close();}}
    }
    public static void run(GameController game){require(running==null,"exclusive scene");running=new Scene(game);try{running.begin();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){var scene=running;if(scene==null)return;try{if(!scene.step())return;scene.close();running=null;SkyWars2.LOGGER.info("[check] PASS static-charge-rewind (actual marker126 cloud, accepted pending hit, whole coordinator native body/counter/arc lifetime replay, TimeStop hold, exact bounded native damage cascade and cloud reapplication)");}catch(Throwable error){try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL static-charge-rewind",error);}}
}
