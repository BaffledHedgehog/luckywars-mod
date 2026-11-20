package me.skywars2.game;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Actual held wand, swept collision, native packets and destruction across real server ticks. */
public final class ExasperatoSpeedChecks {
    private ExasperatoSpeedChecks(){}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Exasperato speed: "+why);}
    public static void run(GameController game){require(running==null,"exclusive fixture");var fixture=new Fixture(game);
        try{fixture.initialize();running=fixture;}catch(Throwable error){fixture.close();throw error;}}
    public static void tick(GameController game){if(running==null)return;try{
        if(!running.check())return;
        running.close();running=null;SkyWars2.LOGGER.info("[check] PASS exasperato-fast (warm and cold-edge swept impacts, all85 native explosions and50 lightning, three real publication ticks, bounded native retry and no ticket leaks)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}
        SkyWars2.LOGGER.error("[check] FAIL exasperato-fast",error);}}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final boolean invulnerable;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        LbcExasperato.Charge charge;BlockPos wall;int started,impact=-1;long clock;boolean closed,cold;
        Set<LightningBolt> previousBolts=Set.of();
        Set<ChunkPos> coldTickets=Set.of();
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
        List<LightningBolt> newBolts(){return game.items().electro().transientSnapshot().stream()
                .filter(bolt->!previousBolts.contains(bolt)&&!base.before.contains(bolt)&&!bolt.isRemoved()).toList();}
        @SuppressWarnings("unchecked") Map<ChunkPos,Object> leases(){
            var service=game.items().exasperato();
            return ((Map<ServerLevel,Map<ChunkPos,Object>>)get(get(service,"impactLeases"),"leases")).getOrDefault(base.owner.level(),Map.of());
        }
        void noTickets(Set<ChunkPos> requested){
            var storage=(TicketStorage)get(base.owner.level().getChunkSource(),"ticketStorage");
            var type=(TicketType)get(game.items().exasperato(),"IMPACT_TICKET");
            require(leases().isEmpty()&&requested.stream().noneMatch(pos->storage.getTickets(pos.pack()).stream().anyMatch(t->t.getType()==type)),
                    "every native impact ticket was released");
        }
        void terminalFailure(Vec3 origin){
            var service=game.items().exasperato();var level=base.owner.level();var chunk=ChunkPos.containing(BlockPos.containing(origin));
            require(!NativeChunkLoad.resident(level,BlockPos.containing(origin)),"controlled failure footprint starts cold");
            call(service,"burst",new Class<?>[]{ServerPlayer.class,Vec3.class,boolean.class},base.owner,origin,false);
            var lease=leases().get(chunk);require(lease!=null&&service.pendingShortBlasts()==85,"accepted all sources before native failure");
            var footprint=Set.copyOf(leases().keySet());
            var first=CompletableFuture.completedFuture(ChunkResult.error("controlled Ortkelé first load failure"));
            set(lease,"load",first);service.blastTick();
            int now=game.server().getTickCount();
            require(service.pendingShortBlasts()==85&&(int)get(lease,"retryTick")==now&&get(lease,"load")!=first,
                    "exactly one native reissue and no partial output on the first error");
            var last=CompletableFuture.completedFuture(ChunkResult.error("controlled Ortkelé retry failure"));
            set(lease,"load",last);service.blastTick();
            require(service.pendingShortBlasts()==85&&get(lease,"load")==last,"same-tick revisit keeps the bounded retry");
            set(lease,"retryTick",now-1);service.blastTick();
            require(service.pendingShortBlasts()==0&&explosions()==0&&newBolts().isEmpty(),"second completed error retires the entire unpublished impact");
            noTickets(footprint);
        }
        void coldImpact(){
            var owner=base.owner;var level=owner.level();var service=game.items().exasperato();var cell=owner.blockPosition();
            int regionX=(cell.getX()&~511)+4096,regionZ=(cell.getZ()&~511)+4096;
            wall=new BlockPos(regionX+255,BlockPos.containing(owner.getEyePosition()).getY(),regionZ+248);
            var start=Vec3.atCenterOf(wall).add(0,0,-2);
            var across=wall.east();
            var impactChunk=ChunkPos.containing(wall);
            level.getChunk(impactChunk.x(),impactChunk.z());
            require(NativeChunkLoad.resident(level,wall)&&!NativeChunkLoad.resident(level,across),
                    "impact wall is in a FULL chunk and its immediate neighbor is genuinely cold");
            require(level.getBlockState(wall).isAir(),"isolated remote target is empty");
            blocks.put(wall,game.maps().snapshot(level,wall));
            LbcDropBlocks.setBlock(level,wall,Blocks.STONE.defaultBlockState(),true);
            previousBolts=Set.copyOf(game.items().electro().transientSnapshot());
            base.recording.packets.clear();
            terminalFailure(Vec3.atCenterOf(wall).add(96,0,64));

            charge=new LbcExasperato.Charge(LbcExasperato.TYPE,level);
            charge.setOwner(owner);charge.setPos(start);charge.setDeltaMovement(new Vec3(0,0,3));
            call(service,"step",new Class<?>[]{LbcExasperato.Charge.class,ServerPlayer.class},charge,owner);
            require(charge.isRemoved()&&service.pendingShortBlasts()==85,
                    "real swept wall contact accepts all native sources across the cold boundary");
            coldTickets=Set.copyOf(leases().keySet());
            require(coldTickets.contains(ChunkPos.containing(across)),"the native ticket covers the cold side of the footprint");
            service.blastTick();
            require(service.pendingShortBlasts()==85&&newBolts().isEmpty()&&level.getBlockState(wall).is(Blocks.STONE),
                    "cold start publishes no partial TNT, lightning or terrain");
            // The real native explosion packets can now reach the collector. The impact was already accepted while remote.
            owner.teleportTo(level,start.x,start.y+18,start.z,Set.of(),0,0,true);
            owner.setDeltaMovement(Vec3.ZERO);
            cold=true;impact=-1;started=game.server().getTickCount();clock=System.nanoTime();
        }
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
            if(cold)return checkCold();
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
            coldImpact();
            return false;
        }
        boolean checkCold(){
            int now=game.server().getTickCount();long count=explosions();var level=base.owner.level();
            require(now-started<250,"cold footprint native loading deadline");
            if(count==0) {
                require(game.items().exasperato().pendingShortBlasts()==85&&newBolts().isEmpty(),"no offset dropped or published while awaiting FULL");
                return false;
            }
            if(impact<0) {
                impact=now;
                var bolts=newBolts();
                require(bolts.size()==50&&bolts.stream().anyMatch(bolt->bolt.getBlockX()>wall.getX()),
                        "all50 lightning sources include the authored offsets across the formerly cold edge");
            }
            require(count==switch(now-impact){case 0->29;case 1->58;case 2->85;default->-1;},
                    "exact native29/29/27 publication once FULL, count="+count+" ticks="+(now-impact+1));
            if(count<85)return false;
            require(game.items().exasperato().pendingShortBlasts()==0&&level.getBlockState(wall).isAir(),
                    "original target destroyed and exact native queue completed");
            noTickets(coldTickets);
            SkyWars2.LOGGER.info("[check] exasperato-fast coldTicks={} nativePackets={} coldChunks={}",now-started,count,coldTickets);
            return true;
        }
        @Override public void close(){if(closed)return;closed=true;game.items().exasperato().stop();
            var spawned=new ArrayList<Entity>();for(var entity:base.owner.level().getAllEntities())if(!base.before.contains(entity)&&!(entity instanceof net.minecraft.server.level.ServerPlayer))spawned.add(entity);
            for(var bolt:game.items().electro().transientSnapshot())if(!base.before.contains(bolt))spawned.add(bolt);
            spawned.forEach(Entity::discard);
            blocks.forEach((p,s)->game.maps().restoreSnapshot(base.owner.level(),p,s));base.owner.getAbilities().invulnerable=invulnerable;base.close();}
    }

    private static java.lang.reflect.Field field(Object target,String name){
        try{
            var field=target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Native fixture field "+name,error);}
    }
    private static Object get(Object target,String name){
        try{return field(target,name).get(target);}
        catch(IllegalAccessException error){throw new IllegalStateException(error);}
    }
    private static void set(Object target,String name,Object value){
        try{field(target,name).set(target,value);}
        catch(IllegalAccessException error){throw new IllegalStateException(error);}
    }
    private static void call(Object target,String name,Class<?>[] signature,Object... arguments){
        try{
            var method=target.getClass().getDeclaredMethod(name,signature);
            method.setAccessible(true);
            method.invoke(target,arguments);
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Native fixture call "+name,error);}
    }
}
