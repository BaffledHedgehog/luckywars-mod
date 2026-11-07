package me.skywars2.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/** Native consume, retained recipe catalyst and carried compass presentation on genuinely mixed TMTs. */
public final class TmtInventoryTraitChecks {
    private TmtInventoryTraitChecks(){}
    private static Scene running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT inventory traits: "+why);}
    static void run(GameController game){require(running==null,"exclusive native trait scene");var scene=new Scene(game);running=scene;
        try{scene.begin();}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    static void tick(GameController game){var scene=running;if(scene==null)return;try{require(scene.game==game,"same controller");if(!scene.step())return;running=null;scene.close();
        SkyWars2.LOGGER.info("[check] PASS tmt-inventory-traits (actual physical split-drop full component/property restoration while producer copies retain context, mixed native consume/reproduction/attribute reward with one food debit, actual result-slot reforge/intact catalyst, native death tracker with procedural model preservation)");
    }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-inventory-traits",error);}}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final HashSet<UUID> initial=new HashSet<>();
        final java.lang.reflect.Field deathField;final Object oldDeath;
        final net.minecraft.stats.Stat<net.minecraft.world.item.Item> foodStat;final int used;
        final net.minecraft.stats.Stat<net.minecraft.world.item.Item> dropStat;final int dropped;
        ItemStack compass;CustomData compassData;net.minecraft.resources.Identifier model;GlobalPos target;int started;boolean closed;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);for(var e:f.level.getAllEntities())initial.add(e.getUUID());
            try{java.lang.reflect.Field found=null;for(Class<?> type=f.owner.getClass();type!=null&&found==null;type=type.getSuperclass())try{found=type.getDeclaredField("lastDeathLocation");}catch(NoSuchFieldException ignored){}
                require(found!=null,"native remembered death field");deathField=found;deathField.setAccessible(true);oldDeath=deathField.get(f.owner);
            }catch(IllegalAccessException error){throw new IllegalStateException(error);}
            foodStat=net.minecraft.stats.Stats.ITEM_USED.get(LbcTmtItems.prototype(4).getItem());used=f.owner.getStats().getValue(foodStat);
            dropStat=net.minecraft.stats.Stats.ITEM_DROPPED.get(LbcTmtItems.prototype(0).getItem());dropped=f.owner.getStats().getValue(dropStat);}
        void death(Object value){try{deathField.set(f.owner,value);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
        LbcTmtAbilities.Ability property(String flag){return game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream())
                .filter(property->((ItemData.IntReader)(Object)property.data()).skywars2$readInt(flag,0)==1).findFirst().orElseThrow();}
        ItemStack mixed(int carrier,int count,String... flags){var stack=LbcTmtItems.prototype(carrier);stack.set(DataComponents.MAX_STACK_SIZE,64);stack.setCount(count);var values=new ArrayList<LbcTmtAbilities.Ability>();for(var flag:flags)values.add(property(flag));
            var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();LbcTmtAbilities.write(data,values);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;}
        void begin(){f.prepareParticipants();started=game.server().getTickCount();game.items().mana().baseRegen(f.owner,0);physicalDrop();consume();craft();
            // The native one-second owner updates a real remembered target but never reaches its column here.
            target=GlobalPos.of(f.level.dimension(),BlockPos.containing(f.owner.position().add(32,0,0)));death(Optional.of(target));
            compass=mixed(0,1,"death_compass","one_punch");compassData=compass.get(DataComponents.CUSTOM_DATA);model=compass.get(DataComponents.ITEM_MODEL);f.owner.getInventory().setItem(9,compass);}
        void physicalDrop(){var ordinary=new ItemStack(Items.STICK);require(LbcTmtAbilities.physicalDrop(ordinary)==ordinary,"ordinary native drop fast path retains the original stack");
            var stack=mixed(0,2,"eat_shit","death_compass");stack.set(DataComponents.POTION_CONTENTS,new net.minecraft.world.item.alchemy.PotionContents(Optional.empty(),Optional.of(123456),List.of(),Optional.empty()));
            var expected=stack.copyWithCount(1);f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var before=new HashSet<UUID>();for(var e:f.level.getAllEntities())before.add(e.getUUID());
            require(LbcTmtAbilities.invoke(stack,property("eat_shit").id(),()->{
                var producer=stack.copy();require(!ItemData.stringValue(producer,"tmt_active_ability","").isEmpty()&&!producer.has(DataComponents.POTION_CONTENTS),"deliberate producer copy retains the selected native context");
                f.owner.drop(false);return true;}),"real partial inventory drop inside a native property scope");
            require(stack.getCount()==1&&ItemStack.matches(stack,expected),"remaining selected stack retains the complete source");int found=0;
            for(var e:f.level.getAllEntities())if(!before.contains(e.getUUID())&&e instanceof ItemEntity item){f.own(item);require(ItemStack.matches(item.getItem(),expected),"physical split drop retains all properties and removes temporary source context");found++;}
            require(found==1,"exactly one native physical split drop");f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);}
        void consume(){var food=mixed(4,2,"eat_shit","attack_damageattribute");f.owner.setItemInHand(InteractionHand.MAIN_HAND,food);f.owner.getFoodData().setFoodLevel(2);f.owner.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10);
            int foodCount=game.items().mana().score(f.owner,"food_count").get();var data=food.get(DataComponents.CUSTOM_DATA);var before=new HashSet<UUID>();for(var e:f.level.getAllEntities())before.add(e.getUUID());
            food.finishUsingItem(f.level,f.owner);
            require(food.getCount()==1&&food.get(DataComponents.CUSTOM_DATA)==data&&LbcTmtAbilities.read(food).size()==2,"one native food debit preserves both property slots");
            require(f.owner.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue()==11&&game.items().mana().score(f.owner,"food_count").get()==foodCount+1,"native attribute reward and one source food count");
            int reproduced=0;for(var e:f.level.getAllEntities())if(!before.contains(e.getUUID())){f.own(e);if(e instanceof ItemEntity item&&item.getItem().is(LbcKfcFood.GOVNO_EATEN))reproduced+=item.getItem().getCount();}
            require(reproduced==1,"real native self-reproduction owner also fires from the mixed source");f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);}
        void craft(){var machine=mixed(0,2,"reroll_machine","hylian");var saved=machine.copy();var donor=new ItemStack(Items.STICK,2);var flags=new net.minecraft.nbt.CompoundTag();flags.putInt("transformable",1);flags.putInt("accessory",1);donor.set(DataComponents.CUSTOM_DATA,CustomData.of(flags));
            var menu=(AbstractCraftingMenu)f.owner.inventoryMenu;var slots=menu.getInputGridSlots();slots.getFirst().setByPlayer(machine);slots.get(1).setByPlayer(donor);
            require(LbcRerollCrafting.isPlaceholder(menu.getResultSlot().getItem()),"native mixed catalyst produces normal reforge preview");
            game.items().mana().maximum(f.owner,100000);game.items().mana().set(f.owner,20000);
            GameChecks.inventoryClick(f.owner,0,0,ContainerInput.PICKUP);
            require(ItemStack.matches(slots.getFirst().getItem(),saved)&&donor.getCount()==1&&game.items().mana().get(f.owner)==15000,"native result take retains full mixed catalyst and spends one donor/5000 mana");
            require(!LbcRerollCrafting.isPlaceholder(menu.getCarried()),"preview token is never delivered as a result");menu.setCarried(ItemStack.EMPTY);for(var slot:slots)slot.setByPlayer(ItemStack.EMPTY);f.owner.getInventory().clearContent();}
        boolean step(){f.client.acknowledge();f.audience.acknowledge();int elapsed=game.server().getTickCount()-started;require(elapsed<50,"bounded native compass cadence");
            var tracker=compass.get(DataComponents.LODESTONE_TRACKER);if(tracker==null)return false;
            require(tracker.target().equals(Optional.of(target))&&!tracker.tracked(),"native carried compass resolves remembered death");
            require(java.util.Objects.equals(compass.get(DataComponents.ITEM_MODEL),model)&&compass.get(DataComponents.CUSTOM_DATA)==compassData&&LbcTmtAbilities.read(compass).size()==2,"native tracker preserves procedural presentation and every private property");return true;}
        public void close(){if(closed)return;closed=true;try{death(oldDeath);for(var e:com.google.common.collect.Lists.newArrayList(f.level.getAllEntities()))if(!initial.contains(e.getUUID()))f.discard(e);f.owner.getStats().setValue(f.owner,foodStat,used);f.owner.getStats().setValue(f.owner,dropStat,dropped);}finally{f.close();}}
    }
}
