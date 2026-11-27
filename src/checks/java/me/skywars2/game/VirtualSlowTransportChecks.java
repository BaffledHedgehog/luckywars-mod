package me.skywars2.game;

import java.lang.reflect.*;
import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.game.combat.MatchDamageSources;
import me.skywars2.game.entities.projectiles.LbcWhiteArrowItem;
import me.skywars2.game.items.gadgets.LbcImbaTerrainUtilities;
import me.skywars2.game.items.gadgets.LbcImbaTerrainUtilityItems;
import me.skywars2.game.match.checkpoints.MatchQueuedWorkCheckpoint;
import me.skywars2.game.match.checkpoints.MatchRandomState;
import me.skywars2.game.world.terrain.EffectTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Typed live-marker writers, fractional motion/rewind, and fixed accepted finite operations. */
public final class VirtualSlowTransportChecks {
    private VirtualSlowTransportChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Virtual slow transport: "+why);}
    private static void near(Vec3 actual,Vec3 expected,String why){require(actual.distanceToSqr(expected)<1e-16,why+" actual="+actual+" expected="+expected);}
    private static Object read(Object target,String name){return WoolBucketChecks.read(target,name);}
    private static MatchQueuedWorkCheckpoint.Graph graph(GameController game,WoolBucketChecks.Fixture f){
        var actors=new ArrayList<Entity>();f.level.getAllEntities().forEach(actors::add);return new MatchQueuedWorkCheckpoint.Graph(new MatchDamageSources(game,actors));
    }
    private static Runnable restore(MatchQueuedWorkCheckpoint.Graph graph,MatchQueuedWorkCheckpoint.Image image,WoolBucketChecks.Fixture f){
        require(graph.links.resolve(f.level::getEntity).isEmpty(),"actual source links resolve");var jobs=graph.prepare();var registry=image.prepare(graph.links);
        return ()->{jobs.run();registry.run();};
    }
    @SuppressWarnings("unchecked")
    private static void cunny(GameController game,WoolBucketChecks.Fixture f)throws ReflectiveOperationException{
        var terrain=game.items().cunnyTerrain();require(terrain.actorViews(1).isEmpty()&&terrain.view().jobs()==0,"exclusive Cunny markers");
        require(terrain.grass(f.owner),"real autonomous grass source accepted");var initial=terrain.actorViews(1).getFirst();
        for(int n=0;n<100;n++)terrain.transformOrigins(f.level,point->point.add(.012,0,0));
        var moved=terrain.actorViews(1).getFirst();near(moved.position(),initial.position().add(1.2,0,0),"one hundred sub-block pulls retain fractional progress");
        var job=((Collection<?>)read(terrain,"jobs")).iterator().next();var ops=(Deque<Object>)read(job,"ops");var grass=ops.getFirst();
        require(read(grass,"at").equals(BlockPos.containing(moved.position()))&&((Set<?>)read(job,"claims")).contains(BlockPos.containing(moved.position())),"queued grass cell and claims follow its canonical fraction");
        var graph=graph(game,f);var image=terrain.checkpoint(graph);var restore=restore(graph,image,f);
        terrain.transformOrigins(f.level,point->point.add(.17,.25,-.09));restore.run();
        near(terrain.actorViews(1).getFirst().position(),moved.position(),"typed checkpoint restores precise fractional marker origin");

        var rng=MatchRandomState.capture(f.other.getRandom());
        try{
            f.other.getRandom().setSeed(47);f.move(f.other,f.owner.position().add(2,0,0));
            require(terrain.cloneNearby(f.other,15,8,1,Long.MAX_VALUE).bodies()==1,"real clone adapter accepts transported canonical marker");
            var clone=terrain.actorViews(8).stream().filter(v->v.id()!=initial.id()).findFirst().orElseThrow();
            require(!clone.position().equals(Vec3.atCenterOf(BlockPos.containing(clone.position()))),"new clone retains fractional authored destination instead of block-center quantization");
        }finally{rng.restore(f.other.getRandom());}

        // A real accepted Teleport remains an instant queue, and a published finite
        // Emit prefix keeps the original target even while its live Grass travels.
        require(terrain.teleport(f.owner),"actual instant teleport source queued");
        var instant=((Collection<?>)read(terrain,"jobs")).stream().filter(v->v!=job&&((Deque<?>)read(v,"ops")).peek().getClass().getSimpleName().equals("Teleport")).findFirst().orElseThrow();
        Object teleport=((Deque<?>)read(instant,"ops")).getFirst();
        var emitType=Class.forName("me.skywars2.game.world.terrain.LbcCunnyTerrain$Emit");
        var constructor=emitType.getDeclaredConstructor(BlockPos.class,me.skywars2.game.world.maps.MapManager.BlockSnapshot.class);constructor.setAccessible(true);
        var fixedCell=f.owner.blockPosition().above(4);f.set(fixedCell,Blocks.STONE.defaultBlockState());
        Object emit=constructor.newInstance(fixedCell,new me.skywars2.game.world.maps.MapManager.BlockSnapshot(Blocks.DIRT.defaultBlockState(),null));
        ((Deque<Object>)read(job,"ops")).addFirst(emit);
        terrain.transformOrigins(f.level,point->point.add(4.25,1.125,-2.5));
        require(((Deque<?>)read(job,"ops")).contains(emit)&&((Deque<?>)read(instant,"ops")).getFirst()==teleport
                &&f.level.getBlockState(fixedCell).is(Blocks.STONE),"accepted finite target, instant operation and already placed world do not move");
        terrain.transformOrigins(f.level,point->null);
        require(terrain.actorViews(1).isEmpty()&&((Deque<?>)read(job,"ops")).contains(emit)
                &&((Deque<?>)read(instant,"ops")).getFirst()==teleport,"retiring live markers preserves their accepted finite/instant work");
        terrain.stop();
    }
    @SuppressWarnings("unchecked")
    private static void imba(GameController game,WoolBucketChecks.Fixture f)throws ReflectiveOperationException{
        var service=game.items().imbaTerrainUtilities();require(service.ready()&&service.state().jobs()==0,"exclusive prepared native templates");
        var at=f.owner.position().add(0,16,0);f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcImbaTerrainUtilityItems.BASTION));
        require(service.useBastion(f.owner,InteractionHand.MAIN_HAND,at),"actual live prepublication bastion source");
        for(int n=0;n<10;n++)service.transformOrigins(f.level,point->point.add(.012,0,0));
        var before=service.views().getFirst();near(before.origin(),at.add(.12,0,0),"live template marker accumulates fractional motion");
        var graph=graph(game,f);var image=service.checkpoint(graph);var restore=restore(graph,image,f);
        service.transformOrigins(f.level,point->point.add(.25,0,0));restore.run();near(service.views().getFirst().origin(),before.origin(),"template checkpoint restores mutable prepublication origin");

        var work=game.items().stonekivi().queuedSnapshot().stream().filter(w->w.getClass().getName().equals("me.skywars2.game.items.gadgets.LbcImbaTerrainUtilities$Job")).findFirst().orElseThrow();
        var cells=(List<Object>)read(service,"bastion");require(cells.size()>128,"actual source has a finite continuation after its first quantum");
        var origin=BlockPos.containing(before.origin().add(-8,-1,-8));
        var footprint=new ArrayList<BlockPos>();for(int n=0;n<128;n++){var target=origin.offset((BlockPos)read(cells.get(n),"offset"));f.remember(target);footprint.add(target);}
        require(me.skywars2.game.world.maps.NativeChunkLoad.resident(f.level,footprint.getFirst()),"first publication cell resident");
        var tickStarted=EffectTerrain.class.getDeclaredField("tickStarted");tickStarted.setAccessible(true);long oldStart=tickStarted.getLong(null);
        try{EffectTerrain.beginTick();work.step();}finally{tickStarted.setLong(null,oldStart);}
        var draining=service.views().getFirst();require(!draining.cloneable()&&draining.cursor()>0&&draining.cursor()<=128,"one real native publication quantum retires only its source marker");
        var written=footprint.stream().map(f.level::getBlockState).toList();
        require(service.debrisPlaced(f.owner,at.add(2,0,0))&&service.dirterPlaced(f.owner,InteractionHand.MAIN_HAND,new ItemStack(LbcImbaTerrainUtilityItems.DIRTER),at.add(3,0,0)),"actual finite debris and DIRTER queues accepted");
        var fixed=service.views();Object fills=read(((Map<Long,Object>)read(service,"jobs")).values().stream().filter(j->read(j,"kind")==LbcImbaTerrainUtilities.Kind.DIRTER).findFirst().orElseThrow(),"fills");
        service.transformOrigins(f.level,point->point.add(10,20,30));
        require(service.views().equals(fixed)&&written.equals(footprint.stream().map(f.level::getBlockState).toList()),"draining template cursors/origins, finite jobs and already published blocks remain fixed");
        require(read(((Map<Long,Object>)read(service,"jobs")).values().stream().filter(j->read(j,"kind")==LbcImbaTerrainUtilities.Kind.DIRTER).findFirst().orElseThrow(),"fills")==fills,"accepted DIRTER fill geometry remains unchanged");
        service.stop();
    }
    private static void hammerAndArrow(GameController game,WoolBucketChecks.Fixture f){
        var tools=game.items().specialTools();require(tools.hammerCount()==0&&tools.pending()==0,"exclusive hammer family");
        f.owner.setYRot(0);f.owner.setXRot(0);var target=BlockPos.containing(f.owner.getEyePosition().add(0,0,3));f.set(target,Blocks.STONE.defaultBlockState());
        f.owner.setItemInHand(InteractionHand.MAIN_HAND,game.lbc().rollTable(f.level,f.owner.position(),f.owner,"lbc:ported/banhammer").getFirst());game.items().mana().score(f.owner,"swordcharged7").set(1);
        require(f.owner.gameMode.useItem(f.owner,f.level,f.owner.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction()&&tools.hammerCount()==1,"actual hand input publishes one native virtual hammer");
        var hammer=tools.hammerSnapshot().getFirst();var point=hammer.point;var impact=hammer.impact;var shift=new Vec3(.012,.125,-.031);
        tools.transformOrigins(f.level,p->p.add(shift));near(hammer.point,point.add(shift),"falling hammer origin translated");near(hammer.impact,impact.add(shift),"hammer impact anchor translates by same delta");
        hammer.impacted=true;point=hammer.point;impact=hammer.impact;tools.transformOrigins(f.level,p->p.add(20,20,20));
        require(hammer.point.equals(point)&&hammer.impact.equals(impact),"completed impact is stationary and cannot be rearmed by transport");hammer.destroy();

        var arrows=game.items().whiteArrows();require(arrows.state().active()==0,"exclusive live White Arrow family");
        game.items().mana().maximum(f.owner,2000);game.items().mana().set(f.owner,2000);f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcWhiteArrowItem.TOKEN));
        require(f.owner.gameMode.useItem(f.owner,f.level,f.owner.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction(),"actual White Arrow native hand input");
        var before=arrows.views().getFirst();var shot=((Map<?,?>)read(arrows,"shots")).values().iterator().next();Object source=read(shot,"source"),team=read(shot,"team"),last=read(shot,"lastStepTick");
        for(int n=0;n<100;n++)arrows.transformOrigins(f.level,p->p.add(.012,0,0));var after=arrows.views().getFirst();
        near(after.point(),before.point().add(1.2,0,0),"live ray retains fractional transport");
        require(after.id()==before.id()&&after.author().equals(before.author())&&after.direction().equals(before.direction())&&after.age()==before.age()&&after.cloneMask()==before.cloneMask()
                &&read(shot,"source")==source&&Objects.equals(read(shot,"team"),team)&&read(shot,"lastStepTick").equals(last),"ray source/team/direction/lifetime/clone/step state unchanged");
        var counters=arrows.state();arrows.transformOrigins(f.level,p->null);
        require(arrows.state().active()==0&&arrows.state().launched()==counters.launched()&&arrows.state().steps()==counters.steps()&&arrows.state().kills()==counters.kills(),"retirement performs no extra launch, ray step or execution");
    }
    public static void run(GameController game){
        require(game.items().stonekivi().count()==0,"exclusive shared queue before native fixture");
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();
            try{cunny(game,f);imba(game,f);hammerAndArrow(game,f);}
            catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            finally{game.items().cunnyTerrain().stop();game.items().imbaTerrainUtilities().stop();game.items().whiteArrows().stop();game.items().specialTools().stop();game.items().stonekivi().cancelQueuedForRollback();}
        }
        SkyWars2.LOGGER.info("[check] PASS virtual-slow-transport (fractional markers/clone/rewind, native first publication and fixed finite cursors/fills/world, pre-impact hammer, native live White Arrow state)");
    }
}
