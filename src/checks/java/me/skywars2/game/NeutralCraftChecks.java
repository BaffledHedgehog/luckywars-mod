package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/** Bounded synchronous checks through the native menu and ResultSlot consumption routes. */
public final class NeutralCraftChecks {
    private static final String ADV="lbc:true_advancements/staffs/neutral";
    private NeutralCraftChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Neutral crafts: "+why);}
    private static Object copy(Object value){if(value instanceof int[] array)return array.clone();if(value instanceof Set<?> set)return new HashSet<>(set);return value;}
    private static Map<Field,Object> fields(Object target){
        var result=new LinkedHashMap<Field,Object>();
        try{for(var field:target.getClass().getFields())if(!Modifier.isStatic(field.getModifiers()))result.put(field,copy(field.get(target)));}
        catch(IllegalAccessException error){throw new IllegalStateException(error);}return result;
    }
    private static void restoreFields(Object target,Map<Field,Object> values){
        try{for(var entry:values.entrySet())entry.getKey().set(target,copy(entry.getValue()));}
        catch(IllegalAccessException error){throw new IllegalStateException(error);}
    }
    private static final class Saved {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState state;final PlayerStore.Profile profile;
        final Map<Field,Object> profileFields;final boolean shift;
        final Map<AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<Stat<Identifier>,Integer> custom=new LinkedHashMap<>();
        final Map<Stat<Item>,Integer> crafted=new LinkedHashMap<>();
        Saved(GameController game,ServerPlayer player){
            this.player=player;state=new GameChecks.WandThirdPlayerState(player,game.items().mana());shift=player.isShiftKeyDown();
            profile=game.players().get(player.getUUID());profileFields=fields(profile);
            for(var advancement:game.server().getAdvancements().getAllAdvancements())advancements.put(advancement,criteria(player,advancement));
            for(var id:BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);custom.put(stat,player.getStats().getValue(stat));}
            for(var item:BuiltInRegistries.ITEM){var stat=Stats.ITEM_CRAFTED.get(item);crafted.put(stat,player.getStats().getValue(stat));}
        }
        void restore(GameController game){
            // Regrant first: source rewards may write inventory, XP, currencies and custom stats.
            advancements.forEach((adv,initial)->{for(var key:adv.value().criteria().keySet())if(initial.contains(key))player.getAdvancements().award(adv,key);else player.getAdvancements().revoke(adv,key);});
            restoreFields(profile,profileFields);state.restore(game);player.setShiftKeyDown(shift);
            custom.forEach((stat,value)->player.getStats().setValue(player,stat,value));crafted.forEach((stat,value)->player.getStats().setValue(player,stat,value));
            game.items().magic().inventoryChanged(player);
        }
    }
    private record Case(String donor,Item result,String criterion,LbcCraftingRecipe.Kind kind){}
    private static Set<String> criteria(ServerPlayer player,AdvancementHolder advancement){
        var result=new HashSet<String>();player.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(result::add);return result;
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,other;final Saved saved,otherSaved;
        final AdvancementHolder advancement;final Map<Field,Object> config;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();final BlockPos table;
        final int admission;
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.playing()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two active participants");
            require(owner.containerMenu==owner.inventoryMenu&&other.containerMenu==other.inventoryMenu,"initial player inventory menus");
            require(!game.items().timeStop().stopped(owner.level())&&!game.items().frost().isFrozen(owner)&&game.items().frost().cold(owner)==0,"clean initial passive state");
            advancement=game.server().getAdvancements().get(Identifier.parse(ADV));require(advancement!=null&&advancement.value().criteria().keySet().equals(Set.of("fire","stone","void","ice")),"source four-criterion advancement");
            saved=new Saved(game,owner);otherSaved=new Saved(game,other);config=fields(game.config());owner.level().getAllEntities().forEach(before::add);
            admission=game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("neutral_ball_mana_activate"),"lbc.math").get();
            table=owner.blockPosition().offset(0,6,0);
        }
        ItemStack loot(String leaf){var rolls=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/"+leaf);require(rolls.size()==1&&rolls.getFirst().getCount()==1,"native single loot "+leaf);return rolls.getFirst();}
        void init(){
            require(owner.level().hasChunkAt(table),"resident table fixture");
            blocks.put(table,game.maps().snapshot(owner.level(),table));owner.level().setBlock(table,Blocks.CRAFTING_TABLE.defaultBlockState(),2);
            var at=Vec3.atBottomCenterOf(table);
            for(var player:List.of(owner,other)){
                player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);player.setShiftKeyDown(false);player.removeAllEffects();
                player.getInventory().clearContent();player.inventoryMenu.getCraftSlots().clearContent();player.containerMenu.setCarried(ItemStack.EMPTY);player.getInventory().setSelectedSlot(0);
                game.items().mana().noMagic(player,0);game.players().get(player.getUUID()).lbcSkill=0;
            }
            // Beta is closer to the table: source @p would select the wrong author here.
            owner.teleportTo(owner.level(),at.x+2,at.y,at.z,Set.of(),0,0,true);other.teleportTo(other.level(),at.x+.1,at.y,at.z,Set.of(),0,0,true);
            game.config().lbcSkills=false;game.items().magic().inventoryChanged(owner);game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();
            game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("neutral_ball_mana_activate"),"lbc.math").set(5000);
        }
        void resetCriteria(){for(var player:List.of(owner,other)){for(var key:advancement.value().criteria().keySet())player.getAdvancements().revoke(advancement,key);game.players().get(player.getUUID()).lbcUnlocks.remove(ADV);}}
        AbstractCraftingMenu menu(boolean bench){
            clear();if(bench)owner.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,pl)->new CraftingMenu(id,inv,ContainerLevelAccess.create(owner.level(),table)),net.minecraft.network.chat.Component.literal("neutral craft fixture")));
            return (AbstractCraftingMenu)owner.containerMenu;
        }
        void clear(){
            if(owner.containerMenu instanceof AbstractCraftingMenu menu){menu.setCarried(ItemStack.EMPTY);menu.getInputGridSlots().forEach(slot->slot.setByPlayer(ItemStack.EMPTY));}
            owner.closeContainer();owner.getInventory().clearContent();owner.inventoryMenu.getCraftSlots().clearContent();
        }
        void inputs(AbstractCraftingMenu menu,Case test,boolean academy,int count){
            var slots=menu.getInputGridSlots();slots.getFirst().setByPlayer(loot(academy?"academy_neutral_wand":"neutral_wand").copyWithCount(count));slots.getLast().setByPlayer(loot(test.donor).copyWithCount(count));
        }
        int balls(AbstractCraftingMenu menu){
            int total=0;for(int i=0;i<owner.getInventory().getContainerSize();i++){var stack=owner.getInventory().getItem(i);if(LbcIceMaterials.isNeutralBall(stack))total+=stack.getCount();}
            for(var slot:menu.getInputGridSlots())if(LbcIceMaterials.isNeutralBall(slot.getItem()))total+=slot.getItem().getCount();
            return total;
        }
        int inventory(Item item){int count=0;for(int i=0;i<owner.getInventory().getContainerSize();i++){var stack=owner.getInventory().getItem(i);if(stack.is(item))count+=stack.getCount();}return count;}
        int drops(){int count=0;for(var entity:owner.level().getAllEntities())if(entity instanceof net.minecraft.world.entity.item.ItemEntity||entity instanceof net.minecraft.world.entity.ExperienceOrb)count++;return count;}
        void authored(Case test,Set<String> expected){
            require(criteria(owner,advancement).equals(expected),test.criterion+" exact real-crafter criteria "+criteria(owner,advancement));
            require(criteria(other,advancement).isEmpty()&&!game.players().get(other.getUUID()).lbcUnlocks.contains(ADV),"nearby Beta criteria/unlock untouched");
        }
        void pickup(Case test,boolean bench,boolean academy){
            resetCriteria();var menu=menu(bench);int dropsBefore=drops();inputs(menu,test,academy,2);var slots=menu.getInputGridSlots();
            require(menu.getResultSlot().getItem().is(test.result)&&menu.getResultSlot().getItem().getCount()==1,"ordinary output preview "+test.criterion);
            for(int i=0;i<3;i++)menu.slotsChanged(owner.getInventory());
            require(criteria(owner,advancement).isEmpty()&&criteria(other,advancement).isEmpty()&&balls(menu)==0&&drops()==dropsBefore,"preview has no advancement, Neutral Ball or drops");
            slots.get(1).setByPlayer(new ItemStack(Items.STICK));require(menu.getResultSlot().getItem().isEmpty(),"extra ingredient rejected");slots.get(1).setByPlayer(ItemStack.EMPTY);
            var donor=slots.getLast().getItem().copy();slots.get(1).setByPlayer(donor.copy());require(menu.getResultSlot().getItem().isEmpty(),"duplicate material rejected");slots.get(1).setByPlayer(ItemStack.EMPTY);
            var neutral=slots.getFirst().getItem().copy();slots.getLast().setByPlayer(neutral.copy());require(menu.getResultSlot().getItem().isEmpty(),"two Neutral Wands rejected");
            slots.getLast().setByPlayer(new ItemStack(Items.DIRT));require(menu.getResultSlot().getItem().isEmpty(),"wrong material rejected");slots.getLast().setByPlayer(donor);
            slots.getFirst().setByPlayer(donor.copy());require(menu.getResultSlot().getItem().isEmpty(),"two materials without Neutral Wand rejected");slots.getFirst().setByPlayer(neutral);
            var input=CraftingInput.of(2,1,List.of(neutral.copyWithCount(1),donor.copyWithCount(1)));
            var remainder=test.kind.recipe.getRemainingItems(input);
            require(remainder.size()==2&&LbcIceMaterials.isNeutralBall(remainder.getFirst())&&remainder.getFirst().getCount()==1&&remainder.getLast().isEmpty(),"native remainder belongs only to Neutral donor slot");
            var reversed=test.kind.recipe.getRemainingItems(CraftingInput.of(2,1,List.of(donor.copyWithCount(1),neutral.copyWithCount(1))));
            require(reversed.getFirst().isEmpty()&&LbcIceMaterials.isNeutralBall(reversed.getLast())&&reversed.getLast().getCount()==1,"reversed donors put remainder in actual Neutral slot");
            require(balls(menu)==0&&criteria(owner,advancement).isEmpty(),"remainder calculation does not inject items/criteria");
            GameChecks.inventoryClick(owner,0,0,ContainerInput.PICKUP);
            require(menu.getCarried().is(test.result)&&menu.getCarried().getCount()==1&&slots.getFirst().getItem().getCount()==1&&slots.getLast().getItem().getCount()==1,"native pickup debits one each from stacked ingredients "+bench+"/"+academy);
            require(balls(menu)==1,"exactly one real-take Neutral Ball");authored(test,Set.of(test.criterion));
            require(!owner.getAdvancements().getOrStartProgress(advancement).isDone()&&!game.players().get(owner.getUUID()).lbcUnlocks.contains(ADV),"one criterion leaves Neutral locked");
            clear();
        }
        void shift(Case test,boolean bench){
            resetCriteria();var menu=menu(bench);inputs(menu,test,true,1);
            var slots=menu.getInputGridSlots();var neutralSlot=bench?slots.getLast():slots.getFirst();var materialSlot=bench?slots.getFirst():slots.getLast();
            if(bench){var neutral=slots.getFirst().getItem().copy();var material=slots.getLast().getItem().copy();slots.getFirst().setByPlayer(material);slots.getLast().setByPlayer(neutral);}
            // Every ordinary inventory slot is occupied, so vanilla cannot move the result.
            for(int i=0;i<36;i++)owner.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
            GameChecks.inventoryClick(owner,0,0,ContainerInput.QUICK_MOVE);
            require(menu.getResultSlot().getItem().is(test.result)&&neutralSlot.getItem().is(LbcWands.ACADEMY_NEUTRAL)&&materialSlot.getItem().getCount()==1,"full inventory shift leaves both donors/result");
            require(criteria(owner,advancement).isEmpty()&&balls(menu)==0,"failed shift awards nothing");authored(test,Set.of());
            owner.getInventory().setItem(9,ItemStack.EMPTY);owner.getInventory().setItem(10,ItemStack.EMPTY);
            GameChecks.inventoryClick(owner,0,0,ContainerInput.QUICK_MOVE);
            require(inventory(test.result)==1&&menu.getCarried().isEmpty()&&materialSlot.getItem().isEmpty(),"native shift moves one ordinary output and consumes material");
            require(LbcIceMaterials.isNeutralBall(neutralSlot.getItem())&&balls(menu)==1,"single Neutral donor replaced in its own native grid slot");authored(test,Set.of(test.criterion));
            var ball=neutralSlot.getItem().copy();clear();functional(ball);
        }
        void functional(ItemStack ball){
            require(ball.is(LbcIceMaterials.NEUTRAL_BALL)&&Identifier.parse("lbc:neutral_ball").equals(ball.get(DataComponents.ITEM_MODEL))&&ball.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("neutral_ball",0)==1,"ported functional Neutral Ball identity/model/flag");
            var mana=game.items().mana();owner.setItemInHand(InteractionHand.MAIN_HAND,ball);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);mana.set(owner,5000);mana.baseRegen(owner,10);
            game.items().iceMaterials().tickPlayer(owner);require(mana.get(owner)==0&&mana.baseRegen(owner)==11,"crafted ball performs native mainhand exchange");
            owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ball);game.items().iceMaterials().tickPlayer(owner);
            require(mana.get(owner)==5000&&mana.baseRegen(owner)==10,"crafted ball performs native offhand exchange");owner.getInventory().clearContent();
        }
        void complete(List<Case> tests){
            resetCriteria();var completed=new HashSet<String>();
            var all=new ArrayList<>(tests);all.add(new Case("runic_ice_shard",LbcWands.ICE,"ice",LbcCraftingRecipe.Kind.ICE));
            for(var test:all){
                var menu=menu(true);inputs(menu,test,false,1);require(menu.getResultSlot().getItem().is(test.result),"fourth-family native preview "+test.criterion);
                require(criteria(owner,advancement).equals(completed),"preview preserves accumulated partial criteria");GameChecks.inventoryClick(owner,0,0,ContainerInput.PICKUP);
                completed.add(test.criterion);authored(test,completed);require(balls(menu)==1,"completion craft exact Neutral Ball");
                boolean full=completed.size()==4;require(owner.getAdvancements().getOrStartProgress(advancement).isDone()==full,"native Neutral advancement only completes after all four actual crafts");
                require(!game.players().get(owner.getUUID()).lbcUnlocks.contains(ADV),"completion uses native criteria without an administrative profile grant");clear();
            }
        }
        @Override public void close(){
            clear();restoreFields(game.config(),config);
            for(var entry:blocks.entrySet())game.maps().restoreSnapshot(owner.level(),entry.getKey(),entry.getValue());
            saved.restore(game);otherSaved.restore(game);
            game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("neutral_ball_mana_activate"),"lbc.math").set(admission);
            // Includes rewards/drops produced during advancement restoration itself.
            for(var entity:owner.level().getAllEntities())if(!before.contains(entity)&&(entity instanceof net.minecraft.world.entity.item.ItemEntity||entity instanceof net.minecraft.world.entity.ExperienceOrb))entity.discard();
            game.players().save();game.items().magic().flushCleansing();
        }
    }
    public static void run(GameController game){
        var tests=List.of(new Case("kiuaskivi",LbcWands.FIRE_WAND,"fire",LbcCraftingRecipe.Kind.NEUTRAL_FIRE),new Case("tannerkivi",LbcWands.STONE_WAND,"stone",LbcCraftingRecipe.Kind.NEUTRAL_STONE),new Case("void_arcanum",LbcWands.RUBY_WAND,"void",LbcCraftingRecipe.Kind.NEUTRAL_RUBY));
        try(var fixture=new Fixture(game)){
            fixture.init();for(var test:tests)for(boolean bench:new boolean[]{false,true}){for(boolean academy:new boolean[]{false,true})fixture.pickup(test,bench,academy);fixture.shift(test,bench);}fixture.complete(tests);
        }
        SkyWars2.LOGGER.info("[check] PASS neutral-crafts (native 2x2/3x3 pickup/shift/full inventory, remainder, actual author and four-criterion unlock)");
    }
}
