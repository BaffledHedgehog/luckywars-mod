package me.skywars2.mixin;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import me.skywars2.game.LbcItems;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import org.spongepowered.asm.mixin.Mixin;

/** Chemical clouds are server bookkeeping; the authored mod particles are their only client visual. */
@Mixin(AreaEffectCloud.class)
public abstract class ChemicalCloudVisualMixin implements PolymerEntity {
    @Override public EntityType<?> getPolymerEntityType(PacketContext context){
        var self=(AreaEffectCloud)(Object)this;
        return LbcItems.chemicalAmplifier(self.get(DataComponents.POTION_CONTENTS))!=0?EntityTypes.MARKER:EntityTypes.AREA_EFFECT_CLOUD;
    }
}
