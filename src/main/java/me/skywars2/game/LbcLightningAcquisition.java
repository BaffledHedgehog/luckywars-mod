package me.skywars2.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Source items_tick_act natural lightning acquisition, without an invented caster. */
public final class LbcLightningAcquisition {
    private final GameController game;
    private final Set<ItemEntity> transformableItems=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<LightningBolt> lightning=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<ItemEntity,Integer> capturedTick=new IdentityHashMap<>();
    private final Map<ItemEntity,Request> pending=new IdentityHashMap<>();
    private final ArrayDeque<Request> queue=new ArrayDeque<>();
    private LbcStonekivi.Work worker;
    private int generation;
    private int workTick=Integer.MIN_VALUE,usedUnits;
    private long workNanos;

    LbcLightningAcquisition(GameController game) { this.game=game; }
    static boolean transformable(ItemStack stack) {
        // Source adds both transformable and working labels from this same flag.
        return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("transformable",0)==1;
    }
    void loaded(Entity entity) {
        if(entity instanceof ItemEntity item && transformable(item.getItem())) transformableItems.add(item);
        if(entity instanceof LightningBolt bolt) lightning.add(bolt);
    }
    void unloaded(Entity entity) {
        if(entity instanceof ItemEntity item) {
            transformableItems.remove(item); capturedTick.remove(item); pending.remove(item);
        }
        if(entity instanceof LightningBolt bolt) lightning.remove(bolt);
    }
    private boolean activeLevel(ServerLevel level) {
        String dimension=level.dimension().identifier().toString();
        return !dimension.equals("minecraft:nexus") && !dimension.equals("minecraft:imprinted")
                && game.stage()!=GameController.Stage.LOADING && game.stage()!=GameController.Stage.ENDING;
    }
    private boolean paused(ServerLevel level) {
        return game.items().timeStop().stopped(level);
    }
    boolean eligibleRecipient(ServerPlayer player,ServerLevel level) {
        return player!=null && player.level()==level && player.isAlive() && !player.isRemoved()
                && !player.isSpectator() && !game.isObserver(player) && game.canUseGameplayItems(player)
                && !game.awaitingRespawn(player.getUUID());
    }
    private ServerPlayer recipient(ItemEntity item) {
        ServerPlayer nearest=null; double distance=Double.POSITIVE_INFINITY;
        for(var player:((ServerLevel)item.level()).players()) if(eligibleRecipient(player,(ServerLevel)item.level())) {
            double candidate=player.position().distanceToSqr(item.position());
            if(candidate<distance) { nearest=player; distance=candidate; }
        }
        return nearest;
    }
    /** Called before native thunderHit so ordinary item health cannot race acquisition. */
    public void beforeLightningTick(LightningBolt bolt) {
        if(!(bolt.level() instanceof ServerLevel level) || bolt.isRemoved() || !activeLevel(level)) return;
        lightning.add(bolt);
        if(paused(level)) return;
        for(var item:level.getEntitiesOfClass(ItemEntity.class,new AABB(bolt.position(),bolt.position()).inflate(4),
                candidate->candidate.isAlive() && transformable(candidate.getItem())
                        && candidate.position().distanceToSqr(bolt.position())<=16)) capture(item);
    }
    /** Only this lightning's source transformable inputs; never broad item invulnerability. */
    public boolean protectsLightning(ItemEntity item,LightningBolt bolt) {
        return item.level()==bolt.level() && item.level() instanceof ServerLevel level && activeLevel(level)
                && transformable(item.getItem()) && item.position().distanceToSqr(bolt.position())<=16;
    }
    private void capture(ItemEntity item) {
        int tick=game.server().getTickCount();
        if(capturedTick.getOrDefault(item,Integer.MIN_VALUE)==tick) return;
        capturedTick.put(item,tick); transformableItems.add(item);
        if(recipient(item)==null) return;
        var request=pending.get(item);
        if(request!=null) {
            // One source unit each unstopped tick, never one unit per overlapping bolt.
            request.budget=Math.min(request.input.remaining,request.budget+1);
            return;
        }
        var input=game.items().bolts().lightningReforgeInput(item);
        if(input==null) return;
        request=new Request(item,input,game.items().epoch());
        pending.put(item,request); queue.add(request); ensureWorker();
    }
    private final class Request {
        final ItemEntity item;
        final LbcBolts.ReforgeInput input;
        final String epoch;
        int budget=1;
        Request(ItemEntity item,LbcBolts.ReforgeInput input,String epoch) {
            this.item=item; this.input=input; this.epoch=epoch;
        }
        boolean step() {
            if(pending.get(item)!=this || item.isRemoved() || !epoch.equals(game.items().epoch())
                    || !(item.level() instanceof ServerLevel level) || !activeLevel(level)) return true;
            var player=recipient(item);
            if(player==null) return true;
            boolean finished=input.step(player,result->deliver(player,result));
            return finished || --budget<=0;
        }
    }
    private void deliver(ServerPlayer player,ItemStack result) {
        // Inventory overflow must not become another input during the same source tick.
        LbcReforgeCrafting.deliver(player,result,dropped->capturedTick.put(dropped,game.server().getTickCount()));
    }
    private void ensureWorker() {
        if(worker!=null) return;
        int expected=generation;
        worker=new LbcStonekivi.Work() {
            public boolean ready() {
                return expected!=generation || workTick!=game.server().getTickCount()
                        || usedUnits<8 && workNanos<1_000_000;
            }
            public boolean step() {
                if(expected!=generation) { cancel(); return true; }
                int tick=game.server().getTickCount();
                if(workTick!=tick) { workTick=tick; usedUnits=0; workNanos=0; }
                long start=System.nanoTime();
                int pausedRequests=0;
                while(!queue.isEmpty() && usedUnits<8 && workNanos+System.nanoTime()-start<1_000_000) {
                    var request=queue.remove();
                    if(request.item.level() instanceof ServerLevel level && paused(level)) {
                        queue.add(request);
                        if(++pausedRequests>=queue.size()) break;
                        continue;
                    }
                    pausedRequests=0; usedUnits++;
                    if(request.step()) pending.remove(request.item,request); else queue.add(request);
                }
                workNanos+=System.nanoTime()-start;
                if(queue.isEmpty()) { worker=null; return true; }
                return false;
            }
            public void cancel() { if(expected==generation) { queue.clear(); pending.clear(); worker=null; } }
        };
    }
    public void tick() {
        // Query local items per live bolt, including items arriving after its native tick.
        for(var bolt:java.util.List.copyOf(lightning)) {
            if(bolt.isRemoved()) lightning.remove(bolt); else beforeLightningTick(bolt);
        }
        flushQueued();
    }
    void flushQueued() { if(worker!=null) worker.step(); }
    int indexedItemCount() { return transformableItems.size(); }
    int pendingCount() { return pending.size(); }
    public void stop() {
        generation++; worker=null; pending.clear(); queue.clear(); capturedTick.clear();
        transformableItems.clear(); lightning.clear(); workTick=Integer.MIN_VALUE; usedUnits=0; workNanos=0;
    }
}
