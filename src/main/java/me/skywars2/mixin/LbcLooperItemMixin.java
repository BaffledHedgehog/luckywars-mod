package me.skywars2.mixin;

import me.skywars2.game.LbcSkills;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tick tail runs only after native item ticking has passed its pause guards. */
@Mixin(ItemEntity.class)
public abstract class LbcLooperItemMixin {
    @org.spongepowered.asm.mixin.Unique private static final net.minecraft.resources.Identifier skywars2$nexusDimension=net.minecraft.resources.Identifier.parse("minecraft:nexus");
    /** Event-driven reindexing for transformed dropped wires; constructor stacks are not yet tracked. */
    @Inject(method="setItem",at=@At("TAIL"))
    private void skywars2$wireStackChanged(net.minecraft.world.item.ItemStack stack,CallbackInfo ci){
        var self=(ItemEntity)(Object)this;
        if(self.level() instanceof net.minecraft.server.level.ServerLevel level
                &&level.dimension().identifier().equals(skywars2$nexusDimension)
                &&level.getEntity(self.getUUID())==self){
            var game=me.skywars2.SkyWars2.game();
            if(game!=null)game.items().nexusAcquisition().itemChanged(self);
        }
    }

    @Inject(method="tick",at=@At("TAIL"))
    private void skywars2$looper(CallbackInfo ci){
        LbcSkills.groundTick((ItemEntity)(Object)this);
    }
}
