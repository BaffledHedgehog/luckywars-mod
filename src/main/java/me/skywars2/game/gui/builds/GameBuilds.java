package me.skywars2.game.gui.builds;

import com.google.gson.Gson;

import me.skywars2.game.GameController;
import me.skywars2.game.core.config.GameConfig;
import me.skywars2.game.gui.LbcCosmetics;
import me.skywars2.game.gui.OriginalGuiDesign;
import me.skywars2.game.players.PlayerStore;
import me.skywars2.game.players.PlayerStore.Profile;

import java.io.IOException;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.action.CustomAll;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.dialog.body.ItemBody;
import net.minecraft.server.dialog.input.TextInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import me.skywars2.game.gui.loadouts.LbcLoadouts;

/** The original 24 personal build slots, using native client dialogs and the canonical player store. */
public final class GameBuilds {
    static final int SLOTS = 24;
    private static final Gson JSON = new Gson();
    private final GameController game;
    private final PlayerStore players;
    // Weak player-body keys retire disconnected/replaced bodies without a polling task.
    private final Map<ServerPlayer, Session> sessions = new WeakHashMap<>();
    private record Session(String token, int saveSlot, String epoch) {}
    public record Build(String name, int kit, int skill, int lbcKit, int lbcSkill,
                 int challenge, int challengeKit, int[] levelups, int[] trinkets,
                 int sound, int cage, int suffix, int winEffect, int killEffect) {}

    public GameBuilds(GameController game, PlayerStore players) { this.game = game; this.players = players; }

    private boolean allowed(ServerPlayer player) {
        return game.server().getPlayerList().getPlayer(player.getUUID()) == player
                && game.canUseMenus(player) && !game.items().actionsBlocked(player);
    }

    public static Build[] read(PlayerStore.Profile profile) {
        if (profile.savedBuilds == null || profile.savedBuilds.isBlank()) return new Build[SLOTS];
        var records = JSON.fromJson(profile.savedBuilds, Build[].class);
        return records == null ? new Build[SLOTS] : Arrays.copyOf(records, SLOTS);
    }

    private Session session(ServerPlayer player, int saveSlot) {
        var session = new Session(UUID.randomUUID().toString(), saveSlot, game.items().epoch());
        sessions.put(player, session); return session;
    }

    private static Identifier action(Session session, String verb, int slot) {
        return Identifier.parse("skywars2:build/" + session.token + "/" + verb + "/" + slot);
    }

    private static ActionButton button(Component label, Component tooltip, int width, Identifier id) {
        return new ActionButton(new CommonButtonData(label, Optional.ofNullable(tooltip), width),
                Optional.of(new StaticAction(new ClickEvent.Custom(id, Optional.empty()))));
    }

    private static CommonDialogData common(String title, List<DialogBody> body, List<Input> inputs) {
        return new CommonDialogData(Component.translatable(title), Optional.empty(), true, false,
                DialogAction.CLOSE, body, inputs);
    }

    public void open(ServerPlayer player) {
        if (!allowed(player)) return;
        final Build[] records;
        try { records = read(players.get(player.getUUID())); }
        catch (RuntimeException invalid) { failure(player, invalid); return; }
        var session = session(player, -1); var actions = new ArrayList<ActionButton>();
        for (int slot = 0; slot < SLOTS; slot++) {
            var build = records[slot];
            actions.add(button(build == null ? Component.translatable("empty") : Component.literal(build.name),
                    build == null ? null : preview(player, build), 150, action(session, "open", slot)));
            actions.add(button(Component.translatable("delete"), Component.translatable("delete_tooltip"),
                    20, action(session, "delete", slot)));
        }
        player.openDialog(Holder.direct(new MultiActionDialog(common("build_manager", List.of(), List.of()),
                actions, Optional.of(button(Component.translatableWithFallback("close", "Close"), null, 150, action(session, "back", 0))), 6)));
    }

    private void saveDialog(ServerPlayer player, int slot) {
        var session = session(player, slot); var build = capture(player, "");
        var body = new ArrayList<DialogBody>();
        for (var row : rows(player, build)) {
            // Native dialog item rows render the original model, including custom kit icons.
            var icon = eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(row.item,
                    net.minecraft.world.item.TooltipFlag.NORMAL, player.connection.getPacketContext(), player.registryAccess());
            body.add(new ItemBody(ItemStackTemplate.fromNonEmptyStack(icon, 1),
                    Optional.of(new PlainMessage(row.label(), 280)), false, true, 16, 16));
        }
        var input = new Input("build_name", new TextInput(300, Component.translatable("build_name"), true,
                "", 64, Optional.empty()));
        var save = new ActionButton(new CommonButtonData(Component.translatable("save"), 120),
                Optional.of(new CustomAll(action(session, "save", slot), Optional.empty())));
        player.openDialog(Holder.direct(new MultiActionDialog(common("build_manager_save", body, List.of(input)),
                List.of(save), Optional.of(button(Component.translatableWithFallback("close", "Close"), null, 150, action(session, "main", 0))), 1)));
    }

    /** Handles only an action from the currently issued personal dialog, once. */
    public void click(ServerPlayer player, Identifier id, Optional<Tag> payload) {
        if (!allowed(player)) { sessions.remove(player); return; }
        var session = sessions.get(player); if (session == null) return;
        if (!game.items().currentEpoch(session.epoch)) { sessions.remove(player); return; }
        String prefix = "build/" + session.token + "/";
        if (!id.getNamespace().equals("skywars2") || !id.getPath().startsWith(prefix)) return;
        String[] path = id.getPath().substring(prefix.length()).split("/", -1);
        if (path.length != 2) return;
        final int slot;
        try { slot = Integer.parseInt(path[1]); } catch (NumberFormatException invalid) { return; }
        if (slot < 0 || slot >= SLOTS || !Set.of("open", "save", "delete", "main", "back").contains(path[0])) return;
        if (path[0].equals("save") && session.saveSlot != slot) return;
        if (!path[0].equals("save") && !path[0].equals("main") && session.saveSlot >= 0) return;
        sessions.remove(player);
        try {
            var profile = players.get(player.getUUID()); var records = read(profile);
            switch (path[0]) {
                case "open" -> { if (records[slot] == null) saveDialog(player, slot); else { apply(player, records[slot]); open(player); } }
                case "delete" -> { records[slot] = null; persist(profile, records); open(player); }
                case "save" -> {
                    if (records[slot] != null) { open(player); return; }
                    String name = payload.orElse(null) instanceof CompoundTag tag ? tag.getString("build_name").orElse("").strip() : "";
                    name = name.replaceAll("[\\p{Cntrl}]", "");
                    if (name.isBlank()) name = "build_" + (slot + 1);
                    if (name.length() > 64) name = name.substring(0, 64);
                    records[slot] = capture(player, name); persist(profile, records); open(player);
                }
                case "main" -> open(player);
                case "back" -> game.menus().open(player);
            }
        } catch (IOException | RuntimeException error) { failure(player, error); }
    }

    private void persist(PlayerStore.Profile profile, Build[] records) throws IOException {
        String previous = profile.savedBuilds; profile.savedBuilds = JSON.toJson(records);
        try { players.saveAtomic(); } catch (IOException | RuntimeException error) { profile.savedBuilds = previous; throw error; }
    }

    private Build capture(ServerPlayer player, String name) {
        var p = players.get(player.getUUID());
        return new Build(name, p.kit, p.skill, p.lbcKit, p.lbcSkill, p.lbcChallenge, p.lbcChallengeKit,
                p.levelups.clone(), p.trinkets.clone(), p.killSound, p.cage, p.suffix, p.winEffect, p.killEffect);
    }

    private static int vanilla(ServerPlayer player, PlayerStore.Profile p, GameConfig config, int page, int id, int max) {
        return id > 0 && id <= max && (page != 3000 || id != 19)
                && OriginalGuiDesign.unlocked(player, "swrg", page, id - 1, p, config) ? id : 0;
    }

    private void apply(ServerPlayer player, Build build) {
        var p = players.get(player.getUUID()); var config = game.config();
        p.kit = vanilla(player, p, config, 3000, build.kit, 24);
        p.skill = vanilla(player, p, config, 4000, build.skill, 13);
        game.lbc().restoreSelection(player, build.lbcKit, build.lbcSkill,
                build.levelups == null ? new int[0] : build.levelups, build.trinkets == null ? new int[0] : build.trinkets);
        p.lbcChallengeKit = build.challengeKit == 1 && game.lbc().challengeRewardAvailable(player) ? 1 : 0;
        p.lbcChallenge = build.challenge == 1 && !game.lbc().challengeRewardAvailable(player)
                && game.lbc().hasUnlock(player, "lbc:true_advancements/legends/academy/root") ? 1 : 0;
        p.lbcChallengeProgress = 0;
        var cosmetics = game.items().cosmetics();
        p.killSound = cosmetic(player, LbcCosmetics.Kind.SOUND, build.sound);
        p.cage = cosmetic(player, LbcCosmetics.Kind.CAGE, build.cage);
        p.suffix = cosmetic(player, LbcCosmetics.Kind.SUFFIX, build.suffix);
        p.winEffect = cosmetic(player, LbcCosmetics.Kind.WIN, build.winEffect);
        p.killEffect = cosmetic(player, LbcCosmetics.Kind.KILL, build.killEffect);
        players.saveDeferred(); game.lbc().refreshKnowledgeView(player); cosmetics.refreshSuffixes(List.of(player));
        if (game.menus().debugAccess(player) && game.playing()) {
            game.events().giveDebugKit(player); game.events().giveSkill(player);
            game.lbc().refreshDebugSelection(player, LbcLoadouts.Kind.SKILLS);
            game.lbc().refreshDebugSelection(player, LbcLoadouts.Kind.LEVELUPS);
        }
    }

    private int cosmetic(ServerPlayer player, LbcCosmetics.Kind kind, int id) {
        return game.items().cosmetics().unlocked(player, kind, id) ? id : 0;
    }

    private Component preview(ServerPlayer player, Build build) {
        var result = Component.empty();
        for (var row : rows(player, build)) result.append(row.label()).append("\n");
        return result;
    }

    private record Row(ItemStack item, int count, boolean trinket) {
        Component label() {
            var label = Component.empty();
            if (trinket) label.append(Component.literal("x" + count + " ").withColor(0x55ff55));
            return label.append(item.getDisplayName());
        }
    }

    private List<Row> rows(ServerPlayer player, Build build) {
        var rows = new ArrayList<Row>();
        if (build.kit > 0) add(rows, OriginalGuiDesign.button(player, "swrg", 3000, build.kit - 1), 1, false);
        if (build.skill > 0) add(rows, OriginalGuiDesign.button(player, "swrg", 4000, build.skill - 1), 1, false);
        if (build.lbcKit > 0) add(rows, game.lbc().preview(player, LbcLoadouts.Kind.KITS, build.lbcKit), 1, false);
        if (build.lbcSkill > 0) add(rows, game.lbc().preview(player, LbcLoadouts.Kind.SKILLS, build.lbcSkill), 1, false);
        if (build.levelups != null) for (int id : build.levelups) if (id != 0)
            add(rows, game.lbc().preview(player, LbcLoadouts.Kind.LEVELUPS, id), 1, false);
        var counts = new LinkedHashMap<Integer, Integer>();
        if (build.trinkets != null) for (int id : build.trinkets) if (id != 0) counts.merge(id, 1, Integer::sum);
        counts.forEach((id, count) -> add(rows, game.lbc().preview(player, LbcLoadouts.Kind.TRINKETS, id), count, true));
        return rows;
    }

    private static void add(List<Row> rows, ItemStack item, int count, boolean trinket) {
        if (item != null && !item.isEmpty()) rows.add(new Row(item, count, trinket));
    }

    private static void failure(ServerPlayer player, Exception error) {
        me.skywars2.SkyWars2.LOGGER.error("Cannot process personal build for {}", player.getUUID(), error);
        player.sendSystemMessage(Component.translatableWithFallback("skywars2.gui.builds.save_failed", "Could not save or load this build.").withColor(0xff5555));
    }
}
