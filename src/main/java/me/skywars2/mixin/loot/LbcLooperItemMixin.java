package me.skywars2.mixin.loot;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.skywars2.game.skills.LbcSkills;
import me.skywars2.game.GameController;

/** Tick tail runs only after native item ticking has passed its pause guards. */
@Mixin(ItemEntity.class)
public abstract class LbcLooperItemMixin {
    /** Event-driven reindexing for transformed dropped wires; constructor stacks are not yet tracked. */
    @Inject(method="setItem",at=@At("TAIL"))
    private void skywars2$wireStackChanged(net.minecraft.world.item.ItemStack stack,CallbackInfo ci){
        var self=(ItemEntity)(Object)this;
        if(self.level() instanceof net.minecraft.server.level.ServerLevel level
                &&level.getEntity(self.getUUID())==self){
            var game=me.skywars2.SkyWars2.game();
            if(game!=null){
                if(me.skywars2.game.entities.LbcDimensionItems.nexus(level))game.items().nexusAcquisition().itemChanged(self);
                else{game.items().supernova().itemChanged(self);game.items().chargedBows().itemChanged(self);game.items().cursedPumpkin().itemChanged(self);}
            }
        }
    }

    @Inject(method="tick",at=@At("TAIL"))
    private void skywars2$looper(CallbackInfo ci){
        var item=(ItemEntity)(Object)this;
        if(!me.skywars2.game.entities.LbcDimensionItems.nexus(item.level()))LbcSkills.groundTick(item);
    }
}
