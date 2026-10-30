package me.skywars2.mixin;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.clock.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.gamerules.*;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reviewed native timeline/metadata fields; transport objects and saved-data owners stay live. */
public final class WorldCheckpointAccess {
    private WorldCheckpointAccess() { }
    @Mixin(ServerClockManager.class) public interface Clocks {
        @Accessor("clocks") Map<Holder<WorldClock>,ServerClockManager.ServerClockInstance> skywars2$clocks();
    }
    @Mixin(ServerClockManager.ServerClockInstance.class) public interface Clock {
        @Accessor("timeMarkers") Map<ResourceKey<ClockTimeMarker>,ClockTimeMarker> skywars2$markers();
    }
    @Mixin(GameRules.class) public interface Rules {
        @Accessor("rules") GameRuleMap skywars2$rules();
    }
    @Mixin(GameRuleMap.class) public interface RuleValues {
        @Accessor("map") Reference2ObjectMap<GameRule<?>,Object> skywars2$values();
    }
    @Mixin(PrimaryLevelData.class) public interface Primary {
        @Accessor("settings") LevelSettings skywars2$settings();
        @Accessor("settings") void skywars2$settings(LevelSettings value);
    }
    @Mixin(MinecraftServer.class) public interface Server {
        @Accessor("effectiveRespawnData") void skywars2$effectiveSpawn(LevelData.RespawnData value);
    }
    @Mixin(Level.class) public interface Weather {
        @Accessor("oRainLevel") float skywars2$oldRain();
        @Accessor("oRainLevel") void skywars2$oldRain(float value);
        @Accessor("rainLevel") float skywars2$rain();
        @Accessor("rainLevel") void skywars2$rain(float value);
        @Accessor("oThunderLevel") float skywars2$oldThunder();
        @Accessor("oThunderLevel") void skywars2$oldThunder(float value);
        @Accessor("thunderLevel") float skywars2$thunder();
        @Accessor("thunderLevel") void skywars2$thunder(float value);
        @Accessor("skyDarken") int skywars2$sky();
        @Accessor("skyDarken") void skywars2$sky(int value);
    }
    @Mixin(WorldBorder.class) public interface Border {
        @Accessor("initialized") boolean skywars2$initialized();
        @Accessor("initialized") void skywars2$initialized(boolean value);
        @Accessor("centerX") void skywars2$x(double value);
        @Accessor("centerZ") void skywars2$z(double value);
        @Accessor("damagePerBlock") void skywars2$damage(double value);
        @Accessor("safeZone") void skywars2$safe(double value);
        @Accessor("warningTime") void skywars2$time(int value);
        @Accessor("warningBlocks") void skywars2$blocks(int value);
        @Accessor("absoluteMaxSize") void skywars2$max(int value);
    }
    @Mixin(targets="net.minecraft.world.level.border.WorldBorder$MovingBorderExtent") public interface Moving {
        @Accessor("lerpProgress") long skywars2$remaining();
        @Accessor("lerpProgress") void skywars2$remaining(long value);
        @Accessor("size") double skywars2$size();
        @Accessor("size") void skywars2$size(double value);
        @Accessor("previousSize") double skywars2$previous();
        @Accessor("previousSize") void skywars2$previous(double value);
    }
    @Mixin(targets="net.minecraft.world.level.border.WorldBorder$StaticBorderExtent") public interface Static {
        @Invoker("updateBox") void skywars2$rebuild();
    }
}
