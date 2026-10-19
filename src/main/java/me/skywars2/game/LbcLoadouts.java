package me.skywars2.game;

import com.google.gson.JsonParser;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.LootParams;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** LBC selection, initial grants and attribute cleanup; no legacy tick/function engine. */
public final class LbcLoadouts {
    public enum Kind { KITS, SKILLS, LEVELUPS, TRINKETS }
    private record Modifier(Holder<Attribute> attribute, AttributeModifier value) {}
    private record UnlockNotification(String subtitle, List<String> requiresAll) {}
    private record Entry(int id, int page, int slot, String table, String gate,
                         String lockedKey, List<Modifier> modifiers) {}
    private static final EnumMap<Kind, List<Entry>> CATALOG = readCatalog();
    private static final Map<String, UnlockNotification> UNLOCK_NOTIFICATIONS = readUnlockNotifications();
    private final GameController game;
    private final GameConfig config;
    private final PlayerStore players;
    private final java.util.Map<java.util.UUID, ServerPlayer> resistanceOwners = new java.util.HashMap<>();
    private int skillTickCounter;
    // Сетевое отображение читает только опубликованный снимок, не изменяемые профили/файлы с Netty-потока.
    private final java.util.concurrent.ConcurrentMap<java.util.UUID, Boolean> artifactReaders = new java.util.concurrent.ConcurrentHashMap<>();

    public LbcLoadouts(GameController game, GameConfig config, PlayerStore players) {
        this.game = game; this.config = config; this.players = players;
    }

    /** Резервная сверка косметического доступа после команд/внешних правок профиля, без изменения предметов. */
    public void tick() {
        if (++skillTickCounter < 20) return;
        skillTickCounter = 0;
        tickSecond();
    }

    /** Периодическая сверка только флагов; инвентари и сущности отправляются лишь при изменившемся доступе. */
    void tickSecond() {
        refreshArtifactViews();
        artifactReaders.keySet().removeIf(id -> game.server().getPlayerList().getPlayer(id) == null);
    }
    public boolean readsArtifacts(ServerPlayer player) { return artifactReaders.getOrDefault(player.getUUID(), false); }
    public void refreshArtifactViews() {
        for (ServerPlayer player : game.server().getPlayerList().getPlayers()) refreshArtifactView(player);
    }
    public void refreshArtifactView(ServerPlayer player) {
        boolean enabled = config.lbcSkills && players.get(player.getUUID()).lbcSkill == 3;
        var previous = artifactReaders.put(player.getUUID(), enabled);
        if (enabled != Boolean.TRUE.equals(previous)) LbcSkills.refreshViewer(player);
    }

    private static Map<String, UnlockNotification> readUnlockNotifications() {
        try (var input = LbcLoadouts.class.getResourceAsStream("/skywars2/lbc_unlock_notifications.json")) {
            if (input == null) throw new IllegalStateException("Missing LBC unlock notification map");
            var root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            var notifications = new HashMap<String, UnlockNotification>();
            for (var entry : root.getAsJsonObject("notifications").entrySet()) {
                var value = entry.getValue().getAsJsonObject();
                var requirements = new ArrayList<String>();
                if (value.has("requires_all")) for (var id : value.getAsJsonArray("requires_all")) {
                    requirements.add(id.getAsString());
                }
                notifications.put(entry.getKey(), new UnlockNotification(value.get("subtitle").getAsString(), List.copyOf(requirements)));
            }
            return Map.copyOf(notifications);
        } catch (Exception failure) {
            throw new IllegalStateException("Invalid LBC unlock notification map", failure);
        }
    }

    /** Source subtitle for mapping audits; null means this advancement has no restored gain notification. */
    public static Component sourceSubtitle(AdvancementHolder advancement) {
        var notification = UNLOCK_NOTIFICATIONS.get(advancement.id().toString());
        return notification == null ? null : Component.translatable(notification.subtitle);
    }

    /** Called after a native criterion award; emits only a newly completed, source-mapped unlock. */
    public boolean advancementAwarded(ServerPlayer player, AdvancementHolder advancement) {
        if (game.isObserver(player)) return false;
        var notification = UNLOCK_NOTIFICATIONS.get(advancement.id().toString());
        if (notification == null || !player.getAdvancements().getOrStartProgress(advancement).isDone()) return false;
        for (String requiredId : notification.requiresAll) {
            var required = game.server().getAdvancements().get(Identifier.parse(requiredId));
            if (required == null || !player.getAdvancements().getOrStartProgress(required).isDone()) return false;
        }
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal(" ")));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.translatable(notification.subtitle)));
        return true;
    }

    private static EnumMap<Kind, List<Entry>> readCatalog() {
        var catalog = new EnumMap<Kind, List<Entry>>(Kind.class);
        try (var input = LbcLoadouts.class.getResourceAsStream("/skywars2/lbc_loadouts.json")) {
            if (input == null) throw new IllegalStateException("Missing LBC catalog");
            var json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Kind kind : Kind.values()) {
                var entries = new ArrayList<Entry>();
                for (var element : json.getAsJsonArray(kind.name().toLowerCase(java.util.Locale.ROOT))) {
                    var entry = element.getAsJsonObject();
                    var modifiers = new ArrayList<Modifier>();
                    if (entry.has("modifiers")) for (var raw : entry.getAsJsonArray("modifiers")) {
                        var m = raw.getAsJsonObject();
                        var holder = BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(m.get("attribute").getAsString())).orElseThrow();
                        modifiers.add(new Modifier(holder, new AttributeModifier(Identifier.parse(m.get("key").getAsString()),
                                m.get("amount").getAsDouble(), AttributeModifier.Operation.valueOf(m.get("operation").getAsString().toUpperCase(java.util.Locale.ROOT)))));
                    }
                    entries.add(new Entry(entry.get("id").getAsInt(), entry.get("page").getAsInt(), entry.get("slot").getAsInt(),
                            entry.has("table") ? entry.get("table").getAsString() : null,
                            entry.has("gate") ? entry.get("gate").getAsString() : null,
                            entry.has("lockedKey") ? entry.get("lockedKey").getAsString() : null, List.copyOf(modifiers)));
                }
                catalog.put(kind, List.copyOf(entries));
            }
            return catalog;
        } catch (Exception failure) {
            throw new IllegalStateException("Invalid prepared LBC loadout catalog", failure);
        }
    }

    private static Entry byId(Kind kind, int id) {
        return CATALOG.get(kind).stream().filter(entry -> entry.id == id).findFirst().orElse(null);
    }

    private static Entry bySlot(Kind kind, int page, int slot) {
        return CATALOG.get(kind).stream().filter(entry -> entry.page == page && entry.slot == slot).findFirst().orElse(null);
    }

    public static Set<String> unlockIds() {
        return CATALOG.values().stream().flatMap(List::stream).map(Entry::gate)
                .filter(java.util.Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }

    /** Explicit administrative migration grant; ordinary selection never grants itself access. */
    public boolean unlock(ServerPlayer player, String advancement) {
        if (!game.canUseMenus(player) || !unlockIds().contains(advancement)) return false;
        players.get(player.getUUID()).lbcUnlocks.add(advancement);
        players.save();
        return true;
    }

    private boolean allowed(ServerPlayer player, Entry entry) {
        if (config.ranked || entry.gate == null || players.get(player.getUUID()).lbcUnlocks.contains(entry.gate)) return true;
        // Нативное достижение либо явный административный grant открывает исходный перк.
        var advancement = game.server().getAdvancements().get(Identifier.parse(entry.gate));
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    public boolean select(ServerPlayer player, Kind kind, int page, int slot) {
        if (!game.canUseMenus(player)) return false;
        Entry entry = bySlot(kind, page, slot);
        if (entry == null) {
            player.sendSystemMessage(Component.translatable("skywars2.lbc.pending"));
            return false;
        }
        if (!allowed(player, entry)) {
            player.sendSystemMessage(Component.translatable(entry.lockedKey));
            return false;
        }
        var profile = players.get(player.getUUID());
        switch (kind) {
            case KITS -> profile.lbcKit = entry.id;
            case SKILLS -> { profile.lbcSkill = entry.id; refreshArtifactView(player); }
            case TRINKETS -> {
                int limit = Math.max(1, config.lbcTrinketLimit);
                int slotIndex = Math.floorMod(profile.lbcTrinketSlot, limit);
                profile.trinkets[slotIndex] = entry.id;
                profile.lbcTrinketSlot = (slotIndex + 1) % limit;
                profile.lbcTrinket = entry.id;
            }
            case LEVELUPS -> {
                // Source selection replaces a cyclic slot, allowing repeated IDs.
                int slotIndex = Math.floorMod(profile.lbcLevelupSlot, config.lbcLevelupLimit);
                profile.levelups[slotIndex] = entry.id;
                profile.lbcLevelupSlot = (slotIndex + 1) % config.lbcLevelupLimit;
            }
        }
        players.save();
        return true;
    }

    public boolean selected(PlayerStore.Profile profile, Kind kind, int page, int slot) {
        Entry entry = bySlot(kind, page, slot);
        if (entry == null) return false;
        return switch (kind) {
            case KITS -> profile.lbcKit == entry.id;
            case SKILLS -> profile.lbcSkill == entry.id;
            case TRINKETS -> java.util.Arrays.stream(profile.trinkets, 0, Math.min(config.lbcTrinketLimit, profile.trinkets.length))
                    .anyMatch(id -> id == entry.id);
            case LEVELUPS -> java.util.Arrays.stream(profile.levelups, 0, Math.min(config.lbcLevelupLimit, profile.levelups.length))
                    .anyMatch(id -> id == entry.id);
        };
    }

    /** Called only at start/respawn. Registry tables preserve source names, models and components. */
    public void apply(ServerPlayer player) {
        apply(player, true);
    }

    public void apply(ServerPlayer player, boolean grantTrinkets) {
        // При старте и возрождении сначала снимаются старые бонусы, затем выдаются комплект и выбранные улучшения.
        clear(player);
        var profile = players.get(player.getUUID());
        if (config.lbcKits) {
            Entry kit = byId(Kind.KITS, profile.lbcKit);
            if (kit != null && allowed(player, kit)) {
                grant(player, kit);
            }
        }
        if (grantTrinkets) for (int index = 0; index < Math.min(config.lbcTrinketLimit, profile.trinkets.length); index++) {
            Entry trinket = byId(Kind.TRINKETS, profile.trinkets[index]);
            if (trinket != null && allowed(player, trinket)) grant(player, trinket);
        }
        if (config.lbcLevelups) for (int index = 0; index < Math.min(config.lbcLevelupLimit, profile.levelups.length); index++) {
            Entry levelup = byId(Kind.LEVELUPS, profile.levelups[index]);
            if (levelup == null || !allowed(player, levelup)) continue;
            for (Modifier modifier : levelup.modifiers) {
                var attribute = player.getAttribute(modifier.attribute);
                if (attribute == null) continue;
                // Same source modifier ID means duplicates do not multiply the bonus.
                attribute.removeModifier(modifier.value.id());
                attribute.addTransientModifier(modifier.value);
            }
        }
        Entry skill = byId(Kind.SKILLS, profile.lbcSkill);
        if (config.lbcSkills && skill != null && allowed(player, skill)) {
            // Native hidden-effect chain reproduces I -> II -> III -> IV application order.
            for (int amplifier = 0; amplifier < 4; amplifier++) {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, (50 - amplifier * 10) * 20, amplifier));
            }
            resistanceOwners.put(player.getUUID(), player);
        }
        player.setHealth(player.getMaxHealth());
    }

    private void grant(ServerPlayer player, Entry entry) {
        grantTable(player, entry.table);
    }

    /** Общая нативная выдача таблицы: используется наборами и проверками отдельных перенесённых предметов. */
    void grantTable(ServerPlayer player, String tableId) {
        // Нативная вставка объединяет одинаковые тринкеты и обрабатывает переполнение инвентаря.
        for (var stack : rollTable(player.level(), player.position(), player, tableId)) {
            player.getInventory().placeItemBackInInventory(stack, net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }

    /** Перенесённые loot conditions, включая /loot, читают прежний score; настройкой владеет config. */
    void syncRanked() {
        var rankedScore = game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("ranked"), "lbc.event");
        int ranked = config.ranked ? 1 : 0;
        if (rankedScore.get() != ranked) rankedScore.set(ranked);
    }

    List<net.minecraft.world.item.ItemStack> rollTable(net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.phys.Vec3 origin, net.minecraft.world.entity.Entity source, String tableId) {
        syncRanked();
        var table = game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(tableId)));
        var params = new LootParams.Builder(level).withOptionalParameter(LootContextParams.THIS_ENTITY, source)
                .withParameter(LootContextParams.ORIGIN, origin).create(LootContextParamSets.COMMAND);
        return table.getRandomItems(params);
    }

    /** Remove only this catalog's modifiers, including old persistent copies after a restart. */
    public void clear(ServerPlayer player) {
        if (resistanceOwners.remove(player.getUUID(), player)) player.removeEffect(MobEffects.RESISTANCE);
        for (Entry entry : CATALOG.get(Kind.LEVELUPS)) for (Modifier modifier : entry.modifiers) {
            var attribute = player.getAttribute(modifier.attribute);
            if (attribute != null) attribute.removeModifier(modifier.value.id());
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }
}
