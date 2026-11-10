package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Long Chef routes observed once per real server tick, sharing the player's exact test terrain. */
final class ChefRouteChecks {
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Native Chef route: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture base;final BotRouteChecks.Kind kind;
        final Vec3 origin=new Vec3(-160.5,120,-160.5);final Set<Long> forced=new HashSet<>();
        Map<BlockPos,BlockState> terrain;List<Vec3> points;Cow victim;LbcWtf.Chef chef;
        Vec3 previous;float yaw,head,body;int at,waypoint,fastTicks,airTicks;double distance;
        Fixture(GameController game,BotRouteChecks.Kind kind){this.game=game;this.kind=kind;base=new WoolBucketChecks.Fixture(game,false);}
        void setup(){
            require(game.items().wtf().pendingCount()==0,"no unrelated Chef or fountain state");base.prepareParticipants();
            base.move(base.owner,origin.add(120,40,120));base.move(base.other,origin.add(124,40,120));
            var center=BlockPos.containing(origin);
            for(int x=(center.getX()-25)>>4;x<=(center.getX()+25)>>4;x++)for(int z=(center.getZ()-25)>>4;z<=(center.getZ()+25)>>4;z++){
                long key=ChunkPos.pack(x,z);if(!base.level.getForceLoadedChunks().contains(key)){forced.add(key);base.level.setChunkForced(x,z,true);}
                base.level.getChunk(x,z);
            }
            terrain=BotRouteChecks.surface(base,origin,kind);points=BotRouteChecks.points(origin,kind);
            victim=EntityTypes.COW.create(base.level,EntitySpawnReason.COMMAND);require(victim!=null,"native fixed victim");
            victim.setNoAi(true);victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);victim.setHealth(1000);
            victim.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);victim.setPos(points.getFirst());
            require(base.level.addFreshEntity(victim),"publish real victim");base.owned.add(victim.getUUID());
            chef=game.items().wtf().spawn(base.owner,victim);require(chef!=null,"actual shared Chef factory");base.owned.add(chef.getUUID());
            chef.setPos(origin.add(-20,kind==BotRouteChecks.Kind.NOISE?1:0,kind==BotRouteChecks.Kind.PARKOUR?0:-20));
            chef.setDeltaMovement(Vec3.ZERO);chef.setOnGround(true);
            previous=chef.position();yaw=chef.getYRot();head=chef.getYHeadRot();body=chef.yBodyRot;at=game.server().getTickCount();
        }
        boolean step(){
            base.client.acknowledge();base.audience.acknowledge();int elapsed=game.server().getTickCount()-at;
            require(!chef.isRemoved()&&chef.target==victim&&victim.isAlive()&&!chef.isNoGravity(),"same live native actor, target and normal gravity");
            require(elapsed<(kind==BotRouteChecks.Kind.NOISE?900:kind==BotRouteChecks.Kind.PARKOUR?320:600),
                    "bounded long route "+kind+" "+chef.position()+" "+chef.brain.diagnostics());
            require(turn(yaw,chef.getYRot())<=25.01&&turn(head,chef.getYHeadRot())<=25.01&&turn(body,chef.yBodyRot)<=25.01,
                    "native camera/body rotations remain bounded at "+elapsed+" yaw="+turn(yaw,chef.getYRot())
                    +" head="+turn(head,chef.getYHeadRot())+" body="+turn(body,chef.yBodyRot));yaw=chef.getYRot();head=chef.getYHeadRot();body=chef.yBodyRot;
            require(chef.getY()>=origin.y-.5,"native route does not fall into gaps: "+chef.position());
            require(chef.position().distanceToSqr(previous)<2.25,"native route contains no rescue teleport");
            double travelled=chef.position().subtract(previous).horizontalDistance();distance+=travelled;previous=chef.position();
            if(travelled>=.31)fastTicks++;if(!chef.onGround())airTicks++;
            if(elapsed%40==0)SkyWars2.LOGGER.info("[check] Native Chef route {} ticks={} waypoint={} distance={} fastTicks={} airTicks={} at={} {}",
                    kind,elapsed,waypoint,distance,fastTicks,airTicks,chef.position(),chef.brain.diagnostics());
            if(chef.position().subtract(points.get(waypoint)).horizontalDistanceSqr()<1.8*1.8){
                if(waypoint+1<points.size()){victim.setPos(points.get(++waypoint));}
                else if(victim.getHealth()<1000){
                    require(victim.getKillCredit()==base.owner,"accepted physical Chef hit keeps summon credit");
                    for(var entry:terrain.entrySet())require(base.level.getBlockState(entry.getKey()).equals(entry.getValue()),"walkable native route needs no terrain edits");
                    if(kind==BotRouteChecks.Kind.FLAT)require(distance>=145&&fastTicks>=200&&airTicks>=120,"long native sprint-jump throughput");
                    SkyWars2.LOGGER.info("[check] Native Chef route completed {} ticks={} distance={} fastTicks={} airTicks={}",kind,elapsed,distance,fastTicks,airTicks);return true;
                }
            }
            return false;
        }
        static float turn(float before,float after){return Math.abs(net.minecraft.util.Mth.wrapDegrees(after-before));}
        @Override public void close(){
            try{game.items().wtf().forget(base.owner);}
            finally{try{base.close();}finally{for(long key:forced)base.level.setChunkForced(ChunkPos.getX(key),ChunkPos.getZ(key),false);}}
        }
    }
    static void run(GameController game,String name){
        require(running==null,"one actual native route");running=new Fixture(game,BotRouteChecks.Kind.valueOf(name.toUpperCase(Locale.ROOT)));
        try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}
    }
    static void tick(GameController game){
        if(running==null)return;var test=running;String suite="chef-route-"+test.kind.name().toLowerCase(Locale.ROOT);
        try{if(!test.step())return;test.close();running=null;SkyWars2.LOGGER.info("[check] PASS {} (actual full server ticks, native chase and unchanged shared terrain)",suite);}
        catch(Throwable error){try{test.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL "+suite,error);}
    }
}
