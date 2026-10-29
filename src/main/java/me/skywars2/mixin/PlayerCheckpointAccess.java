package me.skywars2.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

public final class PlayerCheckpointAccess {
    private PlayerCheckpointAccess() { }
    @Mixin(SynchedEntityData.class) public interface Metadata {
        @Accessor("itemsById") SynchedEntityData.DataItem<?>[] skywars2$items();
    }
    @Mixin(StatsCounter.class) public interface Stats {
        @Accessor("stats") Object2IntMap<Stat<?>> skywars2$stats();
    }
    @Mixin(ServerRecipeBook.class) public interface Recipes {
        @Invoker("apply") void skywars2$apply(ServerRecipeBook.Packed image);
    }
    @Mixin(ItemCooldowns.class) public interface Cooldowns {
        @Accessor("cooldowns") Map<Identifier,Object> skywars2$cooldowns();
        @Accessor("tickCount") int skywars2$tick();
        @Accessor("tickCount") void skywars2$tick(int tick);
        @Invoker("onCooldownStarted") void skywars2$publish(Identifier group,int duration);
    }
    @Mixin(targets="net.minecraft.world.item.ItemCooldowns$CooldownInstance") public interface Cooldown {
        @Accessor("endTime") int skywars2$end();
    }
    @Mixin(PlayerAdvancements.class) public interface Advancements {
        @Accessor("progress") Map<AdvancementHolder,AdvancementProgress> skywars2$progress();
        @Accessor("visible") Set<AdvancementHolder> skywars2$visible();
        @Accessor("progressChanged") Set<AdvancementHolder> skywars2$changed();
        @Accessor("rootsToUpdate") Set<AdvancementNode> skywars2$roots();
        @Accessor("tree") AdvancementTree skywars2$tree();
        @Accessor("lastSelectedTab") AdvancementHolder skywars2$tab();
        @Accessor("isFirstPacket") void skywars2$first(boolean first);
        @Invoker("startProgress") void skywars2$start(AdvancementHolder holder,AdvancementProgress progress);
        @Invoker("registerListeners") void skywars2$listeners(ServerAdvancementManager manager);
    }
}
