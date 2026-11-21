package me.skywars2.game.match.checkpoints;

import java.util.LinkedHashSet;
import java.util.Set;

import me.skywars2.game.core.config.GameConfig;
import me.skywars2.game.players.PlayerStore;
import me.skywars2.game.players.PlayerStore.Profile;

/** Exact snapshots of config and profiles, including identity of their mutable containers. */
public final class MatchValueSnapshot {
    private record ArrayValue(int[] original, int[] values) {
        ArrayValue(int[] value) {
            this(value, value == null ? null : value.clone());
        }
        int[] restore() {
            if (original != null) System.arraycopy(values, 0, original, 0, values.length);
            return original;
        }
    }

    private record SetValue(Set<String> original, Set<String> values) {
        SetValue(Set<String> value) {
            this(value, value == null ? null : new LinkedHashSet<>(value));
            if (value != null) {
                for (Object entry : value) {
                    if (!(entry instanceof String)) throw new IllegalStateException("Non-string checkpoint profile set");
                }
            }
        }
        Set<String> restore() {
            if (original != null && !original.equals(values)) {
                original.clear();
                original.addAll(values);
            }
            return original;
        }
    }

    private final Object original;
    private final Runnable restoreValues;

    public MatchValueSnapshot(GameConfig config) {
        original = config;
        var saved = new GameConfig();
        copyConfig(config, saved);
        restoreValues = () -> copyConfig(saved, config);
    }

    public MatchValueSnapshot(PlayerStore.Profile profile) {
        original = profile;
        var saved = new PlayerStore.Profile();
        copyProfileValues(profile, saved);
        var keys = new ArrayValue(profile.keys);
        var trinkets = new ArrayValue(profile.trinkets);
        var levelups = new ArrayValue(profile.levelups);
        var legacyUnlocks = new SetValue(profile.legacyUnlocks);
        var revokedLegacyUnlocks = new SetValue(profile.revokedLegacyUnlocks);
        var dropRequestOverrides = new SetValue(profile.dropRequestOverrides);
        var lbcUnlocks = new SetValue(profile.lbcUnlocks);
        restoreValues = () -> {
            copyProfileValues(saved, profile);
            profile.keys = keys.restore();
            profile.trinkets = trinkets.restore();
            profile.levelups = levelups.restore();
            profile.legacyUnlocks = legacyUnlocks.restore();
            profile.revokedLegacyUnlocks = revokedLegacyUnlocks.restore();
            profile.dropRequestOverrides = dropRequestOverrides.restore();
            profile.lbcUnlocks = lbcUnlocks.restore();
        };
    }

    public Object restore() {
        restoreValues.run();
        return original;
    }

    private static void copyConfig(GameConfig source, GameConfig target) {
        target.lives = source.lives;
        target.health = source.health;
        target.countdownSeconds = source.countdownSeconds;
        target.stageOneSeconds = source.stageOneSeconds;
        target.stageTwoSeconds = source.stageTwoSeconds;
        target.endingSeconds = source.endingSeconds;
        target.stageOneEvent = source.stageOneEvent;
        target.stageTwoEvent = source.stageTwoEvent;
        target.pvpMode = source.pvpMode;
        target.tntProtection = source.tntProtection;
        target.gameMode = source.gameMode;
        target.bedwarsDirectBedBreaking = source.bedwarsDirectBedBreaking;
        target.luckyModifier = source.luckyModifier;
        target.day = source.day;
        target.healthDisplay = source.healthDisplay;
        target.imbaDropChance = source.imbaDropChance;
        target.prefixes = source.prefixes;
        target.ranked = source.ranked;
        target.synchroRanked = source.synchroRanked;
        target.eventHalloween = source.eventHalloween;
        target.eventNovember = source.eventNovember;
        target.eventTournament = source.eventTournament;
        target.vanillaKits = source.vanillaKits;
        target.vanillaSkills = source.vanillaSkills;
        target.lbcKits = source.lbcKits;
        target.lbcSkills = source.lbcSkills;
        target.lbcLevelups = source.lbcLevelups;
        target.lbcChallenges = source.lbcChallenges;
        target.offhandWands = source.offhandWands;
        target.lbcLevelupLimit = source.lbcLevelupLimit;
        target.lbcTrinketLimit = source.lbcTrinketLimit;
        target.noBorders = source.noBorders;
        target.mapId = source.mapId;
        target.broken = source.broken;
        target.developerMode = source.developerMode;
        target.gamesPlayed = source.gamesPlayed;
    }

    private static void copyProfileValues(PlayerStore.Profile source, PlayerStore.Profile target) {
        target.username = source.username;
        target.coins = source.coins;
        target.memcoins = source.memcoins;
        target.games = source.games;
        target.wins = source.wins;
        target.kills = source.kills;
        target.deaths = source.deaths;
        target.legacyScoreboardImported = source.legacyScoreboardImported;
        target.kit = source.kit;
        target.skill = source.skill;
        target.killSound = source.killSound;
        target.suffix = source.suffix;
        target.winEffect = source.winEffect;
        target.cage = source.cage;
        target.killEffect = source.killEffect;
        target.fovEffects = source.fovEffects;
        target.modeScroll = source.modeScroll;
        target.casePending = source.casePending;
        target.savedBuilds = source.savedBuilds;
        target.lbcKit = source.lbcKit;
        target.lbcSkill = source.lbcSkill;
        target.lbcTrinket = source.lbcTrinket;
        target.lbcTrinketSlot = source.lbcTrinketSlot;
        target.lbcChallenge = source.lbcChallenge;
        target.lbcChallengeProgress = source.lbcChallengeProgress;
        target.lbcChallengeKit = source.lbcChallengeKit;
        target.standOverheavenMatches = source.standOverheavenMatches;
        target.lbcLevelupSlot = source.lbcLevelupSlot;
    }
}
