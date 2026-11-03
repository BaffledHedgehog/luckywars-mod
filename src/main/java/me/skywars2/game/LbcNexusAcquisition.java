package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;

/** Persistent environmental cages and the source entity-proximity wire recipe. */
public final class LbcNexusAcquisition implements LbcNexusEcology.Callbacks {
    static final String TAG="skywars2.nexus_cage";
    private static final TicketType TICKET=new TicketType(0,TicketType.FLAG_LOADING|TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    public static void register(){Registry.register(BuiltInRegistries.TICKET_TYPE,Identifier.parse("skywars2:nexus_acquisition"),TICKET);}
    private static final String[] COLORS={"","red","green","black","white"};
    static final class Cage {
        final UUID id;final ServerLevel level;final int color;final Vec3 position;
        UUID rewardId,frameId;Interaction body;ItemDisplay reward,frame;
        Cage(UUID id,ServerLevel level,int color,Vec3 position){this.id=id;this.level=level;this.color=color;this.position=position;}
    }
    private static final class Wire {
        private final ItemEntity body;private final int born,kind;private int visited=Integer.MIN_VALUE;
        Wire(ItemEntity body,int born){this.body=body;this.born=born;this.kind=LbcNexusWireItems.kind(body.getItem());}
        ItemEntity body(){return body;}int born(){return born;}
    }
    private record Image(UUID id,ServerLevel level,int color,Vec3 position,UUID reward,UUID frame,boolean loaded,boolean rewardLoaded,boolean frameLoaded){}
    private final GameController game;
    private final LbcNexusEcology ecology;
    private final Map<UUID,Cage> cages=new LinkedHashMap<>();
    private final Map<UUID,UUID> components=new HashMap<>();
    private final Map<UUID,Wire> wires=new LinkedHashMap<>();
    private final Deque<UUID> reds=new ArrayDeque<>(),pairings=new ArrayDeque<>();
    private final Set<UUID> pending=new HashSet<>(),retired=new HashSet<>(),exchanging=new HashSet<>();
    private final Deque<Clear> clears=new ArrayDeque<>();
    private record Chunk(ServerLevel level,ChunkPos position){}
    private final Map<Chunk,CompletableFuture<?>> loading=new HashMap<>();
    private CleanupWork cleanup;
    private record Clear(ServerLevel level,BlockPos position,LbcIndirectKills.Owner cause){}
    private boolean restoring;private int lastTick=Integer.MIN_VALUE;
    LbcNexusAcquisition(GameController game){this.game=game;ecology=new LbcNexusEcology(game,(level,point,owner)->true,this);}
    LbcNexusEcology ecology(){return ecology;}
    List<Cage> cages(){return List.copyOf(cages.values());}
    int pendingPairings(){return pending.size();}
    int pendingTerrain(){return clears.size();}
    int loadingChunks(){return loading.size();}
    private void releaseTickets(){for(var key:loading.keySet())key.level().getChunkSource().removeTicketWithRadius(TICKET,key.position(),0);loading.clear();}
    private Set<Chunk> chunks(Clear clear){var result=new HashSet<Chunk>();var p=clear.position();for(int x=(p.getX()-1)>>4;x<=((p.getX()+1)>>4);x++)for(int z=(p.getZ()-1)>>4;z<=((p.getZ()+1)>>4);z++)result.add(new Chunk(clear.level(),new ChunkPos(x,z)));return result;}
    private boolean resident(Clear clear){boolean ready=true;for(var key:chunks(clear)){
        if(key.level().getChunkSource().getChunkNow(key.position().x(),key.position().z())!=null)continue;
        var future=loading.get(key);if(future!=null&&(future.isCompletedExceptionally()||future.isCancelled())){key.level().getChunkSource().removeTicketWithRadius(TICKET,key.position(),0);loading.remove(key);}
        if(loading.size()<8)loading.computeIfAbsent(key,k->k.level().getChunkSource().addTicketAndLoadWithRadius(TICKET,k.position(),0));ready=false;
    }return ready;}
    private static boolean nexus(ServerLevel level){return level.dimension().equals(LbcHedgehogDimensions.NEXUS);}
    private static int color(Entity entity){for(int i=1;i<COLORS.length;i++)if(entity.entityTags().contains("wire_converter_"+COLORS[i]))return i;return 0;}
    private static UUID identity(Entity entity,String key){String prefix=TAG+"."+key+".";for(var tag:entity.entityTags())if(tag.startsWith(prefix))try{return UUID.fromString(tag.substring(prefix.length()));}catch(IllegalArgumentException ignored){}return null;}
    private static void identity(Entity entity,String key,UUID value){if(value!=null)entity.entityTags().add(TAG+"."+key+"."+value);}
    private void queuePair(Cage cage){if(pending.add(cage.id))pairings.addLast(cage.id);}
    private void bind(Cage cage,ItemDisplay display,boolean reward){
        UUID owner=components.get(display.getUUID());if(owner!=null&&!owner.equals(cage.id))return;
        UUID tagged=identity(display,"owner");if(tagged!=null&&!tagged.equals(cage.id))return;
        components.put(display.getUUID(),cage.id);identity(display,"owner",cage.id);
        if(reward){cage.reward=display;cage.rewardId=display.getUUID();identity(cage.body,"reward",cage.rewardId);}
        else{cage.frame=display;cage.frameId=display.getUUID();identity(cage.body,"frame",cage.frameId);}
    }
    private void pair(Cage cage){
        if(cage.body==null||cage.body.isRemoved())return;
        var point=cage.position.add(0,.5,0);
        var displays=cage.level.getEntitiesOfClass(ItemDisplay.class,new AABB(point,point).inflate(1),d->!d.isRemoved()&&d.position().distanceToSqr(point)<=1);
        displays.sort(Comparator.comparingDouble(d->d.position().distanceToSqr(point)));
        for(var display:displays){
            if(display.entityTags().contains("wire_converter_item")&&cage.reward==null&&(cage.rewardId==null||cage.rewardId.equals(display.getUUID())))bind(cage,display,true);
            if(display.entityTags().contains("wire_converter_frame")&&cage.frame==null&&(cage.frameId==null||cage.frameId.equals(display.getUUID())))bind(cage,display,false);
        }
        if(cage.reward!=null&&cage.frame!=null)pending.remove(cage.id);else queuePair(cage);
    }
    void loaded(Entity entity){
        if(entity.isRemoved())return;
        if(restoring)return;
        ecology.loaded(entity);
        if(retired.remove(entity.getUUID())){entity.discard();return;}
        if(!(entity.level() instanceof ServerLevel level))return;
        if(entity instanceof ItemEntity item&&nexus(level)&&LbcNexusWireItems.kind(item.getItem())>=1&&LbcNexusWireItems.kind(item.getItem())<=3){
            if(wires.putIfAbsent(item.getUUID(),new Wire(item,game.server().getTickCount()))==null&&LbcNexusWireItems.isRed(item.getItem()))reds.addLast(item.getUUID());
        }
        if(entity instanceof Interaction body&&color(body)>0){
            var cage=cages.computeIfAbsent(body.getUUID(),id->new Cage(id,level,color(body),body.position()));cage.body=body;
            if(cage.rewardId==null)cage.rewardId=identity(body,"reward");if(cage.frameId==null)cage.frameId=identity(body,"frame");
            body.entityTags().add(TAG);queuePair(cage);pair(cage);
        }else if(entity instanceof ItemDisplay display&&(display.entityTags().contains("wire_converter_item")||display.entityTags().contains("wire_converter_frame"))){
            UUID owner=identity(display,"owner");var known=owner==null?null:cages.get(owner);if(known!=null){queuePair(known);pair(known);}
            else for(var body:level.getEntitiesOfClass(Interaction.class,display.getBoundingBox().inflate(1.5),b->color(b)>0)){
                var cage=cages.get(body.getUUID());if(cage!=null){queuePair(cage);pair(cage);}
            }
        }
    }
    /** Native ItemEntity#setItem notifications also admit previously unrelated dropped stacks. */
    public void itemChanged(ItemEntity item){
        if(item.isRemoved()){wires.remove(item.getUUID());return;}
        if(restoring)return;var previous=wires.get(item.getUUID());int kind=LbcNexusWireItems.kind(item.getItem());
        if(previous!=null&&previous.kind!=kind)wires.remove(item.getUUID());
        if(kind>=1&&kind<=3)loaded(item);else wires.remove(item.getUUID());
    }
    void unloaded(Entity entity){
        ecology.unloaded(entity);wires.remove(entity.getUUID());var cage=cages.get(entity.getUUID());
        if(cage!=null&&cage.body==entity){cage.body=null;if(entity.getRemovalReason()!=Entity.RemovalReason.UNLOADED_TO_CHUNK){remove(cage);return;}queuePair(cage);}
        UUID owner=components.get(entity.getUUID());if(owner!=null){cage=cages.get(owner);if(cage!=null){if(cage.reward==entity)cage.reward=null;if(cage.frame==entity)cage.frame=null;queuePair(cage);}}
    }
    private void discard(UUID id,Entity body){if(id==null)return;components.remove(id);if(body!=null)body.discard();else retired.add(id);}
    private void remove(Cage cage){if(cages.remove(cage.id)!=cage)return;pending.remove(cage.id);discard(cage.rewardId,cage.reward);discard(cage.frameId,cage.frame);if(cage.body!=null)cage.body.discard();else retired.add(cage.id);}
    private boolean paused(Entity entity){return restoring||entity.entityTags().contains("time_stopped")||entity.level() instanceof ServerLevel level&&game.items().timeStop().stopped(level);}
    private boolean running(){return !restoring&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.server().tickRateManager().runsNormally()&&!game.items().wands().legacyTimeStopped();}
    public InteractionResult use(ServerPlayer player,InteractionHand hand,Entity target){
        if(!(target instanceof Interaction)||color(target)==0)return InteractionResult.PASS;
        if(!running()||target.isRemoved()||player==null||!player.isAlive()||player.isSpectator()||game.isObserver(player)||!game.canUseGameplayItems(player)
                ||player.level().dimension().equals(LbcHedgehogDimensions.IMPRINTED)||game.items().actionsBlocked(player)||!game.items().wands().validRoleTarget(player,false)
                ||game.items().wands().inputTimeStopped(player)||paused(player)||paused(target)||player.level()!=target.level()||!player.isWithinEntityInteractionRange(target,0))return InteractionResult.FAIL;
        var cage=cages.get(target.getUUID());if(cage==null){loaded(target);cage=cages.get(target.getUUID());}
        if(cage==null||cage.body!=target||!exchanging.add(cage.id))return InteractionResult.FAIL;
        try{
            pair(cage);var held=player.getItemInHand(hand);
            if(LbcNexusWireItems.kind(held)!=cage.color||cage.reward==null||cage.frame==null||cage.reward.isRemoved()||cage.frame.isRemoved()||paused(cage.reward)||paused(cage.frame)||cage.reward.getItemStack().isEmpty())return InteractionResult.FAIL;
            var output=drop(cage.level,cage.position.add(0,.5,0),cage.reward.getItemStack().copy());output.setDeltaMovement(Vec3.ZERO);
            if(!cage.level.addFreshEntity(output))return InteractionResult.FAIL;
            LbcPhantomArmor.used(player,InteractionResult.SUCCESS);held.shrink(1);player.getInventory().setChanged();game.items().mana().inventoryChanged(player);remove(cage);
            LbcOverload.sound(cage.level,cage.position,"minecraft:block.beacon.deactivate",1,1,0,0);
            cage.level.sendParticles(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER,cage.position.x,cage.position.y,cage.position.z,1000,1,1,1,0);
            return InteractionResult.SUCCESS;
        }finally{exchanging.remove(target.getUUID());}
    }
    private static ItemEntity drop(ServerLevel level,Vec3 point,ItemStack stack){var entity=new ItemEntity(level,point.x,point.y,point.z,stack);entity.setDefaultPickUpDelay();entity.setNoGravity(true);return entity;}
    @Override public boolean emitGreen(ServerLevel level,Vec3 point,LbcIndirectKills.Owner cause){
        var output=drop(level,point,new ItemStack(LbcNexusWireItems.BLUE));output.setGlowingTag(true);if(!level.addFreshEntity(output))return false;
        LbcOverload.sound(level,point,"minecraft:lbcsounds.laser_mg_rev_down",3,0,0,0);return true;
    }
    @Override public void emitBlack(ServerLevel level,Vec3 point,LbcIndirectKills.Owner cause){level.addFreshEntity(drop(level,point,new ItemStack(LbcNexusWireItems.BLACK)));}
    @Override public boolean nearCage(ServerLevel level,Vec3 point,double minimum,double maximum){
        for(var display:level.getEntitiesOfClass(ItemDisplay.class,new AABB(point,point).inflate(maximum),d->d.entityTags().contains("wire_converter_frame"))){double distance=display.position().distanceToSqr(point);if(distance>=minimum*minimum&&distance<=maximum*maximum)return true;}return false;
    }
    private void clearCube(ServerLevel level,BlockPos center,LbcIndirectKills.Owner cause){game.items().indirectKills().capturedWorldChanges(level,cause,()->{
        for(var pos:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1)))if(!level.isOutsideBuildHeight(pos))LbcDropBlocks.setBlock(level,pos,Blocks.AIR.defaultBlockState(),true);
    });}
    @Override public void createCage(ServerLevel level,Vec3 point,int color,LbcIndirectKills.Owner cause){
        if(restoring||!nexus(level)||color<1||color>4)return;
        var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,point).create(LootContextParamSets.COMMAND);
        var rolled=game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:nexus_item_"+COLORS[color]))).getRandomItems(params);
        if(rolled.size()!=1||rolled.getFirst().isEmpty())return;
        clearCube(level,BlockPos.containing(point),cause);
        var base=Vec3.atBottomCenterOf(BlockPos.containing(point));var body=new Interaction(EntityTypes.INTERACTION,level);body.setPos(base);body.setWidth(1);body.setHeight(1);body.setResponse(true);body.entityTags().addAll(Set.of("nexus_entity","wire_converter_"+COLORS[color],TAG));
        var cage=new Cage(body.getUUID(),level,color,base);cage.body=body;
        var reward=display(level,base.add(0,.5,0),"wire_converter_item",rolled.getFirst().copy());
        var shell=new ItemStack(Items.REPEATING_COMMAND_BLOCK);shell.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:nexus_item_cage_"+COLORS[color]));
        var frame=display(level,base.add(0,.5,0),"wire_converter_frame",shell);
        bind(cage,reward,true);bind(cage,frame,false);cages.put(cage.id,cage);
        if(!level.addFreshEntity(reward)||!level.addFreshEntity(frame)||!level.addFreshEntity(body))remove(cage);
    }
    private static ItemDisplay display(ServerLevel level,Vec3 point,String tag,ItemStack stack){var display=new ItemDisplay(EntityTypes.ITEM_DISPLAY,level);display.setPos(point);display.setItemStack(stack);display.setItemTransform(ItemDisplayContext.FIXED);display.setTransformationInterpolationDuration(3);display.setPosRotInterpolationDuration(3);display.entityTags().addAll(Set.of("nexus_entity",tag,TAG));return display;}
    @Override public void finishTree(ServerLevel level,Vec3 point,LbcIndirectKills.Owner cause){
        for(var cage:List.copyOf(cages.values()))if(cage.level==level&&cage.frame!=null&&!cage.frame.isRemoved())clears.addLast(new Clear(level,cage.frame.blockPosition(),cause));ecology.clearTrees(level,point,100);queueCleanup();
    }
    private void queueCleanup(){if(cleanup==null&&!clears.isEmpty()){cleanup=new CleanupWork();game.items().stonekivi().queuedWork(cleanup);}}
    /** Shared construction work makes each strict cube a barrier-owned atomic write. */
    private final class CleanupWork implements LbcStonekivi.Work {
        boolean cancelled;
        @Override public boolean ready(){return cancelled||running();}
        @Override public boolean step(){
            if(cancelled)return true;if(clears.isEmpty()){if(cleanup==this)cleanup=null;return true;}
            var clear=clears.removeFirst();var level=clear.level();
            if(game.items().timeStop().stopped(level)||!resident(clear))clears.addLast(clear);
            else{clearCube(level,clear.position(),clear.cause());for(var key:chunks(clear))if(loading.remove(key)!=null)key.level().getChunkSource().removeTicketWithRadius(TICKET,key.position(),0);}
            if(clears.isEmpty()){if(cleanup==this)cleanup=null;return true;}return false;
        }
        @Override public void cancel(){cancelled=true;releaseTickets();if(cleanup==this)cleanup=null;}
        @Override public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            // The acquisition's typed checkpoint owns the immutable remaining cube sequence.
            return links->()->{cancelled=false;cleanup=this;};
        }
    }
    @Override public void clearSpreadDisplays(ServerLevel level,Vec3 point){
        var owned=new HashSet<UUID>();for(var body:level.getEntitiesOfClass(Interaction.class,new AABB(point,point).inflate(2.5),b->color(b)>0&&b.position().distanceToSqr(point)<=6.25))owned.add(body.getUUID());
        for(var display:level.getEntitiesOfClass(ItemDisplay.class,new AABB(point,point).inflate(2.5),d->components.containsKey(d.getUUID())&&d.position().distanceToSqr(point)<=6.25))owned.add(components.get(display.getUUID()));
        for(var id:owned){var cage=cages.get(id);if(cage!=null)remove(cage);}
    }
    private void craft(Wire red,int tick){
        var body=red.body();if(body.isRemoved()||paused(body)||!LbcNexusWireItems.isRed(body.getItem()))return;
        var level=(ServerLevel)body.level();ItemEntity green=null,black=null;
        for(var item:level.getEntitiesOfClass(ItemEntity.class,body.getBoundingBox().inflate(3),e->e.position().distanceToSqr(body.position())<=9&&!e.isRemoved()&&!paused(e))){
            var indexed=wires.get(item.getUUID());if(indexed==null)continue;
            int kind=LbcNexusWireItems.kind(item.getItem());if(kind==2&&green==null)green=item;else if(kind==3&&black==null)black=item;if(green!=null&&black!=null)break;
        }
        if(green==null||black==null)return;
        if(!level.addFreshEntity(drop(level,body.position(),new ItemStack(LbcNexusWireItems.WHITE))))return;
        for(var item:List.of(body,green,black)){var stack=item.getItem().copy();stack.shrink(1);if(stack.isEmpty())item.discard();else item.setItem(stack);}
        var point=body.position();level.sendParticles(ParticleTypes.CRIT,point.x,point.y,point.z,100,.1,.1,.1,0);LbcOverload.sound(level,point,"minecraft:entity.squid.death",1,0,0,0);
    }
    public void tick(){
        int tick=game.server().getTickCount();if(lastTick==tick||!running())return;lastTick=tick;ecology.tick();
        if(cleanup==null&&!clears.isEmpty()){cleanup=new CleanupWork();game.items().stonekivi().queuedWork(cleanup);}
        long now=System.nanoTime(),until=Math.max(now+100_000,Math.min(EffectTerrain.deadlineNanos(),now+1_000_000));int work=0;
        int pairs=pairings.size();while(pairs-->0&&!pairings.isEmpty()&&work++<64&&System.nanoTime()<until){var id=pairings.removeFirst();if(!pending.remove(id))continue;var cage=cages.get(id);if(cage!=null){pair(cage);if(cage.body==null)queuePair(cage);}}
        int count=reds.size();while(count-->0&&!reds.isEmpty()&&work++<192&&System.nanoTime()<until){var id=reds.removeFirst();var wire=wires.get(id);if(wire==null||wire.visited==tick)continue;wire.visited=tick;if(wire.body().isRemoved()||!LbcNexusWireItems.isRed(wire.body().getItem())){wires.remove(id);continue;}craft(wire,tick);if(wires.containsKey(id))reds.addLast(id);}
        queueCleanup();
        if(tick%20==0)for(var player:game.server().getPlayerList().getPlayers())if(nexus(player.level())&&!player.isSpectator()&&!game.isObserver(player)&&nearCage(player.level(),player.position(),0,5))game.items().mana().award(player,"lbc:true_advancements/nexus/cage");
    }
    public void stop(){ecology.stop();if(cleanup!=null)cleanup.cancel();releaseTickets();restoring=false;}
    public void clear(){for(var cage:List.copyOf(cages.values()))remove(cage);ecology.clear();if(cleanup!=null)cleanup.cancel();releaseTickets();wires.clear();reds.clear();pairings.clear();pending.clear();clears.clear();}
    void prepareFrozen(){ecology.prepareFrozen();if(cleanup!=null)cleanup.cancel();releaseTickets();cages.clear();components.clear();wires.clear();reds.clear();pairings.clear();pending.clear();retired.clear();clears.clear();restoring=true;}
    void publishFrozen(){restoring=false;lastTick=game.server().getTickCount();ecology.publishFrozen();}
    Checkpoint checkpoint(){return new Checkpoint();}
    final class Checkpoint {
        final List<Image> images=cages.values().stream().filter(c->c.body==null||!c.body.isRemoved()).map(c->new Image(c.id,c.level,c.color,c.position,c.rewardId,c.frameId,c.body!=null,c.reward!=null&&!c.reward.isRemoved(),c.frame!=null&&!c.frame.isRemoved())).toList();
        final List<UUID> wireIds=wires.values().stream().filter(w->!w.body().isRemoved()).map(w->w.body().getUUID()).toList(),redOrder=reds.stream().filter(wireIds::contains).toList();final List<Clear> terrain=List.copyOf(clears);final Set<UUID> cancelled=Set.copyOf(retired);
        final LbcNexusEcology.Checkpoint environment=ecology.checkpoint();
        MatchFieldCheckpoint.Plan prepare(Function<UUID,Entity> lookup){
            var actors=environment.prepare(lookup);var missing=new LinkedHashSet<>(actors.pending());var restored=new LinkedHashMap<UUID,Cage>();var restoredWires=new LinkedHashMap<UUID,Wire>();
            for(var image:images){var cage=new Cage(image.id(),image.level(),image.color(),image.position());cage.rewardId=image.reward();cage.frameId=image.frame();
                if(lookup.apply(cage.id) instanceof Interaction body)cage.body=body;else if(image.loaded())missing.add(cage.id);
                if(cage.rewardId!=null&&lookup.apply(cage.rewardId) instanceof ItemDisplay reward)cage.reward=reward;else if(image.rewardLoaded()&&cage.rewardId!=null)missing.add(cage.rewardId);
                if(cage.frameId!=null&&lookup.apply(cage.frameId) instanceof ItemDisplay frame)cage.frame=frame;else if(image.frameLoaded()&&cage.frameId!=null)missing.add(cage.frameId);restored.put(cage.id,cage);
            }
            for(var id:wireIds)if(lookup.apply(id) instanceof ItemEntity item)restoredWires.put(id,new Wire(item,-1));else missing.add(id);
            return new MatchFieldCheckpoint.Plan(Set.copyOf(missing),()->{actors.install().run();cages.clear();cages.putAll(restored);components.clear();pairings.clear();pending.clear();for(var cage:cages.values()){if(cage.rewardId!=null)components.put(cage.rewardId,cage.id);if(cage.frameId!=null)components.put(cage.frameId,cage.id);if(cage.body==null||cage.reward==null||cage.frame==null)queuePair(cage);}wires.clear();wires.putAll(restoredWires);reds.clear();reds.addAll(redOrder);clears.clear();clears.addAll(terrain);retired.clear();retired.addAll(cancelled);});
        }
    }
}
