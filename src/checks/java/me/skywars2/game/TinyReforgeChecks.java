package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Real native clicked dispatch, with temporary native loot tables and seeded chance checks. */
public final class TinyReforgeChecks {
    private TinyReforgeChecks() { }
    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException("Tiny reforge: " + message);
    }
    private static ItemStack donor(String flag) {
        var stack = new ItemStack(Items.STICK, 5);
        var data = new CompoundTag(); data.putInt("transformable", 1); data.putInt(flag, 1);
        data.putString("fixture_identity", "preserve components");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); return stack;
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ServerPlayer player;
        final GameChecks.WandThirdPlayerState saved;
        final Set<Entity> initial = new HashSet<>();
        final BlockPos pos;
        final MapManager.BlockSnapshot block;
        AbstractContainerMenu menu;
        int size;
        Fixture(GameController game) {
            this.game = game;
            player = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(game.playing() && player != null && game.allowDamage(player), "active native participant");
            saved = new GameChecks.WandThirdPlayerState(player, game.items().mana());
            game.maps().level().getAllEntities().forEach(initial::add);
            player.closeContainer(); player.getInventory().clearContent();
            player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true);
            player.entityTags().remove("time_stopped");
            player.teleportTo(game.maps().level(), 80.5, 280, 80.5, Set.of(), 0, 0, true);
            player.hasChangedDimension();
            pos = new BlockPos(81, 279, 80); block = game.maps().snapshot(pos);
            game.maps().level().setBlock(pos, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        }
        void open(int width) {
            clear(); player.closeContainer(); player.getInventory().clearContent();
            if (width == 3) player.openMenu(new SimpleMenuProvider((id, inv, owner) ->
                    new CraftingMenu(id, inv, ContainerLevelAccess.create(player.level(), pos)),
                    Component.literal("Tiny random craft check")));
            menu = player.containerMenu; size = width * width; reset("accessory");
        }
        void dropsClear() { drops().forEach(Entity::discard); }
        List<ItemEntity> drops() {
            var result = new ArrayList<ItemEntity>();
            game.maps().level().getAllEntities().forEach(e -> {
                if (!initial.contains(e) && e instanceof ItemEntity item) result.add(item);
            }); return result;
        }
        void clear() {
            if (menu != null) {
                menu.setCarried(ItemStack.EMPTY);
                for (int i = 1; i <= size; i++) menu.getSlot(i).setByPlayer(ItemStack.EMPTY);
            }
            dropsClear();
        }
        void reset(String flag) {
            clear(); player.getInventory().clearContent();
            menu.getSlot(1).setByPlayer(new ItemStack(LbcWands.TINY_TELEPORT, 4));
            menu.getSlot(size).setByPlayer(donor(flag));
        }
        void click(ContainerInput input, int button) { menu.clicked(0, button, input, player); }
        int inventory(Item item) {
            int result = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                var stack = player.getInventory().getItem(i); if (stack.is(item)) result += stack.getCount();
            } return result;
        }
        int dropped(Item item) { return drops().stream().filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum(); }
        int all(Item item) {
            return inventory(item) + dropped(item) + (menu.getCarried().is(item) ? menu.getCarried().getCount() : 0);
        }
        String deliveryState() {
            var items = new ArrayList<String>();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                var stack = player.getInventory().getItem(i);
                if (!stack.isEmpty()) items.add(i + "=" + stack);
            }
            return " cursor=" + menu.getCarried() + " inventory=" + items
                    + " drops=" + drops().stream().map(e -> e.getItem().toString()).toList()
                    + " diamond=" + all(Items.DIAMOND) + " seeds=" + all(Items.WHEAT_SEEDS)
                    + " tiny=" + menu.getSlot(1).getItem() + " donor=" + menu.getSlot(size).getItem();
        }
        void counts(int tiny, int donors, String why) {
            require(menu.getSlot(1).getItem().getCount() == tiny && menu.getSlot(size).getItem().getCount() == donors,
                    why + " exact ingredient counts");
            require(ItemStack.isSameItemSameComponents(menu.getSlot(size).getItem(), donor("accessory")),
                    why + " donor components retained");
        }
        void noLeak() {
            require(!LbcTinyReforge.isPlaceholder(menu.getCarried()), "cursor never receives preview");
            for (int i = 0; i < player.getInventory().getContainerSize(); i++)
                require(!LbcTinyReforge.isPlaceholder(player.getInventory().getItem(i)), "inventory never receives preview");
            for (var drop : drops()) require(!LbcTinyReforge.isPlaceholder(drop.getItem()), "ground never receives preview");
        }
        public void close() {
            clear(); player.closeContainer(); dropsClear(); game.maps().restoreSnapshot(pos, block); saved.restore(game);
        }
    }

    public static void run(GameController game) {
        try (var f = new Fixture(game)) {
            for (int width : new int[] {2, 3}) {
                f.open(width);
                var probe = game.lbc().rollTable(game.maps().level(), f.player.position(), f.player,
                        "lbc:transformables_accessory_all");
                require(probe.stream().filter(s -> s.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum() == 2
                                && probe.stream().filter(s -> s.is(Items.WHEAT_SEEDS)).mapToInt(ItemStack::getCount).sum() == 7,
                        "native accessory fixture override yields full expected bundle: " + probe);
                var random = game.maps().level().getRandom();
                random.setSeed(73019L); long expected = random.nextLong(); random.setSeed(73019L);
                {
                    var preview = f.menu.getSlot(0).getItem().copy();
                    require(LbcTinyReforge.isPlaceholder(preview), width + " stable preview exists");
                    for (int i = 0; i < 10; i++) {
                        f.menu.getSlot(f.size).setByPlayer(donor("accessory"));
                        f.menu.broadcastChanges();
                        require(ItemStack.matches(preview, f.menu.getSlot(0).getItem()), "repeated grid refresh has stable preview");
                    }
                    require(random.nextLong() == expected, "refresh never advances native chance RNG");
                    random.setSeed(73019L);
                    f.menu.getSlot(2).setByPlayer(new ItemStack(Items.DIRT));
                    require(f.menu.getSlot(0).getItem().isEmpty(), "third ingredient rejects random craft");
                    f.menu.getSlot(2).setByPlayer(ItemStack.EMPTY);
                    f.menu.getSlot(f.size).setByPlayer(new ItemStack(LbcWands.TINY_TELEPORT));
                    require(!LbcTinyReforge.isPlaceholder(f.menu.getSlot(0).getItem())
                            && f.menu.getSlot(0).getItem().is(LbcWands.TELEPORT_WAND), "identical Tiny pair keeps ordinary recipe precedence");
                    f.menu.getSlot(f.size).setByPlayer(donor("unsupported_fixture"));
                    require(f.menu.getSlot(0).getItem().isEmpty(), "unsupported transformable category has no preview");
                    f.reset("accessory");
                    random.setSeed(73019L);
                    f.menu.setCarried(new ItemStack(Items.DIRT));
                    f.click(ContainerInput.PICKUP, 0); f.click(ContainerInput.THROW, 0);
                    f.counts(4, 5, "blocked cursor"); f.menu.setCarried(ItemStack.EMPTY);
                    f.player.getInventory().setItem(0, new ItemStack(Items.DIRT));
                    f.click(ContainerInput.SWAP, 0); f.counts(4, 5, "occupied hotbar");
                    f.player.getInventory().clearContent();
                    for (int button : new int[] {-1, 9, 39, 41}) {
                        f.click(ContainerInput.SWAP, button); f.counts(4, 5, "invalid swap target");
                    }
                    for (var input : List.of(ContainerInput.CLONE, ContainerInput.PICKUP_ALL, ContainerInput.QUICK_CRAFT)) {
                        f.click(input, 0); f.counts(4, 5, "unsupported " + input); f.noLeak();
                    }
                    require(random.nextLong() == expected, "rejected clicks never sample randomness");
                    int committed = 0;
                    for (var input : List.of(ContainerInput.PICKUP, ContainerInput.QUICK_MOVE, ContainerInput.SWAP,
                            ContainerInput.SWAP, ContainerInput.THROW, ContainerInput.THROW)) {
                        f.reset("accessory");
                        int button = input == ContainerInput.SWAP && committed == 3 ? 40
                                : input == ContainerInput.THROW && committed == 5 ? 1 : 0;
                        random.setSeed(committed + 100L);
                        f.click(input, button); committed++;
                        f.counts(f.menu.getSlot(1).getItem().getCount(), 4, input + " one transaction");
                        require(f.menu.getSlot(1).getItem().getCount() >= 3, "at most one Tiny consumed");
                        require(f.all(Items.DIAMOND) == 2 && f.all(Items.WHEAT_SEEDS) == 7,
                                input + " complete primary and secondary bundle conserved; " + f.deliveryState());

                        if (input == ContainerInput.PICKUP) require(f.menu.getCarried().is(Items.DIAMOND), "pickup places actual primary on cursor");
                        if (input == ContainerInput.SWAP) require(f.player.getInventory().getItem(button).is(Items.DIAMOND), "swap targets selected hotbar/offhand");
                        if (input == ContainerInput.THROW) require(f.dropped(Items.DIAMOND) == 2 && f.dropped(Items.WHEAT_SEEDS) == 7, "throw drops complete bundle");
                        f.noLeak();
                    }
                    f.reset("accessory");
                    for (int slot = 0; slot < 36; slot++) f.player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
                    f.click(ContainerInput.QUICK_MOVE, 0); committed++;
                    f.counts(f.menu.getSlot(1).getItem().getCount(), 4, "full inventory overflow");
                    require(f.dropped(Items.DIAMOND) == 2 && f.dropped(Items.WHEAT_SEEDS) == 7, "full inventory retains complete bundle as drops");
                    f.noLeak();
                    f.reset("accessory"); f.player.setGameMode(GameType.CREATIVE);
                    random.setSeed(73019L);
                    f.click(ContainerInput.CLONE, 2); f.counts(4, 5, "creative clone blocked");
                    require(random.nextLong() == expected, "creative clone never samples chance"); f.noLeak();
                    f.click(ContainerInput.PICKUP, 0); committed++;
                    f.counts(f.menu.getSlot(1).getItem().getCount(), 4, "creative pays donor"); f.noLeak();
                    f.player.setGameMode(GameType.SURVIVAL);
                    boolean consumed = false, retained = false;
                    // Adjacent seeds share initial bits in the native legacy RNG; spread them deterministically.
                    var seeds = new java.util.Random(94103L);
                    for (int attempt = 0; attempt < 64 && !(consumed && retained); attempt++) {
                        f.reset("accessory"); random.setSeed(seeds.nextLong()); f.click(ContainerInput.PICKUP, 0);
                        int left = f.menu.getSlot(1).getItem().getCount();
                        require(left == 3 || left == 4, "native chance spends zero or one Tiny");
                        consumed |= left == 3; retained |= left == 4;
                        f.counts(left, 4, "seeded native chance");
                        require(f.all(Items.DIAMOND) == 2 && f.all(Items.WHEAT_SEEDS) == 7, "seeded chance preserves entire loot bundle");
                    }
                    require(consumed && retained, "actual native RNG exercises both Tiny chance branches");
                    f.reset("accessory"); random.setSeed(73019L);
                    var stop = game.items().timeStop();
                    require(!stop.stopped(game.maps().level()), "fixture begins outside time stop");
                    try {
                        stop.begin(game.maps().level(), 20);
                        f.click(ContainerInput.PICKUP, 0); f.counts(4, 5, "time stop blocks crafting");
                    } finally { stop.stop(); }
                    require(random.nextLong() == expected, "time stop rejects before chance RNG");
                    f.reset("accessory"); f.player.setGameMode(GameType.SPECTATOR);
                    random.setSeed(73019L);
                    f.click(ContainerInput.PICKUP, 0); f.counts(4, 5, "spectator blocks crafting");
                    require(random.nextLong() == expected, "spectator rejects before chance RNG");
                    f.player.setGameMode(GameType.SURVIVAL);
                    f.reset("accessory"); random.setSeed(73019L);
                    f.clear(); f.player.closeContainer();
                    require(random.nextLong() == expected, "cancelled preview/close never samples chance");
                }
                // This uses the live native Ultra shooter table, with no fixture replacement.
                f.open(width); f.reset("shooter"); f.click(ContainerInput.PICKUP, 0);
                require(!f.menu.getCarried().isEmpty(), "native shooter table delivers an actual result");
                require(f.menu.getSlot(f.size).getItem().getCount() == 4, "native shooter consumes exactly one donor");
                require(f.menu.getSlot(1).getItem().getCount() >= 3, "native shooter consumes at most one Tiny"); f.noLeak();
            }
        }
    }

    /** Requires the runner's empty native transformables_pickaxe_all datapack override. */
    public static void empty(GameController game) {
        try (var f = new Fixture(game)) {
            for (int width : new int[] {2, 3}) {
                f.open(width);
                for (var input : List.of(ContainerInput.PICKUP, ContainerInput.QUICK_MOVE, ContainerInput.SWAP, ContainerInput.THROW)) {
                    f.reset("pickaxe");
                    var tiny = f.menu.getSlot(1).getItem().copy(); var donor = f.menu.getSlot(f.size).getItem().copy();
                    require(LbcTinyReforge.isPlaceholder(f.menu.getSlot(0).getItem()), "empty native table still has neutral preview");
                    f.click(input, 0);
                    require(ItemStack.matches(tiny, f.menu.getSlot(1).getItem()) && ItemStack.matches(donor, f.menu.getSlot(f.size).getItem()),
                            input + " empty native roll preserves full inputs");
                    require(f.menu.getCarried().isEmpty() && f.drops().isEmpty(), "empty native roll emits no output"); f.noLeak();
                }
            }
        }
    }
}
