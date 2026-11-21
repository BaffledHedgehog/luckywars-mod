package me.skywars2.mixin.world;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.raid.Raids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Native raid containers and body links, without replaying gameplay callbacks. */
public final class RaidCheckpointAccess {
    private RaidCheckpointAccess() { }

    @Mixin(Raids.class)
    public interface Manager {
        @Accessor("raidMap") Int2ObjectMap<Raid> skywars2$raidMap();
        @Accessor("nextId") int skywars2$raidNextId();
        @Accessor("nextId") void skywars2$raidNextId(int value);
        @Accessor("tick") int skywars2$raidTick();
        @Accessor("tick") void skywars2$raidTick(int value);
    }

    @Mixin(Raider.class)
    public interface Body {
        @Accessor("wave") int skywars2$raidWave();
        @Accessor("wave") void skywars2$raidWave(int value);
        @Accessor("canJoinRaid") boolean skywars2$raidCanJoin();
        @Accessor("canJoinRaid") void skywars2$raidCanJoin(boolean value);
        @Accessor("ticksOutsideRaid") int skywars2$raidOutside();
        @Accessor("ticksOutsideRaid") void skywars2$raidOutside(int value);
    }

    @Mixin(Raid.class)
    public interface State {
        @Accessor("groupToLeaderMap") Map<Integer,Raider> skywars2$raidLeaders();
        @Accessor("groupRaiderMap") Map<Integer,Set<Raider>> skywars2$raidGroups();
        @Accessor("heroesOfTheVillage") Set<UUID> skywars2$raidHeroes();
        @Accessor("random") RandomSource skywars2$raidRandom();
        @Accessor("raidEvent") ServerBossEvent skywars2$raidEvent();
        @Accessor("ticksActive") long skywars2$raidTicksActive();
        @Accessor("ticksActive") void skywars2$raidTicksActive(long value);
        @Accessor("center") BlockPos skywars2$raidCenter();
        @Accessor("center") void skywars2$raidCenter(BlockPos value);
        @Accessor("started") boolean skywars2$raidStarted();
        @Accessor("started") void skywars2$raidStarted(boolean value);
        @Accessor("totalHealth") float skywars2$raidTotalHealth();
        @Accessor("totalHealth") void skywars2$raidTotalHealth(float value);
        @Accessor("raidOmenLevel") int skywars2$raidOmenLevel();
        @Accessor("raidOmenLevel") void skywars2$raidOmenLevel(int value);
        @Accessor("active") boolean skywars2$raidActive();
        @Accessor("active") void skywars2$raidActive(boolean value);
        @Accessor("groupsSpawned") int skywars2$raidGroupsSpawned();
        @Accessor("groupsSpawned") void skywars2$raidGroupsSpawned(int value);
        @Accessor("postRaidTicks") int skywars2$raidPostRaidTicks();
        @Accessor("postRaidTicks") void skywars2$raidPostRaidTicks(int value);
        @Accessor("raidCooldownTicks") int skywars2$raidCooldownTicks();
        @Accessor("raidCooldownTicks") void skywars2$raidCooldownTicks(int value);
        @Accessor("numGroups") int skywars2$raidNumGroups();
        @Accessor("status") Raid.RaidStatus skywars2$raidStatus();
        @Accessor("status") void skywars2$raidStatus(Raid.RaidStatus value);
        @Accessor("celebrationTicks") int skywars2$raidCelebrationTicks();
        @Accessor("celebrationTicks") void skywars2$raidCelebrationTicks(int value);
        @Accessor("waveSpawnPos") Optional<BlockPos> skywars2$raidWaveSpawnPos();
        @Accessor("waveSpawnPos") void skywars2$raidWaveSpawnPos(Optional<BlockPos> value);
    }
}
