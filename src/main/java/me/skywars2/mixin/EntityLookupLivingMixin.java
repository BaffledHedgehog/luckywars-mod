package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Collection;
import java.util.Collections;
import me.skywars2.game.NativeLivingEntities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Living view with the same visibility and insertion order as the native lookup. */
@Mixin(EntityLookup.class)
abstract class EntityLookupLivingMixin implements NativeLivingEntities {
    @Unique private final Int2ObjectLinkedOpenHashMap<LivingEntity> skywars2$livingById = new Int2ObjectLinkedOpenHashMap<>();
    @Unique private final Collection<LivingEntity> skywars2$living = Collections.unmodifiableCollection(skywars2$livingById.values());

    @WrapOperation(method = "add", at = @At(value = "INVOKE", target =
            "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;put(ILjava/lang/Object;)Ljava/lang/Object;"))
    private Object skywars2$addLiving(Int2ObjectMap<?> visible, int id, Object entity, Operation<Object> original) {
        Object previous = original.call(visible, id, entity);
        if (entity instanceof LivingEntity living) {
            if (previous != null && !(previous instanceof LivingEntity)) {
                // Replacing an occupied ID keeps that slot's position in native order.
                skywars2$livingById.clear();
                for (var entry : visible.int2ObjectEntrySet())
                    if (entry.getValue() instanceof LivingEntity body) skywars2$livingById.put(entry.getIntKey(), body);
            } else {
                skywars2$livingById.put(id, living);
            }
        } else if (previous instanceof LivingEntity) {
            skywars2$livingById.remove(id);
        }
        return previous;
    }

    @WrapOperation(method = "remove", at = @At(value = "INVOKE", target =
            "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;remove(I)Ljava/lang/Object;"))
    private Object skywars2$removeLiving(Int2ObjectMap<?> visible, int id, Operation<Object> original) {
        Object removed = original.call(visible, id);
        if (removed instanceof LivingEntity) skywars2$livingById.remove(id);
        return removed;
    }

    @Override
    public Collection<LivingEntity> skywars2$living() {
        return skywars2$living;
    }
}
