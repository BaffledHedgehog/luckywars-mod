package me.skywars2.game;

import java.util.*;
import java.lang.reflect.Field;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Long actual native movement: intent-only waypoints, existing surfaces and no motion injection. */
final class BotRouteChecks {
    enum Kind{FLAT,NOISE,PARKOUR,NARROW,LEDGE}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Bot route: "+why);}
    static Map<BlockPos,BlockState> surface(WoolBucketChecks.Fixture base,Vec3 origin,Kind kind){
        var cell=BlockPos.containing(origin);var terrain=new LinkedHashMap<BlockPos,BlockState>();
        for(int x=-24;x<=24;x++)for(int z=-24;z<=24;z++)for(int y=kind==Kind.LEDGE?-2:-1;y<=4;y++){
            var state=y==-1?Blocks.STONE.defaultBlockState():y==0&&kind==Kind.NOISE?
                    ChefNavigationChecks.noisyState(x,z):Blocks.AIR.defaultBlockState();
            if(y==-1&&kind==Kind.PARKOUR&&(x>=-13&&x<=-10||x>=0&&x<=2||x>=11&&x<=14))state=Blocks.AIR.defaultBlockState();
            if(y==0&&kind==Kind.NOISE&&Math.abs(x)==20&&Math.abs(z)==20)state=Blocks.STONE.defaultBlockState();
            if(kind==Kind.NARROW||kind==Kind.LEDGE){
                boolean island=x==0&&z==0||x==-4&&z==0||x==-4&&z==4||x==0&&z==4||x==4&&z==4||x==4&&z==0;
                boolean wall=x==1&&Math.abs(z)<=2||z==1&&Math.abs(x)<=2;
                state=y==4||y>=0&&wall?Blocks.BEDROCK.defaultBlockState():
                        y==-1&&island||kind==Kind.LEDGE&&y==-2&&x==-1&&z==0?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState();
            }
            var pos=cell.offset(x,y,z);base.set(pos,state);terrain.put(pos,state);
        }
        return terrain;
    }
    static List<Vec3> points(Vec3 origin,Kind kind){
        if(kind==Kind.NARROW||kind==Kind.LEDGE)return List.of(origin.add(4,0,0));
        int up=kind==Kind.NOISE?1:0;
        return kind==Kind.PARKOUR?List.of(origin.add(20,0,0)):
                List.of(origin.add(20,up,-20),origin.add(20,up,20),origin.add(-20,up,20),origin.add(-20,up,-20));
    }
    private static final class Fixture extends BotStarterChecks.Fixture {
        final Kind kind;final Map<BlockPos,BlockState> terrain=new LinkedHashMap<>();final Map<String,Field> access=new HashMap<>();
        List<Vec3> points;Vec3 previous;int at,waypoint,fastTicks,airTicks;double distance;float lastPitch;boolean started;
        Fixture(GameController game,Kind kind){super(game);this.kind=kind;}
        @Override void setup(){super.setup();at=game.server().getTickCount();}
        void begin(){
            terrain.putAll(surface(base,origin,kind));
            int up=kind==Kind.NOISE?1:0;
            var start=kind==Kind.NARROW||kind==Kind.LEDGE?origin:origin.add(-20,up,kind==Kind.PARKOUR?0:-20);resetStarterBody(start);
            if(kind==Kind.NARROW||kind==Kind.LEDGE)for(int slot=0;slot<36;slot++)if(bot.getInventory().getItem(slot).getItem() instanceof net.minecraft.world.item.BlockItem)
                bot.getInventory().setItem(slot,net.minecraft.world.item.ItemStack.EMPTY);
            points=points(origin,kind);
            previous=bot.position();lastYaw=bot.getYRot();lastPitch=bot.getXRot();at=game.server().getTickCount();started=true;
        }
        void set(Object state,String name,Object value){
            try{var field=access.get(name);if(field==null){field=state.getClass().getDeclaredField(name);field.setAccessible(true);access.put(name,field);}field.set(state,value);}
            catch(ReflectiveOperationException error){throw new IllegalStateException("Native route intent",error);}
        }
        void intent(){
            var state=state();if(state==null)return;
            set(state,"startingLoot",false);set(state,"nextScan",Integer.MAX_VALUE);set(state,"nextRoam",Integer.MAX_VALUE);set(state,"chest",null);
            ((Collection<?>)WoolBucketChecks.read(state,"chests")).clear();
            set(state,"goal",points.get(waypoint));
        }
        @Override boolean step(){
            base.client.acknowledge();base.audience.acknowledge();int elapsed=game.server().getTickCount()-at;
            require(bot.isAlive()&&game.server().getPlayerList().getPlayer(bot.getUUID())==bot,"same ordinary body follows the whole route "+kind);
            if(!started){require(elapsed<1200,"bounded real starter preparation");if(!prepareKit())return false;begin();return false;}
            require(elapsed<(kind==Kind.NOISE?900:kind==Kind.PARKOUR?320:600),"long native route deadline "+kind+" "+game.carpetBots().diagnostics(bot));
            require(Math.abs(net.minecraft.util.Mth.wrapDegrees(bot.getYRot()-lastYaw))<=12.01&&Math.abs(bot.getXRot()-lastPitch)<=8.01,"actual camera turns stay bounded each native tick");
            lastYaw=bot.getYRot();lastPitch=bot.getXRot();require(!bot.isNoGravity(),"native gravity stays enabled on the whole route");
            require(bot.getY()>=origin.y-.5,"route never falls through a gap "+kind+" "+game.carpetBots().diagnostics(bot));
            double travelled=bot.position().subtract(previous).horizontalDistance();
            require(bot.position().distanceToSqr(previous)<2.25,"native movement contains no teleport "+kind);
            previous=bot.position();distance+=travelled;if(travelled>=.31)fastTicks++;if(!bot.onGround())airTicks++;
            if(elapsed%40==0)SkyWars2.LOGGER.info("[check] Native route {} ticks={} waypoint={} distance={} fastTicks={} airTicks={} {}",kind,elapsed,waypoint,distance,fastTicks,airTicks,game.carpetBots().diagnostics(bot));
            if(bot.position().subtract(points.get(waypoint)).horizontalDistanceSqr()<1.8*1.8&&(!(kind==Kind.NARROW||kind==Kind.LEDGE)||bot.onGround())){
                if(++waypoint==points.size()){
                    require(game.carpetBots().observation(bot).placements()==0,"existing walkable/parkour terrain needs no constructed bypass "+kind);
                    for(var entry:terrain.entrySet())require(base.level.getBlockState(entry.getKey()).equals(entry.getValue()),"native route preserves the existing surface "+kind);
                    if(kind==Kind.FLAT)require(distance>=145&&fastTicks>=200&&airTicks>=120,"long flat route sustains native sprint-jump movement");
                    SkyWars2.LOGGER.info("[check] Native route completed {} ticks={} distance={} fastTicks={} airTicks={}",kind,elapsed,distance,fastTicks,airTicks);return true;
                }
                var state=state();if(state!=null)((LbcChefBrain)WoolBucketChecks.read(state,"brain")).reset();
            }
            intent();return false;
        }
    }
    static void run(GameController game,String name){
        require(running==null,"exclusive native route");running=new Fixture(game,Kind.valueOf(name.toUpperCase(Locale.ROOT)));
        try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}
    }
    static void tick(GameController game){
        if(running==null)return;String name=running.kind.name().toLowerCase(Locale.ROOT);
        try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS bot-route-{} (native full-tick player controls, long existing terrain, bounded camera and no construction or teleport)",name);}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL bot-route-"+name,error);}
    }
}
