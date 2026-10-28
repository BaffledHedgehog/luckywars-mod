package me.skywars2.mixin;

import me.skywars2.game.LbcFleshArmor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Load equipped attributes before native health validation, rather than restoring lost HP later. */
@Mixin(LivingEntity.class)
public abstract class EquipmentHealthLoadMixin {
    @Shadow @Final private EntityEquipment equipment;
    @Shadow private void detectEquipmentUpdates(){throw new AssertionError();}

    @Inject(method="readAdditionalSaveData",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private void skywars2$equipmentBeforeHealth(ValueInput input,CallbackInfo ci){
        var entity=(LivingEntity)(Object)this;
        if(!(entity.level() instanceof ServerLevel))return;
        equipment.setAll(input.read("equipment",EntityEquipment.CODEC).orElseGet(EntityEquipment::new));
        if(entity instanceof Player player){
            player.getInventory().load(input.listOrEmpty("Inventory",ItemStackWithSlot.CODEC));
            player.getInventory().setSelectedSlot(input.getIntOr("SelectedItemSlot",0));
        }
        // Use the ordinary equipment engine and its cache once. The later vanilla load
        // installs equivalent stacks, so the first tick cannot remove/readd HP modifiers.
        detectEquipmentUpdates();
        if(entity instanceof ServerPlayer player)LbcFleshArmor.prepareLoadedEquipment(player);
    }
}
