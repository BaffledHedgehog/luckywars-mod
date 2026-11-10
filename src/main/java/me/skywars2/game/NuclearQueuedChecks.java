package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real queued32-source payload, disconnected terrain, pause and typed continuation. */
final class NuclearQueuedChecks {
    private NuclearQueuedChecks(){}
    private static Fixture running;
    private static void require(boolean condition,String why){if(!condition)throw new IllegalStateException("Queued nuclear: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final boolean invulnerable;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();final List<BlockPos> core=new ArrayList<>(),far=new ArrayList<>();
        final Set<Long> forced=new HashSet<>();Vec3 at;BlockPos bedrock;Mob victim;int started,pausedAt=-1;boolean armed,closed;
        Fixture(GameController game){this.game=game;require(game.items().exasperato().pendingBlasts()==0,"exclusive queue");
            base=new ElementalGrimoireChecks.Fixture(game,true);base.retain=true;invulnerable=base.owner.getAbilities().invulnerable;}
        void set(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
            blocks.putIfAbsent(pos.immutable(),game.maps().snapshot(base.owner.level(),pos));LbcDropBlocks.setBlock(base.owner.level(),pos,state,true);
        }
        void setup(){
            base.initialize();base.owner.getAbilities().invulnerable=true;at=base.owner.position().add(4,0,0);var center=BlockPos.containing(at);var level=base.owner.level();
            for(int x=(center.getX()-8)>>4;x<=(center.getX()+32)>>4;x++)for(int z=(center.getZ()-8)>>4;z<=(center.getZ()+8)>>4;z++){
                long key=net.minecraft.world.level.ChunkPos.pack(x,z);if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(x,z,true);}level.getChunk(x,z);
            }
            for(int offset:new int[]{0,28})for(var pos:BlockPos.betweenClosed(center.offset(offset-2,-8,-2),center.offset(offset+2,-1,2))){
                require(level.getBlockState(pos).isAir(),"real empty high-altitude island frame");set(pos,Blocks.STONE.defaultBlockState());(offset==0?core:far).add(pos.immutable());
            }
            bedrock=center.offset(28,-4,0);set(bedrock,Blocks.BEDROCK.defaultBlockState());far.remove(bedrock);
            victim=EntityTypes.ZOMBIE.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);require(victim!=null,"native victim");
            victim.setPos(at);victim.setNoAi(true);victim.setNoGravity(true);victim.setSilent(true);require(level.addFreshEntity(victim),"native victim publication");
            started=game.server().getTickCount();base.recording.packets.clear();
        }
        long packets(){return base.recording.packets.stream().filter(ClientboundExplodePacket.class::isInstance).count();}
        boolean step(){
            int now=game.server().getTickCount();require(now-started<140,"bounded real queued continuation");var level=base.owner.level();
            if(!armed){
                require(level.getEntities((Entity)null,victim.getBoundingBox().inflate(1)).contains(victim),"victim crossed native spatial boundary");
                game.items().exasperato().exopersoBurst(base.owner,at,null);
                require(!victim.isAlive()&&packets()==1&&game.items().exasperato().pendingBlasts()==31,
                        "first native lethal wave is synchronous and unique: victimHP="+victim.getHealth()+" packets="+packets()+" remainingSources="+game.items().exasperato().pendingBlasts());
                var actors=new ArrayList<Entity>();level.getAllEntities().forEach(actors::add);
                var graph=new MatchBlastCheckpoint.Graph(new MatchDamageSources(game,actors));var image=game.items().exasperato().checkpointBlasts(graph);
                require(graph.links.resolve(level::getEntity).isEmpty(),"typed author graph resolves");
                game.items().exasperato().prepareBlastCheckpoint();graph.prepare().run();image.prepare(graph.links).run();
                require(packets()==1&&game.items().exasperato().pendingBlasts()==31,"typed replay does not repeat accepted first damage");
                game.items().timeStop().begin(level,100);pausedAt=now;armed=true;return false;
            }
            if(pausedAt>=0){
                require(packets()==1&&core.stream().allMatch(p->level.getBlockState(p).is(Blocks.STONE))
                        &&far.stream().allMatch(p->level.getBlockState(p).is(Blocks.STONE)),"TimeStop pauses terrain and all remaining native sources");
                if(now-pausedAt<5)return false;game.items().timeStop().end(level);pausedAt=-1;return false;
            }
            if(game.items().exasperato().pendingBlasts()!=0)return false;
            require(packets()==32,"all32 actual native explosion packets, with no duplicated first source: "+packets());
            require(core.stream().allMatch(p->level.getBlockState(p).isAir()),"queued blast has a real empty core");
            long removed=far.stream().filter(p->level.getBlockState(p).isAir()).count();
            require(removed>0&&removed<far.size(),"native air propagation reaches the disconnected28-block island with a surviving edge: "+removed);
            require(level.getBlockState(bedrock).is(Blocks.BEDROCK),"native resistance retains bedrock");
            SkyWars2.LOGGER.info("[check] Queued nuclear nativePackets={} realTicks={} farIslandRemoved={}/{}",packets(),now-started,removed,far.size());return true;
        }
        public void close(){if(closed)return;closed=true;var level=base.owner.level();game.items().timeStop().end(level);game.items().exasperato().stop();
            var spawned=new ArrayList<Entity>();for(var entity:level.getAllEntities())if(!base.before.contains(entity)&&!(entity instanceof net.minecraft.server.level.ServerPlayer))spawned.add(entity);
            spawned.forEach(Entity::discard);blocks.forEach((pos,snapshot)->game.maps().restoreSnapshot(level,pos,snapshot));
            for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
            base.owner.getAbilities().invulnerable=invulnerable;base.close();
        }
    }
    static void run(GameController game){require(running==null,"one fixture");var next=new Fixture(game);try{next.setup();running=next;}catch(Throwable error){next.close();throw error;}}
    static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS nuclear-queued (actual32 native sources, synchronous lethal, disconnected terrain, TimeStop, typed replay and resistance)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL nuclear-queued",error);}}
}
