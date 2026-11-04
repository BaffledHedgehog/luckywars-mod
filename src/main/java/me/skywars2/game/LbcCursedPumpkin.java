package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Blocks;

/** Source inventory walk. Immutable item components own storage; this owner keeps only sparse clocks/indexes. */
public final class LbcCursedPumpkin {
    private static final EquipmentSlot[] EQUIPMENT={EquipmentSlot.OFFHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    // Exact finite legacy GUI graph, not world/inventory-grid geometric inference.
    private static final int[][] NEXT={
        {1,27},{0,2,28},{1,3,29},{2,4,30},{3,5,31},{4,6,32},{5,7,33},{6,8,34},{7,35},
        {10,18,40},{9,11,19},{10,12,20},{11,13,21},{12,14,22,36},{13,15,23},{14,16,24},{15,17,25},{16,26},
        {9,19,27},{10,18,20,28},{11,19,21,29},{12,20,22,30},{13,21,23,31},{14,22,24,32},{15,23,25,33},{16,24,26,34},{17,25,35},
        {18,28,0},{19,27,29,1},{20,28,30,2},{21,29,31,3},{22,30,32,4},{23,31,33,5},{24,32,34,6},{25,33,35,7},{26,34,8},
        {40,13},{38},{37,39},{38,40},{39,36,9}
    };
    private final GameController game;
    private final Set<UUID> holders=new LinkedHashSet<>(),dirty=new LinkedHashSet<>();
    private final Map<UUID,ItemEntity> dropped=new LinkedHashMap<>();
    private final Map<UUID,Long> cursors=new LinkedHashMap<>();
    private final Map<ServerLevel,Integer> phases=new IdentityHashMap<>();
    private int lastTick=Integer.MIN_VALUE;
    private boolean restoring;
    public LbcCursedPumpkin(GameController game){this.game=game;}
    private boolean paused(Entity actor){return !game.server().tickRateManager().runsNormally()
            ||game.items().timeStop().frozen(actor)||game.items().timeStop().stopped((ServerLevel)actor.level());}
    private boolean ordinary(Entity actor){return LbcDimensionItems.ordinary(actor.level());}
    private boolean eligible(ServerPlayer p){return p.isAlive()&&!p.isRemoved()&&!p.isSpectator()&&!game.isObserver(p)
            &&game.canUseGameplayItems(p)&&ordinary(p)&&!paused(p)&&!game.items().actionsBlocked(p);}
    static ItemStack get(ServerPlayer p,int slot){return slot<36?p.getInventory().getItem(slot):p.getItemBySlot(EQUIPMENT[slot-36]);}
    static void set(ServerPlayer p,int slot,ItemStack stack){if(slot<36)p.getInventory().setItem(slot,stack);else p.setItemSlot(EQUIPMENT[slot-36],stack);}
    public void inventoryChanged(ServerPlayer p){if(!restoring)dirty.add(p.getUUID());}
    public void loaded(Entity actor){
        if(restoring)return;
        if(actor instanceof ServerPlayer p){inventoryChanged(p);return;}
        if(actor instanceof ItemEntity item)itemChanged(item);
    }
    public void itemChanged(ItemEntity item){
        if(restoring)return;
        if(!item.isRemoved()&&LbcCursedPumpkinItems.curse(item.getItem())){dropped.put(item.getUUID(),item);phases.putIfAbsent((ServerLevel)item.level(),0);if(item.getItem().has(LbcCursedPumpkinItems.PROGRESS))cursors.put(item.getUUID(),item.getItem().get(LbcCursedPumpkinItems.PROGRESS));}
        else {dropped.remove(item.getUUID());cursors.remove(item.getUUID());}
    }
    public void unloaded(Entity actor){if(restoring)return;dropped.remove(actor.getUUID());if(actor.getRemovalReason()!=Entity.RemovalReason.UNLOADED_TO_CHUNK)cursors.remove(actor.getUUID());}
    public void forget(ServerPlayer p){holders.remove(p.getUUID());dirty.remove(p.getUUID());}
    /** Legacy storage is decoded once on admission, never serialized/read wholesale every tick. */
    private List<ItemStackTemplate> contents(ItemStack stack,ServerLevel level){
        List<ItemStackTemplate> saved=stack.getOrDefault(LbcCursedPumpkinItems.CONTENTS,List.of());
        if(!ItemData.contains(stack,"storage"))return saved;
        var legacy=stack.get(DataComponents.CUSTOM_DATA).copyTag();var result=new ArrayList<ItemStackTemplate>(saved);
        for(var tag:legacy.getListOrEmpty("storage")){
            var value=ItemStack.CODEC.parse(level.registryAccess().createSerializationContext(NbtOps.INSTANCE),tag).result().orElse(null);
            if(value==null)return null; // Invalid old data never consumes or overwrites another stack.
            if(!value.isEmpty())result.add(ItemStackTemplate.fromNonEmptyStack(value));
        }
        var immutable=List.copyOf(result);stack.set(LbcCursedPumpkinItems.CONTENTS,immutable);
        legacy.remove("storage");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(legacy));return immutable;
    }
    private void walk(ServerPlayer p){
        if(!eligible(p)||game.items().magic().suppressed(p)||game.items().inventoryHazards().protectedInventory(p))return;
        boolean changed=false;
        for(int slot=0;slot<NEXT.length;slot++){
            var pumpkin=get(p,slot);if(!LbcCursedPumpkinItems.curse(pumpkin))continue;
            var saved=contents(pumpkin,p.level());if(saved==null)continue;
            int target=NEXT[slot][p.getRandom().nextInt(NEXT[slot].length)];var victim=get(p,target);
            var moved=pumpkin.copy();
            if(!victim.isEmpty()){
                var all=new ArrayList<ItemStackTemplate>(saved);all.add(ItemStackTemplate.fromNonEmptyStack(victim));
                moved.set(LbcCursedPumpkinItems.CONTENTS,List.copyOf(all));
            }
            // Source slot9/offhand stamp boots even when moving back into the bag;
            // feet stamps leggings even for its offhand/bag destinations.
            if(slot==9||slot==36||slot>=37){
                var equipment=slot==9||slot==36?EquipmentSlot.FEET:slot==40?EquipmentSlot.LEGS:EQUIPMENT[target-36];
                moved.set(DataComponents.EQUIPPABLE,Equippable.builder(equipment).build());
            }
            set(p,target,moved);set(p,slot,new ItemStack(Items.PUMPKIN_SEEDS));changed=true;
        }
        if(changed){p.inventoryMenu.broadcastChanges();inventoryChanged(p);LbcOverload.sound(p.level(),p.position(),"minecraft:lbcsounds.hahahahaha",.4f,2,0,0);}
    }
    public void damaged(LivingEntity victim,DamageSource source){
        if(!(victim instanceof ServerPlayer target)||!(source.getEntity() instanceof ServerPlayer author)
                ||!LbcDamage.isMeleeAttack(source,author)||!LbcCursedPumpkinItems.sword(author.getMainHandItem())
                ||!eligible(author)||!eligible(target)||!game.items().wands().projectileTarget(author,target)
                ||game.items().magic().suppressed(author)||game.items().magic().suppressed(target)
                ||game.items().inventoryHazards().protectedInventory(target))return;
        for(var stack:game.lbc().rollTable(target.level(),target.position(),target,"lbc:halloween/pumpkin_curse")){
            if(!target.getInventory().add(stack)&&!stack.isEmpty()){
                var drop=new ItemEntity(target.level(),target.getX(),target.getY(),target.getZ(),stack);
                drop.setDefaultPickUpDelay();drop.setThrower(target);target.level().addFreshEntity(drop);
            }
        }
        inventoryChanged(target);
        target.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.PUMPKIN.defaultBlockState()),target.getX(),target.getY()+1,target.getZ(),59,.3,.6,.3,0);
        LbcOverload.sound(target.level(),target.position(),"minecraft:lbcsounds.hahahahaha",1,1,0,0);
    }
    private void unpack(ItemEntity item,int budget){
        if(item.isRemoved()||!ordinary(item)||paused(item)||!item.level().hasChunkAt(item.blockPosition()))return;
        var stack=item.getItem();if(!LbcCursedPumpkinItems.curse(stack))return;
        var saved=contents(stack,(ServerLevel)item.level());if(saved==null)return;
        long total=saved.size(),cursor=Math.clamp(stack.getOrDefault(LbcCursedPumpkinItems.PROGRESS,0L),0,total);
        if(total==0){item.discard();dropped.remove(item.getUUID());cursors.remove(item.getUUID());return;}
        item.setPickUpDelay(32767);item.setPermanentlyInvulnerable(true);item.setUnlimitedLifetime();
        long end=Math.min(total,cursor+budget);
        while(cursor<end){
            var output=saved.get((int)(cursor%saved.size())).create();
            var result=new ItemEntity(item.level(),item.getX(),item.getY(),item.getZ(),output);
            result.setDefaultPickUpDelay();if(item.getOwner()!=null)result.setThrower(item.getOwner());
            game.items().markCurrentItemEpoch(result);game.items().spellSources().inherit(item,result);
            if(!((ServerLevel)item.level()).addFreshEntity(result))break;
            cursor++;
        }
        cursors.put(item.getUUID(),cursor);
        stack.set(LbcCursedPumpkinItems.PROGRESS,cursor);
        if(cursor==total){item.discard();dropped.remove(item.getUUID());cursors.remove(item.getUUID());}
    }
    public void tick(){
        int now=game.server().getTickCount();if(restoring||lastTick==now||!game.playing())return;lastTick=now;
        for(var id:List.copyOf(dirty)){
            dirty.remove(id);var p=game.server().getPlayerList().getPlayer(id);if(p==null){holders.remove(id);continue;}
            boolean found=false;for(int slot=0;slot<NEXT.length&&!found;slot++)found=LbcCursedPumpkinItems.curse(get(p,slot));
            if(found){holders.add(id);phases.putIfAbsent(p.level(),0);}else holders.remove(id);
        }
        for(var level:List.copyOf(phases.keySet())){
            if(!game.server().tickRateManager().runsNormally()||game.items().timeStop().stopped(level))continue;
            int phase=phases.get(level)+1;phases.put(level,phase);
            if(phase%10==0)for(var id:List.copyOf(holders)){var p=game.server().getPlayerList().getPlayer(id);if(p!=null&&p.level()==level)walk(p);}
            int remaining=64;
            for(var item:List.copyOf(dropped.values())){
                if(item.isRemoved()){dropped.remove(item.getUUID());continue;}
                if(item.level()!=level||phase%20!=0&&!cursors.containsKey(item.getUUID())||remaining<=0)continue;
                int batch=Math.min(16,remaining);unpack(item,batch);remaining-=batch;
            }
        }
    }
    public void clear(){holders.clear();dirty.clear();dropped.clear();cursors.clear();phases.clear();restoring=false;}
    public void prepareFrozen(){restoring=true;holders.clear();dirty.clear();dropped.clear();cursors.clear();phases.clear();}
    public void publishFrozen(){restoring=false;lastTick=game.server().getTickCount();}
    Checkpoint checkpoint(){return new Checkpoint();}
    final class Checkpoint{
        final Set<UUID> savedHolders=Set.copyOf(holders),savedDirty=Set.copyOf(dirty);
        final Map<ServerLevel,Integer> clocks=new IdentityHashMap<>(phases);
        final Map<UUID,Long> progress=Map.copyOf(cursors);
        final List<UUID> items=List.copyOf(dropped.keySet());
        MatchFieldCheckpoint.Plan prepare(Function<UUID,Entity> lookup){
            var restored=new LinkedHashMap<UUID,ItemEntity>();var missing=new LinkedHashSet<UUID>();
            for(var id:items)if(lookup.apply(id) instanceof ItemEntity item)restored.put(id,item);else missing.add(id);
            return new MatchFieldCheckpoint.Plan(Set.copyOf(missing),()->{holders.clear();holders.addAll(savedHolders);dirty.clear();dirty.addAll(savedDirty);phases.clear();phases.putAll(clocks);dropped.clear();dropped.putAll(restored);cursors.clear();cursors.putAll(progress);});
        }
    }
}
