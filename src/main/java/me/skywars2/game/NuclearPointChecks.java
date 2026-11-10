package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Actual point payload: source AIR masks, resistant materials, native lower lobe and TimeStop. */
public final class NuclearPointChecks {
    private NuclearPointChecks(){}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Point nuclear: "+why);}
    public static void run(GameController game){require(running==null,"exclusive fixture");var next=new Fixture(game);
        try{next.initialize();running=next;}catch(Throwable error){next.close();throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.check())return;running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS nuclear-point (distinct12/13 source masks, actual24wave payload, lower native lobe, AIR ignores resistance, native bedrock survives outside masks, TimeStop)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL nuclear-point",error);}}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final boolean invulnerable;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();final List<BlockPos> resistant=new ArrayList<>();
        final Set<Long> forced=new HashSet<>();
        Vec3 at;BlockPos lobe,protectedBedrock;int start,thaw=-1;long waves;boolean closed;
        Fixture(GameController game){this.game=game;require(game.items().nukeEvent().jobCount()==0,"exclusive point jobs");
            base=new ElementalGrimoireChecks.Fixture(game,true);base.retain=true;invulnerable=base.owner.getAbilities().invulnerable;}
        void set(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
            require(base.owner.level().hasChunkAt(pos),"resident owned test cell");blocks.putIfAbsent(pos.immutable(),game.maps().snapshot(base.owner.level(),pos));
            LbcDropBlocks.setBlock(base.owner.level(),pos,state,true);
        }
        void initialize(){base.initialize();base.owner.getAbilities().invulnerable=true;at=Vec3.atCenterOf(base.owner.blockPosition());
            // This scenario promises a complete24-source resident payload. Random
            // island spawns do not guarantee all distant cardinal chunks are loaded.
            int loaded=0;
            for(int radius:new int[]{39,18})for(int y:new int[]{0,40,-40})for(var offset:List.of(new Vec3(radius,y,0),new Vec3(-radius,y,0),new Vec3(0,y,radius),new Vec3(0,y,-radius))){
                var pos=BlockPos.containing(at.add(offset));if(!base.owner.level().hasChunkAt(pos))loaded++;
                long key=net.minecraft.world.level.ChunkPos.pack(pos.getX()>>4,pos.getZ()>>4);
                if(!base.owner.level().getForceLoadedChunks().contains(key)){forced.add(key);base.owner.level().setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);}
                base.owner.level().getChunkAt(pos);require(base.owner.level().hasChunkAt(pos),"all24 native origins resident before activation");
            }
            SkyWars2.LOGGER.info("[check] nuclear-point prepared actual24-source footprint newlyLoadedOrigins={}",loaded);
            var point=NuclearTerrain.point(at,0,false,41);var marker=NuclearTerrain.point(at,0,true,41);
            require(point.power==25&&point.cubes.size()==12&&marker.power==16&&marker.cubes.size()==13,"source profiles are not one shared sphere");
            var center=BlockPos.containing(at);
            for(var pos:BlockPos.betweenClosed(center.offset(-14,-15,-14),center.offset(14,0,14)))set(pos,Blocks.STONE.defaultBlockState());
            for(int x=-5;x<=5;x+=2)for(int z=-5;z<=5;z+=2){var pos=center.offset(x,-8,z);set(pos,Blocks.BEDROCK.defaultBlockState());resistant.add(pos);}
            lobe=center.offset(18,-40,0);for(var pos:BlockPos.betweenClosed(lobe.offset(-2,-2,-2),lobe.offset(2,2,2)))set(pos,Blocks.STONE.defaultBlockState());
            protectedBedrock=center.offset(0,-30,0);set(protectedBedrock,Blocks.BEDROCK.defaultBlockState());
            nativeReference();waves=game.items().nukeEvent().blasts();
            require(game.items().nukeEvent().pointBurst(base.owner,base.owner.level(),at),"actual ordinary point producer accepted");
            require(game.items().nukeEvent().blasts()==waves+1,"existing synchronous damage wave is retained");
            game.items().timeStop().begin(base.owner.level(),100);start=game.server().getTickCount();
        }
        void nativeReference(){
            String property="skywars2.disableExplosionCache",previous=System.getProperty(property);var level=base.owner.level();
            try{
                var constructor=Arrays.stream(Class.forName("me.skywars2.game.LbcNukeEvent$BlastSource").getDeclaredConstructors()).filter(c->c.getParameterCount()==3).findFirst().orElseThrow();constructor.setAccessible(true);
                var calculate=net.minecraft.world.level.ServerExplosion.class.getDeclaredMethod("calculateExplodedPositions");calculate.setAccessible(true);
                var source=(Entity)constructor.newInstance(level,null,true);require(LbcNukeEvent.cacheableNativeBlast(source),"actual reviewed marker source");
                for(float power:new float[]{16,25})for(long seed:new long[]{47,991,917458}){
                    var blast=new net.minecraft.world.level.ServerExplosion(level,source,level.damageSources().explosion(source,base.owner),null,at,power,false,net.minecraft.world.level.Explosion.BlockInteraction.DESTROY);
                    System.setProperty(property,"true");level.getRandom().setSeed(seed);var expected=new HashSet<>((List<?>)calculate.invoke(blast));
                    System.clearProperty(property);level.getRandom().setSeed(seed);var actual=new HashSet<>((List<?>)calculate.invoke(blast));
                    require(!expected.isEmpty()&&expected.equals(actual),"native16/25 cached positions match unmodified calculator at identical seed");
                }
                source.discard();SkyWars2.LOGGER.info("[check] nuclear-point actual marker cached/native reference PASS6 exact sets");
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            finally{if(previous==null)System.clearProperty(property);else System.setProperty(property,previous);}
        }
        boolean check(){int now=game.server().getTickCount();var level=base.owner.level();
            require(now-start<600,"bounded real point payload completion");
            if(thaw<0){
                require(resistant.stream().allMatch(p->level.getBlockState(p).is(Blocks.BEDROCK))&&level.getBlockState(lobe).is(Blocks.STONE),"paused payload performs no terrain writes");
                if(now-start<5)return false;game.items().timeStop().end(level);thaw=now;
            }
            if(game.items().nukeEvent().jobCount()>0)return false;
            require(game.items().nukeEvent().blasts()==waves+24,"all24 damage waves, no extra hit for missing terrain source actual="+(game.items().nukeEvent().blasts()-waves));
            require(level.getBlockState(lobe).isAir(),"native25 creeper lobe really removes solid stone40 blocks below the AIR band");
            require(level.getBlockState(protectedBedrock).is(Blocks.BEDROCK),"native lobes retain bedrock outside the AIR masks");
            require(resistant.stream().filter(p->level.getBlockState(p).isAir()).count()>resistant.size()/2,"source AIR integrity can clear resistant blocks independently of native blast resistance");
            SkyWars2.LOGGER.info("[check] nuclear-point native job complete activeTicks={} masks12 waves24",now-thaw);return true;
        }
        @Override public void close(){if(closed)return;closed=true;game.items().timeStop().end(base.owner.level());game.items().nukeEvent().stop();game.items().nuclearVisuals().stop();
            var added=new ArrayList<Entity>();for(var e:base.owner.level().getAllEntities())if(!base.before.contains(e)&&!(e instanceof net.minecraft.server.level.ServerPlayer))added.add(e);added.forEach(Entity::discard);
            blocks.forEach((p,s)->game.maps().restoreSnapshot(base.owner.level(),p,s));
            for(long key:forced)base.owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
            base.owner.getAbilities().invulnerable=invulnerable;base.close();}
    }
}
