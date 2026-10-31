package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Capture structure children before tracking: preparation chunks may be loaded but invisible. */
@Mixin(ServerLevel.class)
public abstract class GrimoireDungeonSpawnMixin {
    @Inject(method="addEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;addNewEntity(Lnet/minecraft/world/level/entity/EntityAccess;)Z"))
    private void skywars2$dungeonChildren(Entity entity,CallbackInfoReturnable<Boolean> cir){
        var game=SkyWars2.game();if(game!=null)game.items().grimoireDungeon().capture(entity);
    }
}
