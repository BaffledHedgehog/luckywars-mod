package me.skywars2.validation.mixin;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import me.skywars2.game.GameController;
import me.skywars2.game.gui.GameMenus;
import me.skywars2.game.players.PlayerStore;
import me.skywars2.game.world.maps.MapManager;
import me.skywars2.game.world.terrain.BoundsJournalWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Native map loader state observed by the isolated validation server. */
public final class MapValidationAccess {
    private MapValidationAccess() { }

    @Mixin(value = MapManager.class, remap = false)
    public interface Manager {
        @Accessor("journalWriter") BoundsJournalWriter validation$writer();
        @Accessor("boundsJournal") Path validation$journal();
        @Accessor("lastBoundsWrite") CompletableFuture<Boolean> validation$lastBoundsWrite();
        @Accessor("completedSlices") int validation$completedSlices();
        @Accessor("preparationGeneration") long validation$preparationGeneration();
        @Accessor("pendingMap") String validation$pendingMap();
        @Accessor("sweptMode") int validation$sweptMode();
        @Accessor("lobbyVisible") Map<BlockPos, ?> validation$lobbyVisible();
        @Accessor("preparation") CompletableFuture<?> validation$preparation();
        @Accessor("respawnIslands") Map<BlockPos, Map<BlockPos, ?>> validation$respawnIslands();
        @Accessor("spawns") void validation$spawns(List<MapManager.Spawn> spawns);
        @Accessor("imprintedBounds") List<BoundingBox> validation$imprintedBounds();
        @Accessor("work") Queue<?> validation$work();
        @Invoker("flushChangedBlockBounds") void validation$flushChangedBlockBounds();
    }

    @Mixin(value = GameController.class, remap = false)
    public interface Controller {
        @Accessor("participants") Map<UUID, ?> validation$participants();
        @Accessor("mapFailure") Component validation$mapFailure();
    }

    @Mixin(targets = "me.skywars2.game.GameController$Participant", remap = false)
    public interface Participant {
        @Accessor("spawn") MapManager.Spawn validation$assignedSpawn();
        @Accessor("lives") int validation$lives();
        @Accessor("lives") void validation$lives(int lives);
        @Accessor("awaitingRespawn") boolean validation$awaitingRespawn();
        @Accessor("awaitingRespawn") void validation$awaitingRespawn(boolean awaiting);
        @Accessor("disconnected") boolean validation$disconnected();
        @Accessor("disconnected") void validation$disconnected(boolean disconnected);
    }

    @Mixin(value = GameMenus.class, remap = false)
    public interface Menus {
        @Accessor("mapPages") Map<UUID, Integer> validation$mapPages();
    }

    @Mixin(value = PlayerStore.class, remap = false)
    public interface Profiles {
        @Accessor("profiles") Map<UUID, PlayerStore.Profile> validation$profiles();
    }

    @Mixin(value = BoundsJournalWriter.class, remap = false)
    public interface Journal {
        @Accessor("sink") BoundsJournalWriter.Sink validation$sink();
        @Accessor("sink") void validation$sink(BoundsJournalWriter.Sink sink);
        @Accessor("pending") LinkedHashMap<Path, ?> validation$pending();
    }
}
