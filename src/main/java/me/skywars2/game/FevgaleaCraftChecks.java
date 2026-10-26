package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Exact source leaf and real native two-grid crafting, including imported donor flags. */
public final class FevgaleaCraftChecks {
    private FevgaleaCraftChecks() { }
    private static void require(boolean condition,String detail){if(!condition)throw new IllegalStateException("Fevgalea craft: "+detail);}
    private static Component translation(Component component,String key){
        if(component.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals(key))return component;
        for(var child:component.getSiblings()){var found=translation(child,key);if(found!=null)return found;}return null;
    }
    private static void styled(Component component,String key,int color){
        var found=translation(component,key);require(found!=null&&found.getStyle().getColor()!=null&&found.getStyle().getColor().getValue()==color&&!found.getStyle().isItalic(),"exact styled translation "+key);
    }
    private static Number particleField(Object particle,Class<?> type){
        try{for(Class<?> c=particle.getClass();c!=null;c=c.getSuperclass())for(var field:c.getDeclaredFields())
            if(field.getType()==type&&!java.lang.reflect.Modifier.isStatic(field.getModifiers())){field.setAccessible(true);return (Number)field.get(particle);}}
        catch(ReflectiveOperationException error){throw new IllegalStateException("native dust inspection",error);}throw new IllegalStateException("missing native dust field "+type);
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;
        final BlockPos table;final MapManager.BlockSnapshot beforeTable;
        final int crafted;
        Fixture(GameController game){
            this.game=game;base=new ElementalGrimoireChecks.Fixture(game);base.initialize();
            crafted=base.owner.getStats().getValue(Stats.ITEM_CRAFTED.get(LbcFevgaleaStar.SWORD));
            table=base.owner.blockPosition().offset(0,0,2);require(base.owner.level().hasChunkAt(table),"resident native crafting table");
            beforeTable=game.maps().snapshot(base.owner.level(),table);base.owner.level().setBlock(table,Blocks.CRAFTING_TABLE.defaultBlockState(),2);
        }
        ItemStack loot(String leaf){var list=game.lbc().rollTable(base.owner.level(),base.owner.position(),base.owner,"lbc:ported/"+leaf);require(list.size()==1,"singleton source leaf "+leaf);return list.getFirst();}
        ItemStack flagged(net.minecraft.world.item.Item item,String flag){var stack=new ItemStack(item);var data=new net.minecraft.nbt.CompoundTag();data.putInt(flag,1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;}
        ItemStack sword(int variant){return variant==3?flagged(LbcWands.TINY_TERRA,"true_terra"):variant==2?flagged(Items.CARROT_ON_A_STICK,"true_terra"):loot("true_terra_sword");}
        ItemStack stars(int variant){return variant==3?flagged(Items.STICK,"truefallingstars"):variant==2?flagged(Items.MILK_BUCKET,"truefallingstars"):loot(variant==1?"academy_falling_stars_wand":"falling_stars_wand");}
        void clear(){
            if(base.owner.containerMenu instanceof AbstractCraftingMenu menu){menu.setCarried(ItemStack.EMPTY);menu.getInputGridSlots().forEach(s->s.setByPlayer(ItemStack.EMPTY));}
            base.owner.closeContainer();base.owner.getInventory().clearContent();base.owner.inventoryMenu.getCraftSlots().clearContent();base.recording.packets.clear();
        }
        AbstractCraftingMenu menu(boolean bench){
            clear();if(bench)base.owner.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,p)->new CraftingMenu(id,inv,ContainerLevelAccess.create(p.level(),table)),Component.literal("Fevgalea craft fixture")));
            return (AbstractCraftingMenu)base.owner.containerMenu;
        }
        void presentation(ItemStack stack){
            require(stack.is(LbcFevgaleaStar.SWORD)&&stack.getCount()==1&&stack.getMaxStackSize()==1&&stack.getMaxDamage()==4000&&stack.getDamageValue()==0,"native identity/count/durability/fresh output");
            require(Identifier.parse("lbc:fevgalea_star").equals(stack.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_x").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"exact existing model and tierX");
            styled(stack.getHoverName(),"fevgalea_star",0x55ff55);
            var lore=stack.get(DataComponents.LORE).lines();require(lore.size()==12,"twelve source lore rows");styled(lore.getFirst(),"type",0x55ffff);styled(lore.getFirst(),"sword",0x55ffff);
            for(int i=1;i<=11;i++)styled(lore.get(i),"fevgalea_star.lore."+i,0xffffff);
            var expected=new net.minecraft.nbt.CompoundTag();for(var key:List.of("swordwithability","transformable","sword","magicwand","fevgalea_star"))expected.putInt(key,1);
            require(LbcFartus.data(stack).equals(expected),"exact five source flags without invented wand price/mode/Academy metadata");
            var attributes=stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();require(attributes.size()==2,"exact two authored melee modifiers");
            require(attributes.stream().anyMatch(a->a.attribute().equals(Attributes.ATTACK_DAMAGE)&&a.modifier().id().equals(Identifier.parse("minecraft:base_attack_damage"))&&a.modifier().amount()==16&&a.modifier().operation()==AttributeModifier.Operation.ADD_VALUE&&a.slot()==EquipmentSlotGroup.MAINHAND),"source attack damage16/mainhand/base id");
            require(attributes.stream().anyMatch(a->a.attribute().equals(Attributes.ATTACK_SPEED)&&a.modifier().id().equals(Identifier.parse("minecraft:base_attack_speed"))&&a.modifier().amount()==-2.2&&a.modifier().operation()==AttributeModifier.Operation.ADD_VALUE&&a.slot()==EquipmentSlotGroup.MAINHAND),"source attack speed-2.2/mainhand/base id");
        }
        void feedback(boolean taken){
            var particles=base.recording.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast).toList();
            var sounds=base.recording.packets.stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast).toList();
            require(particles.size()==(taken?1:0)&&sounds.size()==(taken?1:0),"feedback only once on actual native take, taken="+taken);
            if(!taken)return;var p=particles.getFirst();
            require(p.particle() instanceof DustParticleOptions&&p.count()==100&&p.x()==base.owner.getX()&&p.y()==base.owner.getY()+1&&p.z()==base.owner.getZ()&&p.xDist()==1&&p.yDist()==1&&p.zDist()==1&&p.xMaxSpeed()==1&&p.yMaxSpeed()==1&&p.zMaxSpeed()==1&&p.overrideLimiter(),"source dust point/count/spread/speed/force");
            require(particleField(p.particle(),int.class).intValue()==(net.minecraft.util.ARGB.colorFromFloat(1,.129f,.82f,.43932f)&0xffffff)&&particleField(p.particle(),float.class).floatValue()==1,"exact authored craft dust RGB and scale1");
            var sound=sounds.getFirst();require(sound.getSound().value().location().toString().equals("minecraft:block.enchantment_table.use")&&sound.getVolume()==1&&sound.getPitch()==1,"source craft sound volume1/pitch1");
        }
        void pickup(boolean bench,int variant,boolean reverse){
            var menu=menu(bench);var slots=menu.getInputGridSlots();var a=sword(variant).copyWithCount(2);var b=stars(variant).copyWithCount(2);
            a.set(DataComponents.CUSTOM_NAME,Component.literal("participating sword"));b.set(DataComponents.CUSTOM_NAME,Component.literal("participating star"));if(a.isDamageableItem())a.setDamageValue(123);
            var swordSlot=reverse?slots.getLast():slots.getFirst();var starsSlot=reverse?slots.getFirst():slots.getLast();swordSlot.setByPlayer(a);starsSlot.setByPlayer(b);
            var preview=menu.getResultSlot().getItem().copy();presentation(preview);
            var expectedSword=a.copyWithCount(1);var expectedStars=b.copyWithCount(1);int used=base.owner.getStats().getValue(Stats.ITEM_CRAFTED.get(LbcFevgaleaStar.SWORD));
            base.recording.packets.clear();for(int i=0;i<3;i++)menu.slotsChanged(base.owner.getInventory());feedback(false);
            require(swordSlot.getItem().getCount()==2&&starsSlot.getItem().getCount()==2,"repeated preview does not debit donors");
            var input=CraftingInput.of(2,1,List.of(a.copyWithCount(1),b.copyWithCount(1)));
            require(LbcCraftingRecipe.craftingKind(input)==LbcCraftingRecipe.Kind.FEVGALEA&&LbcCraftingRecipe.Kind.FEVGALEA.recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty),"source consumes both donors without an invented milk-bucket remainder");
            GameChecks.inventoryClick(base.owner,0,0,ContainerInput.PICKUP);
            require(menu.getCarried().getCount()==1&&ItemStack.isSameItemSameComponents(menu.getCarried(),preview),"real native ResultSlot transfers exact fresh source output");
            require(swordSlot.getItem().getCount()==1&&starsSlot.getItem().getCount()==1&&ItemStack.isSameItemSameComponents(swordSlot.getItem(),expectedSword)&&ItemStack.isSameItemSameComponents(starsSlot.getItem(),expectedStars),"one donor each consumed; remaining donor names/durability/components untouched");
            require(base.owner.getStats().getValue(Stats.ITEM_CRAFTED.get(LbcFevgaleaStar.SWORD))==used+1,"one actual native crafted stat");feedback(true);clear();
        }
        void invalid(boolean bench){
            var menu=menu(bench);var slots=menu.getInputGridSlots();
            for(var pair:List.of(List.of(sword(0),sword(0)),List.of(stars(0),stars(1)),List.of(loot("terra_sword"),stars(0)),List.of(sword(0),LbcWands.BLAZE_WAND.getDefaultInstance()),List.of(LbcWands.TINY_TERRA.getDefaultInstance(),stars(0)),List.of(new ItemStack(Items.STICK),stars(0)))) {
                slots.forEach(s->s.setByPlayer(ItemStack.EMPTY));slots.getFirst().setByPlayer(pair.getFirst());slots.getLast().setByPlayer(pair.getLast());
                require(!menu.getResultSlot().getItem().is(LbcFevgaleaStar.SWORD),"wrong/base/Tiny/duplicate donors cannot create Fevgalea");
            }
            slots.forEach(s->s.setByPlayer(ItemStack.EMPTY));slots.getFirst().setByPlayer(sword(0));require(!menu.getResultSlot().getItem().is(LbcFevgaleaStar.SWORD),"single donor rejected");
            slots.getLast().setByPlayer(stars(0));slots.get(1).setByPlayer(new ItemStack(Items.DIRT));require(!menu.getResultSlot().getItem().is(LbcFevgaleaStar.SWORD),"third occupied ingredient rejected");clear();
        }
        void shift(boolean bench){
            var menu=menu(bench);var slots=menu.getInputGridSlots();slots.getFirst().setByPlayer(sword(0));slots.getLast().setByPlayer(stars(1));
            for(int i=0;i<36;i++)base.owner.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));base.recording.packets.clear();
            GameChecks.inventoryClick(base.owner,0,0,ContainerInput.QUICK_MOVE);require(menu.getResultSlot().getItem().is(LbcFevgaleaStar.SWORD)&&slots.getFirst().getItem().getCount()==1&&slots.getLast().getItem().getCount()==1,"full inventory shift preserves both exact donor slots");feedback(false);
            base.owner.getInventory().setItem(9,ItemStack.EMPTY);GameChecks.inventoryClick(base.owner,0,0,ContainerInput.QUICK_MOVE);
            int outputs=0;for(int i=0;i<36;i++)if(base.owner.getInventory().getItem(i).is(LbcFevgaleaStar.SWORD)){outputs+=base.owner.getInventory().getItem(i).getCount();presentation(base.owner.getInventory().getItem(i));}
            require(outputs==1&&slots.getFirst().getItem().isEmpty()&&slots.getLast().getItem().isEmpty()&&menu.getCarried().isEmpty(),"native shift moves exactly one output and fully consumes final donors");feedback(true);clear();
        }
        @Override public void close(){
            clear();game.maps().restoreSnapshot(base.owner.level(),table,beforeTable);base.owner.getStats().setValue(base.owner,Stats.ITEM_CRAFTED.get(LbcFevgaleaStar.SWORD),crafted);
            var fresh=new ArrayList<Entity>();for(var entity:base.owner.level().getAllEntities())if(!base.before.contains(entity)&&(entity instanceof net.minecraft.world.entity.item.ItemEntity||entity instanceof net.minecraft.world.entity.ExperienceOrb))fresh.add(entity);fresh.forEach(Entity::discard);base.close();
        }
    }
    public static void run(GameController game){
        try(var fixture=new Fixture(game)){fixture.presentation(fixture.loot("fevgalea_star"));for(boolean bench:new boolean[]{false,true}){for(int variant=0;variant<4;variant++)for(boolean reverse:new boolean[]{false,true})fixture.pickup(bench,variant,reverse);fixture.invalid(bench);fixture.shift(bench);}}
        SkyWars2.LOGGER.info("[check] PASS fevgalea-craft (exact source leaf/attributes/lore, native2x2/3x3 ordinary/Academy/imported flags, pickup/shift/full inventory, exact donor debit/remainders and real-take feedback)");
    }
}
