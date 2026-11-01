package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.*;
import java.util.*;

/** Native item-component preservation and once-only mutations, independent of reward randomness. */
public final class InventorySkillChecks {
    private InventorySkillChecks(){}
    private static final EquipmentSlot[] ARMOR={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final Item[] PIECES={Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS};
    private static final List<ResourceKey<Enchantment>> TEN=List.of(Enchantments.BLAST_PROTECTION,Enchantments.FEATHER_FALLING,Enchantments.FIRE_PROTECTION,Enchantments.PROJECTILE_PROTECTION,Enchantments.PROTECTION,Enchantments.RESPIRATION,Enchantments.SOUL_SPEED,Enchantments.SWIFT_SNEAK,Enchantments.THORNS,Enchantments.UNBREAKING);
    private static void require(boolean ok,String text){if(!ok)throw new IllegalStateException("Inventory skills: "+text);}
    private static void near(double actual,double expected,String text){require(Math.abs(actual-expected)<1e-6,text+" actual="+actual+" expected="+expected);}
    private static Holder<Enchantment> enchant(ServerPlayer owner,ResourceKey<Enchantment> key){return owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);}
    private static ItemAttributeModifiers attributes(ItemStack stack){return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);}
    private static ItemStack decorated(ServerPlayer owner,Item item){
        var stack=new ItemStack(item);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Native skill fixture").withStyle(net.minecraft.ChatFormatting.AQUA));stack.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("preserve authored lore"))));stack.set(DataComponents.REPAIR_COST,17);
        if(stack.has(DataComponents.DAMAGE))stack.setDamageValue(7);
        var data=new net.minecraft.nbt.CompoundTag();data.putString("skill_fixture","preserve");var nested=new net.minecraft.nbt.CompoundTag();nested.putInt("neighbor",83);data.put("nested",nested);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        var ench=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);ench.set(enchant(owner,Enchantments.SHARPNESS),7);ench.set(enchant(owner,Enchantments.MENDING),9);stack.set(DataComponents.ENCHANTMENTS,ench.toImmutable());return stack;
    }
    private static void preserved(ItemStack before,ItemStack after,Set<DataComponentType<?>> mutable){
        require(before.getItem()==after.getItem()&&before.getCount()==after.getCount(),"native stack identity/count retained");
        for(var component:before.getComponents())if(!mutable.contains(component.type()))require(Objects.equals(component.value(),after.get(component.type())),"unrelated component retained: "+component.type());
        var data=after.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();require(data.getStringOr("skill_fixture","").equals("preserve")&&data.getCompoundOrEmpty("nested").getIntOr("neighbor",0)==83,"nested source custom data survives guards");
    }
    private static void equipment(ServerPlayer player){try{var method=LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");method.setAccessible(true);method.invoke(player);}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}}
    public static void run(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(owner!=null&&game.canUseGameplayItems(owner),"actual active participant");
        var snapshot=new RegenerationBeltChecks.Snapshot(game,owner);boolean skills=game.config().lbcSkills,ranked=game.config().ranked;
        try{
            owner.closeContainer();owner.getInventory().clearContent();owner.removeAllEffects();game.players().get(owner.getUUID()).lbcSkill=0;equipment(owner);
            owner.getAttribute(Attributes.ARMOR_TOUGHNESS).removeModifiers();
            armor(owner);enchantArmor(owner);hands(owner);inventoryEvents(game,owner);parkedProtection(game,owner);selectionAndPause(game,owner);
            SkyWars2.LOGGER.info("[check] PASS inventory-skills");
        }finally{game.config().lbcSkills=skills;game.config().ranked=ranked;snapshot.restore(game);snapshot.restoreAttributes();snapshot.restoreUse();game.items().armorSets().inventoryChanged(owner);game.players().save();}
    }
    private static void clearArmor(ServerPlayer owner){for(var slot:ARMOR)owner.setItemSlot(slot,ItemStack.EMPTY);}
    private static void armor(ServerPlayer owner){
        for(int selected=0;selected<ARMOR.length;selected++){
            clearArmor(owner);var original=decorated(owner,PIECES[selected]);var source=attributes(original);owner.setItemSlot(ARMOR[selected],original.copy());
            require(LbcInventorySkills.apply(owner,4),"one independently worn armor slot receives skill4: "+ARMOR[selected]);
            var changed=owner.getItemBySlot(ARMOR[selected]);preserved(original,changed,Set.of(DataComponents.ATTRIBUTE_MODIFIERS,DataComponents.CUSTOM_DATA,DataComponents.UNBREAKABLE));
            require(changed.has(DataComponents.UNBREAKABLE),"existing durability-bearing armor becomes unbreakable");require(attributes(changed).modifiers().containsAll(source.modifiers()),"implicit vanilla armor/knockback attributes preserved");
            var added=new ArrayList<>(attributes(changed).modifiers());for(var entry:source.modifiers())require(added.remove(entry),"original native attribute entry preserved");
            require(added.size()==1&&added.getFirst().attribute().equals(Attributes.ARMOR_TOUGHNESS)&&added.getFirst().modifier().operation()==AttributeModifier.Operation.ADD_VALUE&&added.getFirst().slot().test(ARMOR[selected]),"only one slot-correct native toughness modifier is added");near(added.getFirst().modifier().amount(),3,"exact +3 armor toughness");
            var once=changed.copy();for(int i=0;i<5;i++)require(!LbcInventorySkills.apply(owner,4),"each armor slot has its own stable once-only guard");require(ItemStack.matches(once,owner.getItemBySlot(ARMOR[selected])),"repeated armor processing is byte-equivalent");
            equipment(owner);require(owner.getAttribute(Attributes.ARMOR_TOUGHNESS).getModifier(added.getFirst().modifier().id())!=null,"actual native equipment update consumes +3 modifier");
        }
        clearArmor(owner);var ids=new HashSet<Identifier>();
        for(int i=0;i<ARMOR.length;i++)owner.setItemSlot(ARMOR[i],decorated(owner,PIECES[i]));require(LbcInventorySkills.apply(owner,4),"all four worn pieces upgrade together");
        for(int i=0;i<ARMOR.length;i++){var stack=owner.getItemBySlot(ARMOR[i]);for(var entry:attributes(stack).modifiers())if(!attributes(new ItemStack(PIECES[i])).modifiers().contains(entry)&&entry.attribute().equals(Attributes.ARMOR_TOUGHNESS))require(ids.add(entry.modifier().id()),"four equipped bonuses use noncolliding IDs");}
        require(ids.size()==4,"four independent armor bonuses");
        equipment(owner);double expected=owner.getAttribute(Attributes.ARMOR_TOUGHNESS).getBaseValue()+12;for(var item:PIECES)for(var entry:attributes(new ItemStack(item)).modifiers())if(entry.attribute().equals(Attributes.ARMOR_TOUGHNESS)&&entry.modifier().operation()==AttributeModifier.Operation.ADD_VALUE)expected+=entry.modifier().amount();near(owner.getAttributeValue(Attributes.ARMOR_TOUGHNESS),expected,"native four-piece sum includes ordinary toughness plus12");
        clearArmor(owner);var custom=decorated(owner,Items.LEATHER_CHESTPLATE);var entries=new ArrayList<>(attributes(custom).modifiers());entries.add(new ItemAttributeModifiers.Entry(Attributes.ARMOR_TOUGHNESS,new AttributeModifier(Identifier.parse("skywars2:inventory_neighbor"),-.5,AttributeModifier.Operation.ADD_MULTIPLIED_BASE),EquipmentSlotGroup.CHEST,ItemAttributeModifiers.Display.override(Component.literal("preserve modifier presentation"))));custom.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(entries));owner.setItemSlot(EquipmentSlot.CHEST,custom.copy());require(LbcInventorySkills.apply(owner,4),"custom armor receives native upgrade");require(attributes(owner.getItemBySlot(EquipmentSlot.CHEST)).modifiers().containsAll(entries),"custom operations and display metadata survive");preserved(custom,owner.getItemBySlot(EquipmentSlot.CHEST),Set.of(DataComponents.ATTRIBUTE_MODIFIERS,DataComponents.CUSTOM_DATA,DataComponents.UNBREAKABLE));
    }
    private static void enchantArmor(ServerPlayer owner){
        for(int seed:new int[]{0,300,Integer.MAX_VALUE-1,Integer.MAX_VALUE}){
            clearArmor(owner);var before=new ArrayList<ItemStack>();
            for(int i=0;i<ARMOR.length;i++){var stack=decorated(owner,PIECES[i]);var mutable=new ItemEnchantments.Mutable(stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY));mutable.set(enchant(owner,Enchantments.PROTECTION),seed);stack.set(DataComponents.ENCHANTMENTS,mutable.toImmutable());before.add(stack.copy());owner.setItemSlot(ARMOR[i],stack);}
            require(LbcInventorySkills.apply(owner,8),"source enchantment augmentation applies to all worn slots");
            for(int i=0;i<ARMOR.length;i++){var after=owner.getItemBySlot(ARMOR[i]);var component=after.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);for(var key:TEN){int initial=before.get(i).getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).getLevel(enchant(owner,key));int expected=(int)Math.min(Integer.MAX_VALUE,(long)initial+1);require(component.getLevel(enchant(owner,key))==expected,"source +1 includes absent/incompatible enchantments without overflow: "+key+" seed="+seed);}
                require(component.getLevel(enchant(owner,Enchantments.SHARPNESS))==7&&component.getLevel(enchant(owner,Enchantments.MENDING))==9,"unrelated enchantments are retained");preserved(before.get(i),after,Set.of(DataComponents.ENCHANTMENTS,DataComponents.CUSTOM_DATA));before.set(i,after.copy());}
            require(!LbcInventorySkills.apply(owner,8),"armor enchantment guards prevent repeated increment");for(int i=0;i<ARMOR.length;i++)require(ItemStack.matches(before.get(i),owner.getItemBySlot(ARMOR[i])),"guarded enchantment component remains unchanged");
        }
    }
    private static double handAmount(ItemAttributeModifiers.Entry source,EquipmentSlotGroup slot){return slot==EquipmentSlotGroup.OFFHAND&&source.attribute().equals(Attributes.ATTACK_SPEED)&&source.modifier().amount()<0?source.modifier().amount()*.3:source.modifier().amount();}
    private static boolean equivalent(ItemAttributeModifiers.Entry actual,ItemAttributeModifiers.Entry source,EquipmentSlotGroup slot){return actual.attribute().equals(source.attribute())&&actual.slot()==slot&&actual.modifier().operation()==source.modifier().operation()&&actual.modifier().amount()==handAmount(source,slot)&&actual.display().equals(source.display());}
    private static List<ItemAttributeModifiers.Entry> handEntries(ItemStack before,ItemStack after){
        var original=attributes(before).modifiers();var remaining=new ArrayList<>(attributes(after).modifiers());var copies=new ArrayList<ItemAttributeModifiers.Entry>();
        var originalIds=new HashSet<Identifier>();for(var entry:original)originalIds.add(entry.modifier().id());var copiedIds=new HashSet<Identifier>();
        for(var source:original){
            if((source.slot()==EquipmentSlotGroup.HAND||source.slot()==EquipmentSlotGroup.ANY)&&source.attribute().equals(Attributes.ATTACK_SPEED)&&source.modifier().amount()<0){
                var split=new LinkedHashSet<ItemAttributeModifiers.Entry>();
                for(var slot:EquipmentSlot.values())if(source.slot().test(slot)){
                    var part=remaining.stream().filter(e->e.slot().test(slot)&&e.attribute().equals(source.attribute())&&e.display().equals(source.display())&&e.modifier().operation()==source.modifier().operation()&&e.modifier().amount()==(slot==EquipmentSlot.OFFHAND?source.modifier().amount()*.3:source.modifier().amount())&&(slot==EquipmentSlot.OFFHAND?!originalIds.contains(e.modifier().id()):e.modifier().id().equals(source.modifier().id()))).findFirst().orElseThrow(()->new IllegalStateException("Inventory skills: broad left-speed slot coverage lost: "+slot));
                    split.add(part);if(slot==EquipmentSlot.OFFHAND){require(copiedIds.add(part.modifier().id()),"split left penalty ID is fresh and noncolliding");copies.add(part);}
                }
                for(var part:split)for(var slot:EquipmentSlot.values())require(!part.slot().test(slot)||source.slot().test(slot),"split penalty never widens original native slot coverage");remaining.removeAll(split);continue;
            }
            var preserved=remaining.stream().filter(e->e.modifier().id().equals(source.modifier().id())&&equivalent(e,source,source.slot())).findFirst().orElseThrow(()->new IllegalStateException("Inventory skills: original entry/display lost: "+source));remaining.remove(preserved);
        }
        for(var source:original)if(source.slot()==EquipmentSlotGroup.MAINHAND||source.slot()==EquipmentSlotGroup.OFFHAND){
            var opposite=source.slot()==EquipmentSlotGroup.MAINHAND?EquipmentSlotGroup.OFFHAND:EquipmentSlotGroup.MAINHAND;
            var copy=remaining.stream().filter(e->equivalent(e,source,opposite)).findFirst().orElseThrow(()->new IllegalStateException("Inventory skills: opposite-hand entry missing: "+source));
            require(!originalIds.contains(copy.modifier().id())&&copiedIds.add(copy.modifier().id()),"derived hand-copy IDs cannot collide with originals or other copies");remaining.remove(copy);copies.add(copy);
        }
        require(remaining.isEmpty(),"only negative broad left-speed coverage splits; other HAND/ANY/armor entries stay exact");return copies;
    }
    private static void hands(ServerPlayer owner){
        clearArmor(owner);
        for(var item:List.of(Items.DIAMOND_SWORD,Items.TRIDENT,Items.IRON_AXE)){
            var original=decorated(owner,item);owner.setItemSlot(EquipmentSlot.MAINHAND,original.copy());owner.setItemSlot(EquipmentSlot.OFFHAND,original.copy());
            require(LbcInventorySkills.apply(owner,11),"both held native/default items are converted: "+item);
            for(var slot:List.of(EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND)){var after=owner.getItemBySlot(slot);handEntries(original,after);preserved(original,after,Set.of(DataComponents.ATTRIBUTE_MODIFIERS,DataComponents.CUSTOM_DATA));}
            var right=owner.getMainHandItem().copy();var left=owner.getOffhandItem().copy();require(!LbcInventorySkills.apply(owner,11)&&ItemStack.matches(right,owner.getMainHandItem())&&ItemStack.matches(left,owner.getOffhandItem()),"repeated hand conversion never weakens speed twice or duplicates entries");
        }
        // More than the source's old seven entries, with every operation and both original hands.
        var custom=decorated(owner,Items.DIAMOND_SWORD);var entries=new ArrayList<ItemAttributeModifiers.Entry>();var operations=AttributeModifier.Operation.values();var slots=new EquipmentSlotGroup[]{EquipmentSlotGroup.MAINHAND,EquipmentSlotGroup.OFFHAND,EquipmentSlotGroup.HAND,EquipmentSlotGroup.ANY,EquipmentSlotGroup.HEAD};
        for(int i=0;i<31;i++){double value=i%3==0?-.2*(i+1):i%3==1?0:.05*(i+1);entries.add(new ItemAttributeModifiers.Entry(i%2==0?Attributes.ATTACK_SPEED:Attributes.ATTACK_DAMAGE,new AttributeModifier(Identifier.parse("skywars2:hand_source_"+i),value,operations[i%operations.length]),slots[i%slots.length],i%3==0?ItemAttributeModifiers.Display.hidden():i%3==1?ItemAttributeModifiers.Display.override(Component.literal("authored entry "+i)):ItemAttributeModifiers.Display.attributeModifiers()));}
        for(int i=0;i<operations.length;i++)for(int kind=0;kind<4;kind++){double amount=kind==2?0:kind==3?.15:-.35-i*.1;var slot=kind==0?EquipmentSlotGroup.MAINHAND:EquipmentSlotGroup.OFFHAND;entries.add(new ItemAttributeModifiers.Entry(Attributes.ATTACK_SPEED,new AttributeModifier(Identifier.parse("skywars2:hand_operation_"+i+"_"+kind),amount,operations[i]),slot,ItemAttributeModifiers.Display.override(Component.literal("all-operation left coefficient"))));}
        custom.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(entries));owner.setItemSlot(EquipmentSlot.MAINHAND,custom.copy());owner.setItemSlot(EquipmentSlot.OFFHAND,custom.copy());require(LbcInventorySkills.apply(owner,11),"arbitrary custom modifier list converts in both held slots");var copies=handEntries(custom,owner.getOffhandItem());handEntries(custom,owner.getMainHandItem());preserved(custom,owner.getOffhandItem(),Set.of(DataComponents.ATTRIBUTE_MODIFIERS,DataComponents.CUSTOM_DATA));
        var converted=owner.getOffhandItem().copy();require(!LbcInventorySkills.apply(owner,11)&&ItemStack.matches(converted,owner.getOffhandItem()),"custom all-operation speed conversion is idempotent");
        equipment(owner);for(var entry:copies){var nativeModifier=owner.getAttribute(entry.attribute()).getModifier(entry.modifier().id());require(nativeModifier!=null&&nativeModifier.equals(entry.modifier()),"actual native equipment engine consumes every copied operation from the active hand");}
        // Forge a legitimate preexisting entry at a derived ID discovered from a real conversion.
        var collision=decorated(owner,Items.DIAMOND_SWORD);var source=entries.stream().filter(e->e.slot()==EquipmentSlotGroup.MAINHAND).findFirst().orElseThrow();var derived=copies.stream().filter(e->equivalent(e,source,EquipmentSlotGroup.OFFHAND)).findFirst().orElseThrow();var occupied=new ItemAttributeModifiers.Entry(source.attribute(),new AttributeModifier(derived.modifier().id(),3.75,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.OFFHAND,ItemAttributeModifiers.Display.hidden());collision.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(List.of(source,occupied)));owner.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);owner.setItemSlot(EquipmentSlot.OFFHAND,collision.copy());require(LbcInventorySkills.apply(owner,11),"derived-ID collision admits a distinct safe copy");handEntries(collision,owner.getOffhandItem());
        broadLeftSpeed(owner);nativeHandSum(owner);
    }
    private static void broadLeftSpeed(ServerPlayer owner){
        for(var group:List.of(EquipmentSlotGroup.HAND,EquipmentSlotGroup.ANY))for(var operation:AttributeModifier.Operation.values()){
            var original=decorated(owner,Items.DIAMOND_SWORD);var modifier=new AttributeModifier(Identifier.parse("skywars2:broad_speed_"+group.getSerializedName()+"_"+operation.getSerializedName()),-.625,operation);var source=new ItemAttributeModifiers.Entry(Attributes.ATTACK_SPEED,modifier,group,ItemAttributeModifiers.Display.override(Component.literal("broad speed display")));original.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(List.of(source)));owner.setItemSlot(EquipmentSlot.MAINHAND,original.copy());owner.setItemSlot(EquipmentSlot.OFFHAND,original.copy());require(LbcInventorySkills.apply(owner,11),"sole negative HAND/ANY speed entry qualifies for left-penalty conversion");
            for(var held:List.of(owner.getMainHandItem(),owner.getOffhandItem())){handEntries(original,held);preserved(original,held,Set.of(DataComponents.ATTRIBUTE_MODIFIERS,DataComponents.CUSTOM_DATA));for(var slot:EquipmentSlot.values()){
                var effective=new ArrayList<AttributeModifier>();attributes(held).forEach(slot,(attribute,value)->{if(attribute.equals(Attributes.ATTACK_SPEED))effective.add(value);});require(effective.size()==(group.test(slot)?1:0),"native forEach retains exact original slot cardinality: "+group+"/"+slot);
                if(group.test(slot)){var value=effective.getFirst();near(value.amount(),slot==EquipmentSlot.OFFHAND?modifier.amount()*.3:modifier.amount(),"native broad speed only weakens OFFHAND for every operation");require(value.operation()==operation&&(slot==EquipmentSlot.OFFHAND?!value.id().equals(modifier.id()):value.id().equals(modifier.id())),"non-left original IDs and operations remain exact");}
            }}
            var main=owner.getMainHandItem().copy();var off=owner.getOffhandItem().copy();require(!LbcInventorySkills.apply(owner,11)&&ItemStack.matches(main,owner.getMainHandItem())&&ItemStack.matches(off,owner.getOffhandItem()),"broad negative hand-speed split is idempotent");
        }
    }
    private static void nativeHandSum(ServerPlayer owner){
        owner.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);owner.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);equipment(owner);var speed=owner.getAttribute(Attributes.ATTACK_SPEED);speed.removeModifiers();speed.setBaseValue(4);
        owner.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));owner.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.DIAMOND_SWORD));require(LbcInventorySkills.apply(owner,11),"native sword pair conversion");equipment(owner);
        near(owner.getAttributeValue(Attributes.ATTACK_SPEED),.88,"actual equipped sword+sword speed is4-2.4-.72");
        var right=attributes(owner.getMainHandItem()).modifiers().stream().filter(e->e.attribute().equals(Attributes.ATTACK_SPEED)&&e.slot()==EquipmentSlotGroup.MAINHAND).findFirst().orElseThrow();var left=attributes(owner.getOffhandItem()).modifiers().stream().filter(e->e.attribute().equals(Attributes.ATTACK_SPEED)&&e.slot()==EquipmentSlotGroup.OFFHAND).findFirst().orElseThrow();require(!right.modifier().id().equals(left.modifier().id())&&speed.getModifier(right.modifier().id())!=null&&speed.getModifier(left.modifier().id())!=null,"native equipment owner applies main and derived offhand IDs simultaneously");
    }
    private static void inventoryEvents(GameController game,ServerPlayer owner){
        clearArmor(owner);owner.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);owner.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);game.config().lbcSkills=true;game.config().ranked=false;owner.entityTags().remove("skywars2.debug_perks");var profile=game.players().get(owner.getUUID());
        var gates=List.of("lbc:true_advancements/arcanums/tannerkivi","lbc:true_advancements/armor/randchanted","lbc:true_advancements/armor/techno_full");
        for(var gate:gates){profile.lbcUnlocks.remove(gate);var advancement=game.server().getAdvancements().get(Identifier.parse(gate));require(advancement!=null,"original source advancement exists: "+gate);for(var criterion:advancement.value().criteria().keySet())owner.getAdvancements().revoke(advancement,criterion);}
        profile.lbcSkill=8;var locked=decorated(owner,Items.DIAMOND_HELMET);owner.setItemSlot(EquipmentSlot.HEAD,locked);var untouched=locked.copy();net.minecraft.advancements.triggers.CriteriaTriggers.INVENTORY_CHANGED.trigger(owner,owner.getInventory(),locked);require(ItemStack.matches(untouched,owner.getItemBySlot(EquipmentSlot.HEAD)),"real inventory event preserves original achievement unlock gate");
        for(var gate:gates){var advancement=game.server().getAdvancements().get(Identifier.parse(gate));for(var criterion:advancement.value().criteria().keySet())owner.getAdvancements().award(advancement,criterion);}
        for(int skill:new int[]{4,8,11})for(var tag:List.of("", "stopper_magic", "stopper")){
            net.minecraft.world.entity.Marker jammer=null;
            try{
                if(!tag.isEmpty()){jammer=new net.minecraft.world.entity.Marker(net.minecraft.world.entity.EntityTypes.MARKER,owner.level());jammer.setPos(owner.position());jammer.entityTags().add(tag);require(owner.level().addFreshEntity(jammer),"native jammer fixture");game.items().magic().loaded(jammer);require(tag.equals("stopper")?game.items().magic().mechanismSuppressed(owner):game.items().magic().beaconSuppressed(owner.position()),"real jammer is active before physical inventory skill");}
                clearArmor(owner);owner.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);owner.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);profile.lbcSkill=skill;var fresh=decorated(owner,skill==11?Items.TRIDENT:Items.DIAMOND_HELMET);var before=fresh.copy();owner.setItemSlot(skill==11?EquipmentSlot.OFFHAND:EquipmentSlot.HEAD,fresh);net.minecraft.advancements.triggers.CriteriaTriggers.INVENTORY_CHANGED.trigger(owner,owner.getInventory(),fresh);
                if(skill==4)require(fresh.has(DataComponents.UNBREAKABLE)&&attributes(fresh).modifiers().size()==attributes(before).modifiers().size()+1,"selected skill4 actual inventory event remains unjammed: "+tag);
                else if(skill==8)require(fresh.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).getLevel(enchant(owner,Enchantments.PROTECTION))==1,"selected skill8 actual inventory event remains unjammed: "+tag);
                else handEntries(before,fresh);
            }finally{if(jammer!=null){game.items().magic().unloaded(jammer);jammer.discard();}}
        }
        profile.lbcSkill=0;var ordinary=decorated(owner,Items.DIAMOND_HELMET);owner.setItemSlot(EquipmentSlot.HEAD,ordinary);var expected=ordinary.copy();net.minecraft.advancements.triggers.CriteriaTriggers.INVENTORY_CHANGED.trigger(owner,owner.getInventory(),ordinary);require(ItemStack.matches(expected,owner.getItemBySlot(EquipmentSlot.HEAD)),"unselected inventory event never grants a skill mutation");
    }
    private static void selectionAndPause(GameController game,ServerPlayer owner){
        clearArmor(owner);owner.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);owner.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        var profile=game.players().get(owner.getUUID());profile.lbcSkill=0;
        require(GameController.operator(owner),"real operator fixture for in-game debug selection");
        boolean debug=owner.entityTags().contains("skywars2.debug_perks");game.menus().openDebug(owner);game.menus().open(owner,GameMenus.Page.LBC_SKILLS);
        try{for(int[] row:new int[][]{{4,3},{8,7},{11,10}}){
                require(eu.pb4.sgui.api.SguiUtils.getCurrentGui(owner) instanceof eu.pb4.sgui.api.gui.SimpleGui,"actual operator debug menu is open");
                var menu=(eu.pb4.sgui.api.gui.SimpleGui)eu.pb4.sgui.api.SguiUtils.getCurrentGui(owner);
                var button=menu.getGuiElement(row[1]);require(button!=null,"native skill button exists");
                button.getGuiCallback().click(row[1],eu.pb4.sgui.api.ClickType.MOUSE_LEFT,net.minecraft.world.inventory.ContainerInput.PICKUP,menu);
                require(profile.lbcSkill==row[0]&&game.lbc().selected(profile,LbcLoadouts.Kind.SKILLS,10000,row[1]),"actual native menu selection/catalog source slot: "+row[0]);
            }}finally{owner.closeContainer();if(!debug)owner.entityTags().remove("skywars2.debug_perks");}
        profile.lbcSkill=4;var paused=decorated(owner,Items.DIAMOND_HELMET);
        game.items().timeStop().begin(owner.level(),40);
        try{
            owner.setItemSlot(EquipmentSlot.HEAD,paused);net.minecraft.advancements.triggers.CriteriaTriggers.INVENTORY_CHANGED.trigger(owner,owner.getInventory(),paused);
            require(!paused.has(DataComponents.UNBREAKABLE)&&!ItemData.contains(paused,"tanner_upgraded"),"actual equipment/inventory events defer while time is stopped");
        }finally{game.items().timeStop().end(owner.level());}
        game.lbc().tick();require(paused.has(DataComponents.UNBREAKABLE)&&ItemData.contains(paused,"tanner_upgraded"),"event-owned deferred repair resumes after time stop");
        game.config().lbcSkills=false;var disabled=decorated(owner,Items.DIAMOND_BOOTS);owner.setItemSlot(EquipmentSlot.FEET,disabled);
        net.minecraft.advancements.triggers.CriteriaTriggers.INVENTORY_CHANGED.trigger(owner,owner.getInventory(),disabled);
        require(!ItemData.contains(disabled,"tanner_upgraded"),"global skill setting still disables new inventory upgrades");game.config().lbcSkills=true;
        profile.lbcSkill=0;
    }
    private static void parkedProtection(GameController game,ServerPlayer owner){
        clearArmor(owner);owner.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);owner.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);game.players().get(owner.getUUID()).lbcSkill=0;
        var loot=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:neko_set_erosion/helmet");require(loot.size()==1,"actual native erosion helmet leaf");var stack=loot.getFirst();var protection=enchant(owner,Enchantments.PROTECTION);EnchantmentHelper.updateEnchantments(stack,mutable->mutable.set(protection,77));owner.setItemSlot(EquipmentSlot.HEAD,stack);
        var jammer=new net.minecraft.world.entity.Marker(net.minecraft.world.entity.EntityTypes.MARKER,owner.level());jammer.setPos(owner.position());jammer.entityTags().add("stopper_magic");require(owner.level().addFreshEntity(jammer),"native parked protection jammer");game.items().magic().loaded(jammer);
        try{
            game.items().armorSets().inventoryChanged(owner);require(LbcErosionSuppression.parked(stack)&&stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).getLevel(protection)==0,"MAG parks actual erosion Protection before upgrade");
            game.players().get(owner.getUUID()).lbcSkill=8;net.minecraft.advancements.triggers.CriteriaTriggers.INVENTORY_CHANGED.trigger(owner,owner.getInventory(),stack);require(LbcErosionSuppression.parked(stack)&&stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).getLevel(protection)==0,"skill8 upgrades saved baseline without leaking live Protection through MAG");
            game.players().get(owner.getUUID()).lbcSkill=0;
        }finally{game.items().magic().unloaded(jammer);jammer.discard();game.items().armorSets().inventoryChanged(owner);}
        require(!LbcErosionSuppression.parked(stack)&&stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).getLevel(protection)==78,"leaving MAG recovers exactly upgraded Protection77+1");
        require(!LbcInventorySkills.apply(owner,8)&&stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).getLevel(protection)==78,"parked upgrade guard survives ordinary recovery");
    }
}
