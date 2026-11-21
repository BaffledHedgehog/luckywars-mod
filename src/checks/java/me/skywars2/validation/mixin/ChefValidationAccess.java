package me.skywars2.validation.mixin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import me.skywars2.game.match.events.LbcApocalypseEvents;

public final class ChefValidationAccess {
    private ChefValidationAccess() {}

    @Mixin(value=LbcApocalypseEvents.class,remap=false)
    public interface Apocalypse {
        @Invoker("phase")
        void skywars2$validationPhase(LbcApocalypseEvents.Authority author, LbcApocalypseEvents.Kind kind,
                                      int phase, CommandSourceStack context);
    }

    @Mixin(targets="me.skywars2.game.entities.bots.LbcChefSurvival",remap=false)
    public interface Supplies {
        @Accessor("bucket") void skywars2$validationBucket(ItemStack bucket);
    }
}
