package me.skywars2.game;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;

import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.UUID;

/** All player-facing lobby menus, kept together so navigation and actions share one contract. */
public final class GameMenus {
    private static final int PAGE_SIZE = 25;
    private static final int[] MAP_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 19, 20, 21, 22, 23, 24, 25
    };
    private static final String[] STAGE_ONE_EVENTS = {
        "speed", "haste", "refill", "jump", "health", "center_chests", "invisibility", "glowing"
    };
    private static final String[] STAGE_TWO_EVENTS = {
        "wither",
        "dragon",
        "wither_again",
        "tnt_rain",
        "border_shrink",
        "levitation",
        "chest_minecarts"
    };
    private static final String[] ORIGINAL_KIT_IDS = {
        "a1", "a2", "a3", "a4", "a5", "a6", "a7",
        "b1", "b2", "b3", "b4", "b5",
        "c1", "c2", "c3", "c4", "c5", "d1",
        null, "d2", "d3", null, null, null
    };
    private static final String[] ORIGINAL_KIT_NAME_KEYS = {
        "swrg.kit.a1", "swrg.kit.a2", "swrg.kit.a3", "swrg.kit.a4", "swrg.kit.a5", "swrg.kit.a6", "swrg.kit.a7",
        "swrg.kit.b1", "swrg.kit.b2", "swrg.kit.b3", "swrg.kit.b4", "swrg.kit.b5",
        "swrg.kit.c1", "swrg.kit.c2", "swrg.kit.c3", "swrg.kit.c4", "swrg.kit.c5", "swrg.kit.d1",
        null, "swrg.kit.d2", "swrg.kit.d3", "golden_pumpkin", "mace", "wind_charge"
    };
    private static final String[] ORIGINAL_SKILL_KEYS = {
        "swrg.skill.a1", "swrg.skill.a2", "swrg.skill.a3", "swrg.skill.a4",
        "swrg.skill.b1", "swrg.skill.b2", "swrg.skill.b3", "swrg.skill.c1",
        "swrg.skill.c2", "swrg.skill.d1", "swrg.skill.d2", "wind_runner_skill", "dualist_skill"
    };
    private static final String[] ORIGINAL_SKILL_LORE_KEYS = {
        "swrg.lore.skill.a1.0", "swrg.lore.skill.a2.0", "swrg.lore.skill.a3.0", "swrg.lore.skill.a4.0",
        "swrg.lore.skill.b1.0", "swrg.lore.skill.b2.0", "swrg.lore.skill.b3.0", "swrg.lore.skill.c1.0",
        "swrg.lore.skill.c2.0", "swrg.lore.skill.d1.0", "swrg.lore.skill.d2.0",
        "wind_runner_skill.lore.1", "dualist_skill.lore"
    };
    private final MinecraftServer server;
    private final GameController controller;
    private final GameConfig config;
    private final PlayerStore players;
    private final MapManager maps;
    private final java.util.Map<UUID, Integer> mapPages = new java.util.HashMap<>();

    public GameMenus(
            MinecraftServer server,
            GameController controller,
            GameConfig config,
            PlayerStore players,
            MapManager maps) {
        this.server = server;
        this.controller = controller;
        this.config = config;
        this.players = players;
        this.maps = maps;
    }

    private static Component t(String key, Object... args) {
        return Component.translatable("skywars2.gui." + key, args);
    }

    private static Component c(String key, Object... args) {
        return Component.translatable("skywars2.common." + key, args);
    }

    public void open(ServerPlayer player) {
        open(player, Page.HUB);
    }

    public void open(ServerPlayer player, Page page) {
        if (!controller.canUseMenus() && page != Page.STATS) {
            player.sendSystemMessage(c("lobby_only"));
            return;
        }
        PlayerStore.Profile profile = players.get(player.getUUID(), player.getName().getString());
        Menu gui = new Menu(player, page);
        gui.setTitle(Component.translatable("container.enderchest"));
        gui.setLockPlayerInventory(true);
        switch (page) {
            case HUB -> renderHub(gui, player, profile);
            case SETTINGS -> renderSettings(gui, player);
            case MAPS -> renderMaps(gui, player);
            case VANILLA_KITS -> renderKits(gui, player, profile);
            case VANILLA_SKILLS -> renderSkills(gui, player, profile);
            case KEY_SHOP -> renderKeyShop(gui, player, profile);
            case STATS -> renderStats(gui, player, profile);
            case LBC_KITS, LBC_SKILLS, LBC_TRINKETS, LBC_LEVELUPS, LBC_CHALLENGE ->
                    renderLbc(gui, player, profile);
            case COSMETICS -> renderCosmetics(gui, player, profile);
            case CASES -> renderCases(gui);
            case BEDWARS -> renderUnsupported(gui, "bedwars.title", "bedwars.boundary");
        }
        gui.open();
    }

    private void renderHub(Menu gui, ServerPlayer player, PlayerStore.Profile p) {
        original(gui, player, 0, 0, legacyName("swrg.page0.s0", ChatFormatting.RED), List.of(),
                () -> open(player, Page.SETTINGS));
        original(gui, player, 0, 1, legacyName("swrg.page0.s1", ChatFormatting.GREEN), List.of(),
                () -> controller.requestStart(player, true));
        original(gui, player, 0, 2, legacyName("swrg.page0.s2", ChatFormatting.AQUA), List.of(),
                () -> open(player, Page.MAPS));
        original(gui, player, 0, 6, legacyName("swrg.page0.s6", ChatFormatting.YELLOW),
                List.of(balance(p.coins, p.memcoins)), () -> open(player, Page.KEY_SHOP));
        original(gui, player, 0, 7, legacyName("swrg.page0.s7", ChatFormatting.YELLOW), List.of(),
                () -> open(player, Page.COSMETICS));
        original(gui, player, 0, 8, profileName(p), profileLore(p), () -> open(player, Page.STATS));
        original(gui, player, 0, 9, legacyName("swrg.page0.s9", ChatFormatting.WHITE), List.of(),
                () -> open(player, Page.BEDWARS));
        original(gui, player, 0, 12, legacyName("swrg.page0.s12", ChatFormatting.GOLD),
                List.of(selectedLabel(selected(p.kit))), () -> open(player, Page.VANILLA_KITS));
        original(gui, player, 0, 13, legacyName("swrg.page0.s13", ChatFormatting.LIGHT_PURPLE), List.of(),
                () -> open(player, Page.CASES));
        original(gui, player, 0, 14, legacyName("swrg.page0.s14", ChatFormatting.AQUA),
                List.of(selectedLabel(selected(p.skill))), () -> open(player, Page.VANILLA_SKILLS));
        original(gui, player, 0, 17, legacyName("swrg.page0.s17", ChatFormatting.WHITE), List.of(), null);
        original(gui, player, 0, 19, legacyName("swrg.page0.s19", ChatFormatting.AQUA), List.of(), null);
        original(gui, player, 0, 20,
                legacyName("swrg.page0.s20", ChatFormatting.WHITE)
                        .append(Component.literal(Integer.toString(p.lbcTrinket)))
                        .append(legacyName("swrg.page0.s201", ChatFormatting.WHITE)),
                List.of(), () -> open(player, Page.LBC_TRINKETS));
        original(gui, player, 0, 21,
                legacyName("swrg.page0.s21", ChatFormatting.WHITE)
                        .append(Component.literal(Integer.toString(p.levelups.length)))
                        .append(legacyName("swrg.page0.s211", ChatFormatting.WHITE)),
                List.of(), () -> open(player, Page.LBC_LEVELUPS));
        original(gui, player, 0, 22, legacyName("swrg.page0.s22", ChatFormatting.WHITE), List.of(),
                () -> open(player, Page.LBC_KITS));
        original(gui, player, 0, 23, legacyName("swrg.page0.s23", ChatFormatting.WHITE), List.of(),
                () -> open(player, Page.LBC_SKILLS));
        original(gui, player, 0, 24, legacyName("swrg.page0.s24", ChatFormatting.WHITE), List.of(),
                () -> open(player, Page.LBC_CHALLENGE));
        original(gui, player, 0, 25, legacyName("swrg.page0.s25", ChatFormatting.AQUA), List.of(), null);
        original(gui, player, 0, 26, legacyName("swrg.page0.s26", ChatFormatting.AQUA), List.of(
                legacyName("swrg.page0.s26.0", ChatFormatting.GREEN),
                legacyName("swrg.page0.s26.1", ChatFormatting.GREEN),
                legacyName("swrg.page0.s26.2", ChatFormatting.AQUA).withStyle(style -> style.withUnderlined(true)),
                legacyName("swrg.page0.s26.3", ChatFormatting.DARK_GREEN).withStyle(style -> style.withUnderlined(true))), null);
    }

    private void renderSettings(Menu gui, ServerPlayer p) {
        original(gui, p, 1000, 0, storageLabel(0, config.pvpMode), List.of(),
                () -> change(p, () -> config.pvpMode = cycle(config.pvpMode, 1, 2)));
        original(gui, p, 1000, 1, storageLabel(1, config.day), List.of(),
                () -> change(p, () -> config.day = cycle(config.day, 1, 3)));
        original(gui, p, 1000, 3, storageLabel(3, config.vanillaKits ? 1 : 0), List.of(),
                () -> change(p, () -> config.vanillaKits = !config.vanillaKits));
        original(gui, p, 1000, 4, storageLabel(4, config.vanillaSkills ? 1 : 0), List.of(),
                () -> change(p, () -> config.vanillaSkills = !config.vanillaSkills));
        original(gui, p, 1000, 5, storageLabel(5, config.stageOneEvent), List.of(),
                () -> change(p, () -> config.stageOneEvent = cycle(config.stageOneEvent, 0, 9)));
        original(gui, p, 1000, 6, storageLabel(6, config.stageTwoEvent), List.of(),
                () -> change(p, () -> config.stageTwoEvent = cycle(config.stageTwoEvent, 0, 7)));
        original(gui, p, 1000, 9, legacyName("swrg.lives_" + config.lives, ChatFormatting.WHITE), List.of(),
                () -> change(p, () -> config.lives = cycle(config.lives, 1, 5)));
        original(gui, p, 1000, 10, legacyName(config.noBorders ? "swrg.no_borders_on" : "swrg.no_borders_off", ChatFormatting.WHITE), List.of(),
                () -> change(p, () -> config.noBorders = !config.noBorders));
        int[] healthPresets = {2, 10, 20, 40, 60};
        int healthPresetIndex = java.util.Arrays.binarySearch(healthPresets, config.health);
        int nextHealth = healthPresets[(healthPresetIndex < 0 ? 0 : healthPresetIndex + 1) % healthPresets.length];
        original(gui, p, 1000, 24, healthLabel(config.health), List.of(),
                () -> change(p, () -> config.health = nextHealth));
        for (int slot = 0; slot < 27; slot++) {
            if (slot == 0 || slot == 1 || slot == 3 || slot == 4 || slot == 5 || slot == 6
                    || slot == 9 || slot == 10 || slot == 18 || slot == 24 || slot == 26) continue;
            original(gui, p, 1000, slot,
                    legacyName("swrg.page1000.s" + slot, ChatFormatting.WHITE), List.of(), null);
        }
        originalTable(gui, p, 26, "swrg:gui/reset", legacyName("swrg.page1000.s26", ChatFormatting.GREEN),
                List.of(), () -> {
                    if (!controller.inLobby()) {
                        p.sendSystemMessage(c("lobby_only"));
                        return;
                    }
                    copyConfig(GameConfig.resetDefaults(), config);
                    open(p, Page.SETTINGS);
                });
        originalTable(gui, p, 18, "swrg:gui/back", legacyName("swrg.gui.back", ChatFormatting.RED),
                List.of(), () -> open(p, Page.HUB));
    }

    private void renderMaps(Menu gui, ServerPlayer player) {
        List<MapManager.MapInfo> catalog = maps.catalog();
        int pages = Math.max(1, (catalog.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(mapPages.getOrDefault(player.getUUID(), 0), pages - 1));
        mapPages.put(player.getUUID(), page);
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && start + i < catalog.size(); i++) {
            MapManager.MapInfo map = catalog.get(start + i);
            int slot = MAP_SLOTS[i];
            boolean selected = map.id().equals(config.mapId);
            OriginalGuiDesign.SourceMapButton source = OriginalGuiDesign.sourceMapButton(player, map.id(), selected);
            Runnable selectMap =
                    map.available() && source != null
                            ? () -> {
                                MapManager.MapInfo current = maps.info(map.id());
                                if (current == null || !current.available()) {
                                    player.sendSystemMessage(t("maps.now_unavailable"));
                                    return;
                                }
                                if (current.playerCapacity() > 0
                                        && server.getPlayerList().getPlayers().size()
                                                > current.playerCapacity()) {
                                    player.sendSystemMessage(t("maps.capacity"));
                                    return;
                                }
                                if (controller.selectMap(player, map.id(), config.broken)) {
                                    config.mapId = map.id();
                                    config.save();
                                    open(player, Page.MAPS);
                                }
                            }
                            : null;
            if (source != null) {
                ItemStack stack = source.stack();
                GuiElementBuilder builder = new GuiElementBuilder(stack);
                if (source.selected()) builder.setLoreRaw(List.of(Component.translatable("swrg.gui.select")
                        .withStyle(style -> style.withColor(ChatFormatting.GREEN).withItalic(false))));
                if (selectMap != null) builder.setCallback((index, type, action, menu) -> {
                    if (!controller.canUseMenus()) {
                        player.sendSystemMessage(c("lobby_only"));
                        return;
                    }
                    selectMap.run();
                });
                gui.setSlot(slot, builder.build());
            }
        }
        // Datapack maps page uses slots 18 and 26 for back/next.
        original(gui, player, 2000, 18, legacyName("swrg.gui.back", ChatFormatting.RED), List.of(),
                () -> {
                    if (page > 0) {
                        mapPages.put(player.getUUID(), page - 1);
                        open(player, Page.MAPS);
                    } else open(player, Page.HUB);
                });
        if (page + 1 < pages)
            original(gui, player, 2000, 26, legacyName("swrg.gui.next", ChatFormatting.RED), List.of(), () -> {
                mapPages.put(player.getUUID(), page + 1);
                open(player, Page.MAPS);
            });
    }

    private void renderKits(Menu gui, ServerPlayer player, PlayerStore.Profile p) {
        for (int id = 1; id <= 24; id++) {
            int kit = id;
            int sourceSlot = id <= 18 ? id - 1 : id - 1;
            if (id == 19) continue;
            String originalId = ORIGINAL_KIT_IDS[id - 1];
            String nameKey = ORIGINAL_KIT_NAME_KEYS[id - 1];
            Component name = legacyName(nameKey,
                    originalId == null ? ChatFormatting.WHITE : kitColor(originalId));
            List<Component> lore = originalId == null
                    ? originalKitLore(specialKitLoreKeys(id))
                    : originalKitLore(originalKitLoreKeys(originalId));
            original(gui, player, 3000, sourceSlot, name, lore, () -> {
                p.kit = kit;
                players.save();
                open(player, Page.VANILLA_KITS);
            }, p.kit == kit);
        }
        originalTable(gui, player, 26, "swrg:gui/reset", legacyName("swrg.gui.reset", ChatFormatting.RED),
                List.of(), () -> {
                    p.kit = 0;
                    players.save();
                    open(player, Page.VANILLA_KITS);
                });
        originalTable(gui, player, 18, "swrg:gui/back", legacyName("swrg.gui.back", ChatFormatting.RED),
                List.of(), () -> open(player, Page.HUB));
    }

    private void renderSkills(Menu gui, ServerPlayer player, PlayerStore.Profile p) {
        for (int i = 0; i < ORIGINAL_SKILL_KEYS.length; i++) {
            int skill = i + 1;
            Component name = legacyName(ORIGINAL_SKILL_KEYS[i], skillColor(i));
            Component lore = Component.translatable(ORIGINAL_SKILL_LORE_KEYS[i])
                    .withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false));
            original(gui, player, 4000, i, name, List.of(lore), () -> {
                p.skill = skill;
                players.save();
                open(player, Page.VANILLA_SKILLS);
            }, p.skill == skill);
        }
        originalTable(gui, player, 26, "swrg:gui/reset", legacyName("swrg.gui.reset", ChatFormatting.RED),
                List.of(), () -> {
                    p.skill = 0;
                    players.save();
                    open(player, Page.VANILLA_SKILLS);
                });
        originalTable(gui, player, 18, "swrg:gui/back", legacyName("swrg.gui.back", ChatFormatting.RED),
                List.of(), () -> open(player, Page.HUB));
    }

    private void renderKeyShop(Menu gui, ServerPlayer player, PlayerStore.Profile p) {
        int[][] costs = {
            {100, 400, 700}, {150, 600, 1050}, {150, 600, 1050}, {200, 800, 1400}, {100, 400, 750}
        };
        int[][] previousCosts = {
            {0, 500, 1000}, {0, 750, 1500}, {0, 750, 1500}, {0, 1000, 2000}, {0, 500, 1000}
        };
        for (int key = 0; key < 5; key++)
            for (int tier = 0; tier < 3; tier++) {
                int k = key,
                        amount = tier == 0 ? 1 : tier == 1 ? 5 : 10,
                        price = costs[key][tier],
                slot = key * 3 + tier;
                boolean mem = key == 4;
            Component name = legacyName("swrg.page6000.s" + slot, switch (key) {
                case 0 -> ChatFormatting.GREEN;
                case 1 -> ChatFormatting.YELLOW;
                case 2 -> ChatFormatting.AQUA;
                case 3 -> ChatFormatting.LIGHT_PURPLE;
                default -> ChatFormatting.GOLD;
            });
            List<Component> lore = new java.util.ArrayList<>();
            lore.add(Component.translatable("swrg.page6000.s" + slot + ".0")
                    .withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false)));
            Component priceLine = Component.translatable("skywars2.gui.original.price_prefix")
                    .withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false))
                    .append(Component.literal(Integer.toString(price))
                            .withStyle(style -> style.withColor(ChatFormatting.GOLD).withItalic(false)));
            if (previousCosts[key][tier] > price) {
                priceLine = priceLine.append(Component.literal(" ")
                                .withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false)))
                        .append(Component.literal(Integer.toString(previousCosts[key][tier]))
                                .withStyle(style -> style.withColor(ChatFormatting.GOLD)
                                        .withStrikethrough(true).withItalic(false)));
            }
            if (mem) priceLine = priceLine.append(Component.literal(" "))
                    .append(Component.translatable("swrg.gui.memcoins")
                            .withStyle(style -> style.withColor(ChatFormatting.YELLOW).withItalic(false)));
            lore.add(priceLine);
            if (mem) {
                lore.add(Component.translatable("memcoin")
                        .withStyle(style -> style.withColor(ChatFormatting.YELLOW).withItalic(false))
                        .append(Component.literal(Integer.toString(p.memcoins))
                                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withItalic(false))));
            } else {
                lore.add(legacyName("swrg.gui.balance", ChatFormatting.WHITE)
                        .append(Component.literal(Integer.toString(p.coins))
                                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withItalic(false))));
            }
            original(gui, player, 6000, slot, name, lore, () -> {
                            int current = mem ? p.memcoins : p.coins;
                            if (current < price) {
                                player.sendSystemMessage(c("insufficient_funds"));
                                return;
                            }
                            if (mem) p.memcoins -= price;
                            else p.coins -= price;
                            p.keys[k] += amount;
                            players.save();
                            open(player, Page.KEY_SHOP);
                        });
            }
        originalTable(gui, player, 18, "swrg:gui/back", legacyName("swrg.gui.back", ChatFormatting.RED),
                List.of(), () -> open(player, Page.HUB));
    }

    private void renderStats(Menu gui, ServerPlayer player, PlayerStore.Profile p) {
        original(gui, player, 0, 8, profileName(p), profileLore(p), null);
        originalTable(gui, player, 18, "swrg:gui/back", legacyName("swrg.gui.back", ChatFormatting.RED),
                List.of(), () -> open(player, Page.HUB));
    }

    private void renderLbc(Menu gui, ServerPlayer player, PlayerStore.Profile p) {
        int sourcePage = switch (gui.page) {
            case LBC_KITS -> 11000;
            case LBC_SKILLS -> 8000;
            case LBC_TRINKETS -> 9000;
            case LBC_LEVELUPS -> 10000;
            case LBC_CHALLENGE -> 12000;
            default -> throw new IllegalArgumentException("Not an LBC selector page: " + gui.page);
        };
        for (int slot = 0; slot < 27; slot++) {
            originalNs(gui, player, "lbc", sourcePage, slot, null, List.of(), null);
        }
        originalTable(gui, player, 18, "swrg:gui/back", legacyName("swrg.gui.back", ChatFormatting.RED),
                List.of(), () -> open(player, Page.HUB));
    }

    private void renderCosmetics(Menu gui, ServerPlayer player, PlayerStore.Profile p) {
        for (int slot : new int[] {4, 10, 12, 14, 16}) {
            Component name = switch (slot) {
                case 4 -> legacyName("kill_effects", ChatFormatting.RED);
                case 10 -> legacyName("death_effects", ChatFormatting.GREEN);
                case 12 -> legacyName("swrg.page7000.s12", ChatFormatting.WHITE);
                case 14 -> legacyName("swrg.page7000.s14", ChatFormatting.WHITE);
                default -> legacyName("suffixes", ChatFormatting.WHITE);
            };
            original(gui, player, 7000, slot, name, List.of(), null);
        }
        originalTable(gui, player, 18, "swrg:gui/back", legacyName("swrg.gui.back", ChatFormatting.RED),
                List.of(), () -> open(player, Page.HUB));
    }

    private void renderCases(Menu gui) {
        for (int slot = 0; slot < 5; slot++) {
            original(gui, gui.getPlayer(), 5000, slot,
                    legacyName("swrg.page5000.s" + slot, ChatFormatting.WHITE),
                    List.of(Component.translatable("swrg.page5000.s" + slot + ".0")
                            .withStyle(style -> style.withItalic(false))), null);
        }
        originalTable(gui, gui.getPlayer(), 18, "swrg:gui/back", legacyName("swrg.gui.back", ChatFormatting.RED),
                List.of(), () -> open(gui.getPlayer(), Page.HUB));
    }

    private void renderUnsupported(Menu gui, String title, String explanation) {
        // The page is still a navigation destination, but has no transferred source implementation.
    }

    private void change(ServerPlayer player, Runnable mutation) {
        if (!controller.inLobby()) {
            player.sendSystemMessage(c("lobby_only"));
            return;
        }
        mutation.run();
        config.save();
        open(player, Page.SETTINGS);
    }

    private void put(
            Menu gui, int slot, Item item, Component name, Component lore, Runnable click) {
        put(gui, slot, item, name, lore, false, click);
    }

    private void put(
            Menu gui,
            int slot,
            Item item,
            Component name,
            Component lore,
            boolean allowOutsideLobby,
            Runnable click) {
        put(gui, slot, item, name, List.of(lore), allowOutsideLobby, click);
    }

    private void put(
            Menu gui,
            int slot,
            Item item,
            Component name,
            List<Component> lore,
            boolean allowOutsideLobby,
            Runnable click) {
        GuiElementBuilder builder = new GuiElementBuilder(item).setName(name).setLore(lore);
        if (click != null)
            builder.setCallback(
                    (index, type, action, source) -> {
                        if (!controller.canUseMenus() && gui.page != Page.STATS && !allowOutsideLobby) {
                            gui.getPlayer().sendSystemMessage(c("lobby_only"));
                            return;
                        }
                        click.run();
                    });
        gui.setSlot(slot, builder.build());
    }

    private void put(
            Menu gui, int slot, Item item, Component name, List<Component> lore, Runnable click) {
        put(gui, slot, item, name, lore, false, click);
    }

    private void putOriginal(
            Menu gui, int slot, Item item, Component name, List<Component> lore, Runnable click) {
        putOriginal(gui, slot, item, name, lore, false, click);
    }

    private void putOriginal(
            Menu gui,
            int slot,
            Item item,
            Component name,
            List<Component> lore,
            boolean allowOutsideLobby,
            Runnable click) {
        GuiElementBuilder builder =
                new GuiElementBuilder(item)
                        .setComponent(DataComponents.CUSTOM_NAME, name)
                        .setLoreRaw(lore);
        if (click != null)
            builder.setCallback(
                    (index, type, action, source) -> {
                        if (!controller.canUseMenus() && gui.page != Page.STATS && !allowOutsideLobby) {
                            gui.getPlayer().sendSystemMessage(c("lobby_only"));
                            return;
                        }
                        click.run();
                    });
        gui.setSlot(slot, builder.build());
    }

    private void original(
            Menu gui,
            ServerPlayer player,
            int page,
            int slot,
            Component name,
            List<Component> lore,
            Runnable click) {
        originalNs(gui, player, "swrg", page, slot, name, lore, click);
    }

    private void originalNs(
            Menu gui,
            ServerPlayer player,
            String namespace,
            int page,
            int slot,
            Component name,
            List<Component> lore,
            Runnable click) {
        original(gui, player, slot, OriginalGuiDesign.button(player, namespace, page, slot), name, lore, click);
    }

    private void originalTable(
            Menu gui,
            ServerPlayer player,
            int slot,
            String tableId,
            Component name,
            List<Component> lore,
            Runnable click) {
        original(gui, player, slot, OriginalGuiDesign.button(player, tableId), name, lore, click);
    }

    private void original(
            Menu gui,
            ServerPlayer player,
            int slot,
            ItemStack stack,
            Component name,
            List<Component> lore,
            Runnable click) {
        original(gui, player, slot, stack, name, lore, click, false);
    }

    private void original(
            Menu gui,
            ServerPlayer player,
            int slot,
            ItemStack stack,
            Component name,
            List<Component> lore,
            Runnable click,
            boolean selected) {
        if (stack.isEmpty()) return;
        GuiElementBuilder builder = new GuiElementBuilder(stack);
        if (name != null) builder.setComponent(DataComponents.CUSTOM_NAME, name);
        List<Component> renderedLore = new java.util.ArrayList<>();
        if (selected) {
            renderedLore.add(Component.translatable("swrg.gui.select")
                    .withStyle(style -> style.withColor(ChatFormatting.GREEN).withItalic(false)));
            builder.setComponent(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        renderedLore.addAll(lore);
        if (!renderedLore.isEmpty()) builder.setLoreRaw(renderedLore);
        if (click != null) {
            builder.setCallback((index, type, action, source) -> {
                if (!controller.canUseMenus() && gui.page != Page.STATS) {
                    gui.getPlayer().sendSystemMessage(c("lobby_only"));
                    return;
                }
                click.run();
            });
        }
        gui.setSlot(slot, builder.build());
    }

    private void putOriginalName(
            Menu gui, int slot, Item item, Component name, List<Component> lore, Runnable click) {
        putOriginalName(gui, slot, item, name, lore, false, click);
    }

    private void putOriginalName(
            Menu gui,
            int slot,
            Item item,
            Component name,
            List<Component> lore,
            boolean allowOutsideLobby,
            Runnable click) {
        GuiElementBuilder builder =
                new GuiElementBuilder(item)
                        .setComponent(DataComponents.CUSTOM_NAME, name)
                        .setLore(lore);
        if (click != null)
            builder.setCallback(
                    (index, type, action, source) -> {
                        if (!controller.canUseMenus() && gui.page != Page.STATS && !allowOutsideLobby) {
                            gui.getPlayer().sendSystemMessage(c("lobby_only"));
                            return;
                        }
                        click.run();
                    });
        gui.setSlot(slot, builder.build());
    }

    private void putRawLore(
            Menu gui,
            int slot,
            Item item,
            Component name,
            Component lore,
            Runnable click) {
        putRawLore(gui, slot, item, name, List.of(lore), click);
    }

    private void putRawLore(
            Menu gui,
            int slot,
            Item item,
            Component name,
            List<Component> lore,
            Runnable click) {
        GuiElementBuilder builder = new GuiElementBuilder(item).setName(name).setLoreRaw(lore);
        if (click != null)
            builder.setCallback(
                    (index, type, action, source) -> {
                        if (!controller.canUseMenus() && gui.page != Page.STATS) {
                            gui.getPlayer().sendSystemMessage(c("lobby_only"));
                            return;
                        }
                        click.run();
                    });
        gui.setSlot(slot, builder.build());
    }

    private static Component legacyName(String key, ChatFormatting color) {
        return Component.translatable(key)
                .withStyle(style -> style.withColor(color).withItalic(false));
    }

    private static Component storageLabel(int slot, int value) {
        return legacyName("swrg.page1000.s" + slot + "-" + value, ChatFormatting.WHITE);
    }

    private static Component profileName(PlayerStore.Profile profile) {
        return legacyName("swrg.stat.profile", ChatFormatting.GREEN)
                .append(Component.literal(profile.username).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static List<Component> profileLore(PlayerStore.Profile profile) {
        double winRate = profile.games == 0 ? 0.0 : profile.wins * 100.0 / profile.games;
        double killDeath = profile.deaths == 0 ? profile.kills : (double) profile.kills / profile.deaths;
        return List.of(
                legacyName("swrg.stat.game_played", ChatFormatting.AQUA)
                        .append(Component.literal(Integer.toString(profile.games)).withStyle(ChatFormatting.AQUA)),
                legacyName("swrg.stat.winrate", ChatFormatting.AQUA)
                        .append(Component.literal(String.format(java.util.Locale.ROOT, "%.1f", winRate))
                                .withStyle(ChatFormatting.AQUA)),
                legacyName("swrg.stat.kd", ChatFormatting.AQUA)
                        .append(Component.literal(String.format(java.util.Locale.ROOT, "%.1f", killDeath))
                                .withStyle(ChatFormatting.AQUA)));
    }

    private static Component healthLabel(int value) {
        int[] presets = {2, 10, 20, 40, 60};
        for (int i = 0; i < presets.length; i++) if (presets[i] == value)
            return legacyName("swrg.page1000.s24-" + i, ChatFormatting.WHITE);
        return Component.translatable("skywars2.gui.original.health", value)
                .withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false));
    }

    private static ChatFormatting kitColor(String originalId) {
        return switch (originalId.charAt(0)) {
            case 'a' -> ChatFormatting.AQUA;
            case 'b' -> ChatFormatting.GOLD;
            case 'c' -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.RED;
        };
    }

    private static ChatFormatting skillColor(int index) {
        return switch (index) {
            case 0, 1, 2, 3 -> ChatFormatting.AQUA;
            case 4, 5, 6 -> ChatFormatting.GOLD;
            case 7, 8 -> ChatFormatting.LIGHT_PURPLE;
            case 11, 12 -> ChatFormatting.WHITE;
            default -> ChatFormatting.RED;
        };
    }

    private static List<Component> originalKitLore(List<String> keys) {
        List<Component> lore = new java.util.ArrayList<>();
        for (String key : keys) {
            lore.add(Component.translatable(key)
                    .withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false)));
        }
        return lore;
    }

    private static List<String> specialKitLoreKeys(int id) {
        return switch (id) {
            case 22 -> List.of("golden_pumpkin.lore");
            case 23 -> List.of("mace.lore.1", "mace.lore.2", "mace.lore.3", "mace.lore.4");
            case 24 -> List.of("wind_charge.lore");
            default -> List.of();
        };
    }

    private static List<String> originalKitLoreKeys(String id) {
        return switch (id) {
            case "a1" -> List.of("swrg.lore.kit.a1.0", "swrg.lore.kit.a1.1", "swrg.lore.kit.a1.2");
            case "a2" -> List.of("swrg.lore.kit.a2.0", "swrg.lore.kit.a2.1");
            case "a3" -> List.of("swrg.lore.kit.a3.0", "swrg.lore.kit.a3.1", "swrg.lore.kit.a3.2");
            case "a4" -> List.of("swrg.lore.kit.a4.0", "swrg.lore.kit.a4.1");
            case "a5" -> List.of("swrg.lore.kit.a5.0", "swrg.lore.kit.a5.1", "swrg.lore.kit.a5.2");
            case "a6" -> List.of("swrg.lore.kit.a6.0");
            case "a7" -> List.of("swrg.lore.kit.a7.0");
            case "b1" -> List.of("swrg.lore.kit.b1.0", "swrg.lore.kit.b1.1", "swrg.lore.kit.b1.2");
            case "b2" -> List.of("swrg.lore.kit.b2.0", "swrg.lore.kit.b2.1", "swrg.lore.kit.b2.2", "swrg.lore.kit.b2.3");
            case "b3" -> List.of("swrg.lore.kit.b3.0");
            case "b4" -> List.of("swrg.lore.kit.b4.0", "swrg.lore.kit.b4.1", "swrg.lore.kit.b4.2", "swrg.lore.kit.b4.3");
            case "b5" -> List.of("swrg.lore.kit.b5.0", "swrg.lore.kit.b5.1", "swrg.lore.kit.b5.2", "swrg.lore.kit.b5.3", "swrg.lore.kit.b5.4");
            case "c1" -> List.of("swrg.lore.kit.c1.0", "swrg.lore.kit.c1.1");
            case "c2" -> List.of("swrg.lore.kit.c2.0", "swrg.lore.kit.c2.1");
            case "c3" -> List.of("swrg.lore.kit.c3.0");
            case "c4" -> List.of("swrg.lore.kit.c4.0", "swrg.lore.kit.c4.1", "swrg.lore.kit.c4.2", "swrg.lore.kit.c4.3");
            case "c5" -> List.of("swrg.lore.kit.c5.0", "swrg.lore.kit.c5.1", "swrg.lore.kit.c5.2", "swrg.lore.kit.c5.3", "swrg.lore.kit.c5.4");
            case "d1" -> List.of("swrg.lore.kit.d1.0");
            case "d2" -> List.of("swrg.lore.kit.d2.0", "swrg.lore.kit.d2.1", "swrg.lore.kit.d2.2", "swrg.lore.kit.d2.3");
            case "d3" -> List.of("swrg.lore.kit.d3.0", "swrg.lore.kit.d3.1");
            default -> List.of();
        };
    }

    private static Component balance(int coins, int memcoins) {
        return Component.translatable("swrg.gui.balance")
                .withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false))
                .append(Component.literal(Integer.toString(coins)))
                .append(Component.literal(" "))
                .append(Component.translatable("swrg.gui.coins").withStyle(style -> style.withItalic(false)))
                .append(Component.literal(" / "))
                .append(Component.literal(Integer.toString(memcoins)))
                .append(Component.literal(" "))
                .append(Component.translatable("swrg.gui.memcoins").withStyle(style -> style.withItalic(false)));
    }

    private void back(Menu gui, int slot, ServerPlayer player, Page page) {
        putOriginalName(
                gui,
                slot,
                Items.ARROW,
                legacyName("swrg.gui.back", ChatFormatting.RED),
                List.of(c("back.lore")),
                true,
                () -> {
                    if (controller.canUseMenus()) open(player, page);
                    else player.closeContainer();
                });
    }

    private static void copyConfig(GameConfig from, GameConfig to) {
        to.lives = from.lives;
        to.health = from.health;
        to.countdownSeconds = from.countdownSeconds;
        to.stageOneSeconds = from.stageOneSeconds;
        to.stageTwoSeconds = from.stageTwoSeconds;
        to.endingSeconds = from.endingSeconds;
        to.stageOneEvent = from.stageOneEvent;
        to.stageTwoEvent = from.stageTwoEvent;
        to.pvpMode = from.pvpMode;
        to.day = from.day;
        to.vanillaKits = from.vanillaKits;
        to.vanillaSkills = from.vanillaSkills;
        to.noBorders = from.noBorders;
        to.mapId = from.mapId;
        to.broken = from.broken;
        to.save();
    }

    private static int cycle(int v, int min, int max) {
        return v >= max ? min : Math.max(min, v + 1);
    }

    private static int next(int v, int min, int max) {
        return cycle(v, min, max);
    }

    private static Component eventSelection(int n, String[] events) {
        if (n <= 0) return t("event.off");
        if (n == 1) return t("event.random");
        return t("event.selected", n - 1, t("event." + events[Math.min(n - 2, events.length - 1)]));
    }

    private static Component selected(int n) {
        return n == 0
                ? t("selection.none")
                : Component.translatable("skywars2.gui.selection.number", n);
    }

    private static Component selectedLabel(Component value) {
        return Component.translatable("swrg.gui.select")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN).withItalic(false))
                .append(Component.literal(": "))
                .append(value);
    }

    private static Component yesNo(boolean b) {
        return b ? t("common.yes") : t("common.no");
    }

    private static Component join(int[] values) {
        return Component.translatable(
                "skywars2.gui.stats.keys.list",
                values[0],
                values[1],
                values[2],
                values[3],
                values[4]);
    }

    private static Component title(Page p, ServerPlayer player) {
        return switch (p) {
            case HUB -> t("hub.title");
            case SETTINGS -> legacyName("swrg.page0.s0", ChatFormatting.RED);
            case MAPS -> legacyName("swrg.page0.s2", ChatFormatting.AQUA);
            case VANILLA_KITS -> legacyName("swrg.page0.s12", ChatFormatting.GOLD);
            case VANILLA_SKILLS -> legacyName("swrg.page0.s14", ChatFormatting.AQUA);
            case KEY_SHOP -> legacyName("swrg.page0.s6", ChatFormatting.YELLOW);
            case STATS -> legacyName("swrg.stat.profile", ChatFormatting.GREEN).copy()
                    .append(Component.literal(player.getName().getString()));
            case LBC_KITS -> legacyName("swrg.page0.s22", ChatFormatting.WHITE);
            case LBC_SKILLS -> legacyName("swrg.page0.s23", ChatFormatting.WHITE);
            case LBC_TRINKETS -> t("lbc.trinkets");
            case LBC_LEVELUPS -> t("lbc.levelups");
            case LBC_CHALLENGE -> legacyName("swrg.page0.s24", ChatFormatting.WHITE);
            case COSMETICS -> legacyName("swrg.page0.s7", ChatFormatting.YELLOW);
            case CASES -> legacyName("swrg.page0.s13", ChatFormatting.LIGHT_PURPLE);
            case BEDWARS -> t("bedwars.title");
        };
    }

    private static Component lbcCategoryTitle(Page page) {
        return switch (page) {
            case LBC_KITS -> legacyName("swrg.page0.s22", ChatFormatting.WHITE);
            case LBC_SKILLS -> legacyName("swrg.page0.s23", ChatFormatting.WHITE);
            case LBC_CHALLENGE -> legacyName("swrg.page0.s24", ChatFormatting.WHITE);
            case LBC_TRINKETS -> t("lbc.trinkets");
            case LBC_LEVELUPS -> t("lbc.levelups");
            default -> throw new IllegalArgumentException("Not an LBC selector page: " + page);
        };
    }

    public enum Page {
        HUB,
        SETTINGS,
        MAPS,
        VANILLA_KITS,
        VANILLA_SKILLS,
        KEY_SHOP,
        STATS,
        LBC_KITS,
        LBC_SKILLS,
        LBC_TRINKETS,
        LBC_LEVELUPS,
        LBC_CHALLENGE,
        COSMETICS,
        CASES,
        BEDWARS
    }

    private static final class Menu extends SimpleGui {
        private final Page page;

        private Menu(ServerPlayer player, Page page) {
            super(MenuType.GENERIC_9x3, player, false);
            this.page = page;
        }
    }
}
