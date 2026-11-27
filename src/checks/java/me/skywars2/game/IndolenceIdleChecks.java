package me.skywars2.game;

import me.skywars2.SkyWars2;

/** Real per-second movement accounting; no synthetic server clock or rewritten production timer. */
public final class IndolenceIdleChecks {
    private IndolenceIdleChecks(){}
    private static State running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Indolence idle: "+why);}
    private static final class State implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final int offset,oldTime,oldMoved;
        int stage,started,previous,lastBoundary;
        State(GameController game){
            this.game=game;f=new WoolBucketChecks.Fixture(game,false);f.prepareParticipants();
            try{var field=game.items().shishEncounter().getClass().getDeclaredField("cadenceOffset");field.setAccessible(true);offset=field.getInt(game.items().shishEncounter());}
            catch(ReflectiveOperationException failure){f.close();throw new IllegalStateException(failure);}
            var mana=game.items().mana();oldTime=mana.score(f.owner,"sleep_time").get();oldMoved=mana.score(f.owner,"sleep_moved").get();
            f.move(f.owner,f.home.add(0,60,0));f.owner.setNoGravity(true);game.items().shishEncounter().seedMovement(f.owner);
            mana.score(f.owner,"sleep_time").set(1000);previous=1000;started=game.server().getTickCount();lastBoundary=started;
        }
        boolean tick(){
            f.client.acknowledge();f.audience.acknowledge();int now=game.server().getTickCount();require(now-started<80,"bounded real second accounting");
            int value=game.items().mana().score(f.owner,"sleep_time").get();
            if(now==started)return false;
            if(Math.floorMod(now+offset,20)!=0){require(value==previous,"subsecond displacement never immediately clears saved idle progress");return false;}
            require(now!=lastBoundary,"one check per native second");lastBoundary=now;
            if(stage==0){require(value==1020,"one idle second adds exactly twenty source ticks");f.move(f.owner,f.owner.position().add(.1,0,0));stage=1;}
            else if(stage==1){require(value==820,"a second containing movement removes exactly ten saved seconds");stage=2;}
            else {require(value==840,"idle progress resumes at one second per second");return true;}
            previous=value;return false;
        }
        public void close(){try{game.items().mana().score(f.owner,"sleep_time").set(oldTime);game.items().mana().score(f.owner,"sleep_moved").set(oldMoved);}finally{f.close();}}
    }
    public static void run(GameController game){require(running==null,"exclusive native fixture");running=new State(game);}
    public static void tick(GameController game){if(running==null)return;try{if(!running.tick())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS indolence-idle (native second clock, retained subsecond progress, ten-second movement penalty)");}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}throw error;}}
}
