package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Native nuclear regression fixture. Run only in an isolated match; no native cooldown bypass. */
public final class NuclearBlastChecks {
    private NuclearBlastChecks(){}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Nuclear blast: "+why);}
    public static void run(GameController game){require(running==null,"exclusive fixture");var fixture=new Fixture(game);try{fixture.initialize();running=fixture;}catch(Throwable error){fixture.close();throw error;}}
    public static void tick(GameController game){if(running==null)return;try{running.check();running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS nuclear-blast (synchronous native lethal before terrain queue, native two-totem cooldown sequence, cover exposure, shared dense/noisy resistance terrain and bounded resident steps)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL nuclear-blast",error);}}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;
        final List<Mob> bodies=new ArrayList<>();final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        ServerLevel level;Vec3 at;Mob lethal,totems,covered;boolean closed;
        Fixture(GameController game){this.game=game;require(game.items().nukeEvent().jobCount()==0&&game.items().nukeEvent().markerCount()==0
                &&game.items().exasperato().pendingBlasts()==0&&game.items().nuclearVisuals().count()==0,"exclusive nuclear services");
            base=new ElementalGrimoireChecks.Fixture(game,true);base.retain=true;}
        void initialize(){base.initialize();level=base.owner.level();at=base.owner.position().add(3,0,0);
            base.owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);base.owner.setHealth(1000);
            for(var p:BlockPos.betweenClosed(BlockPos.containing(at).offset(-8,-8,-8),BlockPos.containing(at).offset(8,8,8))){
                require(level.hasChunkAt(p)&&level.getBlockState(p).isAir(),"resident empty owned test frame");blocks.put(p.immutable(),game.maps().snapshot(level,p));}
            lethal=body(at);totems=body(at.add(0,0,3));covered=body(at.add(0,0,-3));
            totems.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.TOTEM_OF_UNDYING));
            totems.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.TOTEM_OF_UNDYING));
        }
        Mob body(Vec3 point){var body=EntityTypes.ZOMBIE.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);require(body!=null,"native zombie factory");
            body.setPos(point);body.setNoAi(true);body.setNoGravity(true);body.setSilent(true);body.setPersistenceRequired();
            require(level.addFreshEntity(body),"native body publication");bodies.add(body);return body;}
        void wave(Vec3 point,float power){var source=new PrimedTnt(level,point.x,point.y,point.z,base.owner);
            source.entityTags().add(LbcDamage.UNPROTECTED_TNT);game.items().markCurrentItemEpoch(source);
            NuclearTerrain.damageWave(source,()->level.explode(source,point.x,point.y,point.z,power,Level.ExplosionInteraction.TNT));}
        void check(){
            // Newly published bodies have crossed a real native visibility boundary before this callback.
            require(level.getEntities((Entity)null,lethal.getBoundingBox().inflate(1)).contains(lethal),"native body spatial visibility");
            // Isolate bodies for each probe without changing production damage/cooldown behavior.
            totems.setPos(at.add(0,0,70));covered.setPos(at.add(0,0,-70));
            long before=game.items().nukeEvent().blasts();int tick=game.server().getTickCount();
            require(game.items().nukeEvent().pointStatusBurst(base.owner,level,at),"actual point status producer admitted");
            require(game.server().getTickCount()==tick&&!lethal.isAlive()&&game.items().nukeEvent().blasts()==before+1
                    &&game.items().nukeEvent().jobCount()==1,"native lethal completes in activation call before queued terrain");
            require(game.items().nukeEvent().pendingStatusBurst(base.owner)
                    &&!game.items().nukeEvent().pointStatusBurst(base.owner,level,at)
                    &&game.items().nukeEvent().jobCount()==1&&game.items().nukeEvent().blasts()==before+1,
                    "surviving native author joins its one pending status payload without another native wave or queue");
            game.items().nukeEvent().stop();game.items().nuclearVisuals().stop();
            require(!game.items().nukeEvent().pendingStatusBurst(base.owner),"completion/cancellation retires only this body status lease");
            // Two increasing native same-tick hits can consume two hand totems; the next lethal hit kills.
            // No resetting damageCooldownTime, removing resistance, or inventing bypass tags between waves.
            totems.setPos(at);wave(at.add(30,0,0),20);
            require(totems.isAlive()&&totems.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()
                    &&!totems.getItemInHand(InteractionHand.OFF_HAND).isEmpty(),"first native wave consumes one hand totem");
            wave(at.add(10,0,0),20);
            require(totems.isAlive()&&totems.getItemInHand(InteractionHand.OFF_HAND).isEmpty(),"larger native wave consumes second totem with ordinary cooldown delta");
            wave(at,20);require(!totems.isAlive(),"third increasing native wave kills after native totem exhaustion");
            covered.setPos(at);var wall=BlockPos.containing(at).offset(-2,0,0);
            for(var p:BlockPos.betweenClosed(wall.offset(0,-2,-3),wall.offset(0,4,3)))LbcDropBlocks.setBlock(level,p,Blocks.OBSIDIAN.defaultBlockState(),true);
            wave(at.add(-4,0,0),4);float protectedHealth=covered.getHealth();require(protectedHealth>=18,"native wall exposure is primary protection");
            for(var p:BlockPos.betweenClosed(wall.offset(0,-2,-3),wall.offset(0,4,3)))LbcDropBlocks.setBlock(level,p,Blocks.AIR.defaultBlockState(),true);
            wave(at.add(-4,0,0),4);require(covered.getHealth()<protectedHealth-5,"fresh native exposure penetrates only after wall destruction");
            shape();
            nativeBlocks();
        }
        void shape(){
            long seed=4711;int inside=0,outside=0;for(int i=0;i<512;i++){double angle=i*Math.PI/256;
                require(NuclearTerrain.contains(seed,.6*Math.cos(angle),0,.6*Math.sin(angle),.6),"dense core has no sparse-ray gaps");
                if(NuclearTerrain.contains(seed,Math.cos(angle),0,Math.sin(angle),1))inside++;else outside++;}
            require(inside>0&&outside>0,"deterministic boundary has spikes and recesses");
            require(NuclearTerrain.breakChance(6,.4)>NuclearTerrain.breakChance(1200,.4)
                    &&NuclearTerrain.breakChance(1200,.4)>0,"harder blocks probabilistically retained");
            var center=BlockPos.containing(at);for(var p:BlockPos.betweenClosed(center.offset(-3,-3,-3),center.offset(3,3,3)))
                LbcDropBlocks.setBlock(level,p,Blocks.STONE.defaultBlockState(),true);
            LbcDropBlocks.setBlock(level,center,Blocks.BEDROCK.defaultBlockState(),true);
            var shape=new NuclearTerrain.Shape(at,5,5);int steps=0;
            while(!shape.step(game,level,game.items().indirectKills().wildCause(base.owner))){require(shape.visited<=NuclearTerrain.CELLS_PER_STEP,"bounded terrain reads per step");require(++steps<100,"finite resident work");}
            require(shape.visited<=NuclearTerrain.CELLS_PER_STEP&&level.getBlockState(center).is(Blocks.BEDROCK),"unbreakable native block retained");
            int air=0;for(var p:BlockPos.betweenClosed(center.offset(-3,-3,-3),center.offset(3,3,3)))if(level.getBlockState(p).isAir())air++;
            require(air>250,"shared terrain clears dense weak core in one operation");
        }
        void nativeBlocks(){
            for(var p:BlockPos.betweenClosed(BlockPos.containing(at).offset(-7,-7,-7),BlockPos.containing(at).offset(7,7,7)))
                LbcDropBlocks.setBlock(level,p,Blocks.AIR.defaultBlockState(),true);
            var shape=new NuclearTerrain.Shape(at,5,5);var tnt=BlockPos.containing(at);BlockPos chest=null;
            for(var p:BlockPos.betweenClosed(tnt.offset(-2,0,-2),tnt.offset(2,0,2)))
                if(!p.equals(tnt)&&NuclearTerrain.unit(shape.seed^p.asLong())<.5){chest=p.immutable();break;}
            require(chest!=null,"deterministically admitted native chest fixture");
            LbcDropBlocks.setBlock(level,tnt,Blocks.TNT.defaultBlockState(),true);
            LbcDropBlocks.setBlock(level,chest,Blocks.CHEST.defaultBlockState(),true);
            require(level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity,"native chest publication");
            ((net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(chest)).setItem(0,new ItemStack(Items.DIAMOND,11));
            var source=new PrimedTnt(level,at.x,at.y,at.z,base.owner);source.entityTags().add(LbcExasperato.EXPLOSIVE);
            game.items().markCurrentItemEpoch(source);
            java.util.Set<Entity> before=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());level.getAllEntities().forEach(before::add);
            int[] constructed={0};var health=base.owner.getHealth();
            java.util.function.Supplier<net.minecraft.world.level.ServerExplosion> factory=()->{
                constructed[0]++;return new net.minecraft.world.level.ServerExplosion(level,source,level.damageSources().explosion(source,base.owner),null,at,20,false,
                        net.minecraft.world.level.Explosion.BlockInteraction.DESTROY);};
            int steps=0;while(!shape.step(game,level,game.items().indirectKills().coldCause(base.owner,false),factory,Runnable::run))require(++steps<100,"bounded native application slices");
            require(constructed[0]==1&&base.owner.getHealth()==health,"one native application object, no extra damage across terrain slices");
            int children=0,diamonds=0;for(var entity:level.getAllEntities())if(!before.contains(entity)){
                if(entity instanceof PrimedTnt primed){children++;require(primed.getOwner()==base.owner
                        &&primed.entityTags().contains(LbcExasperato.EXPLOSIVE),"native chained TNT retains actual author/source family");}
                if(entity instanceof net.minecraft.world.entity.item.ItemEntity item&&item.getItem().is(Items.DIAMOND))diamonds+=item.getItem().getCount();
            }
            require(children==1&&diamonds==11&&level.getBlockState(tnt).isAir()&&level.getBlockState(chest).isAir(),
                    "native TNT primes once and chest contents publish once through shared terrain callbacks");
            var added=new ArrayList<Entity>();
            for(var entity:level.getAllEntities())if(!before.contains(entity))added.add(entity);
            added.forEach(Entity::discard);
        }
        @Override public void close(){if(closed)return;closed=true;
            game.items().nukeEvent().stop();game.items().nuclearVisuals().stop();bodies.forEach(Entity::discard);
            var dropped=new ArrayList<Entity>();
            if(level!=null)for(var entity:level.getAllEntities())
                if(!base.before.contains(entity)&&entity instanceof net.minecraft.world.entity.item.ItemEntity)dropped.add(entity);
            dropped.forEach(Entity::discard);
            for(var e:blocks.entrySet())game.maps().restoreSnapshot(level,e.getKey(),e.getValue());base.close();}
    }
}
