package me.skywars2.game;

import java.util.LinkedHashMap;
import java.util.List;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.ScoreHolder;

/** Production packet/use routing and actual canonical loot, with a scoped native service image. */
public final class LightsaberChecks {
    private LightsaberChecks() { }
    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException("November lightsabers: " + message);
    }
    private static boolean input(ServerPlayer player, InteractionHand hand) {
        return player.gameMode.useItem(player, player.level(), player.getItemInHand(hand), hand).consumesAction();
    }
    private static int count(ServerPlayer player, java.util.function.Predicate<ItemStack> matches) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot); if (matches.test(stack)) count += stack.getCount();
        }
        return count;
    }
    private static ItemStack table(GameController game, ServerPlayer player, Item expected) {
        var result = game.lbc().rollTable(player.level(), player.position(), player, "lbc:lightsaber");
        require(result.size() == 1 && result.getFirst().is(expected), "canonical contextual source table chooses " + expected);
        var stack = result.getFirst(); var defaults = expected.getDefaultInstance();
        for (var component : List.of(DataComponents.ITEM_MODEL, DataComponents.CUSTOM_NAME, DataComponents.LORE,
                DataComponents.ATTRIBUTE_MODIFIERS, DataComponents.CUSTOM_DATA))
            require(stack.get(component) != null, "source component exists " + component);
        require(stack.get(DataComponents.ITEM_MODEL).equals(defaults.get(DataComponents.ITEM_MODEL)), "exact original model");
        require(stack.get(DataComponents.ATTRIBUTE_MODIFIERS).equals(defaults.get(DataComponents.ATTRIBUTE_MODIFIERS)), "damage and attack speed source attributes");
        require(stack.get(DataComponents.CUSTOM_DATA).equals(defaults.get(DataComponents.CUSTOM_DATA)), "exact source custom flags");
        require(stack.get(DataComponents.TOOLTIP_STYLE) == null, "source has no invented tier");
        return stack;
    }

    public static void run(GameController game, LbcNovemberLightsabers service) {
        var saved = service.checkpoint(); boolean november = game.config().eventNovember;
        var board = game.server().getScoreboard();
        var shared = new LinkedHashMap<String, Integer>(); var math = board.getObjective("lbc.math");
        var originalPlayer = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(originalPlayer != null, "active fixture player");
        var scores = new LinkedHashMap<String, Integer>();
        for (var name : List.of("november", "lucky_block_count")) {
            var objective = board.getObjective(name); var score = objective == null ? null : board.getPlayerScoreInfo(originalPlayer, objective);
            scores.put(name, score == null ? null : score.value());
        }
        for (var name : List.of("jedi", "sith")) {
            var value = math == null ? null : board.getPlayerScoreInfo(ScoreHolder.forNameOnly(name), math);
            shared.put(name, value == null ? null : value.value());
        }
        try (var base = new ElementalGrimoireChecks.Fixture(game)) {
            base.initialize(); var player = base.owner; var level = player.level();
            var cells = new LinkedHashMap<BlockPos, MapManager.BlockSnapshot>(); var at = player.blockPosition();
            for (var pos : BlockPos.betweenClosed(at.offset(-4, -1, -4), at.offset(4, 0, 4)))
                cells.put(pos.immutable(), game.maps().snapshot(level, pos));
            var terrain = game.items().indirectKills().captureTerrain(level, cells.keySet());
            try {
                service.clear(); game.config().eventNovember = true;
                game.items().mana().score(player, "november").set(0);
                player.setItemInHand(InteractionHand.MAIN_HAND, table(game, player, LbcNovemberLightsaberItems.SELECTOR));
                player.setYRot(90); player.setXRot(65);
                require(input(player, InteractionHand.MAIN_HAND), "actual selector use with nonzero pitch");
                require(level.getBlockState(at.offset(-2, 0, 1)).is(Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.BLUE))
                        && level.getBlockState(at.offset(-2, 0, -1)).is(Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.RED)), "feet origin local +1/-1 forward2, yaw90 and pitch forced0");
                require(service.faction(player.getUUID()) == LbcNovemberLightsabers.Faction.UNDECIDED
                        && count(player, LbcNovemberLightsaberItems::selector) == 1, "palette use preserves all selectors and faction");
                var jammer = new net.minecraft.world.entity.Marker(net.minecraft.world.entity.EntityTypes.MARKER, level);
                jammer.setPos(player.position()); jammer.entityTags().add("stopper"); jammer.entityTags().add("stopper_magic");
                require(level.addFreshEntity(jammer), "native ordinary jammer marker"); game.items().magic().loaded(jammer);
                try {
                    require(game.items().magic().mechanismSuppressed(player) && game.items().magic().suppressed(player), "positive TECH and MAG suppression");
                    require(input(player, InteractionHand.MAIN_HAND) && count(player, LbcNovemberLightsaberItems::selector) == 1,
                            "author NONE selector still uses through actual TECH and MAG fields");
                } finally { game.items().magic().unloaded(jammer); jammer.discard(); }
                var artifact = new ItemStack(Items.CARROT_ON_A_STICK); var data = new CompoundTag();
                data.putInt("mystical", 1); data.putInt("type", 1); artifact.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                player.getInventory().setItem(6, artifact); game.items().magic().inventoryChanged(player); game.items().magic().flushCleansing();
                try {
                    require(game.items().magic().artifactActive(player), "positive native anti-magic artifact");
                    require(input(player, InteractionHand.MAIN_HAND), "author NONE selector bypasses original artifact wrapper");
                } finally {
                    player.getInventory().setItem(6, ItemStack.EMPTY); game.items().magic().inventoryChanged(player); game.items().magic().flushCleansing();
                }
                level.setBlock(at.below(), Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.BLUE).defaultBlockState(), 3);
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(LbcNovemberLightsaberItems.SELECTOR));
                input(player, InteractionHand.OFF_HAND);
                require(service.faction(player.getUUID()) == LbcNovemberLightsabers.Faction.UNDECIDED
                        && count(player, LbcNovemberLightsaberItems::selector) == 1, "actual offhand cannot choose faction");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(LbcNovemberLightsaberItems.SELECTOR));
                player.getInventory().setItem(8, new ItemStack(LbcNovemberLightsaberItems.SELECTOR, 3));
                var unrelated = new ItemStack(Items.DIAMOND, 7); player.getInventory().setItem(7, unrelated);
                game.items().timeStop().begin(level, 40);
                try {
                    input(player, InteractionHand.MAIN_HAND);
                    require(service.faction(player.getUUID()) == LbcNovemberLightsabers.Faction.UNDECIDED
                            && count(player, LbcNovemberLightsaberItems::selector) == 5, "real world TimeStop preserves faction and every token");
                } finally { game.items().timeStop().end(level); }
                require(input(player, InteractionHand.MAIN_HAND), "actual blue selection");
                require(service.faction(player.getUUID()) == LbcNovemberLightsabers.Faction.JEDI
                        && count(player, LbcNovemberLightsaberItems::selector) == 0
                        && count(player, LbcNovemberLightsaberItems::blade) == 1
                        && player.getInventory().getItem(7) == unrelated && unrelated.getCount() == 7,
                        "source clear ALL selectors across hands, one blue blade, unrelated inventory retained");
                require(table(game, player, LbcNovemberLightsaberItems.JEDI).has(DataComponents.BLOCKS_ATTACKS), "native blue shield blocking retained");
                var chosen = service.checkpoint();
                service.clear();
                game.items().mana().score(player, "november").set(0);
                level.setBlock(at.below(), Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState(), 3);
                player.getInventory().clearContent(); player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(LbcNovemberLightsaberItems.SELECTOR));
                require(input(player, InteractionHand.MAIN_HAND) && service.faction(player.getUUID()) == LbcNovemberLightsabers.Faction.SITH, "actual red selection");
                require(table(game, player, LbcNovemberLightsaberItems.SITH).has(DataComponents.BLOCKS_ATTACKS), "native red shield blocking retained");
                service.clear(); require(service.trackedPlayers() == 0, "derived event membership cleared");
                service.prepareFrozen(); chosen.prepare(id -> game.server().getPlayerList().getPlayer(id)).install().run(); service.publishFrozen();
                require(service.trackedPlayers() == 1 && service.faction(player.getUUID()) == LbcNovemberLightsabers.Faction.SITH,
                        "typed membership restored while canonical native score remains authoritative");
                game.items().mana().score(player, "november").set(1); service.sourceScoreChanged(player);
                require(service.faction(player.getUUID()) == LbcNovemberLightsabers.Faction.JEDI, "external native score mutation immediately changes faction");
                table(game, player, LbcNovemberLightsaberItems.JEDI);
                player.getInventory().clearContent();
                game.items().mana().score(player, "lucky_block_count").set(1);
                service.onLuckyBlockOpened(player); // Root also checks this through an actual normal-block break.
                player.entityTags().add("time_stopped");
                try { service.second(); require(player.getInventory().isEmpty(), "paused acquisition remains pending"); }
                finally { player.entityTags().remove("time_stopped"); }
                service.second();
                require(count(player, stack -> stack.is(LbcNovemberLightsaberItems.JEDI)) == 1
                        && service.luckyBlockCount(player.getUUID()) == -2147483646, "phase acquisition preserves faction and exact signed source sentinel");
                service.second(); require(count(player, LbcNovemberLightsaberItems::blade) == 1, "subsequent source phase cannot repeat acquisition");
            } finally {
                game.items().timeStop().end(level);
                for (var entry : cells.entrySet()) game.maps().restoreSnapshot(level, entry.getKey(), entry.getValue());
                game.items().indirectKills().restoreTerrain(terrain);
            }
        } finally {
            service.prepareFrozen(); saved.prepare(id -> game.server().getPlayerList().getPlayer(id)).install().run(); service.publishFrozen();
            game.config().eventNovember = november;
            for (var entry : scores.entrySet()) {
                var source = board.getObjective(entry.getKey()); if (source == null) continue;
                if (entry.getValue() == null) board.resetSinglePlayerScore(originalPlayer, source);
                else board.getOrCreatePlayerScore(originalPlayer, source, true).set(entry.getValue());
            }
            var objective = board.getObjective("lbc.math");
            if (objective != null) for (var entry : shared.entrySet()) {
                var holder = ScoreHolder.forNameOnly(entry.getKey());
                if (entry.getValue() == null) board.resetSinglePlayerScore(holder, objective);
                else board.getOrCreatePlayerScore(holder, objective, true).set(entry.getValue());
            }
        }
        SkyWars2.LOGGER.info("[check] PASS lightsabers (actual mainhand/offhand input, rotated terrain, source clear-all, canonical three factions, shield carriers, world TimeStop, typed membership restore and canonical source-score acquisition phase)");
    }
}
