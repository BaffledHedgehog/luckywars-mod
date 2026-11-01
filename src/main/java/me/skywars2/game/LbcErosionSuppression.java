package me.skywars2.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** The displaced Protection belongs to the actual stack, including copies and serialized stacks. */
final class LbcErosionSuppression {
    private static final String SAVED="skywars2:erosion_suppression";
    private static final EquipmentSlot[] ARMOR={EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
    private final GameController game;
    private final Predicate<ServerPlayer> eligible;
    private final Map<ServerPlayer,Set<ItemStack>> parked=new IdentityHashMap<>();
    private boolean changing;

    LbcErosionSuppression(GameController game,Predicate<ServerPlayer> eligible){this.game=game;this.eligible=eligible;}
    private static boolean erosion(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("erosion",0)==1;}
    private static CompoundTag saved(ItemStack stack){return LbcFartus.data(stack).getCompound(SAVED).orElse(null);}
    private static boolean same(ItemStack stack,CompoundTag original){return erosion(stack)
            &&original.getStringOr("item","").equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());}
    static boolean parked(ItemStack stack){return saved(stack)!=null;}
    /** Skill8 upgrades canonical Protection without temporarily undoing antimagic suppression. */
    static boolean upgradeParkedProtection(ItemStack stack){
        var original=saved(stack);if(original==null||!same(stack,original))return false;
        int protection=original.getIntOr("protection",0);
        original.putInt("protection",protection==Integer.MAX_VALUE?Integer.MAX_VALUE:protection+1);
        var flags=LbcFartus.data(stack);flags.put(SAVED,original);data(stack,flags);return true;
    }
    private static boolean worn(ServerPlayer player,ItemStack stack){for(var slot:ARMOR)if(player.getItemBySlot(slot)==stack)return true;return false;}
    private boolean jammed(ServerPlayer player){return game.playing()&&eligible.test(player)&&game.items().magic().beaconSuppressed(player.position());}
    private boolean paused(ServerPlayer player){return game.items().wands().timeStopped()||game.items().wands().inputTimeStopped(player)||game.items().timeStop().stopped(player.level());}
    private void remember(ServerPlayer player,ItemStack stack){parked.computeIfAbsent(player,ignored->Collections.newSetFromMap(new IdentityHashMap<>())).add(stack);}
    private static void data(ItemStack stack,CompoundTag value){if(value.isEmpty())stack.remove(DataComponents.CUSTOM_DATA);else stack.set(DataComponents.CUSTOM_DATA,CustomData.of(value));}

    /** Never transfer a displaced level onto an output whose item or armor-family identity changed. */
    boolean recover(ItemStack stack,ServerLevel level){
        var original=saved(stack);if(original==null)return false;
        int amount=original.getIntOr("protection",0);
        boolean same=same(stack,original);
        var flags=LbcFartus.data(stack);flags.remove(SAVED);data(stack,flags);
        if(same&&amount>0){var protection=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
            EnchantmentHelper.updateEnchantments(stack,mutable->mutable.set(protection,amount));}
        return true;
    }
    private boolean suppress(ServerPlayer player,ItemStack stack){
        var original=saved(stack);boolean recovered=original!=null&&!same(stack,original)&&recover(stack,player.level());
        var protection=player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
        int amount=EnchantmentHelper.getItemEnchantmentLevel(protection,stack);
        if(saved(stack)==null){
            if(amount<=0)return recovered;
            var displaced=new CompoundTag();displaced.putInt("protection",amount);displaced.putString("item",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            var flags=LbcFartus.data(stack);flags.put(SAVED,displaced);data(stack,flags);
        }
        remember(player,stack);
        if(amount<=0)return recovered;
        EnchantmentHelper.updateEnchantments(stack,mutable->mutable.set(protection,0));return true;
    }
    private static void changed(ServerPlayer player){player.getInventory().setChanged();player.containerMenu.broadcastChanges();}

    /** Four live equipment references and only previously parked identities; no whole-inventory polling. */
    void tick(ServerPlayer player){
        if(changing)return;changing=true;
        try{
            boolean blocked=jammed(player),changed=false;
            var tracked=parked.get(player);
            if(tracked!=null)for(var stack:List.copyOf(tracked))if(!blocked||!worn(player,stack)||!erosion(stack)){
                changed|=recover(stack,player.level());tracked.remove(stack);
            }
            for(var slot:ARMOR){var stack=player.getItemBySlot(slot);
                if(blocked&&erosion(stack)){
                    if(!paused(player))changed|=suppress(player,stack);
                    else if(parked(stack))remember(player,stack);
                }else changed|=recover(stack,player.level());
            }
            tracked=parked.get(player);if(tracked!=null&&tracked.isEmpty())parked.remove(player);
            if(changed)changed(player);
        }finally{changing=false;}
    }
    private boolean recoverCarried(ServerPlayer player,ItemStack stack,boolean blocked){
        if(blocked&&worn(player,stack)&&erosion(stack)){
            if(!paused(player))return suppress(player,stack);
            if(parked(stack))remember(player,stack);
            return false;
        }
        return recover(stack,player.level());
    }
    /** Event-only recovery includes cursor, crafting and open-container stacks after an actual move/copy. */
    void inventoryChanged(ServerPlayer player){
        if(changing)return;changing=true;
        try{
            boolean blocked=jammed(player),changed=false;
            var tracked=parked.get(player);
            if(tracked!=null)for(var stack:List.copyOf(tracked))if(!blocked||!worn(player,stack)||!erosion(stack)){
                changed|=recover(stack,player.level());tracked.remove(stack);
            }
            var seen=Collections.newSetFromMap(new IdentityHashMap<ItemStack,Boolean>());
            for(int slot=0;slot<player.getInventory().getContainerSize();slot++){var stack=player.getInventory().getItem(slot);if(seen.add(stack))changed|=recoverCarried(player,stack,blocked);}
            for(var slot:player.inventoryMenu.slots)if(seen.add(slot.getItem()))changed|=recoverCarried(player,slot.getItem(),blocked);
            for(var slot:player.containerMenu.slots)if(seen.add(slot.getItem()))changed|=recoverCarried(player,slot.getItem(),blocked);
            if(seen.add(player.inventoryMenu.getCarried()))changed|=recoverCarried(player,player.inventoryMenu.getCarried(),blocked);
            if(seen.add(player.containerMenu.getCarried()))changed|=recoverCarried(player,player.containerMenu.getCarried(),blocked);
            tracked=parked.get(player);if(tracked!=null&&tracked.isEmpty())parked.remove(player);
            if(changed)changed(player);
        }finally{changing=false;}
    }
    void loaded(Entity entity){
        if(entity instanceof ServerPlayer player){inventoryChanged(player);return;}
        if(!(entity.level() instanceof ServerLevel level))return;
        if(entity instanceof ItemEntity item){
            // Work on a copy so native entity-data comparison observes the changed item component.
            var original=item.getItem();if(!parked(original))return;var copy=original.copy();
            if(recover(copy,level)){item.setItem(copy);recover(original,level);}
        }else if(entity instanceof LivingEntity living)for(var slot:EquipmentSlot.values())recover(living.getItemBySlot(slot),level);
    }
    /** Lifecycle cleanup is unconditional, including a wearer still inside a beacon. */
    void forget(ServerPlayer player){
        if(changing)return;changing=true;
        try{
            boolean changed=false;var tracked=parked.remove(player);
            if(tracked!=null)for(var stack:tracked)changed|=recover(stack,player.level());
            for(int slot=0;slot<player.getInventory().getContainerSize();slot++)changed|=recover(player.getInventory().getItem(slot),player.level());
            for(var slot:player.inventoryMenu.slots)changed|=recover(slot.getItem(),player.level());
            for(var slot:player.containerMenu.slots)changed|=recover(slot.getItem(),player.level());
            changed|=recover(player.inventoryMenu.getCarried(),player.level());changed|=recover(player.containerMenu.getCarried(),player.level());
            if(changed)changed(player);
        }finally{changing=false;}
    }
    void stop(){for(var player:List.copyOf(parked.keySet()))forget(player);for(var player:game.server().getPlayerList().getPlayers())forget(player);parked.clear();}
}
