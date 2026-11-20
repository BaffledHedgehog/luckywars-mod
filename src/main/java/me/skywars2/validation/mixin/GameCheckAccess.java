package me.skywars2.validation.mixin;

import java.util.IdentityHashMap;
import java.util.Map;
import me.skywars2.game.ContentGallery;
import me.skywars2.game.LbcExoperso;
import me.skywars2.game.MatchEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

public final class GameCheckAccess {
    private GameCheckAccess() { }

    @Mixin(LivingEntity.class)
    public interface CombatLinks {
        @Accessor("lastHurtByMobTimestamp") int validation$mobTime();
        @Accessor("lastHurtMob") LivingEntity validation$attacked();
        @Accessor("lastHurtMobTimestamp") int validation$attackTime();
    }

    @Mixin(ServerGamePacketListenerImpl.class)
    public interface TeleportAck {
        @Accessor("awaitingTeleport") int validation$teleportId();
        @Accessor("awaitingPositionFromClient") Vec3 validation$awaitingPosition();
        @Accessor("awaitingTeleportTime") int validation$awaitingTeleportTime();
        @Accessor("tickCount") int validation$listenerTicks();
    }

    @Mixin(value = LbcExoperso.class, remap = false)
    public interface Exoperso {
        @Accessor("blastSources") IdentityHashMap<Entity, ?> validation$blastSources();
    }

    @Mixin(value = MatchEvents.class, remap = false)
    public interface Airdrops {
        @Accessor("pendingAirdrops") Map<Chicken, ServerLevel> validation$pending();
    }

    @Mixin(targets = "me.skywars2.game.ContentGallery$Catalog", remap = false)
    public interface GalleryCatalog {
        @Accessor("rolls") Map<?, LootTable> validation$rolls();
    }

    @Mixin(value = ContentGallery.class, remap = false)
    public interface Gallery {
        @Accessor("rolls") void validation$rolls(Map<?, LootTable> rolls);
    }
}
