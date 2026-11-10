package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LbcEquipmentMixin {
    /** Нативный прыжок живёт в LivingEntity; броня реагирует без опроса статистики jump каждый тик. */
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void skywars2$overloadJump(CallbackInfo ci) {
        if((Object)this instanceof ServerPlayer player)me.skywars2.game.GroundJumpEvents.jumped(player);
    }
    /** Normalize old miniguns before vanilla snapshots and applies equipment attribute modifiers. */
    @Inject(method = "onEquipItem", at = @At("HEAD"))
    private void skywars2$minigunEquipment(EquipmentSlot slot, ItemStack previous, ItemStack next, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayer) me.skywars2.game.LbcMinigunMovement.ensureStack(next);
    }
    /** RETURN покрывает ранние выходы vanilla (firstTick/observer), в том числе начальную экипировку. */
    @Inject(method = "onEquipItem", at = @At("RETURN"))
    private void skywars2$rocketEquipment(EquipmentSlot slot, ItemStack previous, ItemStack next, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayer player)
            skywars2$refreshEquipment(player,slot==EquipmentSlot.HEAD,slot==EquipmentSlot.FEET);
    }
    @Shadow private java.util.Map<EquipmentSlot,ItemStack> lastEquipmentItems;
    @Unique private boolean skywars2$refreshingEquipment;
    /** Native snapshots cover hotbar use, dispensers and direct inventory equipment writes too. */
    @Inject(method="detectEquipmentUpdates",at=@At("HEAD"))
    private void skywars2$actualEquipment(CallbackInfo ci){
        if(skywars2$refreshingEquipment || !((Object)this instanceof ServerPlayer player))return;
        boolean changed=false,head=false,feet=false;
        for(var slot:EquipmentSlot.VALUES)if(!ItemStack.matches(lastEquipmentItems.get(slot),player.getItemBySlot(slot))){
            changed=true;head|=slot==EquipmentSlot.HEAD;feet|=slot==EquipmentSlot.FEET;
        }
        if(!changed)return;
        skywars2$refreshingEquipment=true;
        try{skywars2$refreshEquipment(player,head,feet);}finally{skywars2$refreshingEquipment=false;}
    }
    @Unique private static void skywars2$refreshEquipment(ServerPlayer player,boolean head,boolean feet){
        var game=SkyWars2.game();if(game==null)return;
        game.items().mana().inventoryChanged(player);
        game.lbc().inventoryChanged(player);
        game.items().magic().inventoryChanged(player);
        if(head)game.items().clownKit().effectsChanged(player);
        game.items().refreshMinigunMovement(player);
        game.items().shulkerArmor().inventoryChanged(player);
        game.items().armorSets().inventoryChanged(player);
        game.items().shishEncounter().inventoryChanged(player);
        if(feet)game.items().rocketEquipment(player,player.getItemBySlot(EquipmentSlot.FEET));
    }
}
