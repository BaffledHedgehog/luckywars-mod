package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.PlayerInteractTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe the accepted native interaction, after the stand has received its equipment. */
@Mixin(PlayerInteractTrigger.class)
public abstract class ShishInteractionMixin {
    @Inject(method="trigger",at=@At("TAIL"))
    private void skywars2$technoStand(ServerPlayer player,ItemStack used,Entity clicked,CallbackInfo ci){
        var game=SkyWars2.game();
        if(game!=null&&(Object)this==CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY)
            game.items().shishEncounter().technoInteraction(player,clicked,used);
    }
}
