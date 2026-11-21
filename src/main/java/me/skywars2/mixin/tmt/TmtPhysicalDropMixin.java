package me.skywars2.mixin.tmt;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import me.skywars2.game.items.tmt.LbcTmtAbilities;

/** Shared native inventory/death drops; ordinary items and deliberate projectile source copies are untouched. */
@Mixin(LivingEntity.class)
public abstract class TmtPhysicalDropMixin {
    @ModifyVariable(method="createItemStackToDrop",at=@At("HEAD"),argsOnly=true)
    private ItemStack skywars2$physicalTmt(ItemStack stack){return LbcTmtAbilities.physicalDrop(stack);}
}
