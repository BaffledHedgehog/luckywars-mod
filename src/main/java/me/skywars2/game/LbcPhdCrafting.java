package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.scores.ScoreHolder;

/** PhD's reusable catalyst and original per-match ingredient hash; preview is pure. */
public final class LbcPhdCrafting {
    private static final TagKey<Item> BIO=TagKey.create(net.minecraft.core.registries.Registries.ITEM,Identifier.parse("minecraft:bioitems"));
    private static final ScoreHolder SEED=ScoreHolder.forNameOnly("#game_seed");
    private LbcPhdCrafting(){}
    public static void matchStarted(GameController game){
        game.items().mana().score(SEED,"lbc.math").set(game.maps().level().getRandom().nextInt(10_000_000)+1);
    }
    public static int seed(GameController game){return game.items().mana().hudValue(SEED,"lbc.math");}
    /** Every authored LBC Polymer carrier is stable and does not depend on viewer/tooltip projection. */
    public static Item legacyItem(ItemStack stack){
        var item=stack.getItem();
        if(BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("skywars2")&&item instanceof PolymerItem polymer)
            return polymer.getPolymerItem(stack,PacketContext.get());
        return item;
    }
    public static boolean matches(CraftingInput input){
        if(input.width()!=2||input.height()!=2||input.ingredientCount()!=4)return false;
        int certificates=0,bio=0;
        for(var stack:input.items()){
            if(LbcPills.certificate(stack))certificates++;
            else if(legacyItem(stack).builtInRegistryHolder().is(BIO))bio++;
            else return false;
        }
        return certificates==1&&bio==3;
    }
    public static int pillIndex(GameController game,CraftingInput input){
        if(!matches(input))return -1;int sum=seed(game);if(sum<1)return -1;
        for(var stack:input.items())if(!LbcPills.certificate(stack)){
            String id=BuiltInRegistries.ITEM.getKey(legacyItem(stack)).toString();
            // Source reads exactly offsets10..36: the Minecraft prefix is not hashed.
            for(int i=10;i<Math.min(id.length(),37);i++){
                char c=id.charAt(i);if(c>='a'&&c<='z')sum+=c-'a'+1;else if(c=='_')sum+=27;
            }
        }
        return Math.floorMod(sum,LbcPills.OUTPUT_COUNT);
    }
    public static ItemStack result(GameController game,CraftingInput input){
        int id=pillIndex(game,input);return id<0?ItemStack.EMPTY:LbcPills.stack(id);
    }
    public static net.minecraft.core.NonNullList<ItemStack> remaining(CraftingInput input){
        var items=net.minecraft.core.NonNullList.withSize(input.size(),ItemStack.EMPTY);
        if(matches(input))for(int i=0;i<input.size();i++)if(LbcPills.certificate(input.getItem(i)))items.set(i,input.getItem(i).copyWithCount(1));
        return items;
    }
    /** Vanilla owns delivery and ingredient debit; this only rechecks a cached output's admission. */
    public static boolean blockTake(AbstractContainerMenu menu,int slot,ServerPlayer player){
        if(slot!=0||!(menu instanceof AbstractCraftingMenu crafting))return false;
        var slots=crafting.getInputGridSlots();
        if(slots.isEmpty()||!(slots.getFirst().container instanceof CraftingContainer grid)||!matches(grid.asCraftInput()))return false;
        var game=me.skywars2.SkyWars2.game();
        boolean allowed=game!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING
                &&player.isAlive()&&!player.isRemoved()&&game.canUseGameplayItems(player)&&!game.isObserver(player)
                &&player.level()==game.maps().level()&&LbcDimensionItems.ordinary(player)
                &&game.items().wands().validRoleTarget(player,false)&&!game.items().actionsBlocked(player)
                &&!game.server().tickRateManager().isFrozen()&&game.server().tickRateManager().runsNormally()
                &&!game.items().timeStop().stopped(player.level())&&!game.items().wands().inputTimeStopped(player)
                &&player.containerMenu==menu&&menu.stillValid(player)&&seed(game)>0;
        if(!allowed)menu.sendAllDataToRemote();return !allowed;
    }
}
