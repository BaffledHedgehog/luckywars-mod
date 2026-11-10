package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Real Terra producers, private state and Polymer models across the whole native replay coordinator. */
public final class TerraReplayChecks {
    private TerraReplayChecks(){}
    private static Scene running;
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException("Terra replay: "+detail);}
    private static Object read(Object value,String name){return WoolBucketChecks.read(value,name);}
    private record Image(LbcTerra.TerraProjectile body,LbcTerra.Kind kind,int age,Vec3 position,Vec3 motion){}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final LbcTerra terra;final int started;
        final List<Image> saved=new ArrayList<>();final Set<UUID> original=new HashSet<>();
        int phase,when,resumed;boolean restoring,closed;Vec3 origin;Object oldModel;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,true);terra=game.items().terra();started=game.server().getTickCount();f.level.getAllEntities().forEach(e->original.add(e.getUUID()));}
        void next(int value){phase=value;when=game.server().getTickCount();}
        void begin(){
            f.prepareParticipants();require(terra.count()==0&&terra.finishingModels()==0,"exclusive native engine");origin=f.home.add(0,35,0);
            tailReplay();
            for(var kind:LbcTerra.Kind.values()){
                var shot=terra.castAt(f.owner,origin.add(kind.ordinal()*2,0,0),new Vec3(0,1,0),kind,0);
                require(shot!=null&&!shot.isRemoved(),"real producer "+kind);f.own(shot);
            }
            require(terra.count()==LbcTerra.Kind.values().length,"all six native variants live");next(0);
        }
        void tailReplay(){
            var terminal=terra.castAt(f.owner,origin,new Vec3(0,1,0),LbcTerra.Kind.TRUE_BEAM,1);f.own(terminal);
            terra.tick();require(terminal.isRemoved()&&terra.count()==0&&terra.finishingModels()==1,"real terminal produces a detached two-frame model");
            var links=new MatchSpellCheckpoint.Links(new MatchDamageSources(game,MatchActorCheckpoint.currentBodies(game).values()));
            var point=terra.checkpoint(links);require(links.resolve(id->f.level.getEntity(id)).isEmpty(),"tail needs no resurrection of its removed gameplay body");
            var old=read(terra,"finishingModels");var tail=((Deque<?>)old).getFirst();var tailHolder=(eu.pb4.polymer.virtualentity.api.ElementHolder)read(tail,"holder");var offset=tailHolder.getElements().getFirst().getOffset();
            boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
            try{terra.prepareFrozen();require(terra.finishingModels()==0,"future tail detached");point.prepare(links).run();point.publish(links);
                require(terra.count()==0&&terminal.isRemoved()&&terra.finishingModels()==1,"tail replay never resurrects or replays terminal gameplay");
                var restored=((Deque<?>)read(terra,"finishingModels")).getFirst();require(read(restored,"holder")!=tailHolder&&read(restored,"position").equals(terminal.position()),"new virtual tail at exact native impact");
                require(((eu.pb4.polymer.virtualentity.api.ElementHolder)read(restored,"holder")).getElements().getFirst().getOffset().equals(offset),"preserve the terminal display prediction offset as well as native impact");
                terra.tick();require(terra.finishingModels()==1,"tail keeps first remaining frame");terra.tick();require(terra.finishingModels()==0,"tail expires at exact second frame");
            }finally{game.server().tickRateManager().setFrozen(frozen);}
        }
        void capture(){
            f.pause();terra.sourceCharges(f.owner,3);
            for(var shot:terra.transientSnapshot())saved.add(new Image(shot,(LbcTerra.Kind)read(shot,"kind"),(Integer)read(shot,"age"),shot.position(),shot.getDeltaMovement()));
            require(saved.size()==6&&saved.stream().allMatch(i->i.age>0),"each real variant has a partially lived flight");
            oldModel=saved.getFirst().body.display();require(game.vanity().requestCapture(f.owner,false),"whole coordinator accepts all Terra variants");next(1);
        }
        void verify(){
            require(game.items().timeStop().stopped(f.level)&&terra.count()==6&&terra.charges(f.owner)==3,"private active set, sword resources and TimeStop restored");
            for(var image:saved){var shot=image.body;require(f.level.getEntity(shot.getUUID())==shot&&!shot.isRemoved()&&shot.position().equals(image.position)&&shot.getDeltaMovement().equals(image.motion),"same canonical native pose and motion "+image.kind);
                require(read(shot,"kind")==image.kind&&(Integer)read(shot,"age")==image.age&&shot.getOwner()==f.owner,"kind, real age and canonical native owner "+image.kind);
                var display=shot.display();require(display!=null&&display.getItem().get(DataComponents.ITEM_MODEL).equals(image.kind.model),"exact kind model restored "+image.kind);
            }
            var tracked=(Map<?,?>)read(game.items().lifetimes(),"tracked");
            for(var image:saved)require(tracked.containsKey(image.body)==(image.kind.lifetime==0),"shared bounded lifetime review membership "+image.kind);
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();require(!game.vanity().status().startsWith("FAILED"),"coordinator "+game.vanity().status());require(game.playing()&&game.server().getTickCount()-started<900,"bounded phase "+phase);
            if(restoring&&game.vanity().busy())for(var player:game.server().getPlayerList().getPlayers())if(player.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            int elapsed=game.server().getTickCount()-when;
            if(phase==0){if(elapsed<2)return false;capture();return false;}
            if(phase==1){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;verify();f.thaw();next(2);return false;}
            if(phase==2){if(elapsed<2)return false;for(var image:saved)f.discard(image.body);terra.sourceCharges(f.owner,0);
                var future=terra.castAt(f.owner,origin,new Vec3(0,1,0),LbcTerra.Kind.REGULAR,0);f.own(future);require(terra.count()==1,"abandoned future differs from saved six");
                f.pause();require(game.vanity().requestRestoreForCheck(),"native full-world restore");restoring=true;next(3);return false;}
            if(phase==3){if(game.vanity().busy())return false;verify();require(saved.getFirst().body.display()!=oldModel,"virtual model republished without a second gameplay entity");next(4);return false;}
            if(phase==4){verify();if(elapsed<3)return false;game.vanity().stop();f.thaw();next(5);return false;}
            resumed++;
            for(var image:saved){int age=image.age+resumed;var shot=image.body;
                if(image.kind==LbcTerra.Kind.RAZOR&&age>=image.kind.lifetime){require(shot.isRemoved(),"Razor terminates at original age10");continue;}
                require(!shot.isRemoved()&&(Integer)read(shot,"age")==age&&read(shot,"kind")==image.kind,"exact resumed native flight cadence "+image.kind+" age="+age);
            }
            var razor=saved.stream().filter(i->i.kind==LbcTerra.Kind.RAZOR).findFirst().orElseThrow();int after=resumed+razor.age-razor.kind.lifetime;
            if(after<0)return false;require(terra.count()==5,"only original Razor lifetime has ended");
            require(terra.finishingModels()==(after<2?1:0),"terminal tail original two-frame deadline after replay");return after>=2;
        }
        @Override public void close(){if(closed)return;closed=true;try{game.vanity().stop();terra.stop();var fresh=new ArrayList<Entity>();for(var e:f.level.getAllEntities())if(!(e instanceof net.minecraft.server.level.ServerPlayer)&&!original.contains(e.getUUID()))fresh.add(e);fresh.forEach(f::discard);}finally{f.close();}}
    }
    public static void run(GameController game){require(running==null,"exclusive scene");running=new Scene(game);try{running.begin();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){var scene=running;if(scene==null)return;try{if(!scene.step())return;scene.close();running=null;SkyWars2.LOGGER.info("[check] PASS terra-replay (six real producer variants, partial native ages/poses/owners, full coordinator replay, private sword resources and bounded lifetime registry, TimeStop, new virtual model publication, exact Razor expiry and two-frame detached terminal)");}catch(Throwable error){try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL terra-replay",error);}}
}
