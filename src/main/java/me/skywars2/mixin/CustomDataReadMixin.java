package me.skywars2.mixin;

import me.skywars2.game.ItemData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.*;

/** Exposes scalar access only; the backing immutable component's NBT never escapes. */
@Mixin(CustomData.class)
public abstract class CustomDataReadMixin implements ItemData.IntReader {
    @Shadow @Final private CompoundTag tag;
    @Override public int skywars2$readInt(String key,int fallback){return tag.getIntOr(key,fallback);}
}
