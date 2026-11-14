package me.skywars2.mixin;

import java.util.function.BooleanSupplier;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class CloneProjectileTickMixin {
    @Inject(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/entity/EntityTickList;forEach(Ljava/util/function/Consumer;)V"))
    private void skywars2$copyBeforePhysics(BooleanSupplier hasTime,CallbackInfo ci){
        var game=SkyWars2.game();if(game!=null)game.items().cloneSkills().beforeNativeEntities((ServerLevel)(Object)this);
    }
}
