package me.skywars2.game;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import me.skywars2.mixin.WorkstationCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;

/** Menu-local native workspaces, separate from player inventory and terrain block inventories. */
final class MatchWorkstations {
    private static final Set<Class<?>> KINDS=Set.of(CraftingMenu.class,AnvilMenu.class,SmithingMenu.class,
            GrindstoneMenu.class,StonecutterMenu.class,CartographyTableMenu.class,LoomMenu.class,EnchantmentMenu.class);
    static boolean supports(AbstractContainerMenu menu){return KINDS.contains(menu.getClass());}
    private record Site(ServerLevel level,BlockPos pos,Block block) {
        void validate(){if(!level.hasChunkAt(pos)||level.getBlockState(pos).getBlock()!=block)throw new IllegalStateException("Restore native workstation terrain before views: "+pos);}
        ContainerLevelAccess access(){validate();return ContainerLevelAccess.create(level,pos);}
    }
    private record Buffer(int firstSlot,Class<?> kind,List<ItemStack> items,RecipeHolder<?> recipe) {
        void restore(AbstractContainerMenu menu){
            var container=menu.getSlot(firstSlot).container;
            if(container.getClass()!=kind||container.getContainerSize()!=items.size())throw new IllegalStateException("Native workspace shape changed");
            var raw=raw(container);for(int n=0;n<items.size();n++)raw.set(n,items.get(n).copy());
            if(container instanceof ResultContainer result)result.setRecipeUsed(recipe);
        }
    }
    private final Class<?> kind;
    private final Site site;
    private final List<Buffer> buffers;
    private final int[] data;
    private final List<Consumer<AbstractContainerMenu>> runtime=new ArrayList<>();

    private MatchWorkstations(ServerPlayer player) {
        var menu=player.containerMenu;kind=menu.getClass();site=site(menu);validate(player);
        buffers=new ArrayList<>();var seen=new IdentityHashMap<Container,Boolean>();
        for(int i=0;i<menu.slots.size();i++){
            var c=menu.getSlot(i).container;if(c==player.getInventory()||seen.put(c,true)!=null)continue;
            var items=raw(c).stream().map(ItemStack::copy).toList();
            buffers.add(new Buffer(i,c.getClass(),items,c instanceof ResultContainer r?r.getRecipeUsed():null));
        }
        data=((WorkstationCheckpointAccess.Data)menu).skywars2$data().stream().mapToInt(DataSlot::get).toArray();
        if(menu instanceof AnvilMenu){
            var a=(WorkstationCheckpointAccess.Anvil)menu;int repair=a.skywars2$repair();String name=a.skywars2$name();boolean rename=a.skywars2$renaming();
            runtime.add(m->{var t=(WorkstationCheckpointAccess.Anvil)m;t.skywars2$repair(repair);t.skywars2$name(name);t.skywars2$renaming(rename);});
        }
        if(menu instanceof StonecutterMenu){
            var a=(WorkstationCheckpointAccess.Stonecutter)menu;var input=a.skywars2$input().copy();var recipes=new net.minecraft.world.item.crafting.SelectableRecipe.SingleInputSet<>(List.copyOf(a.skywars2$recipes().entries()));long sound=a.skywars2$sound();
            runtime.add(m->{var t=(WorkstationCheckpointAccess.Stonecutter)m;t.skywars2$input(input.copy());t.skywars2$recipes(recipes);t.skywars2$sound(sound);});
        }
        if(menu instanceof LoomMenu){
            var a=(WorkstationCheckpointAccess.Loom)menu;var patterns=List.copyOf(a.skywars2$patterns());long sound=a.skywars2$sound();
            runtime.add(m->{var t=(WorkstationCheckpointAccess.Loom)m;t.skywars2$patterns(patterns);t.skywars2$sound(sound);});
        }
        if(menu instanceof CartographyTableMenu){long sound=((WorkstationCheckpointAccess.Cartography)menu).skywars2$sound();runtime.add(m->((WorkstationCheckpointAccess.Cartography)m).skywars2$sound(sound));}
        for(int i=0;i<menu.slots.size();i++)if(menu.getSlot(i) instanceof ResultSlot){
            int slot=i,count=((WorkstationCheckpointAccess.CraftResult)menu.getSlot(i)).skywars2$removed();
            runtime.add(m->((WorkstationCheckpointAccess.CraftResult)m.getSlot(slot)).skywars2$removed(count));
        }
    }
    static MatchWorkstations capture(ServerPlayer player){return new MatchWorkstations(player);}
    private static ContainerLevelAccess access(AbstractContainerMenu menu){
        if(menu instanceof CraftingMenu)return ((WorkstationCheckpointAccess.Crafting)menu).skywars2$access();
        if(menu instanceof ItemCombinerMenu)return ((WorkstationCheckpointAccess.Combiner)menu).skywars2$access();
        if(menu instanceof GrindstoneMenu)return ((WorkstationCheckpointAccess.Grindstone)menu).skywars2$access();
        if(menu instanceof StonecutterMenu)return ((WorkstationCheckpointAccess.Stonecutter)menu).skywars2$access();
        if(menu instanceof CartographyTableMenu)return ((WorkstationCheckpointAccess.Cartography)menu).skywars2$access();
        if(menu instanceof LoomMenu)return ((WorkstationCheckpointAccess.Loom)menu).skywars2$access();
        if(menu instanceof EnchantmentMenu)return ((WorkstationCheckpointAccess.Enchantment)menu).skywars2$access();
        throw new IllegalStateException("Unsupported native workstation");
    }
    private static Site site(AbstractContainerMenu menu){
        return access(menu).evaluate((level,pos)->{
            if(!(level instanceof ServerLevel server)||!server.hasChunkAt(pos))throw new IllegalStateException("Native workspace must have a resident server site");
            return new Site(server,pos.immutable(),level.getBlockState(pos).getBlock());
        }).orElseThrow(()->new IllegalStateException("Detached workstation needs its own checkpoint adapter"));
    }
    private static NonNullList<ItemStack> raw(Container c){
        if(c instanceof SimpleContainer simple)return simple.getItems();
        if(c instanceof TransientCraftingContainer)return ((WorkstationCheckpointAccess.Grid)c).skywars2$items();
        if(c instanceof ResultContainer)return ((WorkstationCheckpointAccess.Result)c).skywars2$items();
        throw new IllegalStateException("Workspace buffer needs its own checkpoint adapter: "+c.getClass().getName());
    }
    static void validate(ServerPlayer player){validate(player,true);}
    static void validateFuture(ServerPlayer player){validate(player,false);}
    private static void validate(ServerPlayer player,boolean current){
        var menu=player.containerMenu;if(!supports(menu))throw new IllegalStateException("Unsupported workstation kind");if(current)site(menu);
        if(menu instanceof CraftingMenu&&((WorkstationCheckpointAccess.Crafting)menu).skywars2$placing())throw new IllegalStateException("Capture only between native recipe-placement transactions");
        if(menu.slots.stream().filter(s->s.container==player.getInventory()).count()!=36)throw new IllegalStateException("Workspace must bind current player inventory");
        for(var s:menu.slots)if(s.container!=player.getInventory())raw(s.container);
    }
    /** Closing abandoned future workspaces must not return/drop their inputs into the restored timeline. */
    static void discard(AbstractContainerMenu menu){
        if(!supports(menu))return;
        for(var s:menu.slots)if(!(s.container instanceof Inventory)){
            var values=raw(s.container);for(int i=0;i<values.size();i++)values.set(i,ItemStack.EMPTY);
            if(s.container instanceof ResultContainer result)result.setRecipeUsed(null);
        }
    }
    void preflight(){site.validate();}
    boolean matches(ServerPlayer player){
        if(player.containerMenu.getClass()!=kind)return false;
        try{validate(player);return site.equals(site(player.containerMenu));}catch(IllegalStateException bad){return false;}
    }
    AbstractContainerMenu create(int id,Inventory inventory){
        var a=site.access();
        if(kind==CraftingMenu.class)return new CraftingMenu(id,inventory,a);
        if(kind==AnvilMenu.class)return new AnvilMenu(id,inventory,a);
        if(kind==SmithingMenu.class)return new SmithingMenu(id,inventory,a);
        if(kind==GrindstoneMenu.class)return new GrindstoneMenu(id,inventory,a);
        if(kind==StonecutterMenu.class)return new StonecutterMenu(id,inventory,a);
        if(kind==CartographyTableMenu.class)return new CartographyTableMenu(id,inventory,a);
        if(kind==LoomMenu.class)return new LoomMenu(id,inventory,a);
        if(kind==EnchantmentMenu.class)return new EnchantmentMenu(id,inventory,a);
        throw new IllegalStateException("Unsupported native workspace constructor");
    }
    void restore(AbstractContainerMenu menu){
        if(menu.getClass()!=kind)throw new IllegalStateException("Restore correct native workspace first");
        for(var buffer:buffers)buffer.restore(menu);
        var values=((WorkstationCheckpointAccess.Data)menu).skywars2$data();
        if(values.size()!=data.length)throw new IllegalStateException("Native workspace data shape changed");
        for(int i=0;i<data.length;i++)values.get(i).set(data[i]);
        for(var value:runtime)value.accept(menu);
    }
}
