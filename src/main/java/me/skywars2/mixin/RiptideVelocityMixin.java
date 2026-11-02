package me.skywars2.mixin;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Client item projection must cap enchantment components at255, but native server Riptide remains full strength. */
@Mixin(TridentItem.class)
public abstract class RiptideVelocityMixin {
    @Inject(method="releaseUsing",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;push(DDD)V",shift=At.Shift.AFTER))
    private void skywars2$actualSpinVelocity(ItemStack stack,Level level,LivingEntity user,int remaining,
                                           CallbackInfoReturnable<Boolean> cir){
        // This packet REPLACES client-predicted velocity; adding a second impulse would double the boost.
        if(user instanceof ServerPlayer player)player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }
}
