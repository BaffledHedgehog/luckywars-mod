package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

/** Source scheduled bean growth, with native marker positions and shared terrain/loot owners. */
public final class LbcMagicBean {
    private static final String ACTOR="skywars2.magic_bean",OWNER=ACTOR+".owner.",ORDER=ACTOR+".order.";
    static final ResourceKey<LootTable> LOOT=ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:lucky_items_1"));
    private final GameController game;
    private IdentityHashMap<Marker,Bean> beans=new IdentityHashMap<>();
    private int callbackDepth;private boolean rewinding;
    private final IdentityHashMap<Bean,StepFrame> stagedSteps=new IdentityHashMap<>();
    private MatchAuthorityCheckpoint.Clock stagedClock;
    private record StepFrame(MatchAuthorityCheckpoint.Clock clock,int value){}
    private int lastTick=Integer.MIN_VALUE;
    private long steps,completions;

    public LbcMagicBean(GameController game){this.game=game;}
    public static boolean recognized(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("magicbean",0)==1;}
    public static boolean managed(Entity entity){return entity!=null&&entity.entityTags().contains(ACTOR);}
    public boolean ready(ItemEntity token){
        if(!recognized(token.getItem())||token.isRemoved()||!game.playing()||game.stage()==GameController.Stage.ENDING
                ||!(token.level() instanceof ServerLevel level)||level!=game.maps().level()&&!game.items().spellSources().persistent(token)||!LbcDimensionItems.ordinary(level)
                ||game.items().timeStop().stopped(level))return false;
        for(var tag:token.entityTags())if(tag.startsWith("skywars2.items.")&&!game.items().currentEpoch(tag))return false;
        return resident(level,token.blockPosition(),1);
    }
    /** The common special-drop dispatcher owns the single token debit. */
    public boolean spawn(ItemEntity token){
        if(rewinding)return false;
        callbackDepth++;try{
        if(!ready(token))return false;var level=(ServerLevel)token.level();
        var marker=new Marker(EntityTypes.MARKER,level);marker.setPos(token.position());
        marker.entityTags().addAll(List.of(ACTOR,"magicbeaner","magic"));
        var author=LbcIndirectKills.playerSource(game.items().specialDrops().owner(token));
        LbcIndirectKills.Owner cause=game.items().specialDrops().cause(token);
        if(cause!=null){marker.entityTags().add(OWNER+cause.player());marker.entityTags().add(ORDER+cause.order());}
        game.items().spellSources().inherit(token,marker);
        game.items().markCurrentItemEpoch(marker);var bean=new Bean(marker,cause,game.server().getTickCount());beans.put(marker,bean);
        game.items().timeStop().prepareFrozen(marker);
        if(!level.addFreshEntity(marker)){beans.remove(marker);marker.discard();return false;}
        return true;

        }finally{callbackDepth--;}
    }
    public LivingEntity owner(Entity actor){
        if(actor instanceof ItemEntity token&&recognized(token.getItem()))return game.items().specialDrops().owner(token);
        if(!managed(actor))return null;
        for(var tag:actor.entityTags())if(tag.startsWith(OWNER))try{
            var id=UUID.fromString(tag.substring(OWNER.length()));var online=game.server().getPlayerList().getPlayer(id);
            if(online!=null)return online;
            return actor.level() instanceof ServerLevel level&&level.getEntity(id) instanceof LivingEntity living?living:null;
        }catch(IllegalArgumentException ignored){}
        return null;
    }
    private static UUID authorId(Entity marker){
        for(var tag:marker.entityTags())if(tag.startsWith(OWNER))try{return UUID.fromString(tag.substring(OWNER.length()));}catch(IllegalArgumentException ignored){}
        return null;
    }
    public int count(){return beans.size();}
    public long steps(){return steps;}
    public long completions(){return completions;}
    List<Marker> snapshot(){return List.copyOf(beans.keySet());}
    private boolean live(Marker marker){return !marker.isRemoved()&&game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&(marker.level()==game.maps().level()||game.items().spellSources().persistent(marker))&&LbcDimensionItems.ordinary(marker.level())&&game.items().currentEpoch(marker);}
    public void tick(){
        if(rewinding)return;
        callbackDepth++;try{
        int now=game.server().getTickCount();if(lastTick==now)return;lastTick=now;
        for(var bean:List.copyOf(beans.values())){
            var marker=bean.marker;
            if(!live(marker)){remove(bean,true);continue;}
            var level=(ServerLevel)marker.level();
            if(game.items().timeStop().stopped(level)||marker.entityTags().contains("time_stopped"))continue;
            if(game.items().magic().suppressedAt(level,marker.position())){remove(bean,true);continue;}
            // /schedule ... 1 performs its first growth on the following actual server tick.
            if(bean.pending!=null||bean.lastStepTick>=now)continue;
            var work=new Growth(bean);bean.pending=work;game.items().stonekivi().queuedWork(work);
        }

        }finally{callbackDepth--;}
    }
    private final class Bean {
        Marker marker;final LbcIndirectKills.Owner cause;int lastStepTick;Growth pending;
        Bean(Marker marker,LbcIndirectKills.Owner cause,int birth){this.marker=marker;this.cause=cause;lastStepTick=birth;}
    }
    private final class Growth implements LbcStonekivi.Work {
        final Bean bean;boolean cancelled;
        Growth(Bean bean){this.bean=bean;}
        boolean live(){return !cancelled&&beans.get(bean.marker)==bean&&LbcMagicBean.this.live(bean.marker);}
        @Override public boolean ready(){
        if(rewinding)return false;
        callbackDepth++;try{
            if(!live())return true;var marker=bean.marker;var level=(ServerLevel)marker.level();
            if(game.items().timeStop().stopped(level)||marker.entityTags().contains("time_stopped"))return false;
            if(game.items().magic().suppressedAt(level,marker.position()))return true;
            // The next position can move one cell; a terminal footprint extends another twelve.
            return resident(level,marker.blockPosition(),13);

        }finally{callbackDepth--;}
    }
        @Override public boolean step(){
        if(rewinding)return false;
        callbackDepth++;try{
            if(!live()){cancel();return true;}
            if(!ready())return false;
            var marker=bean.marker;var level=(ServerLevel)marker.level();
            if(game.items().magic().suppressedAt(level,marker.position())){remove(bean,true);return true;}
            int now=game.server().getTickCount();
            if(bean.lastStepTick>=now){cancel();return true;}
            bean.lastStepTick=now;
            Runnable growth=()->grow(bean,level);
            if(bean.cause==null)growth.run();else game.items().indirectKills().trapChanges(level,bean.cause,growth);
            cancel();return true;

        }finally{callbackDepth--;}
    }
        @Override public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            checkpointBoundary();var value=new BeanImage(bean,graph.links);boolean stopped=cancelled;
            return links->{checkpointBoundary();var restore=value.prepare(links);return ()->{restore.run();cancelled=stopped;};};
        }
        @Override public void cancel(){
        callbackDepth++;try{cancelled=true;if(bean.pending==this)bean.pending=null;
        }finally{callbackDepth--;}
    }
    }
    private void grow(Bean bean,ServerLevel level){
        var marker=bean.marker;var origin=marker.position();var cell=BlockPos.containing(origin);
        LbcDropBlocks.fill(level,cell.offset(-1,0,-1),cell.offset(1,0,1),Blocks.OAK_WOOD,true);
        var random=level.getRandom();int x=cell.getX(),y=cell.getY(),z=cell.getZ();
        if(random.nextFloat()<.5f)x++;if(random.nextFloat()<.5f)x--;
        if(random.nextFloat()<.5f)z++;if(random.nextFloat()<.5f)z--;if(random.nextFloat()<.5f)y++;
        var moved=new Vec3(x,y,z);marker.setPos(moved);steps++;
        // One completion at the new position, even when both terminal conditions coincide.
        boolean randomEnd=random.nextFloat()<.008f,heightEnd=y>=300;
        if(randomEnd||heightEnd){finish(level,BlockPos.containing(moved));remove(bean,true);}
    }
    private void finish(ServerLevel level,BlockPos at){
        int[][] extents={{8,8},{6,10},{4,12},{10,6},{12,4}};
        for(var rectangle:extents)LbcDropBlocks.fill(level,at.offset(-rectangle[0],0,-rectangle[1]),at.offset(rectangle[0],0,rectangle[1]),Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.WHITE),true);
        var chest=at.above();
        if(!level.isOutsideBuildHeight(chest)&&!level.isDebug())LbcSpecialDrops.placeLootChest(level,chest,LOOT);
        completions++;
    }
    private static boolean resident(ServerLevel level,BlockPos at,int radius){
        for(int x=(at.getX()-radius)>>4;x<=(at.getX()+radius)>>4;x++)for(int z=(at.getZ()-radius)>>4;z<=(at.getZ()+radius)>>4;z++)
            if(level.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }
    private void remove(Bean bean,boolean discard){
        beans.remove(bean.marker);if(bean.pending!=null)bean.pending.cancel();if(discard&&!bean.marker.isRemoved())bean.marker.discard();
    }
    public void loaded(Entity actor){
        callbackDepth++;try{
        if(rewinding||MatchActorCheckpoint.restoring(actor))return;
        if(!managed(actor))return;
        if(!(actor instanceof Marker marker)||!live(marker)){actor.discard();return;}
        if(!beans.containsKey(marker)){
            LbcIndirectKills.Owner cause=null;var author=authorId(marker);
            if(author!=null){
                long stamp=0;for(var tag:marker.entityTags())if(tag.startsWith(ORDER))try{stamp=Long.parseLong(tag.substring(ORDER.length()));}catch(NumberFormatException ignored){}
                var restored=game.items().indirectKills().restoreColdCause(author,stamp,false);
                cause=new LbcIndirectKills.Owner(restored.player(),restored.order(),true,false);
            }
            beans.put(marker,new Bean(marker,cause,game.server().getTickCount()));
        }
        game.items().timeStop().prepareFrozen(marker);

        }finally{callbackDepth--;}
    }
    public void unloaded(Entity actor){
        callbackDepth++;try{if(rewinding||MatchActorCheckpoint.restoring(actor))return;if(actor instanceof Marker marker){var bean=beans.get(marker);if(bean!=null)remove(bean,false);}
        }finally{callbackDepth--;}
    }
    public void stop(){
        callbackDepth++;try{for(var bean:List.copyOf(beans.values()))remove(bean,true);lastTick=Integer.MIN_VALUE;
        }finally{callbackDepth--;}
    }

    /** Native bodies and accepted UUID causes remain canonical; no load/spawn/grow callback is replayed. */
    void checkpointBoundary(){if(callbackDepth!=0)throw new IllegalStateException("Finish native bean load/growth callbacks before checkpointing");}
    void prepareFrozen(){checkpointBoundary();rewinding=true;stagedSteps.clear();stagedClock=null;}
    void publishFrozen(){
        checkpointBoundary();if(!rewinding)return;
        stagedSteps.forEach((bean,frame)->bean.lastStepTick=frame.clock.time(frame.value,game));
        if(stagedClock!=null)lastTick=stagedClock.last(game);
        stagedSteps.clear();stagedClock=null;rewinding=false;
    }
    void cancelFrozen(){
        checkpointBoundary();
        // The shared dispatcher may discard either future or installed work on abort.
        // Clear the matching private pending aliases so a surviving native bean can schedule again.
        if(rewinding)for(var bean:List.copyOf(beans.values()))if(bean.pending!=null)bean.pending.cancel();
        stagedSteps.clear();stagedClock=null;rewinding=false;
    }
    Checkpoint checkpoint(MatchQueuedWorkCheckpoint.Graph graph){checkpointBoundary();return new Checkpoint(graph);}
    private final class BeanImage {
        final Bean original;final MatchDamageSources.Ref marker;final Growth pending;
        final int lastStep;final MatchAuthorityCheckpoint.Clock frame;
        BeanImage(Bean bean,MatchSpellCheckpoint.Links links){
            original=bean;marker=links.key(bean.marker);if(marker.historical()!=null)throw new IllegalStateException("Bean marker requires its canonical native body owner");pending=bean.pending;lastStep=bean.lastStepTick;
            frame=new MatchAuthorityCheckpoint.Clock(game,lastTick,0);
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var current=(Marker)links.get(marker);
            return ()->{original.marker=current;original.pending=pending;stagedSteps.put(original,new StepFrame(frame,lastStep));};
        }
    }
    final class Checkpoint {
        private final MatchSpellCheckpoint.IdentityImage<Marker,Bean> registry;
        private final List<BeanImage> values=new ArrayList<>();
        private final MatchAuthorityCheckpoint.Clock clockImage=new MatchAuthorityCheckpoint.Clock(game,lastTick,0);
        private final long savedSteps=steps,savedCompletions=completions;
        Checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            if(rewinding)throw new IllegalStateException("Publish native bean clocks before checkpointing");
            registry=new MatchSpellCheckpoint.IdentityImage<>(beans,graph.links);
            var seen=Collections.newSetFromMap(new IdentityHashMap<Bean,Boolean>());
            for(var bean:beans.values())if(seen.add(bean)){
                values.add(new BeanImage(bean,graph.links));if(bean.pending!=null)graph.capture(bean.pending);
            }
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var restored=registry.restore(links);var plans=values.stream().map(value->value.prepare(links)).toList();
            return ()->{plans.forEach(Runnable::run);beans=restored;steps=savedSteps;completions=savedCompletions;stagedClock=clockImage;};
        }
    }
}
