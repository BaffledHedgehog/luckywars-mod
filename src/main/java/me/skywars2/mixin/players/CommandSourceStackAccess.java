package me.skywars2.mixin.players;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The original input source survives execute-as entity substitution. */
@Mixin(CommandSourceStack.class)
public interface CommandSourceStackAccess {
    @Accessor("source") CommandSource skywars2$source();
}
