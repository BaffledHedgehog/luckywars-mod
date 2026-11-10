package me.skywars2.game;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import eu.pb4.sgui.api.SguiUtils;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElement;

import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.net.URI;
import java.util.UUID;

/** All player-facing lobby menus, kept together so navigation and actions share one contract. */
public final class GameMenus {
    private static final String[] SKYWARS_MAP_PAGES = {"2000", "2100", "2200"};
    private static final String[] BEDWARS_MAP_PAGES = {"2000b"};
    private final MinecraftServer server;
    private final GameController controller;
    private final GameConfig config;
    private final PlayerStore players;
    private final MapManager maps;
    private final GameBuilds builds;
    private final java.util.Map<UUID, Integer> mapPages = new java.util.HashMap<>();
    private final java.util.Map<UUID, java.util.EnumMap<Page, Integer>> perkPages = new java.util.HashMap<>();

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
        this.builds = new GameBuilds(controller, players);
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

    public GameBuilds builds() { return builds; }

    /** Keep the real chest's native lid, opener count and positional audio with the GUI. */
    public void openFromChest(ServerPlayer player, net.minecraft.core.BlockPos position, boolean debug) {
        if (debug && !GameController.operator(player)) return;
        if (!(player.level().getBlockEntity(position) instanceof net.minecraft.world.level.block.entity.EnderChestBlockEntity chest)) return;
        if (debug) player.entityTags().add("skywars2.debug_perks");
        Menu menu = open(player, Page.HUB, debug);
        if (menu != null) menu.attachChest(chest);
    }

    /** Настройка доступа меняет вид уже открытых меню без закрытия контейнера. */
    void refreshOpen() {
        for (var player : server.getPlayerList().getPlayers())
            if (SguiUtils.getCurrentGui(player) instanceof Menu menu && controller.canUseMenus(player))
                open(player, menu.page);
    }

    public void open(ServerPlayer player, Page page) {
        open(player, page, debugAccess(player));
    }

    /** Only the gallery chest grants this context; ordinary menus keep their normal gates. */
    public void openDebug(ServerPlayer player) {
        if (!GameController.operator(player)) return;
        player.entityTags().add("skywars2.debug_perks");
        open(player, Page.HUB, true);
    }

    public boolean debugAccess(ServerPlayer player) {
        return GameController.operator(player) && SguiUtils.getCurrentGui(player) instanceof Menu menu && menu.debug;
    }

    private Menu open(ServerPlayer player, Page page, boolean debug) {
        debug = debug && GameController.operator(player);
        if (controller.items().shooters().stunned(player)) return null;
        boolean observer = controller.isObserver(player);
        if (observer && !observerPage(page)) {
            player.sendSystemMessage(c("lobby_only"));
            return null;
        }
        if (!debug && !controller.canUseMenus(player) && page != Page.STATS) {
            player.sendSystemMessage(c("lobby_only"));
            return null;
        }
        if ((page == Page.SETTINGS || page == Page.MAPS) && !controller.allowMatchControl(player)) return null;
        // Personal menus are usable by observers after leaving the watched target.
        if (observer && controller.spectators().attached(player)) controller.spectators().detach(player);
        PlayerStore.Profile profile = players.get(player.getUUID(), player.getName().getString());
        Menu gui = new Menu(player, page);
        gui.debug = debug;
        gui.setTitle(Component.translatable("container.enderchest"));
        gui.setLockPlayerInventory(true);
        switch (page) {
            case HUB -> renderHub(gui, player, profile);
            case SETTINGS -> renderSettings(gui, player, profile);
            case MAPS -> renderMaps(gui, player);
            case VANILLA_KITS -> renderKits(gui, player, profile);
            case VANILLA_SKILLS -> renderSkills(gui, player, profile);
            case KEY_SHOP -> renderKeyShop(gui, player, profile);
            case STATS -> renderStats(gui, player, profile);
            case LBC_KITS, LBC_SKILLS, LBC_TRINKETS, LBC_LEVELUPS, LBC_CHALLENGE ->
                    renderLbc(gui, player, profile);
            case COSMETICS -> renderCosmetics(gui, player, profile);
            case COSMETIC_SOUNDS, COSMETIC_CAGES, COSMETIC_SUFFIXES, COSMETIC_SUFFIXES_NEXT,
                    COSMETIC_WINS, COSMETIC_KILLS -> renderCosmeticDetails(gui, player, profile);
            case CASES -> renderCases(gui, player, profile);
            case BEDWARS -> renderBedwarsTeams(gui,player);
        }
        // Render before changing the live menu. Keep its container ID during navigation,
        // so rapid clicks and client close packets cannot target a half-open replacement.
        if (SguiUtils.getCurrentGui(player) instanceof Menu active) {
            active.page = page;
            active.debug = debug;
            for (int slot = 0; slot < 27; slot++) {
                GuiElement element = gui.getGuiElement(slot);
                if (element == null) active.clearSlot(slot);
                else active.setSlot(slot, element);
            }
            player.containerMenu.sendAllDataToRemote();
            return active;
        } else {
            return gui.open() ? gui : null;
        }
    }

    private void renderHub(Menu gui, ServerPlayer player, PlayerStore.Profile p) {
        // Центральная страница связывает кнопки навигации с настройками, выбором карты и запуском матча.
        boolean observer = controller.isObserver(player);
        for (int slot = 0; slot < 27; slot++) {
            if (slot == 18) continue;
            // Наблюдатель может открыть личные разделы, но не настройки и управление матчем.
            if (observer && (slot == 0 || slot == 1 || slot == 2)) continue;
            Runnable click = switch (slot) {
                case 0 -> () -> open(player, Page.SETTINGS);
                case 1 -> () -> controller.requestStart(player, false);
                case 2 -> () -> open(player, Page.MAPS);
                case 6 -> () -> open(player, Page.KEY_SHOP);
                case 7 -> () -> open(player, Page.COSMETICS);
                case 8 -> () -> open(player, Page.STATS);
                case 9 -> () -> open(player, Page.BEDWARS);
                case 12 -> () -> open(player, Page.VANILLA_KITS);
                case 13 -> () -> open(player, Page.CASES);
                case 14 -> () -> open(player, Page.VANILLA_SKILLS);
                case 17 -> () -> builds.open(player);
                case 20 -> () -> open(player, Page.LBC_TRINKETS);
                case 21 -> () -> open(player, Page.LBC_LEVELUPS);
                case 22 -> () -> open(player, Page.LBC_KITS);
                case 23 -> () -> open(player, Page.LBC_SKILLS);
                case 24 -> () -> open(player, Page.LBC_CHALLENGE);
                case 26 -> () -> player.sendSystemMessage(originalLinksMessage());
                default -> null;
            };
            if (slot == 9 && config.gameMode != 3 && config.gameMode != 4) continue;
            ItemStack stack = OriginalGuiDesign.button(player, "swrg", 0, slot, p, config);
            original(gui, player, slot, stack, null, List.of(), click);
        }
    }

    private void renderSettings(Menu gui, ServerPlayer p, PlayerStore.Profile profile) {
        // Эта страница меняет параметры следующих матчей; change проверяет лобби, сохраняет конфиг и перерисовывает её.
        for (int slot = 0; slot < 27; slot++) {
            if (slot == 18 || slot == 25 || slot == 26) continue;
            Runnable click = switch (slot) {
                case 0 -> () -> change(p, () -> config.pvpMode = cycle(config.pvpMode, 1, 2));
                case 1 -> () -> change(p, () -> config.day = cycle(config.day, 1, 3));
                case 2 -> () -> change(p, () -> config.gameMode = cycle(config.gameMode, 1, 4));
                case 3 -> () -> change(p, () -> config.vanillaKits = !config.vanillaKits);
                case 4 -> () -> change(p, () -> config.vanillaSkills = !config.vanillaSkills);
                case 5 -> () -> change(p, () -> config.stageOneEvent = cycle(config.stageOneEvent, 0, 9));
                case 6 -> () -> change(p, () -> config.stageTwoEvent = cycle(config.stageTwoEvent, 0, 8));
                case 7 -> () -> change(p, () -> config.healthDisplay = cycle(config.healthDisplay,0,3));
                case 19 -> () -> change(p, () -> config.prefixes = cycle(config.prefixes,0,4));
                case 23 -> () -> change(p, () -> config.imbaDropChance = cycle(config.imbaDropChance,0,6));
                case 8 -> () -> change(p, () -> config.offhandWands = !config.offhandWands);
                case 11 -> () -> change(p, () -> config.luckyModifier = cycle(config.luckyModifier, 0, 12));
                case 12 -> () -> change(p, () -> config.tntProtection = cycle(config.tntProtection, 0, 5));
                case 13 -> () -> change(p, () -> config.lbcKits = !config.lbcKits);
                case 14 -> () -> change(p, () -> config.lbcSkills = !config.lbcSkills);
                case 15 -> () -> change(p, () -> config.lbcLevelups = !config.lbcLevelups);
                case 16 -> () -> change(p, () -> config.lbcLevelupLimit = cycle(config.lbcLevelupLimit, 1, 12));
                case 17 -> () -> change(p, () -> config.lbcTrinketLimit = cycle(config.lbcTrinketLimit, 1, 12));
                case 22 -> () -> change(p, () -> config.lbcChallenges = !config.lbcChallenges);
                case 9 -> () -> change(p, () -> config.lives = cycle(config.lives, 1, 5));
                case 10 -> () -> change(p, () -> config.noBorders = !config.noBorders);
                case 24 -> {
                    int[] presets = {2, 10, 20, 40, 60};
                    int next = presets[0];
                    for (int preset : presets) {
                        if (preset > config.health) {
                            next = preset;
                            break;
                        }
                    }
                    int selectedHealth = next;
                    yield () -> change(p, () -> config.health = selectedHealth);
                }
                default -> null;
            };
            original(gui, p, slot, OriginalGuiDesign.button(p, "swrg", 1000, slot, profile, config),
                    null, List.of(), click);
        }
        original(gui, p, 26, OriginalGuiDesign.button(p, "swrg", 1000, 26, profile, config), null, List.of(), () -> {
                    if (!controller.inLobby()) {
                        p.sendSystemMessage(c("lobby_only"));
                        return;
                    }
                    if (!controller.allowMatchControl(p)) return;
                    String previousMap = config.mapId;
                    boolean previousBroken = config.broken;
                    copyConfig(GameConfig.resetDefaults(), config);
                    controller.luckyModifiers().syncConfig();
                    if (!previousMap.equals(config.mapId) || previousBroken != config.broken)
                        controller.selectMap(p, config.mapId, config.broken);
                    open(p, Page.SETTINGS);
                });
        originalTable(gui, p, 18, "swrg:gui/back", null, List.of(), () -> open(p, Page.HUB));
    }

    private void renderMaps(Menu gui, ServerPlayer player) {
        boolean bedwars = config.gameMode == 3 || config.gameMode == 4;
        var entries = maps.catalog(bedwars);
        int pages = Math.max(bedwars ? 1 : 3, entries.stream().mapToInt(map -> mapPage(map.id()) + 1).max().orElse(1));
        int page = Math.max(0, Math.min(mapPages.getOrDefault(player.getUUID(), 0), pages - 1));
        mapPages.put(player.getUUID(), page);
        for (var map : entries) {
            if (mapPage(map.id()) != page) continue;
            var source = OriginalGuiDesign.sourceMapButton(player, map.id(), map.id().equals(config.mapId));
            int logical = (mapNumber(map.id()) - 1) % 25;
            int slot = source == null ? (logical < 18 ? logical : logical + 1) : source.slot();
            ItemStack stack = source == null ? new ItemStack(Items.MAP) : source.stack();
            if (source == null) stack.set(DataComponents.CUSTOM_NAME, map.displayName());
            original(gui, player, slot, stack, null, List.of(), () -> {
                if (bedwars != (config.gameMode == 3 || config.gameMode == 4)) { open(player, Page.MAPS); return; }
                var current = maps.info(map.id());
                if (current == null || !current.available()) {
                    player.sendSystemMessage(current == null ? t("maps.now_unavailable") : current.unavailableReason()); return;
                }
                if (!controller.mapCapacity(current,server.getPlayerList().getPlayers().size())) {
                    player.sendSystemMessage(t("maps.capacity")); return;
                }
                if (controller.selectMap(player, map.id(), config.broken)) open(player, Page.MAPS);
            }, map.id().equals(config.mapId));
        }
        originalTable(gui, player, 18, "swrg:gui/back", null, List.of(), () -> {
            if (page > 0) { mapPages.put(player.getUUID(), page - 1); open(player, Page.MAPS); }
            else open(player, Page.HUB);
        });
        if (page + 1 < pages) originalTable(gui, player, 26, "swrg:gui/next", null, List.of(), () -> {
            mapPages.put(player.getUUID(), page + 1); open(player, Page.MAPS);
        });
    }

    static int mapNumber(String id) { return Integer.parseInt(id.endsWith("b") ? id.substring(0, id.length() - 1) : id); }
    static int mapPage(String id) { return (mapNumber(id) - 1) / 25; }

    private void renderKits(Menu gui, ServerPlayer player, PlayerStore.Profile p) {
        for (int id = 1; id <= 24; id++) {
            int kit = id;
            int sourceSlot = id - 1;
            if (id == 19) continue;
            ItemStack stack = OriginalGuiDesign.button(player, "swrg", 3000, sourceSlot, p, config);
            original(gui, player, sourceSlot, stack, null, List.of(), () -> {
                if (!OriginalGuiDesign.unlocked(player, "swrg", 3000, sourceSlot, p, config)) return;
                p.kit = kit;
                players.save();
                if (debugAccess(player) && controller.playing()) controller.events().giveDebugKit(player);
                open(player, Page.VANILLA_KITS);
            }, p.kit == kit && OriginalGuiDesign.unlocked(player, "swrg", 3000, sourceSlot, p, config));
        }
        originalTable(gui, player, 26, "swrg:gui/reset", null, List.of(), () -> {
                    p.kit = 0;
                    players.save();
                    open(player, Page.VANILLA_KITS);
                });
        originalTable(gui, player, 18, "swrg:gui/back", null, List.of(), () -> open(player, Page.HUB));
    }

    private void renderSkills(Menu gui, ServerPlayer player, PlayerStore.Profile p) {
        for (int i = 0; i < 13; i++) {
            int skill = i + 1;
            ItemStack stack = OriginalGuiDesign.button(player, "swrg", 4000, i, p, config);
            int sourceSlot = i;
            original(gui, player, i, stack, null, List.of(), () -> {
                if (!OriginalGuiDesign.unlocked(player, "swrg", 4000, sourceSlot, p, config)) return;
                p.skill = skill;
                players.save();
                if (debugAccess(player) && controller.playing()) controller.events().giveSkill(player);
                open(player, Page.VANILLA_SKILLS);
            }, p.skill == skill && OriginalGuiDesign.unlocked(player, "swrg", 4000, sourceSlot, p, config));
        }
        originalTable(gui, player, 26, "swrg:gui/reset", null, List.of(), () -> {
                    p.skill = 0;
                    players.save();
                    open(player, Page.VANILLA_SKILLS);
                });
        originalTable(gui, player, 18, "swrg:gui/back", null, List.of(), () -> open(player, Page.HUB));
    }

    private void renderKeyShop(Menu gui, ServerPlayer player, PlayerStore.Profile p) {
        for (int slot = 0; slot < 15; slot++) {
            int purchase = slot;
            ItemStack stack = OriginalGuiDesign.button(player, "swrg", 6000, slot, p, config);
            original(gui, player, slot, stack, null, List.of(), () -> {
                int key = purchase / 3;
                int tier = purchase % 3;
                int amount = tier == 0 ? 1 : tier == 1 ? 5 : 10;
                int price = switch (key) {
                    case 0 -> new int[] {100, 400, 700}[tier];
                    case 1, 2 -> new int[] {150, 600, 1050}[tier];
                    case 3 -> new int[] {200, 800, 1400}[tier];
                    default -> new int[] {100, 400, 750}[tier];
                };
                boolean mem = key == 4;
                int current = mem ? p.memcoins : p.coins;
                if (current < price) {
                    player.sendSystemMessage(c("insufficient_funds"));
                    return;
                }
                if (mem) p.memcoins -= price;
                else p.coins -= price;
                p.keys[key] += amount;
                players.save();
                playMenuSound(player, net.minecraft.core.Holder.direct(SoundEvents.PLAYER_LEVELUP), 1.0f);
                open(player, Page.KEY_SHOP);
            });
        }
        originalTable(gui, player, 18, "swrg:gui/back", null, List.of(), () -> open(player, Page.HUB));
    }

    private void renderStats(Menu gui, ServerPlayer player, PlayerStore.Profile p) {
        original(gui, player, 8, OriginalGuiDesign.button(player, "swrg", 0, 8, p, config), null, List.of(), null);
        statsCard(gui, 10, net.minecraft.world.item.Items.IRON_SWORD,
                t("stats.wins_kills", p.wins, p.kills));
        statsCard(gui, 11, net.minecraft.world.item.Items.SKELETON_SKULL, t("stats.deaths", p.deaths));
        statsCard(gui, 13, net.minecraft.world.item.Items.GOLD_NUGGET,
                Component.translatable("swrg.gui.coins"), Component.literal(Integer.toString(p.coins)));
        statsCard(gui, 14, net.minecraft.world.item.Items.IRON_NUGGET,
                Component.translatable("swrg.gui.memcoins"), Component.literal(Integer.toString(p.memcoins)));
        statsCard(gui, 16, net.minecraft.world.item.Items.TRIPWIRE_HOOK,
                Component.translatable("swrg.gui.keys"),
                t("stats.keys.list", p.keys[0], p.keys[1], p.keys[2], p.keys[3], p.keys[4]));
        originalTable(gui, player, 18, "swrg:gui/back", null, List.of(), () -> open(player, Page.HUB));
    }

    private static void statsCard(Menu gui, int slot, net.minecraft.world.item.Item item,
                                  Component name, Component... lore) {
        var builder = new GuiElementBuilder(item).setComponent(DataComponents.CUSTOM_NAME,
                name.copy().withStyle(style -> style.withColor(ChatFormatting.GREEN).withItalic(false)));
        builder.setLoreRaw(java.util.Arrays.stream(lore)
                .map(line -> line.copy().withStyle(style -> style.withColor(ChatFormatting.AQUA).withItalic(false)))
                .map(Component.class::cast).toList());
        gui.setSlot(slot, builder.build());
    }

    private void renderLbc(Menu gui, ServerPlayer player, PlayerStore.Profile p) {
        // Общий рендерер наборов, навыков и улучшений сопоставляет страницу GUI с исходными страницами LBC.
        Page page = gui.page;
        int[] sourcePages = switch (page) {
            case LBC_KITS -> new int[] {9000, 9100};
            case LBC_SKILLS -> new int[] {10000, 10100};
            case LBC_TRINKETS -> new int[] {11000, 11100};
            case LBC_LEVELUPS -> new int[] {8000, 8100};
            case LBC_CHALLENGE -> new int[] {12000};
            default -> throw new IllegalArgumentException("Not an LBC selector page: " + gui.page);
        };
        var pages = perkPages.computeIfAbsent(player.getUUID(), ignored -> new java.util.EnumMap<>(Page.class));
        int index = Math.clamp(pages.getOrDefault(page, 0), 0, sourcePages.length - 1);
        pages.put(page, index);
        int sourcePage = sourcePages[index];
        LbcLoadouts.Kind loadoutKind = switch (page) {
            case LBC_KITS -> LbcLoadouts.Kind.KITS;
            case LBC_SKILLS -> LbcLoadouts.Kind.SKILLS;
            case LBC_LEVELUPS -> LbcLoadouts.Kind.LEVELUPS;
            case LBC_TRINKETS -> LbcLoadouts.Kind.TRINKETS;
            default -> null;
        };
        for (int slot = 0; slot < 27; slot++) {
            if (slot == 18 || slot == 25 || slot == 26) continue;
            int sourceSlot = slot;
            Runnable click = page == Page.LBC_CHALLENGE && (slot == 0 || slot == 24)
                    ? () -> { if (controller.lbc().selectChallenge(player, sourceSlot)) open(player, page); } : null;
            if (loadoutKind != null) {
                click = () -> {
                    if (controller.lbc().select(player, loadoutKind, sourcePage, sourceSlot)) {
                        if (debugAccess(player) && controller.playing()) controller.lbc().refreshDebugSelection(player, loadoutKind);
                        open(player, page);
                    }
                };
            }
            int selectedCount=loadoutKind==null?0:controller.lbc().selectedCount(p,loadoutKind,sourcePage,sourceSlot);
            boolean selected = page == Page.LBC_CHALLENGE && slot == 0
                    ? OriginalGuiDesign.unlocked(player, "lbc", sourcePage, slot, p, config)
                        && controller.lbc().challengeSelected(player)
                    : loadoutKind != null
                    && OriginalGuiDesign.unlocked(player, "lbc", sourcePage, sourceSlot, p, config)
                    && selectedCount>0;
            original(gui, player, slot,
                    OriginalGuiDesign.button(player, "lbc", sourcePage, slot, p, config), null, List.of(), click,
                    selected, loadoutKind==LbcLoadouts.Kind.TRINKETS?selectedCount:0);
        }
        originalTable(gui, player, 18, "swrg:gui/back", null, List.of(), () -> {
            if (index > 0) { pages.put(page, index - 1); open(player, page); }
            else open(player, Page.HUB);
        });
        if (page == Page.LBC_CHALLENGE) {
            // Сброс награды независим от активного испытания и обычных перков.
            originalTable(gui, player, 25, "lbc:gui/page/12000/reset_challenge_perks", null, List.of(), () -> {
                if (controller.lbc().selectChallenge(player, 25)) open(player, page);
            });
            return;
        }
        originalTable(gui, player, 25, "swrg:gui/reset",
                null, List.of(), () -> {
                    switch (page) {
                        case LBC_KITS -> p.lbcKit = 0;
                        case LBC_SKILLS -> {controller.items().godClaw().clear(player);controller.items().expansionSkill().clear(player);controller.items().stoneGolemSkill().clear(player);controller.items().championSkill().clear(player);p.lbcSkill = 0;}
                        case LBC_TRINKETS -> {
                            p.lbcTrinket = 0;
                            java.util.Arrays.fill(p.trinkets, 0);
                            p.lbcTrinketSlot = 0;
                        }
                        case LBC_LEVELUPS -> {
                            java.util.Arrays.fill(p.levelups, 0);
                            p.lbcLevelupSlot = 0;
                        }
                        default -> throw new IllegalArgumentException("Not an LBC selector page: " + page);
                    }
                    players.save();
                    controller.lbc().refreshArtifactView(player);
                    if (debugAccess(player) && controller.playing() && (page == Page.LBC_SKILLS || page == Page.LBC_LEVELUPS))
                        controller.lbc().refreshDebugSelection(player, page == Page.LBC_SKILLS ? LbcLoadouts.Kind.SKILLS : LbcLoadouts.Kind.LEVELUPS);
                    open(player, page);
                });
        if (index + 1 < sourcePages.length)
            originalTable(gui, player, 26, "swrg:gui/next", null, List.of(), () -> {
                pages.put(page, index + 1);
                open(player, page);
            });
    }

    private void renderCosmetics(Menu gui, ServerPlayer player, PlayerStore.Profile p) {
        for (int slot : new int[] {4, 10, 12, 14, 16}) {
            Page target = switch (slot) {
                case 4 -> Page.COSMETIC_KILLS;
                case 10 -> Page.COSMETIC_WINS;
                case 12 -> Page.COSMETIC_SOUNDS;
                case 14 -> Page.COSMETIC_CAGES;
                default -> Page.COSMETIC_SUFFIXES;
            };
            original(gui, player, slot, OriginalGuiDesign.button(player, "swrg", 7000, slot, p, config),
                    null, List.of(), () -> open(player, target));
        }
        originalTable(gui, player, 18, "swrg:gui/back", null, List.of(), () -> open(player, Page.HUB));
    }

    private int cosmeticPage(Page page) {
        return switch (page) {
            case COSMETIC_SOUNDS -> 7100;
            case COSMETIC_CAGES -> 7200;
            case COSMETIC_SUFFIXES -> 7300;
            case COSMETIC_SUFFIXES_NEXT -> 7310;
            case COSMETIC_WINS -> 7400;
            case COSMETIC_KILLS -> 7500;
            default -> throw new IllegalArgumentException("Not a cosmetic detail page");
        };
    }

    private int cosmeticValue(int page, int slot) {
        if (page == 7100) return slot < 13 ? slot + 1 : slot == 26 ? 0 : -1;
        if (page == 7200) return slot < 18 ? slot : slot >= 19 && slot <= 21 ? slot - 1 : -1;
        if (page == 7300) return slot < 18 ? slot + 1 : slot >= 19 && slot <= 24 ? slot : slot == 25 ? 0 : -1;
        if (page == 7310) return slot < 5 ? slot + 25 : slot == 25 ? 0 : -1;
        if (page == 7400) return slot < 2 ? slot + 1 : slot == 25 ? 0 : -1;
        return slot < 3 ? slot + 1 : slot == 25 ? 0 : -1;
    }

    private String cosmeticGate(int page, int value) {
        if (value == 0) return null;
        return switch (page) {
            case 7100 -> "swrg.sound" + value;
            case 7200 -> "swrg.bars" + value;
            case 7300, 7310 -> value <= 26 ? "swrg:rewards/suffix_" + value : "swrg:rewards/turnir/trophy" + (value - 26);
            case 7400 -> value == 1 ? "lbc:true_advancements/event/pumpkin_sword" : "swrg:rewards/turnir/trophy1";
            case 7500 -> switch (value) {
                case 1 -> "lbc:true_advancements/event/soulcorrupt_wand_upgraded";
                case 2 -> "lbc:true_advancements/event/soulhunter_mace";
                default -> "swrg:rewards/turnir/killer";
            };
            default -> throw new IllegalArgumentException("Not a cosmetic detail page");
        };
    }

    private void renderCosmeticDetails(Menu gui, ServerPlayer player, PlayerStore.Profile p) {
        int page = cosmeticPage(gui.page);
        int selected = switch (page) {
            case 7100 -> p.killSound;
            case 7200 -> p.cage;
            case 7300, 7310 -> p.suffix;
            case 7400 -> p.winEffect;
            default -> p.killEffect;
        };
        for (int slot = 0; slot < 27; slot++) {
            int value = cosmeticValue(page, slot);
            String gate = value < 0 ? null : cosmeticGate(page, value);
            boolean allowed = gate == null || LbcSourceUnlocks.has(player, p, gate);
            Runnable click = null;
            if (value >= 0 && allowed) click = () -> {
                // Recheck permanent ownership at acceptance; ranked/debug never bypass cosmetics.
                if (gate != null && !LbcSourceUnlocks.has(player, p, gate)) return;
                switch (page) {
                    case 7100 -> p.killSound = value;
                    case 7200 -> p.cage = value;
                    case 7300, 7310 -> p.suffix = value;
                    case 7400 -> p.winEffect = value;
                    default -> p.killEffect = value;
                }
                players.save();
                if(page==7300||page==7310)controller.items().cosmetics().refreshSuffixes(List.of(player));
                open(player, gui.page);
            };
            if (slot == 18) click = () -> open(player, page == 7310 ? Page.COSMETIC_SUFFIXES : Page.COSMETICS);
            if (slot == 26 && page == 7300) click = () -> open(player, Page.COSMETIC_SUFFIXES_NEXT);
            original(gui, player, slot, OriginalGuiDesign.cosmeticButton(player, page, slot,
                    allowed ? selected : -1, p, config), null, List.of(), click);
        }
    }

    private void renderCases(Menu gui, ServerPlayer player, PlayerStore.Profile p) {
        var view = controller.cases().renderView(player);
        if (view != null) {
            // All five source case pages share this exact frame; logical Slot2 wins at menu slot11.
            for (int i = 0; i < 9; i++)
                original(gui, player, 9 + i, view.strip().get(i).copy(), null, List.of(), null);
            var separator = new ItemStack(Items.IRON_CHAIN);
            separator.set(DataComponents.TOOLTIP_DISPLAY, new net.minecraft.world.item.component.TooltipDisplay(true, new java.util.LinkedHashSet<>()));
            original(gui, player, 2, separator.copy(), null, List.of(), null);
            original(gui, player, 20, separator.copy(), null, List.of(), null);
            gui.lastCasePhase = view.phase();
            return;
        }
        gui.lastCasePhase = -1;
        for (int slot = 0; slot < 5; slot++) {
            int kind = slot;
            original(gui, player, slot, OriginalGuiDesign.button(player, "swrg", 5000, slot, p, config),
                    null, List.of(), () -> {
                        if (controller.cases().open(player, kind)) open(player, Page.CASES);
                    });
        }
        originalTable(gui, player, 18, "swrg:gui/back", null, List.of(), () -> open(player, Page.HUB));
    }

    private void renderBedwarsTeams(Menu gui,ServerPlayer player) {
        var available=controller.bedwars().availableTeams();
        for(int number=1;number<=16;number++){
            if(!available.contains(number))continue;
            int team=number;
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("minecraft:"+LbcBedwars.colorName(team)+"_bed"));
            boolean selected=player.getTeam()!=null&&player.getTeam().getName().equals(Integer.toString(team));
            original(gui,player,team-1,new ItemStack(item),null,List.of(),()->{if(controller.bedwars().selectTeam(player,team))open(player,Page.BEDWARS);},selected);
        }
        original(gui,player,16,new ItemStack(Items.BARRIER),null,List.of(),()->{if(controller.bedwars().selectTeam(player,0))open(player,Page.BEDWARS);},player.getTeam()==null);
        originalTable(gui,player,18,"swrg:gui/back",null,List.of(),()->open(player,Page.HUB));
    }

    private void renderUnsupported(Menu gui, String title, String explanation) {
        // The page is still a navigation destination, but has no transferred source implementation.
    }

    private void change(ServerPlayer player, Runnable mutation) {
        if (!controller.inLobby()) {
            player.sendSystemMessage(c("lobby_only"));
            return;
        }
        if (!controller.allowMatchControl(player)) return;
        mutation.run();
        config.save();
        controller.luckyModifiers().syncConfig();
        controller.lbc().syncRanked();
        controller.lbc().refreshArtifactViews();
        open(player, Page.SETTINGS);
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
        original(gui,player,slot,stack,name,lore,click,selected,0);
    }

    private void original(Menu gui,ServerPlayer player,int slot,ItemStack stack,Component name,
            List<Component> lore,Runnable click,boolean selected,int selectedCount) {
        if (stack.isEmpty()) return;
        GuiElementBuilder builder = new GuiElementBuilder(stack);
        if (name != null) builder.setComponent(DataComponents.CUSTOM_NAME, name);
        List<Component> renderedLore = new java.util.ArrayList<>();
        if (selected) {
            var label=Component.translatable("swrg.gui.select");
            if(selectedCount>0)label.append(": "+selectedCount);
            renderedLore.add(label.withStyle(style -> style.withColor(ChatFormatting.GREEN).withItalic(false)));
            builder.setComponent(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            var sourceLore = stack.get(DataComponents.LORE);
            if (sourceLore != null) renderedLore.addAll(sourceLore.lines());
        }
        renderedLore.addAll(lore);
        if (!renderedLore.isEmpty()) builder.setLoreRaw(renderedLore);
        if (click != null) {
            builder.setCallback((index, type, action, source) -> {
                if (!controller.canUseMenus(player) && gui.page != Page.STATS) {
                    gui.getPlayer().sendSystemMessage(c("lobby_only"));
                    return;
                }
                playOriginalClick(player);
                click.run();
            });
        }
        gui.setSlot(slot, builder.build());
    }

    private static Component originalLinksMessage() {
        var discord = Component.translatable("swrg.gui.links.discord")
                .withStyle(style -> style.withColor(ChatFormatting.BLUE).withItalic(false).withUnderlined(true)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://discord.gg/RVqrP3ZaYV"))));
        Component planetMinecraft = Component.translatable("swrg.gui.links.planet_minecraft")
                .withStyle(style -> style.withColor(ChatFormatting.DARK_GREEN)
                        .withClickEvent(new ClickEvent.OpenUrl(
                                URI.create("https://www.planetminecraft.com/member/swarog/"))));
        return discord.append(planetMinecraft);
    }

    private static void playOriginalClick(ServerPlayer player) {
        if (!player.entityTags().contains("developer")) {
            playMenuSound(player, SoundEvents.UI_BUTTON_CLICK, 2.0f);
        }
    }

    private static void playMenuSound(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound, float volume) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                sound, net.minecraft.sounds.SoundSource.MASTER, player.getX(), player.getY(), player.getZ(),
                volume, 2.0f, player.getRandom().nextLong()));
    }

    private static String mapSourcePage(String id) {
        if (id.endsWith("b")) return "2000b";
        int number = Integer.parseInt(id);
        if (number <= 25) return "2000";
        if (number <= 50) return "2100";
        return "2200";
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
        to.tntProtection = from.tntProtection;
        to.gameMode = from.gameMode;
        to.bedwarsDirectBedBreaking = from.bedwarsDirectBedBreaking;
        to.luckyModifier = from.luckyModifier;
        to.day = from.day;
        to.healthDisplay=from.healthDisplay;to.imbaDropChance=from.imbaDropChance;to.prefixes=from.prefixes;
        // A public lobby reset only resets match options, never OP-only service flags.
        to.vanillaKits = from.vanillaKits;
        to.vanillaSkills = from.vanillaSkills;
        to.lbcKits = from.lbcKits;
        to.lbcSkills = from.lbcSkills;
        to.lbcLevelups = from.lbcLevelups;
        to.lbcChallenges = from.lbcChallenges;
        to.offhandWands = from.offhandWands;
        to.lbcLevelupLimit = from.lbcLevelupLimit;
        to.lbcTrinketLimit = from.lbcTrinketLimit;
        to.noBorders = from.noBorders;
        to.mapId = from.mapId;
        to.broken = from.broken;
        to.save();
    }

    private static int cycle(int v, int min, int max) {
        return v >= max ? min : Math.max(min, v + 1);
    }

    private static boolean observerPage(Page page) {
        return switch (page) {
            case HUB, VANILLA_KITS, VANILLA_SKILLS, KEY_SHOP, STATS,
                    LBC_KITS, LBC_SKILLS, LBC_TRINKETS, LBC_LEVELUPS, LBC_CHALLENGE,
                    COSMETICS, CASES, COSMETIC_SOUNDS, COSMETIC_CAGES, COSMETIC_SUFFIXES,
                    COSMETIC_SUFFIXES_NEXT, COSMETIC_WINS, COSMETIC_KILLS -> true;
            case SETTINGS, MAPS, BEDWARS -> false;
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
        COSMETIC_SOUNDS, COSMETIC_CAGES, COSMETIC_SUFFIXES, COSMETIC_SUFFIXES_NEXT,
        COSMETIC_WINS, COSMETIC_KILLS,
        CASES,
        BEDWARS
    }

    private final class Menu extends SimpleGui {
        private int lastCasePhase = Integer.MIN_VALUE;
        private Page page;
        private boolean debug;
        private net.minecraft.world.level.block.entity.EnderChestBlockEntity sourceChest;

        private Menu(ServerPlayer player, Page page) {
            super(MenuType.GENERIC_9x3, player, false);
            this.page = page;
        }

        void attachChest(net.minecraft.world.level.block.entity.EnderChestBlockEntity chest) {
            if (sourceChest == chest) return;
            releaseChest();
            if (getPlayer().isSpectator()) return;
            sourceChest = chest;
            var inventory = getPlayer().getEnderChestInventory();
            inventory.setActiveChest(chest);
            inventory.startOpen(getPlayer());
        }

        private void releaseChest() {
            var chest = sourceChest;
            sourceChest = null;
            if (chest != null && getPlayer().getEnderChestInventory().isActiveChest(chest))
                getPlayer().getEnderChestInventory().stopOpen(getPlayer());
        }

        @Override
        public void onRemoved() {
            releaseChest();
        }

        @Override
        public void onTick() {
            if (sourceChest != null && (sourceChest.isRemoved() || sourceChest.getLevel() != getPlayer().level()
                    || !sourceChest.stillValid(getPlayer()))) {
                close();
                return;
            }
            if (page != Page.CASES) return;
            var player = getPlayer();
            var view = controller.cases().renderView(player);
            int phase = view == null ? -1 : view.phase();
            if (phase == lastCasePhase) return;
            for (int slot = 0; slot < 27; slot++) clearSlot(slot);
            renderCases(this, player, players.get(player.getUUID(), player.getName().getString()));
        }

        @Override
        public boolean click(int index, ClickType type, ContainerInput action) {
            var game = me.skywars2.SkyWars2.game();
            if (game != null && game.items().shooters().stunned(getPlayer())) {
                getPlayer().containerMenu.sendAllDataToRemote();
                return true;
            }
            // PICKUP_ALL — синтетический пакет сбора предметов после двух обычных PICKUP;
            // он не должен повторно выполнять кнопку третий раз.
            if (action == ContainerInput.PICKUP_ALL || type == ClickType.MOUSE_DOUBLE_CLICK) {
                GuiElement element = getGuiElement(index);
                return onClick(index, type, action, element);
            }
            return super.click(index, type, action);
        }

        @Override
        public boolean onClick(int index, ClickType type, ContainerInput action, GuiElement element) {
            // Клиент заранее меняет предмет/курсор при клике; ответ сервера синхронизирует GUI даже для пустых и неактивных ячеек.
            getPlayer().containerMenu.sendAllDataToRemote();
            return true;
        }

        @Override
        public void handleException(Throwable exception) {
            // При ошибке callback SGui не возобновляет отложенные обновления, поэтому возвращаем их и синхронизируем курсор.
            var menu = getPlayer().containerMenu;
            menu.resumeRemoteUpdates();
            menu.sendAllDataToRemote();
            me.skywars2.SkyWars2.LOGGER.error("Lobby GUI action failed on {}", page, exception);
        }
    }
}
