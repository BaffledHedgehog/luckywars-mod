package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Scheduled native wand scene; immutable target, real countdown and single prepared crater. */
public final class ExopersoRitualChecks {
    private ExopersoRitualChecks(){}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Exoperso ritual: "+why);}
    private static com.mojang.math.Transformation transformation(Display display){try{
        var getter=Display.class.getDeclaredMethod("createTransformation",net.minecraft.network.syncher.SynchedEntityData.class);getter.setAccessible(true);
        return (com.mojang.math.Transformation)getter.invoke(null,display.getEntityData());
    }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final boolean invulnerable;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final List<BlockPos> probes=new ArrayList<>();
        LbcExopersoRitual.State ordinary,academy;Vec3 target;BlockPos bedrock;
        int started,pauseStart=-1,pauseAge;boolean pausedOnce,rewound,academyDone,normalZero,academyZero,closed;
        Fixture(GameController game){this.game=game;require(game.items().exoperso().count()==0&&!game.items().exasperato().pendingNuclearTerrain(),"exclusive scene");
            base=new ElementalGrimoireChecks.Fixture(game,true);base.retain=true;invulnerable=base.owner.getAbilities().invulnerable;}
        void set(BlockPos p,net.minecraft.world.level.block.state.BlockState state){blocks.putIfAbsent(p.immutable(),game.maps().snapshot(base.owner.level(),p));LbcDropBlocks.setBlock(base.owner.level(),p,state,true);}
        void initialize(){
            base.initialize();var owner=base.owner;owner.getAbilities().invulnerable=true;
            owner.teleportTo(owner.level(),owner.getX(),owner.level().getMaxY()-100,owner.getZ(),Set.of(),-90,0,true);
            var wall=BlockPos.containing(owner.getEyePosition().add(4,0,0));require(owner.level().hasChunkAt(wall)&&owner.level().getBlockState(wall).isAir(),"resident clear gaze point");
            set(wall,Blocks.STONE.defaultBlockState());
            require(game.items().exoperso().cast(owner,false)&&game.items().exoperso().cast(owner,true),"both actual wand entry points");
            var states=game.items().exoperso().rituals.snapshot();ordinary=states.get(0);academy=states.get(1);target=ordinary.target;
            for(var state:states)for(var ring:state.rings)require(ring.isCurrentlyGlowing()&&ring.getTeamColor()==0xE75313,"private rune bloom mask on both native wand versions");
            require(target.equals(academy.target)&&target.distanceTo(owner.getEyePosition())<5,"gaze intersects the actual first block");
            for(var state:states){var models=new HashSet<Object>();double previous=0;
                require(state.age==0&&state.anchor.position().equals(target)&&state.rings.size()==30,"one native anchor and thirty rune bodies");
                for(var ring:state.rings){double height=ring.getY()-target.y;require(height>=10&&height<=50&&height>previous,"strict bottom-up heights10..50");previous=height;
                    models.add(ring.getItemStack().get(DataComponents.ITEM_MODEL));var scale=transformation(ring).scale();require(scale.x()==0&&scale.z()==0,"native initial scale is zero");}
                for(int i=0;i<state.scales.length;i++)require(state.scales[i]>=5&&state.scales[i]<=100&&Math.abs(state.speeds[i])>=3.6f&&Math.abs(state.speeds[i])<=36.00001f,"fixed5..100 scale and signed0.2..2 rotations/second");
                require(models.size()==10,"all ten authored rune models occur across thirty rings");
            }
            owner.setYRot(90);started=game.server().getTickCount();
            bedrock=BlockPos.containing(target).below(2);set(bedrock,Blocks.BEDROCK.defaultBlockState());
            for(var p:BlockPos.betweenClosed(BlockPos.containing(target).offset(-1,-1,-1),BlockPos.containing(target).offset(1,0,1)))if(!p.equals(bedrock))probes.add(p.immutable());
        }
        final java.util.Set<BlockPos> craterProfile=new java.util.LinkedHashSet<>();
        void refill(){
            var center=BlockPos.containing(target);
            if(craterProfile.isEmpty())for(var p:BlockPos.betweenClosed(center.offset(-30,-30,-30),center.offset(30,-1,30)))
                if(base.owner.level().hasChunkAt(p)&&!p.equals(bedrock))craterProfile.add(p.immutable());
            require(craterProfile.size()>100_000,"actual continuous resident stone half-space, not sparse sample planes");
            for(var p:craterProfile){set(p,Blocks.STONE.defaultBlockState());require(base.owner.level().getBlockState(p).is(Blocks.STONE),"physical crater sample actually exists inside build height");}
            for(var p:probes)set(p,Blocks.SLIME_BLOCK.defaultBlockState());
        }
        void verifyCrater(){
            var level=base.owner.level();int core=0,coreRemaining=0,transition=0,transitionAir=0,outside=0,outsideAir=0;
            for(var p:craterProfile){
                double radius=Vec3.atCenterOf(p).distanceTo(target);boolean air=level.getBlockState(p).isAir();
                if(radius<18){core++;if(!air)coreRemaining++;}
                else if(radius>=22&&radius<24){transition++;if(air)transitionAir++;}
                else if(radius>=28){outside++;if(air)outsideAir++;}
            }
            require(core>10_000&&coreRemaining==0,"full legacy-size inner18 must be an empty connected cavity, residual="+coreRemaining);
            double transitionDensity=transitionAir/(double)transition,outsideDensity=outsideAir/(double)outside;
            require(transition>1000&&transitionDensity>.30&&transitionDensity<.75,"legacy22..24 transition retains a substantial part of the terrain: "+transitionDensity);
            require(outside>1000&&outsideDensity<.02,"outside28 mostly survives instead of becoming a larger perforated sphere: "+outsideDensity);
            SkyWars2.LOGGER.info("[check] Exoperso full stone half-space coreRemaining={} coreCells={} transitionDensity={} outsideDensity={}",coreRemaining,core,transitionDensity,outsideDensity);
        }
        boolean step(){
            int now=game.server().getTickCount();require(game.playing()&&now-started<400,"finite active native scene");
            require(ordinary.target.equals(target)&&ordinary.anchor.position().equals(target),"camera turn never retargets ritual");
            if(!pausedOnce&&ordinary.age>=35){pauseStart=now;pauseAge=ordinary.age;game.items().timeStop().begin(base.owner.level(),100);pausedOnce=true;}
            if(pauseStart>=0){require(ordinary.age==pauseAge&&academy.age==pauseAge,"entire native rune countdown pauses");
                if(now-pauseStart<8)return false;game.items().timeStop().end(base.owner.level());pauseStart=-1;}
            if(!rewound&&ordinary.age>=60){
                var actors=new ArrayList<Entity>();base.owner.level().getAllEntities().forEach(actors::add);
                var graph=new MatchBlastCheckpoint.Graph(new MatchDamageSources(game,actors));var image=game.items().exoperso().checkpoint(graph);int age=ordinary.age;
                var speeds=ordinary.speeds.clone();ordinary.age=299;academy.age=159;
                require(graph.links.resolve(id->base.owner.level().getEntity(id)).isEmpty(),"typed checkpoint resolves every native anchor/rune link");
                game.items().exoperso().prepareCheckpoint();graph.prepare().run();image.prepare(graph.links).run();
                var restored=game.items().exoperso().rituals.snapshot();ordinary=restored.get(0);academy=restored.get(1);
                for(var state:restored)for(var ring:state.rings)require(ring.isCurrentlyGlowing()&&ring.getTeamColor()==0xE75313,"native replay retains private bloom mask");
                require(ordinary.age==age&&academy.age==age&&Arrays.equals(ordinary.speeds,speeds),"checkpoint restores independent age and fixed random rotation parameters");rewound=true;
            }
            if(!academyDone&&academy.age==100){
                require(academy.preparation.isDone()&&!academy.preparation.isCompletedExceptionally(),"pure geometry ready during countdown");refill();
                var first=transformation(ordinary.rings.getFirst()).scale();var last=transformation(ordinary.rings.getLast()).scale();
                require(first.x()>0&&first.x()==first.z()&&last.x()==ordinary.scales[29],"all thirty rings finish their two-tick growth before tick60");
            }
            if(!academyDone&&academy.age==159){for(var ring:academy.rings)require(transformation(ring).scale().x()==0&&transformation(ring).scale().z()==0,"Academy publishes its zero-scale frame before removal");academyZero=true;}
            if(!academyDone&&academy.anchor.isRemoved()){
                verifyCrater();
                require(academyZero&&academy.age==160&&ordinary.age==160&&game.items().exoperso().count()==1,"Academy finishes at half the ordinary duration");
                for(var p:probes)require(base.owner.level().getBlockState(p).isAir(),"prepared Academy crater consumes blocks placed after preparation");
                require(base.owner.level().getBlockState(bedrock).is(Blocks.BEDROCK)&&!game.items().exasperato().pendingNuclearTerrain(),"one immediate native terrain operation retains bedrock and leaves no delayed crater");
                academyDone=true;refill();
            }
            if(ordinary.age==319){for(var ring:ordinary.rings)require(transformation(ring).scale().x()==0&&transformation(ring).scale().z()==0,"ordinary final zero-scale frame");normalZero=true;}
            if(!ordinary.anchor.isRemoved())return false;
            verifyCrater();
            require(academyDone&&normalZero&&ordinary.age==320&&game.items().exoperso().count()==0,"normal320tick deadline retires only its complete graph");
            for(var p:probes)require(base.owner.level().getBlockState(p).isAir(),"ordinary prepared crater also reads current world state");
            return true;
        }
        public void close(){if(closed)return;closed=true;game.items().timeStop().end(base.owner.level());game.items().exoperso().stop();game.items().exasperato().stop();
            for(var e:blocks.entrySet())game.maps().restoreSnapshot(base.owner.level(),e.getKey(),e.getValue());base.owner.getAbilities().invulnerable=invulnerable;base.close();}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");var next=new Fixture(game);try{next.initialize();running=next;}catch(Throwable error){next.close();throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS exoperso-ritual (actual native320/160tick deadlines, immutable gaze,10 distinct runes, zero-scale growth/collapse frames,8tick TimeStop, explicit typed native graph replay, latest-state single prepared crater)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL exoperso-ritual",error);}}
}
