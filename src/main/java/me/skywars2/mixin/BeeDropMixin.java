package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

/** Native sting and native hive storage remain authoritative. */
public final class BeeDropMixin {
    private BeeDropMixin() { }

    @Mixin(Bee.class)
    public abstract static class BeeMelee {
        @Inject(method="doHurtTarget",at=@At("HEAD"),cancellable=true)
        private void skywars2$beeSting(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
            var game = SkyWars2.game();
            if (game != null && !game.items().beeDrops().canAffect((Bee)(Object)this,target)) cir.setReturnValue(false);
        }
    }

    @Mixin(BeehiveBlockEntity.class)
    public abstract static class Hive {
        @Inject(method="storeBee",at=@At("HEAD"),cancellable=true)
        private void skywars2$admitStoredBee(BeehiveBlockEntity.Occupant occupant, CallbackInfo ci) {
            var game = SkyWars2.game();
            if (game != null && !game.items().beeDrops().acceptStored(occupant)) ci.cancel();
        }

        @Inject(method="storeBee",at=@At("RETURN"))
        private void skywars2$rememberHive(BeehiveBlockEntity.Occupant occupant, CallbackInfo ci) {
            var game = SkyWars2.game();
            if (game != null) game.items().beeDrops().stored((BeehiveBlockEntity)(Object)this);
        }
    }

    @Mixin(BeehiveBlockEntity.class)
    public interface HiveAccess {
        @Invoker("getBees") List<BeehiveBlockEntity.Occupant> skywars2$getBees();
        @Accessor("stored") List<?> skywars2$stored();
    }
}
