package me.skywars2.mixin;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerAdvancementManager.class)
public abstract class AdvancementFilterMixin {
    @Shadow @Final @Mutable private Map<Identifier, AdvancementHolder> advancements;
    @Shadow @Final @Mutable private AdvancementTree tree;

    /** Фильтруем один раз при загрузке/reload, до регистрации критериев игроков; ванильные тики достижений не работают. */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void skywars2$lbcOnly(HolderLookup.Provider registries, CallbackInfo ci) {
        Map<Identifier, AdvancementHolder> allowed = new LinkedHashMap<>();
        advancements.forEach((id, holder) -> {
            if (id.getNamespace().equals("lbc") && (id.getPath().equals("true_advancements")
                    || id.getPath().startsWith("true_advancements/")
                    // Внутренний impossible-флаг награды; без display он не создаёт ещё одну ветку UI.
                    || id.getPath().equals("challenges/no_knowledge"))
                    || me.skywars2.game.LbcSourceUnlocks.caseAdvancement(id)) allowed.put(id, holder);
        });
        advancements = Map.copyOf(allowed);
        tree = new AdvancementTree();
        tree.addAll(advancements.values());
        tree.repositionNodes();
    }
}
