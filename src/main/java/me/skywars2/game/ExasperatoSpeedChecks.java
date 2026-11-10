package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Actual held wand, swept collision, native packets and destruction across real server ticks. */
public final class ExasperatoSpeedChecks {
    private ExasperatoSpeedChecks(){}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Exasperato speed: "+why);}
    public static void run(GameController game){require(running==null,"exclusive fixture");var fixture=new Fixture(game);
        try{fixture.initialize();running=fixture;}catch(Throwable error){fixture.close();throw error;}}
    public static void tick(GameController game){if(running==null)return;try{
        if(!running.check())return;
        running.close();running=null;SkyWars2.LOGGER.info("[check] PASS exasperato-fast (actual wand impact, all85 native explosions and50 lightning, complete within3 real ticks)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}
        SkyWars2.LOGGER.error("[check] FAIL exasperato-fast",error);}}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final boolean invulnerable;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        LbcExasperato.Charge charge;BlockPos wall;int started,impact=-1;long clock;boolean closed;
        Fixture(GameController game){this.game=game;require(game.items().exasperato().count()==0&&game.items().exasperato().pendingBlasts()==0,"exclusive blast scene");
            base=new ElementalGrimoireChecks.Fixture(game,true);base.retain=true;invulnerable=base.owner.getAbilities().invulnerable;}
        void initialize(){
            base.initialize();var owner=base.owner;owner.getAbilities().invulnerable=true;owner.setYRot(0);owner.setXRot(0);
            wall=BlockPos.containing(owner.getEyePosition().add(0,0,4));
            for(var p:BlockPos.betweenClosed(wall.offset(-14,-14,0),wall.offset(14,14,14))){
                require(owner.level().hasChunkAt(p)&&owner.level().getBlockState(p).isAir(),"resident empty continuous target frame");
                blocks.put(p.immutable(),game.maps().snapshot(owner.level(),p));
            }
            for(var p:blocks.keySet())LbcDropBlocks.setBlock(owner.level(),p,Blocks.STONE.defaultBlockState(),true);
            nativeGeometryReference();
            owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcWands.EXASPERATO));base.mana.set(owner,10000);
            base.recording.packets.clear();
            require(base.input(InteractionHand.MAIN_HAND),"native held-wand use accepted");
            require(game.items().exasperato().count()==1&&base.mana.get(owner)==8500,"one native projectile and exact1500 mana cost");
            charge=game.items().exasperato().snapshot().getFirst();started=game.server().getTickCount();clock=System.nanoTime();
        }
        long explosions(){return base.recording.packets.stream().filter(ClientboundExplodePacket.class::isInstance).count();}
        void nativeGeometryReference(){
            String option="skywars2.disableExplosionCache",previous=System.getProperty(option);var level=base.owner.level();
            try{
                var constructor=Class.forName("me.skywars2.game.LbcExasperato$NativeBlastSource").getDeclaredConstructor(
                        net.minecraft.server.level.ServerLevel.class,net.minecraft.world.phys.Vec3.class,net.minecraft.world.entity.LivingEntity.class,boolean.class);
                constructor.setAccessible(true);
                var calculate=net.minecraft.world.level.ServerExplosion.class.getDeclaredMethod("calculateExplodedPositions");calculate.setAccessible(true);
                for(var at:List.of(net.minecraft.world.phys.Vec3.atCenterOf(wall),net.minecraft.world.phys.Vec3.atLowerCornerOf(wall).add(.17,.93,-.001))){
                    var source=(Entity)constructor.newInstance(level,at,base.owner,true);source.entityTags().add(LbcExasperato.EXPLOSIVE);
                    require(LbcExasperato.cacheableNativeBlast(source),"actual opt-in native source class");
                    var blast=new net.minecraft.world.level.ServerExplosion(level,source,level.damageSources().explosion(source,base.owner),null,at,4,false,
                            net.minecraft.world.level.Explosion.BlockInteraction.DESTROY);
                    for(long seed:new long[]{4711,991,917458}){
                        System.setProperty(option,"true");level.getRandom().setSeed(seed);
                        var vanilla=new HashSet<>((List<?>)calculate.invoke(blast));
                        System.clearProperty(option);level.getRandom().setSeed(seed);
                        var cached=new HashSet<>((List<?>)calculate.invoke(blast));
                        require(!vanilla.isEmpty()&&vanilla.equals(cached),"cached/native destroyed-position sets must match at the same seed, including fractional surface origin");
                    }
                    source.discard();
                }
                SkyWars2.LOGGER.info("[check] exasperato-fast cached/native geometry reference PASS6 exact sets");
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Native geometry reference",error);}
            finally{if(previous==null)System.clearProperty(option);else System.setProperty(option,previous);}
        }
        boolean check(){
            int now=game.server().getTickCount();long count=explosions();
            require(now-started<30,"real collision observation deadline");
            if(!charge.isRemoved())return false;
            if(impact<0){
                impact=now;require(count>=29&&count<=85,"first native collision services at least one third of the85 sources");
                long bolts=0;for(var entity:base.owner.level().getAllEntities())if(!base.before.contains(entity)
                        &&entity instanceof net.minecraft.world.entity.LightningBolt bolt&&bolt.entityTags().contains(LbcElectro.SHOCK_BOLT))bolts++;
                require(bolts==50,"all50 original lightning sources retained");
            }
            require(count<=85,"collision must not duplicate accepted explosions");
            if(game.items().exasperato().pendingShortBlasts()!=0){require(now-impact<2,"all85 native blasts must finish by the third real tick");return false;}
            require(count==85&&now-impact<=2,"exact85 native packets completed in at most3 real ticks");
            require(base.owner.level().getBlockState(wall).isAir(),"native blast really destroys the impact wall");
            long removed=blocks.keySet().stream().filter(p->base.owner.level().getBlockState(p).isAir()).count();
            require(removed>100,"real solid target destruction, not packet-only publication");
            SkyWars2.LOGGER.info("[check] exasperato-fast ticks={} nativePackets={} actualStoneRemoved={} elapsedMs={}",now-impact+1,count,removed,(System.nanoTime()-clock)/1_000_000d);
            return true;
        }
        @Override public void close(){if(closed)return;closed=true;game.items().exasperato().stop();
            var spawned=new ArrayList<Entity>();for(var entity:base.owner.level().getAllEntities())if(!base.before.contains(entity)&&!(entity instanceof net.minecraft.server.level.ServerPlayer))spawned.add(entity);
            spawned.forEach(Entity::discard);
            blocks.forEach((p,s)->game.maps().restoreSnapshot(base.owner.level(),p,s));base.owner.getAbilities().invulnerable=invulnerable;base.close();}
    }
}
