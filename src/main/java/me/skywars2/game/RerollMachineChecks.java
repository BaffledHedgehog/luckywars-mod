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
public final class RerollMachineChecks {
    private RerollMachineChecks() { }
    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException("Reroll machine: " + message);
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
        final java.util.Map<net.minecraft.advancements.AdvancementHolder, Set<String>> criteria = new java.util.HashMap<>();
        final BlockPos pos;
        final MapManager.BlockSnapshot block;
        AbstractContainerMenu menu;
        int size;
        Fixture(GameController game) {
            this.game = game;
            player = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(game.playing() && player != null && game.allowDamage(player), "active native participant");
            saved = new GameChecks.WandThirdPlayerState(player, game.items().mana());
            for(String id:List.of("lbc:true_advancements/lightning_smith","lbc:true_advancements/legends/academy/lightning")) {
                var a=game.server().getAdvancements().get(net.minecraft.resources.Identifier.parse(id));
                if(a!=null) { var done=new HashSet<String>(); player.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add); criteria.put(a,done); }
            }
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
                    Component.literal("Reroll machine check")));
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
            menu.getSlot(1).setByPlayer(new ItemStack(LbcReforgeMachineItem.MACHINE, 4));
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
            require(!LbcRerollCrafting.isPlaceholder(menu.getCarried()), "cursor never receives preview");
            for (int i = 0; i < player.getInventory().getContainerSize(); i++)
                require(!LbcRerollCrafting.isPlaceholder(player.getInventory().getItem(i)), "inventory never receives preview");
            for (var drop : drops()) require(!LbcRerollCrafting.isPlaceholder(drop.getItem()), "ground never receives preview");
        }
        public void close() {
            clear(); player.closeContainer(); dropsClear(); game.maps().restoreSnapshot(pos, block); saved.restore(game);
            criteria.forEach((a,done)->{
                var now=new ArrayList<String>(); player.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(now::add);
                now.stream().filter(c->!done.contains(c)).forEach(c->player.getAdvancements().revoke(a,c));
                done.forEach(c->player.getAdvancements().award(a,c));
            });
        }
    }

    private static void mana(Fixture f, int value) { f.game.items().mana().set(f.player,value); }
    private static int mana(Fixture f) { return f.game.items().mana().get(f.player); }
    private static void paid(Fixture f,int start,int donors,String why) {
        require(mana(f)==start-5000*donors,why+" exact signed mana charge");
        require(f.menu.getSlot(1).getItem().getCount()==4,why+" catalyst retained");
        f.noLeak();
    }
    public static void run(GameController game) {
        try(var f=new Fixture(game)) {
            grant(game,f.player);
            for(int width:new int[]{2,3}) {
                f.open(width);
                var random=game.maps().level().getRandom();
                random.setSeed(73019L); long expected=random.nextLong(); random.setSeed(73019L);
                var preview=f.menu.getSlot(0).getItem().copy();
                require(LbcRerollCrafting.isPlaceholder(preview),"neutral preview");
                for(int i=0;i<10;i++) {
                    f.menu.getSlot(f.size).setByPlayer(donor("accessory")); f.menu.broadcastChanges();
                    require(ItemStack.matches(preview,f.menu.getSlot(0).getItem()),"stable opaque preview");
                }
                require(random.nextLong()==expected,"preview refresh does not sample RNG");
                f.menu.getSlot(2).setByPlayer(new ItemStack(Items.DIRT));
                require(f.menu.getSlot(0).getItem().isEmpty(),"unrelated extras rejected");
                f.menu.getSlot(2).setByPlayer(new ItemStack(LbcReforgeMachineItem.MACHINE));
                require(f.menu.getSlot(0).getItem().isEmpty(),"two catalyst slots rejected");
                f.reset("accessory");
                f.menu.getSlot(1).setByPlayer(new ItemStack(LbcWands.TINY_TELEPORT));
                f.menu.getSlot(f.size).setByPlayer(new ItemStack(LbcWands.TINY_TELEPORT));
                require(f.menu.getSlot(0).getItem().is(LbcWands.TELEPORT_WAND),"Tiny pair keeps combine precedence");
                f.reset("accessory"); mana(f,0); random.setSeed(73019L);
                f.menu.setCarried(new ItemStack(Items.DIRT)); f.click(ContainerInput.PICKUP,0);
                f.click(ContainerInput.THROW,0); f.menu.setCarried(ItemStack.EMPTY);
                f.player.getInventory().setItem(0,new ItemStack(Items.DIRT)); f.click(ContainerInput.SWAP,0);
                f.player.getInventory().clearContent();
                for(int button:new int[]{-1,9,39,41}) f.click(ContainerInput.SWAP,button);
                for(var input:List.of(ContainerInput.CLONE,ContainerInput.PICKUP_ALL,ContainerInput.QUICK_CRAFT)) f.click(input,0);
                f.counts(4,5,"rejected actions"); paid(f,0,0,"rejected actions");
                require(random.nextLong()==expected,"rejected actions never sample RNG");
                for(int start:new int[]{100000,0,-25}) {
                    for(var input:List.of(ContainerInput.PICKUP,ContainerInput.QUICK_MOVE,ContainerInput.SWAP,ContainerInput.THROW)) {
                        f.reset("accessory"); mana(f,start); f.click(input,0);
                        f.counts(4,4,input+" unit donor"); paid(f,start,1,input.toString());
                        require(f.all(Items.DIAMOND)==2 && f.all(Items.WHEAT_SEEDS)==7,"entire ordinary reward bundle: "+f.deliveryState());
                    }
                }
                f.reset("accessory"); mana(f,0);
                for(int slot=2;slot<=f.size;slot++) f.menu.getSlot(slot).setByPlayer(donor("accessory"));
                f.click(ContainerInput.PICKUP,0); paid(f,0,f.size-1,"all donor slots");
                for(int slot=2;slot<=f.size;slot++) require(f.menu.getSlot(slot).getItem().getCount()==4,"one unit per occupied donor slot");
                require(f.all(Items.DIAMOND)==2*(f.size-1) && f.all(Items.WHEAT_SEEDS)==7*(f.size-1),"all donor bundles delivered");
                f.reset("accessory"); mana(f,0);
                for(int slot=0;slot<36;slot++) f.player.getInventory().setItem(slot,new ItemStack(Items.COBBLESTONE,64));
                f.click(ContainerInput.QUICK_MOVE,0); paid(f,0,1,"overflow");
                require(f.dropped(Items.DIAMOND)==2 && f.dropped(Items.WHEAT_SEEDS)==7,"overflow preserves full bundle");
                f.reset("accessory"); mana(f,0); f.player.setGameMode(GameType.CREATIVE);
                f.click(ContainerInput.CLONE,2); paid(f,0,0,"creative clone");
                f.click(ContainerInput.PICKUP,0); f.counts(4,4,"creative consumes donor"); paid(f,0,1,"creative");
                f.player.setGameMode(GameType.SURVIVAL);
                f.reset("accessory"); mana(f,0);
                var stop=game.items().timeStop();
                try { stop.begin(game.maps().level(),20); f.click(ContainerInput.PICKUP,0); f.counts(4,5,"time stop"); paid(f,0,0,"time stop"); }
                finally { stop.stop(); }
                f.player.setGameMode(GameType.SPECTATOR); f.click(ContainerInput.PICKUP,0);
                f.counts(4,5,"spectator"); paid(f,0,0,"spectator"); f.player.setGameMode(GameType.SURVIVAL);
                f.reset("shooter"); mana(f,0); f.click(ContainerInput.PICKUP,0);
                require(!f.menu.getCarried().isEmpty(),"actual ordinary shooter pool delivers"); paid(f,0,1,"actual shooter");
            }
        }
    }
    public static void empty(GameController game) {
        try(var f=new Fixture(game)) {
            for(int width:new int[]{2,3}) {
                f.open(width);
                for(var input:List.of(ContainerInput.PICKUP,ContainerInput.QUICK_MOVE,ContainerInput.SWAP,ContainerInput.THROW)) {
                    f.reset("pickaxe"); mana(f,-25);
                    var machine=f.menu.getSlot(1).getItem().copy(); var donor=f.menu.getSlot(f.size).getItem().copy();
                    require(LbcRerollCrafting.isPlaceholder(f.menu.getSlot(0).getItem()),"empty mapped table neutral preview");
                    f.click(input,0);
                    require(ItemStack.matches(machine,f.menu.getSlot(1).getItem()) && ItemStack.matches(donor,f.menu.getSlot(f.size).getItem()),"all empty preserves input components and counts");
                    paid(f,-25,0,"all empty"); require(f.menu.getCarried().isEmpty() && f.drops().isEmpty(),"all empty emits nothing");
                    f.reset("accessory"); f.menu.getSlot(2).setByPlayer(donor("pickaxe")); mana(f,0); f.click(input,0);
                    paid(f,0,1,"mixed empty and successful donors");
                    require(ItemStack.matches(donor("pickaxe"),f.menu.getSlot(2).getItem()),"individual empty donor preserved");
                    require(f.menu.getSlot(f.size).getItem().getCount()==4 && f.all(Items.DIAMOND)==2 && f.all(Items.WHEAT_SEEDS)==7,"other donor commits complete bundle");
                }
            }
        }
    }
    private static final String GATE="lbc:true_advancements/legends/academy/lightning";
    private static boolean key(Component text,String key) {
        return text.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().equals(key)
                || text.getSiblings().stream().anyMatch(c->key(c,key));
    }
    private static void equalComponents(ItemStack expected,ItemStack actual,String label) {
        if(ItemStack.matches(expected,actual)) return;
        var differences=new ArrayList<String>();
        for(var type:net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE) {
            Object left=expected.get(type),right=actual.get(type);
            if(!java.util.Objects.equals(left,right)) differences.add(
                    net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type)
                            +" expected="+left+" actual="+right);
        }
        throw new IllegalStateException("Reroll machine: "+label+" full ItemStack mismatch"
                +" expectedItem="+net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(expected.getItem())
                +" actualItem="+net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(actual.getItem())
                +" expectedCount="+expected.getCount()+" actualCount="+actual.getCount()
                +" componentDifferences="+differences
                +" expectedComponents="+expected.getComponents()
                +" actualComponents="+actual.getComponents()
                +" expectedPatch="+expected.getComponentsPatch()
                +" actualPatch="+actual.getComponentsPatch());
    }
    private static ItemStack reward(GameController game,ServerPlayer player,String id) {
        var items=game.lbc().rollTable(player.level(),player.position(),player,id);
        require(items.size()==1 && items.getFirst().getCount()==1,id+" exactly one reward"); return items.getFirst();
    }
    private static void grant(GameController game,ServerPlayer player) {
        var p=game.players().get(player.getUUID()); int kit=p.lbcKit;
        boolean kits=game.config().lbcKits,skills=game.config().lbcSkills,levels=game.config().lbcLevelups,challenges=game.config().lbcChallenges;
        var unlocks=new HashSet<>(p.lbcUnlocks);
        try {
            p.lbcKit=25; p.lbcUnlocks.add(GATE); game.config().lbcKits=true;
            game.config().lbcSkills=false; game.config().lbcLevelups=false; game.config().lbcChallenges=false;
            game.lbc().apply(player,false);
            int count=0; for(int i=0;i<player.getInventory().getContainerSize();i++) if(player.getInventory().getItem(i).is(LbcReforgeMachineItem.MACHINE)) count+=player.getInventory().getItem(i).getCount();
            require(count==1,"native gameplay kit apply grants exactly one machine");
        } finally {
            p.lbcKit=kit; p.lbcUnlocks.clear(); p.lbcUnlocks.addAll(unlocks);
            game.config().lbcKits=kits; game.config().lbcSkills=skills; game.config().lbcLevelups=levels; game.config().lbcChallenges=challenges;
        }
    }
    private static void click(ServerPlayer player,int slot) { GameChecks.inventoryClick(player,slot,0,ContainerInput.PICKUP); }
    public static void menu(GameController game) {
        require(game.inLobby(),"menu qualification in lobby");
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(player!=null,"native bot connected");
        var source=reward(game,player,"lbc:kit/reroll_machine"); var alias=reward(game,player,"lbc:ported/reroll_machine");
        var defaults=new ItemStack(LbcReforgeMachineItem.MACHINE);
        equalComponents(source,alias,"source alias vs ported alias");
        equalComponents(defaults,source,"native defaults vs source loot alias");
        require(!(source.getItem() instanceof net.minecraft.world.item.BlockItem),"machine cannot place source command block");
        require(source.get(DataComponents.ITEM_MODEL).toString().equals("lbc:reroll_machine") && key(source.getHoverName(),"lbckit.24"),"source model and title");
        var lore=source.get(DataComponents.LORE); require(lore!=null && lore.lines().size()==2,"exact two source lore rows");
        for(int i=0;i<2;i++) require(key(lore.lines().get(i),"lbckit.24.lore."+(i+1)) && Boolean.FALSE.equals(lore.lines().get(i).getStyle().isItalic()) && net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.GRAY).equals(lore.lines().get(i).getStyle().getColor()),"source gray nonitalic lore");
        var data=source.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        require(data.size()==2 && data.getIntOr("reroll_machine",0)==1 && data.getIntOr("craftusable",0)==1,"exact source flags");
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
            @Override public net.minecraft.network.PacketListener getPacketListener() { return player.connection; }
        };
        var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)connection;
        net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider,()->{
            var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(source,net.minecraft.world.item.TooltipFlag.NORMAL,provider.getPacketContext(),player.registryAccess());
            require(proxy.is(Items.PAPER) && proxy.get(DataComponents.ITEM_MODEL).equals(source.get(DataComponents.ITEM_MODEL)),"actual Polymer paper proxy preserves model");
        });
        var profile=game.players().get(player.getUUID()); int kit=profile.lbcKit; boolean ranked=game.config().ranked;
        var unlocks=new HashSet<>(profile.lbcUnlocks);
        var a=game.server().getAdvancements().get(net.minecraft.resources.Identifier.parse(GATE)); require(a!=null,"source gate registered");
        var done=new HashSet<String>(); player.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);
        try {
            game.config().ranked=false; profile.lbcKit=23; profile.lbcUnlocks.remove(GATE);
            done.forEach(c->player.getAdvancements().revoke(a,c));
            game.menus().open(player,GameMenus.Page.LBC_KITS);
            var gui=(eu.pb4.sgui.api.gui.SlotBasedGui)eu.pb4.sgui.api.SguiUtils.getCurrentGui(player);
            // A prior invocation may have left the selector on its second page.
            if(!key(gui.getGuiElement(0).getItemStack().getHoverName(),"lbckit.24")) click(player,26);
            click(player,0); require(profile.lbcKit==23,"locked source page9100 slot0 cannot select");
            for(String criterion:a.value().criteria().keySet()) player.getAdvancements().award(a,criterion);
            game.menus().open(player,GameMenus.Page.LBC_KITS); click(player,0);
            require(profile.lbcKit==25 && player.containerMenu.getCarried().isEmpty(),"native unlocked slot selects kit25");
            var now=new ArrayList<String>(); player.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(now::add);
            now.forEach(c->player.getAdvancements().revoke(a,c)); profile.lbcUnlocks.remove(GATE); profile.lbcKit=23; game.config().ranked=true;
            game.menus().open(player,GameMenus.Page.LBC_KITS); click(player,0); require(profile.lbcKit==25,"ranked bypass source unlock");
        } finally {
            var now=new ArrayList<String>(); player.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(now::add);
            now.stream().filter(c->!done.contains(c)).forEach(c->player.getAdvancements().revoke(a,c)); done.forEach(c->player.getAdvancements().award(a,c));
            profile.lbcKit=kit; profile.lbcUnlocks.clear(); profile.lbcUnlocks.addAll(unlocks); game.config().ranked=ranked;
            game.players().save(); player.closeContainer();
        }
    }
}
