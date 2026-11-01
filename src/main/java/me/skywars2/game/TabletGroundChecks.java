package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.phys.Vec3;

/** Scheduled native ground-tablet qualification; only the final cadence case waits for a real tick. */
public final class TabletGroundChecks {
    private TabletGroundChecks(){}
    private static State running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Ground tablets: "+why);}
    private static final class State implements AutoCloseable {
        final GameController game;final AllGrimoireChecks.Fixture base;final LbcLooseRewards service;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final Vec3 at;final Map<Integer,ItemStack> sources=new HashMap<>();
        ItemEntity clockTablet,clockDonor;int started;boolean closed;
        State(GameController game){this.game=game;base=new AllGrimoireChecks.Fixture(game);base.reset();service=game.items().looseRewards();at=base.at.add(0,5,8);base.level.getAllEntities().forEach(before::add);}
        List<ItemEntity> drops(){var result=new ArrayList<ItemEntity>();for(var entity:base.level.getAllEntities())if(entity instanceof ItemEntity item&&!before.contains(item)&&!item.isRemoved())result.add(item);return result;}
        void clear(){for(var item:drops()){service.unloaded(item);game.items().entityUnloaded(item);item.discard();}}
        ItemEntity drop(ItemStack stack,Vec3 pos){var item=new ItemEntity(base.level,pos.x,pos.y,pos.z,stack);item.setNoGravity(true);item.setNeverPickUp();item.setDeltaMovement(Vec3.ZERO);item.setThrower(base.base.owner);require(base.level.addFreshEntity(item),"native insertion");return item;}
        ItemEntity tablet(int kind){return drop(sources.get(kind).copyWithCount(2),at);}
        ItemEntity donor(ItemStack stack){return drop(stack,at.add(.7,0,0));}
        void sources(){for(int kind=2;kind<=4;kind++){var loot=game.lbc().rollTable(base.level,at,base.base.owner,"lbc:ported/multiply_"+kind);require(loot.size()==1&&LbcMultiplierTablets.kind(loot.getFirst())==kind,"actual source leaf x"+kind);sources.put(kind,loot.getFirst());}}
        void attributes(){
            for(var item:List.of(Items.DIAMOND_SWORD,Items.DIAMOND_CHESTPLATE,Items.TRIDENT)){
                clear();var stack=new ItemStack(item);stack.set(DataComponents.CUSTOM_NAME,Component.literal("native defaults"));
                var defaults=stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);require(!defaults.modifiers().isEmpty(),"native defaults exist "+item);
                var expected=stack.copy();expected.set(DataComponents.ATTRIBUTE_MODIFIERS,LbcMultiplierTablets.doubled(defaults));
                var target=donor(stack);var tablet=tablet(2);require(service.tablet(tablet),"default attributes mutate "+item);
                require(ItemStack.matches(target.getItem(),expected)&&tablet.getItem().getCount()==1,"actual effective defaults and metadata "+item);
            }
            clear();var entries=new ArrayList<ItemAttributeModifiers.Entry>();
            var operations=AttributeModifier.Operation.values();
            for(int i=0;i<31;i++)entries.add(new ItemAttributeModifiers.Entry(Attributes.ATTACK_SPEED,new AttributeModifier(Identifier.parse("skywars2:tablet_check_"+i),i%2==0?-i-.5:i+.5,operations[i%operations.length]),EquipmentSlotGroup.values()[i%EquipmentSlotGroup.values().length],i%3==0?ItemAttributeModifiers.Display.hidden():i%3==1?ItemAttributeModifiers.Display.override(Component.literal("attribute "+i)):ItemAttributeModifiers.Display.attributeModifiers()));
            var stack=new ItemStack(Items.STICK);stack.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(entries));stack.set(DataComponents.CUSTOM_NAME,Component.literal("31 attributes"));
            var expected=stack.copy();expected.set(DataComponents.ATTRIBUTE_MODIFIERS,LbcMultiplierTablets.doubled(stack.get(DataComponents.ATTRIBUTE_MODIFIERS)));
            var target=donor(stack);require(service.tablet(tablet(2))&&ItemStack.matches(target.getItem(),expected),"all31 IDs/operations/negative values/slots/display retained");
            clear();target=donor(new ItemStack(Items.DIRT));var tablet=tablet(2);require(!service.tablet(tablet)&&tablet.getItem().getCount()==2,"empty attributes preserve tablet");
        }
        void enchantments(){
            clear();var registry=base.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var sharp=registry.getOrThrow(Enchantments.SHARPNESS);var curse=registry.getOrThrow(Enchantments.VANISHING_CURSE);
            var levels=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);levels.set(sharp,600_000_000);levels.set(curse,3);
            var stack=new ItemStack(Items.DIAMOND_SWORD);stack.set(DataComponents.ENCHANTMENTS,levels.toImmutable());stack.setDamageValue(19);
            var stored=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);stored.set(sharp,7);stack.set(DataComponents.STORED_ENCHANTMENTS,stored.toImmutable());
            var target=donor(stack);var tablet=tablet(4);require(service.tablet(tablet),"direct enchantment mutation");
            require(target.getItem().getEnchantments().getLevel(sharp)==Integer.MAX_VALUE&&target.getItem().getEnchantments().getLevel(curse)==12,"wide saturating levels and curses");
            require(target.getItem().get(DataComponents.STORED_ENCHANTMENTS).equals(stored.toImmutable())&&target.getItem().getDamageValue()==19,"stored enchantments/damage retained");
            clear();var book=new ItemStack(Items.ENCHANTED_BOOK);book.set(DataComponents.STORED_ENCHANTMENTS,stored.toImmutable());target=donor(book);tablet=tablet(4);
            require(!service.tablet(tablet)&&tablet.getItem().getCount()==2&&ItemStack.matches(target.getItem(),book),"stored-only book is no-op");
            clear();levels=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);levels.set(sharp,Integer.MAX_VALUE);stack=new ItemStack(Items.STICK);stack.set(DataComponents.ENCHANTMENTS,levels.toImmutable());donor(stack);tablet=tablet(4);
            require(!service.tablet(tablet)&&tablet.getItem().getCount()==2,"saturated direct enchantment no-op preserves tablet");
        }
        void quantity(){
            clear();var stack=new ItemStack(Items.DIAMOND,64);stack.set(DataComponents.CUSTOM_NAME,Component.literal("full quantity"));
            var target=donor(stack);target.setDeltaMovement(.1,.2,.3);target.setExtendedLifetime();var motion=target.getDeltaMovement();int age=target.getAge();
            var tablet=tablet(3);require(service.tablet(tablet),"x3 native success");var copies=drops().stream().filter(i->LbcMultiplierTablets.kind(i.getItem())==0).toList();
            require(copies.size()==3&&copies.stream().mapToInt(i->i.getItem().getCount()).sum()==192,"64 becomes full192 in three legal ordinary stacks");
            for(var child:copies){require(child.getItem().getCount()<=child.getItem().getMaxStackSize()&&child.getOwner()==base.base.owner&&child.getAge()==age&&child.hasPickUpDelay()&&child.isNoGravity()&&child.getDeltaMovement().equals(motion)&&(child==target||game.items().currentEpoch(child)),"native split physics/pickup/age/owner/epoch retained: donor="+(child==target)+" age="+child.getAge()+" owner="+child.getOwner()+" gravity="+child.isNoGravity()+" motion="+child.getDeltaMovement()+" epoch="+game.items().currentEpoch(child));require(ItemStack.isSameItemSameComponents(child.getItem(),stack),"split retains all item components");}
            require(copies.stream().map(Entity::getUUID).distinct().count()==3&&tablet.getItem().getCount()==1,"unique native child identity/one debit");
            clear();target=donor(new ItemStack(Items.DIAMOND_SWORD));tablet=tablet(3);require(service.tablet(tablet),"unstackable donor multiplies");
            copies=drops().stream().filter(i->LbcMultiplierTablets.kind(i.getItem())==0).toList();require(copies.size()==3&&copies.stream().allMatch(i->i.getItem().getCount()==1),"unstackable items split to three legal singles");
        }
        void policies(){
            clear();var tablet=tablet(3);require(!service.tablet(tablet)&&tablet.getItem().getCount()==2,"no target preserves source");
            var outside=drop(new ItemStack(Items.DIAMOND,2),at.add(1.00001,0,0));require(!service.tablet(tablet),"true radius boundary rejects outside");outside.discard();
            var target=drop(new ItemStack(Items.DIAMOND,2),at.add(1,0,0));require(service.tablet(tablet)&&target.getItem().getCount()==6,"exact radius1 admitted");
            clear();tablet=tablet(2);var near=donor(new ItemStack(Items.DIRT));var far=drop(new ItemStack(Items.DIAMOND_SWORD),at.add(.9,0,0));var farBefore=far.getItem().copy();require(!service.tablet(tablet)&&ItemStack.matches(far.getItem(),farBefore),"nearest no-op never skips to farther target");near.discard();far.discard();
            var excluded=drop(sources.get(4).copy(),at.add(.1,0,0));require(!service.tablet(tablet),"all multiply custom-data targets excluded");excluded.discard();
            var raw=new ItemStack(Items.DIAMOND_SWORD);var multiply=new net.minecraft.nbt.CompoundTag();multiply.putInt("multiply",0);raw.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(multiply));excluded=donor(raw);require(!service.tablet(tablet),"presence of multiply flag excludes even unknown/zero tablet kinds");excluded.discard();
            for(int kind=2;kind<=4;kind++){
                clear();tablet=tablet(kind);target=donor(kind==2?new ItemStack(Items.DIAMOND_SWORD):new ItemStack(Items.DIAMOND));
                var zone=new Marker(EntityTypes.MARKER,base.level);zone.setPos(at);zone.entityTags().add("stopper_magic");require(base.level.addFreshEntity(zone),"MAG marker inserted");game.items().magic().loaded(zone);
                try{require(game.items().magic().suppressedAt(base.level,at)&&!service.tablet(tablet)&&tablet.getItem().getCount()==2,"MAG protects tablet x"+kind);}finally{game.items().magic().unloaded(zone);zone.discard();}
                game.items().timeStop().begin(base.level,40);try{require(!service.tablet(tablet),"TimeStop blocks x"+kind);}finally{game.items().timeStop().end(base.level);}
            }
            // A zone centered20.5 blocks toward the target includes the target but excludes the tablet.
            clear();tablet=tablet(3);target=donor(new ItemStack(Items.DIAMOND,2));var edge=new Marker(EntityTypes.MARKER,base.level);edge.setPos(at.add(20.5,0,0));edge.entityTags().add("stopper_magic");require(base.level.addFreshEntity(edge),"target-only MAG inserted");game.items().magic().loaded(edge);
            try{require(!game.items().magic().suppressedAt(base.level,at)&&game.items().magic().suppressedAt(base.level,target.position())&&!service.tablet(tablet),"MAG protects target point independently");}finally{game.items().magic().unloaded(edge);edge.discard();}
            var player=base.base.owner;var inventory=player.getInventory();var saved=inventory.getItem(0).copy();
            var artifact=new ItemStack(Items.REPEATING_COMMAND_BLOCK);var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("mystical",1);tag.putInt("type",1);artifact.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
            inventory.setItem(0,artifact);game.items().magic().inventoryChanged(player);game.items().magic().flushCleansing();
            try{require(game.items().magic().artifactActive(player)&&game.items().magic().suppressedAt(base.level,at),"actual inventory artifact active");for(int kind=2;kind<=4;kind++){clear();tablet=tablet(kind);donor(new ItemStack(Items.DIAMOND_SWORD));require(!service.tablet(tablet)&&tablet.getItem().getCount()==2,"artifact blocks x"+kind);}}finally{inventory.setItem(0,saved);game.items().magic().inventoryChanged(player);game.items().magic().flushCleansing();}
            clear();tablet=tablet(3);target=donor(new ItemStack(Items.DIAMOND,2));var tech=new Marker(EntityTypes.MARKER,base.level);tech.setPos(at);tech.entityTags().add("stopper");require(base.level.addFreshEntity(tech),"TECH inserted");game.items().magic().loaded(tech);
            try{require(game.items().magic().mechanismSuppressedAt(base.level,at)&&service.tablet(tablet),"TECH does not jam tablet");}finally{game.items().magic().unloaded(tech);tech.discard();}
            clear();tablet=tablet(3);target=donor(new ItemStack(Items.DIAMOND,2));int enrolled=service.count();service.unloaded(tablet);require(service.count()==enrolled-1,"unload removes shared tracking");service.loaded(tablet);require(service.count()==enrolled,"reload rebuilds shared tracking");
            tablet.entityTags().removeIf(t->t.startsWith("skywars2.items."));require(!service.tablet(tablet)&&tablet.getItem().getCount()==2,"stale epoch cannot mutate");
        }
        void begin(){sources();attributes();enchantments();quantity();policies();clear();clockDonor=donor(new ItemStack(Items.DIAMOND,2));clockTablet=tablet(3);started=game.server().getTickCount();}
        boolean step(){int now=game.server().getTickCount();if(now==started&&clockTablet.getItem().getCount()==2)return false;require(now-started<=5,"bounded native tick");
            require(!clockTablet.isRemoved()&&clockTablet.getItem().getCount()==1&&clockDonor.getItem().getCount()==6,"one native frame applies/debits exactly once");
            service.tick();require(clockTablet.getItem().getCount()==1&&clockDonor.getItem().getCount()==6,"same native tick cannot repeat");return true;}
        public void close(){if(closed)return;closed=true;try{clear();}finally{base.close();}}
    }
    public static void start(GameController game){require(running==null,"exclusive fixture");running=new State(game);try{running.begin();}catch(Throwable failure){running.close();running=null;throw failure;}SkyWars2.LOGGER.info("[check] START tablet-ground");}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS tablet-ground (actual source leaves/native sword armor trident defaults/all31 attributes/negative IDs operations metadata; full192 legal quantity split/native physics age pickup author; direct wide enchantments curses saturation/stored-book no-op; nearest radius no-target/no-op/MAG TECH TimeStop/epoch unload reload; one native tick one change/debit)");}catch(Throwable failure){try{running.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL tablet-ground",failure);}}
}
