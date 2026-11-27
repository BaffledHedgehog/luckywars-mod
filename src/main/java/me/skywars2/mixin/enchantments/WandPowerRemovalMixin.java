package me.skywars2.mixin.enchantments;

import me.skywars2.game.combat.WeaponEnchantments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hidden-section unloads do not always emit a second native tracking-end callback. */
@Mixin(Entity.class)
public abstract class WandPowerRemovalMixin {
    @Inject(method="setRemoved",at=@At("TAIL"))
    private void skywars2$spellActorRemoved(Entity.RemovalReason reason,CallbackInfo ci){
        var entity=(Entity)(Object)this;
        if(entity.level() instanceof ServerLevel)WeaponEnchantments.unloaded(entity);
    }
}
