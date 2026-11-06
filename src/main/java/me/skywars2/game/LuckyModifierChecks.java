package me.skywars2.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import eu.pb4.sgui.api.gui.SlotBasedGui;
import eu.pb4.sgui.api.SguiUtils;
import me.skywars2.SkyWars2;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.scores.ScoreHolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Native settings dispatch and exact source category/loot-condition qualification. */
public final class LuckyModifierChecks {
    private LuckyModifierChecks() { }
    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException("Lucky modifiers: " + message);
    }
    private static ServerPlayer player(GameController game, String name) {
        return java.util.Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid(name)));
    }
    private static SlotBasedGui gui(ServerPlayer player) {
        var current = SguiUtils.getCurrentGui(player);
        require(current instanceof SlotBasedGui, "native SGui container is open");
        return (SlotBasedGui) current;
    }
    private static final class Score implements AutoCloseable {
        final net.minecraft.world.scores.Scoreboard board;
        final net.minecraft.world.scores.Objective objective;
        final ScoreHolder holder;
        final boolean existed;
        final int value;
        Score(GameController game, String name, String objectiveName) {
            board = game.server().getScoreboard(); holder = ScoreHolder.forNameOnly(name);
            objective = board.getObjective(objectiveName);
            require(objective != null, "published objective " + objectiveName);
            var previous = board.getPlayerScoreInfo(holder, objective);
            existed = previous != null; value = existed ? previous.value() : 0;
        }
        int get() { var info = board.getPlayerScoreInfo(holder, objective); return info == null ? 0 : info.value(); }
        void set(int value) { board.getOrCreatePlayerScore(holder, objective).set(value); }
        public void close() { if (existed) set(value); else board.resetSinglePlayerScore(holder, objective); }
    }
    public static void resolver(GameController game) {
        int[] counts = new int[13];
        for (int draw = 1; draw <= 100; draw++) {
            int expected = draw >= 76 && draw <= 86 ? draw - 74 : 1;
            int actual = GameLuckyModifiers.resolve(0, draw);
            require(actual == expected, "source mapping for draw " + draw); counts[actual]++;
            for (int configured = 1; configured <= 12; configured++)
                require(GameLuckyModifiers.resolve(configured, draw) == configured,
                        "fixed identity " + configured + " at draw " + draw);
        }
        require(counts[1] == 89, "89 exact neutral outcomes per 100 draws");
        for (int id = 2; id <= 12; id++) require(counts[id] == 1, "one exact outcome for category " + id);
        require(new GameConfig().luckyModifier == 0 && GameConfig.resetDefaults().luckyModifier == 0,
                "fresh and reset defaults are random0");
        service(game);
        SkyWars2.LOGGER.info("[check] PASS lucky-modifiers-resolver (all100 random draws; 1200 fixed identities)");
    }
    /** An independent owner exercises the explicit API; no match lifecycle/reset policy is inferred. */
    public static void service(GameController game) {
        int liveCurrent = game.luckyModifiers().current();
        int configured = game.config().luckyModifier;
        int mode = game.config().gameMode;
        byte[] persisted = configBytes();
        try (var active = new Score(game, "final_luck_modifier", "lbc.math");
             var configScore = new Score(game, "cfg_lucky_modifier", "swrg.math")) {
            var owner = new GameLuckyModifiers(game);
            for (int id = 1; id <= 12; id++) {
                owner.reset();
                require(owner.current() == 1 && active.get() == 1, "explicit reset publishes neutral category");
                require(owner.commit(id) == id && owner.current() == id && active.get() == id,
                        "fresh owner fixed commit " + id);
                // Passing random0 after a fixed commit must retain the committed value as well.
                for (int next = 0; next <= 12; next++) {
                    require(owner.commit(next) == id && owner.current() == id && active.get() == id,
                            "idempotent fixed commit " + id + " ignores next category " + next);
                    owner.syncConfig();
                    require(owner.current() == id && active.get() == id && configScore.get() == configured,
                            "config publication cannot alter the independent committed owner");
                }
            }
            owner.reset();
            int random = owner.commit(0);
            require(random >= 1 && random <= 12 && active.get() == random,
                    "one random commit publishes a valid categorical ID");
            for (int next = 0; next <= 12; next++)
                require(owner.commit(next) == random && owner.current() == random && active.get() == random,
                        "random commit remains fixed across repeated commits");
            owner.reset(); owner.reset();
            require(owner.current() == 1 && active.get() == 1 && owner.commit(6) == 6 && active.get() == 6,
                    "repeated explicit reset permits a new independent commit");

            // Exercise source scope directly without enabling the unavailable LuckyWars controller mode.
            game.config().gameMode = 1; game.config().luckyModifier = 7;
            active.set(6);
            var retained = new GameLuckyModifiers(game);
            require(retained.current() == 6 && active.get() == 6 && configScore.get() == 7,
                    "startup retains valid previous6 rather than resolving configured7");
            active.set(0);
            var scoped = new GameLuckyModifiers(game);
            require(scoped.current() == 1 && active.get() == 1 && configScore.get() == 7,
                    "startup normalizes absent/invalid result to1 without resolving configured7");
            game.config().gameMode = 2;
            require(scoped.startMatch(game.config().gameMode) == 7 && scoped.current() == 7 && active.get() == 7,
                    "simulated eligible LuckyWars start commits configured7");
            game.config().luckyModifier = 12; scoped.syncConfig();
            require(scoped.startMatch(game.config().gameMode) == 7 && active.get() == 7 && configScore.get() == 12,
                    "repeated LuckyWars start retains committed7 while next-match setting stays12");
            scoped.endMatch(); scoped.endMatch();
            require(scoped.current() == 7 && active.get() == 7 && game.config().luckyModifier == 12
                    && configScore.get() == 12, "end releases commit without resetting result or configured setting");
            game.config().gameMode = 1;
            require(scoped.startMatch(game.config().gameMode) == 7 && active.get() == 7 && configScore.get() == 12,
                    "ordinary configured12 start retains prior LuckyWars7");
            game.config().gameMode = 2; game.config().luckyModifier = 6; scoped.syncConfig();
            require(scoped.startMatch(game.config().gameMode) == 7 && active.get() == 7,
                    "ordinary committed match cannot resolve a changed mode/config midmatch");
            scoped.endMatch();
            require(scoped.current() == 7 && active.get() == 7 && configScore.get() == 6,
                    "ordinary end preserves result and next configured6");
            require(scoped.startMatch(game.config().gameMode) == 6 && scoped.current() == 6 && active.get() == 6,
                    "next simulated LuckyWars committed start resolves configured6");
            scoped.endMatch();
            require(game.config().luckyModifier == 6 && configScore.get() == 6 && active.get() == 6,
                    "LuckyWars end retains result and persistent GUI selector independently");
            game.config().gameMode = mode; game.config().luckyModifier = configured;
            require(game.luckyModifiers().current() == liveCurrent && game.config().luckyModifier == configured,
                    "independent API fixture leaves the live owner and persistent configuration untouched");
            require(java.util.Arrays.equals(persisted, configBytes()),
                    "simulated LuckyWars service starts/ends never rewrite persisted configuration");
        } finally {
            game.config().gameMode = mode; game.config().luckyModifier = configured;
        }
        SkyWars2.LOGGER.info("[check] PASS lucky-modifiers-service (simulated LuckyWars owner branch; ordinary retention; idempotent commit/end; explicit reset; exact restoration)");
    }
    private static byte[] configBytes() {
        try {
            Path file = Path.of("config/luckywars/game.json");
            return Files.exists(file) ? Files.readAllBytes(file) : null;
        } catch (java.io.IOException error) { throw new IllegalStateException("Read config fixture", error); }
    }
    private static Component label(Component component, String key) {
        if (component.getContents() instanceof TranslatableContents contents) {
            if (contents.getKey().equals(key)) return component;
            for (Object argument : contents.getArgs()) if (argument instanceof Component child) {
                Component found = label(child, key); if (found != null) return found;
            }
        }
        for (Component child : component.getSiblings()) { Component found = label(child, key); if (found != null) return found; }
        return null;
    }
    private static void title(ServerPlayer player, int id) {
        var stack = gui(player).getGuiElement(11).getItemStack();
        Component found = label(stack.getHoverName(), "swrg.cfg_lucky_modifier_" + id);
        if (found == null && stack.get(DataComponents.LORE) != null)
            for (Component line : stack.get(DataComponents.LORE).lines()) {
                found = label(line, "swrg.cfg_lucky_modifier_" + id); if (found != null) break;
            }
        require(found != null, "exact source label for category " + id);
        require(java.util.Objects.equals(found.getStyle().getColor(),
                net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.WHITE))
                && found.getStyle().equals(found.getStyle().withItalic(false)), "source white nonitalic category label " + id);
    }
    private static void command(GameController game, ServerPlayer player, String command, boolean accepted) {
        try {
            int result = game.server().getCommands().getDispatcher().execute(command,
                    player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS));
            require(accepted ? result == 1 : result != 1, "command result: " + command);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) {
            require(!accepted, "authorized command: " + command + ": " + expected.getMessage());
        }
    }
    public static void menu(GameController game) {
        require(game.inLobby(), "ready lobby"); resolver(game);
        var alpha = player(game, "TestAlpha"); var beta = player(game, "TestBeta");
        boolean alphaAdmin = alpha.entityTags().contains("admin"), betaAdmin = beta.entityTags().contains("admin");
        int old = game.config().luckyModifier, mode = game.config().gameMode;
        Path file = Path.of("config/luckywars/game.json");
        try (var configured = new Score(game, "cfg_lucky_modifier", "swrg.math");
             var active = new Score(game, "final_luck_modifier", "lbc.math")) {
            byte[] original = Files.exists(file) ? Files.readAllBytes(file) : null;
            try {
                // Test actual JSON deserialization and clamping without replacing the live config object.
                JsonObject json = original == null ? new JsonObject() : JsonParser.parseString(new String(original,
                        java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                json.remove("luckyModifier"); Files.writeString(file, json.toString());
                require(GameConfig.load().luckyModifier == 0, "older JSON missing the field uses random0");
                for (int invalid : new int[]{-25, 91}) {
                    json.addProperty("luckyModifier", invalid); Files.writeString(file, json.toString());
                    var loaded = GameConfig.load();
                    require(loaded.luckyModifier == (invalid < 0 ? 0 : 12), "JSON clamp " + invalid);
                    loaded.save(); require(GameConfig.load().luckyModifier == loaded.luckyModifier, "clamped JSON round trip");
                }
                game.config().save();
                alpha.entityTags().remove("admin"); beta.entityTags().remove("admin");
                command(game, alpha, "settings luckymodifier 0", true);
                for (int id = 0; id <= 12; id++) {
                    game.menus().open(alpha, GameMenus.Page.SETTINGS); title(alpha, id);
                    require(configured.get() == id && GameConfig.load().luckyModifier == id,
                            "live config, published score and disk agree at " + id);
                    GameChecks.inventoryClick(alpha, 11, 0, ContainerInput.PICKUP);
                    require(game.config().luckyModifier == (id + 1) % 13, "network slot11 increments/wraps " + id);
                }
                require(active.get() == active.value, "menu renders/clicks never resolve or mutate active category");
                // A callback captured while authorized rechecks the live online administrator tag.
                alpha.entityTags().add("admin"); game.menus().open(alpha, GameMenus.Page.SETTINGS);
                var captured = gui(alpha); var callback = captured.getGuiElement(11).getGuiCallback();
                alpha.entityTags().remove("admin"); beta.entityTags().add("admin");
                callback.click(11, eu.pb4.sgui.api.ClickType.MOUSE_LEFT, ContainerInput.PICKUP, captured);
                require(game.config().luckyModifier == 0, "stale admin callback cannot mutate");
                require(!game.mayControlMatch(alpha) && game.mayControlMatch(beta), "online admin tag has control priority");
                GameChecks.inventoryClick(alpha, 11, 0, ContainerInput.PICKUP);
                require(game.config().luckyModifier == 0, "nonadmin network click denied with online admin");
                for (var who : List.of(alpha, beta)) {
                    boolean denied = false;
                    try { game.server().getCommands().getDispatcher().execute("settings luckymodifier 7",
                            who.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS)); }
                    catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { denied = true; }
                    require(denied && game.config().luckyModifier == 0, "OP required even with admin tag");
                }
                command(game, alpha, "settings luckymodifier 6", true);
                require(GameConfig.load().luckyModifier == 6 && configured.get() == 6, "OP without tag persists canonical setting");
                command(game, alpha, "settings cfg_lucky_modifier 7", true);
                require(GameConfig.load().luckyModifier == 7 && configured.get() == 7, "source alias persists and publishes");
                for (String bad : List.of("-1", "13", "x", "1.5")) {
                    command(game, alpha, "settings luckymodifier " + bad, false);
                    require(game.config().luckyModifier == 7 && configured.get() == 7
                            && GameConfig.load().luckyModifier == 7, "invalid setting preserves old value: " + bad);
                }
                beta.entityTags().remove("admin");
                for (int unfinished : new int[]{2, 3, 4}) {
                    game.config().gameMode = unfinished; game.requestStart(null, false);
                    require(game.inLobby() && active.get() == active.value,
                            "unfinished mode " + unfinished + " rejected without resolution");
                }
            } finally {
                game.config().luckyModifier = old; game.config().gameMode = mode; game.luckyModifiers().syncConfig();
                if (alphaAdmin) alpha.entityTags().add("admin"); else alpha.entityTags().remove("admin");
                if (betaAdmin) beta.entityTags().add("admin"); else beta.entityTags().remove("admin");
                alpha.closeContainer(); beta.closeContainer();
                if (original == null) Files.deleteIfExists(file); else Files.write(file, original);
            }
        } catch (java.io.IOException error) { throw new IllegalStateException("Lucky modifiers config fixture", error); }
        SkyWars2.LOGGER.info("[check] PASS lucky-modifiers-menu (13 network states/styles; admin priority; OP dispatcher; persistence/clamp)");
    }
    private static LootTable isolated(GameController game, String id, int expectedWeight, int expectedQuality) {
        var source = game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse("lbc:" + id)));
        var ops = game.server().reloadableRegistries().lookup().createSerializationContext(JsonOps.INSTANCE);
        JsonObject root = LootTable.DIRECT_CODEC.encodeStart(ops, source).getOrThrow().getAsJsonObject();
        var pool = root.getAsJsonArray("pools").get(0).getAsJsonObject(); JsonObject selected = null;
        for (var element : pool.getAsJsonArray("entries")) {
            var entry = element.getAsJsonObject();
            if (entry.toString().contains("final_luck_modifier")) {
                require(selected == null, "one conditional leaf in " + id); selected = entry;
            }
        }
        require(selected != null && selected.get("weight").getAsInt() == expectedWeight
                && (selected.has("quality") ? selected.get("quality").getAsInt() : 0) == expectedQuality,
                "original conditional weight/quality in " + id);
        var entries = new JsonArray(); entries.add(selected.deepCopy()); pool.add("entries", entries);
        return LootTable.DIRECT_CODEC.parse(ops, root).getOrThrow();
    }
    public static void conditions(GameController game) {
        var owner = player(game, "TestAlpha");
        var params = new LootParams.Builder(owner.level()).withParameter(LootContextParams.ORIGIN, owner.position())
                .withOptionalParameter(LootContextParams.THIS_ENTITY, owner).create(LootContextParamSets.COMMAND);
        // Same context as LbcLoadouts.rollTable: THIS_ENTITY never implicitly forwards attribute luck.
        try (var score = new Score(game, "final_luck_modifier", "lbc.math")) {
            var artifact = isolated(game, "transformables_accessory", 400, 8);
            var mobgun = isolated(game, "transformables_shooter", 10000, 0);
            for (int id : new int[]{1, 6, 7, 12}) {
                score.set(id);
                require(artifact.getRandomItems(params, 812).isEmpty() == (id != 6), "native accessory conditional at category " + id);
                require(mobgun.getRandomItems(params, 812).isEmpty() == (id != 7), "native shooter conditional at category " + id);
            }
        }
        SkyWars2.LOGGER.info("[check] PASS lucky-modifiers-conditions (native 26.3 codecs; exact source leaves; context luck0)");
    }
    private record OrdinaryState(int current, int configured) { }
    private static OrdinaryState ordinary;
    public static void snapshot(GameController game) {
        require(game.inLobby() && game.config().gameMode == 1, "ordinary snapshot requires ready normal lobby");
        try (var active = new Score(game, "final_luck_modifier", "lbc.math");
             var config = new Score(game, "cfg_lucky_modifier", "swrg.math")) {
            ordinary = new OrdinaryState(game.luckyModifiers().current(), game.config().luckyModifier);
            require(active.get() == ordinary.current && config.get() == ordinary.configured
                    && GameConfig.load().luckyModifier == ordinary.configured,
                    "before ordinary start owner/score and configured GUI/disk agree");
        }
        SkyWars2.LOGGER.info("[check] PASS lucky-modifiers-snapshot");
    }
    public static void run(GameController game) {
        require(game.playing() && game.config().gameMode == 1, "actual committed normal match");
        require(ordinary != null, "ordinary lobby snapshot captured before actual start");
        try (var active = new Score(game, "final_luck_modifier", "lbc.math")) {
            int initial = game.luckyModifiers().current();
            require(initial >= 1 && initial <= 12 && active.get() == initial, "committed owner and active score agree");
            require(initial == ordinary.current && game.config().luckyModifier == ordinary.configured,
                    "actual ordinary start retains previous result and configured GUI selector");
            int configured = game.config().luckyModifier;
            command(game, player(game, "TestAlpha"), "settings luckymodifier 12", false);
            require(game.config().luckyModifier == configured, "in-match operator mutation rejected");
            for (int i = 0; i < 4; i++) {
                game.lbc().rollTable(game.maps().level(), player(game, "TestAlpha").position(), player(game, "TestAlpha"), "lbc:transformables_shooter");
                game.luckyModifiers().syncConfig();
                require(game.luckyModifiers().current() == initial && active.get() == initial,
                        "ordinary loot/config publication never rerolls committed category");
            }
            game.requestStart(null, false);
            require(game.luckyModifiers().current() == initial && active.get() == initial, "invalid second start cannot reroll");
        }
        conditions(game);
        SkyWars2.LOGGER.info("[check] PASS lucky-modifiers (real normal start; coherent owner/score; mutation rejection; stable loot)");
    }
    public static void cleanup(GameController game) {
        require(game.inLobby(), "normal stop returns to lobby");
        require(ordinary != null, "ordinary lifecycle snapshot exists");
        try (var score = new Score(game, "final_luck_modifier", "lbc.math");
             var configured = new Score(game, "cfg_lucky_modifier", "swrg.math")) {
            require(score.get() == ordinary.current && game.luckyModifiers().current() == ordinary.current,
                    "actual ordinary stop retains previous result and published score");
            require(game.config().luckyModifier == ordinary.configured
                    && configured.get() == ordinary.configured && GameConfig.load().luckyModifier == ordinary.configured,
                    "actual ordinary stop retains configured GUI selector and persisted setting");
        }
        ordinary = null;
        SkyWars2.LOGGER.info("[check] PASS lucky-modifiers-cleanup");
    }
}
