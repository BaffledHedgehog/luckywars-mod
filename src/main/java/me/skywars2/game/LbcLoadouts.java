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
    private final Set<java.util.UUID> inventorySkillsChanging=new java.util.HashSet<>();
    private final Set<java.util.UUID> pendingInventorySkills=new java.util.LinkedHashSet<>();
    // Сетевое отображение читает только опубликованный снимок, не изменяемые профили/файлы с Netty-потока.
    private final java.util.concurrent.ConcurrentMap<java.util.UUID, Boolean> artifactReaders = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentMap<java.util.UUID, Boolean> knowledgeViews = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile Set<String> knowledgeChatSenders = Set.of();

    public LbcLoadouts(GameController game, GameConfig config, PlayerStore players) {
        this.game = game; this.config = config; this.players = players;
    }

    /** Резервная сверка косметического доступа после команд/внешних правок профиля, без изменения предметов. */
    public void tick() {
        if(!pendingInventorySkills.isEmpty())for(var id:List.copyOf(pendingInventorySkills)){
            var player=game.server().getPlayerList().getPlayer(id);
            if(player==null){pendingInventorySkills.remove(id);continue;}
            inventoryChanged(player);
        }
        if (++skillTickCounter < 20) return;
        skillTickCounter = 0;
        tickSecond();
    }

    /** Периодическая сверка только флагов; инвентари и сущности отправляются лишь при изменившемся доступе. */
    void tickSecond() {
        refreshArtifactViews();
        refreshKnowledgeViews();
        artifactReaders.keySet().removeIf(id -> game.server().getPlayerList().getPlayer(id) == null);
        knowledgeViews.keySet().removeIf(id -> game.server().getPlayerList().getPlayer(id) == null);
    }

    /** Netty/Polymer читают только опубликованные флаги, без профилей, scoreboard и мира. */
    public boolean hidesKnowledge(ServerPlayer player) {
        return player != null && knowledgeViews.getOrDefault(player.getUUID(), false);
    }

    public boolean isKnowledgeChatSender(Component name) { return knowledgeChatSenders.contains(name.getString()); }

    private void refreshKnowledgeViews() {
        var names = new java.util.HashSet<String>();
        for (var player : game.server().getPlayerList().getPlayers()) {
            names.add(player.getDisplayName().getString());
            names.add(player.getName().getString());
            refreshKnowledgeView(player);reconcileReactiveSkills(player);
        }
        knowledgeChatSenders = Set.copyOf(names);
    }

    public void refreshKnowledgeView(ServerPlayer player) {
        boolean hidden = players.get(player.getUUID()).lbcChallenge == 1;
        boolean wasHidden = hidesKnowledge(player);
        if (hidden == wasHidden) return;
        if (hidden) {
            // Очищаем ранее показанный UI до включения фильтра, иначе клиент сохранит его старые строки.
            game.attributeHud().clear(player);
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(Component.empty()));
            var objectives = new java.util.HashSet<net.minecraft.world.scores.Objective>();
            var board = game.server().getScoreboard();
            for (var slot : net.minecraft.world.scores.DisplaySlot.values()) {
                var objective = board.getDisplayObjective(slot);
                if (objective != null) objectives.add(objective);
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket(slot, null));
            }
            for (var objective : objectives)
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetObjectivePacket(objective, 1));
        }
        knowledgeViews.put(player.getUUID(), hidden);
        LbcSkills.refreshKnowledgeViewer(player);
        game.items().invalidateHud(player);
        game.items().refreshHud(player);
        if (!hidden) {
            var board = game.server().getScoreboard();
            var objectives = new java.util.HashSet<net.minecraft.world.scores.Objective>();
            for (var slot : net.minecraft.world.scores.DisplaySlot.values()) {
                var objective = board.getDisplayObjective(slot);
                if (objective != null && objectives.add(objective))
                    for (var packet : board.getStartTrackingPackets(objective)) player.connection.send(packet);
            }
        }
    }

    public void joined(ServerPlayer player) {
        knowledgeViews.remove(player.getUUID());
        refreshKnowledgeViews();
    }

    public void left(ServerPlayer player) {
        pendingInventorySkills.remove(player.getUUID());
        knowledgeViews.remove(player.getUUID());
        artifactReaders.remove(player.getUUID());
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
        return config.ranked || game.debugPerks(player) || entry.gate == null || hasUnlock(player, entry.gate);
    }

    /** Shared live skill admission; immunity checks never advance clocks or query suppressors. */
    public void reconcileReactiveSkills(ServerPlayer player){
        int selected=players.get(player.getUUID()).lbcSkill;
        game.items().rainbowAura().selected(player,selected==32);
        if(selected==36||game.items().psychblindSkill().enrolled(player))game.items().psychblindSkill().reconcile(player);
    }

    public boolean hasSkill(ServerPlayer player,int skill){
        if(game.balanceLegacy()||!config.lbcSkills||players.get(player.getUUID()).lbcSkill!=skill)return false;
        var entry=byId(Kind.SKILLS,skill);
        return entry!=null&&allowed(player,entry)&&game.playing()&&game.stage()!=GameController.Stage.ENDING
                &&player.isAlive()&&!player.isRemoved()&&player.level()==game.maps().level()
                &&game.canUseGameplayItems(player)&&!game.isObserver(player)&&LbcDimensionItems.ordinary(player)
                &&(game.items().wands().validRoleTarget(player,false)
                    ||skill==19&&player.isSpectator()&&game.items().edibleSpells().polymorphed(player)
                    &&java.util.Collections.disjoint(player.entityTags(),java.util.Set.of("spectator","rcdenyalways","strict_map_object","magic_academy","sauvojen_tuntija")));
    }

    /** Event-only permanent item upgrades; no inventory polling and no jammer admission. */
    public void inventoryChanged(ServerPlayer player){
        var id=player.getUUID();if(inventorySkillsChanging.contains(id))return;
        var profile=players.get(id);int skill=profile.lbcSkill;
        var entry=byId(Kind.SKILLS,skill);
        if(!config.lbcSkills||(skill!=4&&skill!=8&&skill!=11)||entry==null||!allowed(player,entry)
                ||!game.playing()||game.stage()==GameController.Stage.ENDING||!player.isAlive()||player.isRemoved()
                ||player.level()!=game.maps().level()||!game.canUseGameplayItems(player)||game.isObserver(player)
                ||!LbcDimensionItems.ordinary(player)||!game.items().wands().validRoleTarget(player,false)){
            pendingInventorySkills.remove(id);return;
        }
        if(game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()
                ||game.items().timeStop().stopped(player.level())||game.items().wands().inputTimeStopped(player)){
            pendingInventorySkills.add(id);return;
        }
        pendingInventorySkills.remove(id);inventorySkillsChanging.add(id);
        try{if(LbcInventorySkills.apply(player,skill))player.getInventory().setChanged();}
        finally{inventorySkillsChanging.remove(id);}
    }

    private boolean hasUnlock(ServerPlayer player, String gate) {
        if (players.get(player.getUUID()).lbcUnlocks.contains(gate)) return true;
        // Нативное достижение либо явный административный grant открывает исходный перк.
        var advancement = game.server().getAdvancements().get(Identifier.parse(gate));
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    public boolean challengeRewardAvailable(ServerPlayer player) {
        return config.ranked || game.debugPerks(player) || hasUnlock(player, "lbc:challenges/no_knowledge");
    }

    /** В одной исходной ячейке два разных выбора: испытание до достижения, набор после него. */
    public boolean selectChallenge(ServerPlayer player, int slot) {
        if (!game.canUseMenus(player) || game.items().actionsBlocked(player)) return false;
        var profile = players.get(player.getUUID());
        switch (slot) {
            case 0 -> {
                if (challengeRewardAvailable(player)) profile.lbcChallengeKit = 1;
                else {
                    if (!hasUnlock(player, "lbc:true_advancements/legends/academy/root")) {
                        player.sendSystemMessage(Component.translatable("lbc.adv.academy_upgraded.lore"));
                        return false;
                    }
                    profile.lbcChallenge = 1;
                }
            }
            case 24 -> { profile.lbcChallenge = 0; profile.lbcChallengeProgress = 0; }
            case 25 -> profile.lbcChallengeKit = 0;
            default -> { return false; }
        }
        players.save();
        refreshKnowledgeView(player);
        return true;
    }

    public boolean challengeSelected(ServerPlayer player) {
        var profile = players.get(player.getUUID());
        return challengeRewardAvailable(player) ? profile.lbcChallengeKit == 1
                : profile.lbcChallenge == 1 && hasUnlock(player, "lbc:true_advancements/legends/academy/root");
    }

    /** Считаем только состоявшееся повышение у NPC, а не готовность к нему или повторный клик. */
    public void academyPromoted(ServerPlayer player) {
        var profile = players.get(player.getUUID());
        if (profile.lbcChallenge != 1 || !game.playing() || !game.canUseGameplayItems(player)) return;
        if (++profile.lbcChallengeProgress >= 20) {
            profile.lbcChallenge = 0;
            profile.lbcChallengeProgress = 0;
            game.items().mana().award(player, "lbc:challenges/no_knowledge");
            refreshKnowledgeView(player);
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal(" ")));
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                    Component.translatable("challenge_completed")));
        }
        players.save();
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
            case SKILLS -> { game.items().rainbowAura().clear(player);game.items().psychblindSkill().clear(player.getUUID());game.items().godClaw().clear(player);game.items().expansionSkill().clear(player);game.items().stoneGolemSkill().clear(player);game.items().championSkill().clear(player);game.items().symbioteSkill().clear(player);game.items().zeusSkill().clear(player);game.items().machinegunSkill().clear(player);game.items().containedSkills().clear(player);profile.lbcSkill = entry.id; refreshArtifactView(player); LbcParasite.beforeEffects(player);game.items().containedSkills().beforeEffects(player); }
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
        if(kind==Kind.SKILLS){inventoryChanged(player);reconcileReactiveSkills(player);}
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
        apply(player, grantTrinkets, true);
    }

    /** Debug selection updates native bonuses immediately without reissuing an unrelated kit. */
    void refreshDebugSelection(ServerPlayer player, Kind kind) {
        if (!game.debugPerks(player)) return;
        var profile = players.get(player.getUUID());
        if (kind == Kind.KITS || kind == Kind.TRINKETS) {
            var entry = byId(kind, kind == Kind.KITS ? profile.lbcKit : profile.lbcTrinket);
            if (entry != null && allowed(player, entry)) grant(player, entry);
        } else apply(player, false, false);
    }

    /** Historical BedWars life: kit/skill/trinkets only; startup upgrades and challenges are not reissued. */
    public void applyBedwarsRespawn(ServerPlayer player) {
        clear(player);
        var profile=players.get(player.getUUID());
        if(config.lbcKits){var kit=byId(Kind.KITS,profile.lbcKit);if(kit!=null&&allowed(player,kit))grant(player,kit);}
        for(int n=0;n<Math.min(config.lbcTrinketLimit,profile.trinkets.length);n++){
            var trinket=byId(Kind.TRINKETS,profile.trinkets[n]);if(trinket!=null&&allowed(player,trinket))grant(player,trinket);
        }
        var skill=byId(Kind.SKILLS,profile.lbcSkill);
        if(config.lbcSkills&&skill!=null&&skill.id==5&&allowed(player,skill)){
            for(int amplifier=0;amplifier<4;amplifier++)player.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,(50-amplifier*10)*20,amplifier));
            resistanceOwners.put(player.getUUID(),player);
        }
        game.items().govnomancer().effectsChanged(player);LbcParasite.beforeEffects(player);game.items().containedSkills().beforeEffects(player);
        inventoryChanged(player);reconcileReactiveSkills(player);player.setHealth(player.getMaxHealth());
    }

    private void apply(ServerPlayer player, boolean grantTrinkets, boolean grantKits) {
        // При старте и возрождении сначала снимаются старые бонусы, затем выдаются комплект и выбранные улучшения.
        clear(player);
        var profile = players.get(player.getUUID());
        // Награды испытаний включаются отдельно от наборов и повторяются на новой жизни, как в source.
        if (grantKits && config.lbcChallenges && profile.lbcChallengeKit == 1 && challengeRewardAvailable(player))
            grantTable(player, "lbc:ported/academy_token");
        if (grantKits && config.lbcKits) {
            Entry kit = byId(Kind.KITS, profile.lbcKit);
            if (kit != null && allowed(player, kit)) {
                grant(player, kit);
            }
        }
        if (grantTrinkets) for (int index = 0; index < Math.min(config.lbcTrinketLimit, profile.trinkets.length); index++) {
            Entry trinket = byId(Kind.TRINKETS, profile.trinkets[index]);
            if (trinket != null && allowed(player, trinket)) grant(player, trinket);
        }
        boolean randomStat=false;var resourceLevelups=new java.util.HashSet<Integer>();
        if (config.lbcLevelups) for (int index = 0; index < Math.min(config.lbcLevelupLimit, profile.levelups.length); index++) {
            Entry levelup = byId(Kind.LEVELUPS, profile.levelups[index]);
            if (levelup == null || !allowed(player, levelup)) continue;
            if(levelup.id==1)randomStat=true;resourceLevelups.add(levelup.id);
            for (Modifier modifier : levelup.modifiers) {
                var attribute = player.getAttribute(modifier.attribute);
                if (attribute == null) continue;
                // Same source modifier ID means duplicates do not multiply the bonus.
                attribute.removeModifier(modifier.value.id());
                attribute.addTransientModifier(modifier.value);
            }
        }
        LbcManaLevelups.apply(game,player,resourceLevelups);
        if(randomStat)LbcRandomStatLevelup.apply(player);
        Entry skill = byId(Kind.SKILLS, profile.lbcSkill);
        if (config.lbcSkills && skill != null && skill.id == 5 && allowed(player, skill)) {
            // Native hidden-effect chain reproduces I -> II -> III -> IV application order.
            for (int amplifier = 0; amplifier < 4; amplifier++) {
                player.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE, (50 - amplifier * 10) * 20, amplifier));
            }
            resistanceOwners.put(player.getUUID(), player);
        }
        game.items().govnomancer().effectsChanged(player);
        LbcParasite.beforeEffects(player);
        game.items().containedSkills().beforeEffects(player);
        inventoryChanged(player);reconcileReactiveSkills(player);
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
        LbcManaLevelups.clear(game,player);
        pendingInventorySkills.remove(player.getUUID());
        LbcRandomStatLevelup.clear(player);
        if (resistanceOwners.remove(player.getUUID(), player)) player.removeEffect(MobEffects.RESISTANCE);
        for (Entry entry : CATALOG.get(Kind.LEVELUPS)) for (Modifier modifier : entry.modifiers) {
            var attribute = player.getAttribute(modifier.attribute);
            if (attribute != null) attribute.removeModifier(modifier.value.id());
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    /** Outgoing UI policy; only published snapshots are read, including from network threads. */
    public boolean shouldFilterKnowledgePacket(ServerPlayer player, net.minecraft.network.protocol.Packet<?> packet) {
        if (!hidesKnowledge(player)) return false;
        // Player chat also carries signature/acknowledgement state and must reach the client intact.
        if (packet instanceof net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket chat) {
            return !isKnowledgeChatSender(chat.chatType().name());
        }
        return packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket
                || packet instanceof net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket
                || packet instanceof net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
                || packet instanceof net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket
                || packet instanceof net.minecraft.network.protocol.game.ClientboundSetScorePacket
                || packet instanceof net.minecraft.network.protocol.game.ClientboundResetScorePacket;
    }
}
