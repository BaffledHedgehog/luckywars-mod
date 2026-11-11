package me.skywars2.mixin;

import me.skywars2.game.ItemData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.*;

/** Exposes scalar access only; the backing immutable component's NBT never escapes. */
@Mixin(CustomData.class)
public abstract class CustomDataReadMixin implements ItemData.IntReader,me.skywars2.game.LbcTmtAbilities.CachedSlots {
    @Shadow @Final private CompoundTag tag;
    @Unique private volatile java.util.List<me.skywars2.game.LbcTmtAbilities.Ability> skywars2$tmtSlots;
    @Unique private volatile java.util.List<me.skywars2.game.LbcTmtAbilities.Ability> skywars2$tmtNaturalSlots;
    @Override public java.util.List<me.skywars2.game.LbcTmtAbilities.Ability> skywars2$tmtNaturalSlots(){
        var slots=skywars2$tmtNaturalSlots;
        if(slots==null)skywars2$tmtNaturalSlots=slots=me.skywars2.game.LbcTmtAbilities.naturalSlots(skywars2$tmtSlots());
        return slots;
    }
    @Override public java.util.List<me.skywars2.game.LbcTmtAbilities.Ability> skywars2$tmtSlots(){
        var slots=skywars2$tmtSlots;
        if(slots==null)skywars2$tmtSlots=slots=me.skywars2.game.LbcTmtAbilities.parseSlots((CustomData)(Object)this);
        return slots;
    }
    @Override public int skywars2$readInt(String key,int fallback){return tag.getIntOr(key,fallback);}
    @Override public String skywars2$readString(String key,String fallback){return tag.getStringOr(key,fallback);}
    @Override public boolean skywars2$contains(String key){return tag.contains(key);}
}
