package me.skywars2.game;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

/** Generation kernel only. Acquisition and ability composition stay gated until TMT is complete. */
final class LbcTmtProperties {
    private record Source(List<String> names, List<Identifier> sounds, CompoundTag flags) {}
    private static final Source SOURCE = load();
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final AttributeModifier.Operation[] OPERATIONS = {AttributeModifier.Operation.ADD_VALUE,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, AttributeModifier.Operation.ADD_MULTIPLIED_BASE};

    record Domains(List<Holder<Attribute>> attributes, List<Holder<Enchantment>> enchantments,
                   List<String> names, List<Identifier> sounds, List<ItemUseAnimation> animations,
                   List<EquipmentSlotGroup> slots) {
        Domains {
            attributes=List.copyOf(attributes);enchantments=List.copyOf(enchantments);
            names=List.copyOf(names);sounds=List.copyOf(sounds);animations=List.copyOf(animations);slots=List.copyOf(slots);
            if(attributes.isEmpty()||enchantments.isEmpty()||names.isEmpty()||sounds.isEmpty())
                throw new IllegalArgumentException("Empty TMT property domain");
        }
    }

    private final Domains domains;

    /** Build once for a registry generation; no registry/file/NBT scan occurs for each random draw. */
    LbcTmtProperties(ServerLevel level) {
        var attributes=level.registryAccess().lookupOrThrow(Registries.ATTRIBUTE).listElements()
                .sorted(java.util.Comparator.comparing(h->h.key().identifier())).map(h->(Holder<Attribute>)h).toList();
        var enchantments=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
                .sorted(java.util.Comparator.comparing(h->h.key().identifier())).map(h->(Holder<Enchantment>)h).toList();
        var sounds=new LinkedHashSet<>(SOURCE.sounds);
        BuiltInRegistries.SOUND_EVENT.keySet().stream().sorted().forEach(sounds::add);
        var names=new LinkedHashSet<>(SOURCE.names);
        BuiltInRegistries.ITEM.stream().filter(item->item!=Items.AIR).forEach(item->{
            var stack=item.getDefaultInstance();
            collectNames(stack.get(DataComponents.ITEM_NAME),names);
            collectNames(stack.get(DataComponents.CUSTOM_NAME),names);
            var lore=stack.get(DataComponents.LORE);if(lore!=null)lore.lines().forEach(line->collectNames(line,names));
        });
        domains=new Domains(attributes,enchantments,List.copyOf(names),List.copyOf(sounds),
                List.of(ItemUseAnimation.values()),List.of(EquipmentSlotGroup.values()));
    }

    Domains domains(){return domains;}

    /** Seven meaningful, already-resolved ability writes are provided by the composition owner. */
    ItemStack generate(RandomSource random, IntFunction<ItemStack> carrier, List<CompoundTag> abilities) {
        if(abilities.size()!=7)throw new IllegalArgumentException("TMT requires seven ability draws");
        int base=random.nextInt(5);
        var stack=carrier.apply(base).copyWithCount(1);
        if(stack.isEmpty())throw new IllegalArgumentException("Missing TMT carrier");
        var data=SOURCE.flags.copy();
        var nativeData=stack.get(DataComponents.CUSTOM_DATA);if(nativeData!=null)data.merge(nativeData.copyTag());
        for(var ability:abilities)data.merge(ability);
        data.putInt("tmt_texture_seed",random.nextInt(Integer.MAX_VALUE));
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        if(base==4){
            var animation=pick(random,domains.animations);var sound=pick(random,domains.sounds);
            stack.set(DataComponents.FOOD,new FoodProperties(20,20,true));
            stack.set(DataComponents.CONSUMABLE,new Consumable(2,animation,
                    Holder.direct(SoundEvent.createVariableRangeEvent(sound)),true,List.of()));
        }
        stack.remove(DataComponents.CUSTOM_NAME);
        stack.set(DataComponents.ITEM_NAME,text(random));
        var lore=new ArrayList<Component>(6);for(int i=0;i<6;i++)lore.add(text(random));
        stack.set(DataComponents.LORE,new ItemLore(List.copyOf(lore)));
        var slot=ARMOR[random.nextInt(ARMOR.length)];
        // Existing authored assets remain the visual oracle until the procedural atlas renderer is qualified.
        stack.set(DataComponents.EQUIPPABLE,Equippable.builder(slot).setAsset(ResourceKey.create(
                EquipmentAssets.ROOT_ID,Identifier.parse("lbc:tmt/tmt_"+between(random,1,707)))).build());
        stack.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:tmt/tmt_"+between(random,1,12758)));
        var attributes=new ArrayList<ItemAttributeModifiers.Entry>();
        for(int attempt=0;attempt<29;attempt++){
            float chance=attempt==0?1:attempt<=4?.2F:attempt<=12?.1F:.05F;
            if(random.nextFloat()>=chance)continue;
            attributes.add(attribute(random));
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(List.copyOf(attributes)));
        var enchantments=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for(int attempt=0;attempt<21;attempt++){
            if(random.nextFloat()>=(attempt<4?.2F:.1F))continue;
            var roll=enchantment(random);
            enchantments.set(roll.enchantment,roll.level);
        }
        stack.set(DataComponents.ENCHANTMENTS,enchantments.toImmutable());
        return stack;
    }

    ItemAttributeModifiers.Entry attribute(RandomSource random){
        int id=between(random,1,Integer.MAX_VALUE);
        var attribute=pick(random,domains.attributes);
        double amount=attributeAmount(random);
        var slot=pick(random,domains.slots);
        var operation=OPERATIONS[random.nextInt(OPERATIONS.length)];
        return new ItemAttributeModifiers.Entry(attribute,new AttributeModifier(
                Identifier.withDefaultNamespace(Integer.toString(id)),amount,operation),slot);
    }

    record EnchantmentRoll(Holder<Enchantment> enchantment,int level){}
    EnchantmentRoll enchantment(RandomSource random){
        var enchantment=pick(random,domains.enchantments);
        int value=between(random,1,5);
        if(random.nextFloat()<.1F)value=between(random,1,255);
        if(random.nextFloat()<.1F)value=between(random,1,15);
        return new EnchantmentRoll(enchantment,value);
    }

    static double attributeAmount(RandomSource random){
        double amount=between(random,-50,100)*.01;
        // Independent source overwrites: a later successful branch replaces an earlier result.
        if(random.nextFloat()<.01F)amount=between(random,-1000000000,1000000000)*.01;
        if(random.nextFloat()<.1F)amount=between(random,-200,1200)*.01;
        if(random.nextFloat()<.1F)amount=between(random,-100,400)*.01;
        return amount;
    }

    private Component text(RandomSource random){
        String name=pick(random,domains.names);int color=0;
        for(int i=0;i<6;i++)color=color*16+random.nextInt(16);
        boolean italic=random.nextInt(6)==0,bold=random.nextInt(7)==0,underlined=random.nextInt(6)==0,
                obfuscated=random.nextInt(7)==0,strikethrough=random.nextInt(7)==0;
        var component=random.nextInt(6)==0?Component.literal(name):Component.translatable(name);
        int rgb=color;
        return component.withStyle(s->s.withColor(rgb).withItalic(italic).withBold(bold)
                .withUnderlined(underlined).withObfuscated(obfuscated).withStrikethrough(strikethrough));
    }

    private static <T>T pick(RandomSource random,List<T> pool){return pool.get(random.nextInt(pool.size()));}
    private static int between(RandomSource random,int min,int max){return min+random.nextInt(max-min+1);}
    private static void collectNames(Component component,LinkedHashSet<String> names){
        if(component==null)return;
        if(component.getContents() instanceof TranslatableContents text){
            names.add(text.getKey());for(var arg:text.getArgs())if(arg instanceof Component nested)collectNames(nested,names);
        }
        component.getSiblings().forEach(child->collectNames(child,names));
    }
    private static Source load(){
        try(var input=LbcTmtProperties.class.getResourceAsStream("/skywars2/tmt_properties.json")){
            if(input==null)throw new IllegalStateException("Missing TMT property inputs");
            var root=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            var names=new LinkedHashSet<String>();root.getAsJsonArray("names").forEach(v->names.add(v.getAsString()));
            var sounds=new LinkedHashSet<Identifier>();root.getAsJsonArray("sounds").forEach(v->sounds.add(Identifier.parse(v.getAsString())));
            var flags=new CompoundTag();root.getAsJsonObject("baseFlags").entrySet().forEach(e->flags.putInt(e.getKey(),e.getValue().getAsInt()));
            return new Source(List.copyOf(names),List.copyOf(sounds),flags);
        }catch(java.io.IOException e){throw new IllegalStateException("Cannot read TMT property inputs",e);}
    }
}
