package me.skywars2.game;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;

/** Bootstrap the finite utility items from their one canonical authored loot definition. */
final class LbcUtilityItemPrototypes {
    private LbcUtilityItemPrototypes(){}
    static Item.Properties properties(String nativeId,String resource,Item carrier,int maximum){
        return properties(nativeId,resource,carrier,maximum,java.util.Set.of());
    }
    /** Registry-bound components are applied by the canonical loot table after world registries load. */
    static Item.Properties properties(String nativeId,String resource,Item carrier,int maximum,java.util.Set<String> lootComponents){
        return properties(nativeId,resource,carrier,maximum,lootComponents,false);
    }
    static Item.Properties properties(String nativeId,String resource,Item carrier,int maximum,java.util.Set<String> lootComponents,boolean nativeDefaults){
        return properties(nativeId,resource,carrier,maximum,lootComponents,nativeDefaults,false);
    }
    /** Same canonical reader; holder-dependent food/enchantment components bind after registries are ready. */
    static Item.Properties deferredProperties(String nativeId,String resource,Item carrier,int maximum){
        return properties(nativeId,resource,carrier,maximum,java.util.Set.of(),true,true);
    }
    private static Item.Properties properties(String nativeId,String resource,Item carrier,int maximum,java.util.Set<String> lootComponents,boolean nativeDefaults,boolean deferred){
        try(var input=LbcUtilityItemPrototypes.class.getResourceAsStream(resource)){
            if(input==null)throw new IllegalStateException("Missing utility prototype "+resource);
            var entry=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject()
                    .getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
            var rows=entry.has("modifier")?entry.getAsJsonArray("modifier"):entry.getAsJsonArray("functions");
            Identifier sourceModel=BuiltInRegistries.ITEM.getKey(carrier);
            for(var value:rows){var row=value.getAsJsonObject();var type=row.has("type")?row.get("type"):row.get("function");
                if(type.getAsString().equals("minecraft:set_components"))for(var component:row.getAsJsonObject("components").entrySet())
                    if(!component.getKey().startsWith("!")&&Identifier.parse(component.getKey()).equals(Identifier.parse("minecraft:item_model")))
                        sourceModel=Identifier.parse(component.getValue().getAsString());}
            final var model=sourceModel;var key=ResourceKey.create(Registries.ITEM,Identifier.parse(nativeId));
            var result=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}.setId(key).stacksTo(maximum);
            if(nativeDefaults)vanillaVariant(result,carrier);
            for(var value:rows){var row=value.getAsJsonObject();String type=(row.has("type")?row.get("type"):row.get("function")).getAsString();
                switch(type){
                    case "minecraft:set_count", "minecraft:enchant_with_levels" -> {} // Count and stochastic enchantments belong to each loot roll.
                    case "minecraft:set_components" -> {for(var component:row.getAsJsonObject("components").entrySet()){
                        if(lootComponents.contains(component.getKey()))continue;
                        boolean remove=component.getKey().startsWith("!");
                        var componentType=BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse(remove?component.getKey().substring(1):component.getKey()));
                        if(componentType==null)throw new IllegalStateException("Unknown utility component "+component.getKey());
                        if(remove)result.component(componentType,null);
                        else if(deferred)deferredComponent(result,componentType,component.getValue());
                        else component(result,componentType,component.getValue());}}
                    case "minecraft:set_name" -> result.component(DataComponents.CUSTOM_NAME,ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,row.get("name")).getOrThrow());
                    case "minecraft:set_lore" -> {if(!row.get("mode").getAsString().equals("replace_all"))throw new IllegalStateException("Unexpected utility lore operation");
                        var lines=new ArrayList<Component>();for(var line:row.getAsJsonArray("lore"))lines.add(ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,line).getOrThrow());
                        result.component(DataComponents.LORE,new ItemLore(lines));}
                    case "minecraft:set_custom_data" -> result.component(DataComponents.CUSTOM_DATA,CustomData.of(TagParser.parseCompoundFully(row.get("tag").getAsString())));
                    default -> throw new IllegalStateException("Unsupported utility prototype operation "+type);
                }}
            return result;
        }catch(Exception error){throw new IllegalStateException("Invalid utility prototype "+resource,error);}
    }
    private static <T> void component(Item.Properties properties,DataComponentType<T> type,JsonElement source){
        if(type.codec()==null)throw new IllegalStateException("Utility component has no persistent codec");
        properties.component(type,type.codec().parse(JsonOps.INSTANCE,source).getOrThrow());
    }
    private static <T> void deferredComponent(Item.Properties properties,DataComponentType<T> type,JsonElement source){
        if(type.codec()==null)throw new IllegalStateException("Utility component has no persistent codec");
        var frozen=source.deepCopy();
        properties.delayedComponent(type,lookup->type.codec().parse(lookup.createSerializationContext(JsonOps.INSTANCE),frozen).getOrThrow());
    }
    /** Vanilla holder components bind after item registration in 26.3; use its native property builders here. */
    private static void vanillaVariant(Item.Properties p,Item carrier){
        p.overrideDescription(carrier.getDescriptionId());
        if(carrier==Items.ELYTRA)p.durability(432).rarity(Rarity.EPIC).component(DataComponents.GLIDER,net.minecraft.util.Unit.INSTANCE)
                .component(DataComponents.EQUIPPABLE,net.minecraft.world.item.equipment.Equippable.builder(net.minecraft.world.entity.EquipmentSlot.CHEST)
                        .setEquipSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_ELYTRA).setAsset(net.minecraft.world.item.equipment.EquipmentAssets.ELYTRA).setDamageOnHurt(false).build())
                .repairable(Items.PHANTOM_MEMBRANE);
        else if(carrier==Items.BOW)p.durability(384).enchantable(1)
                .cookingFuel(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOD_ITEMS_SMALL);
        else if(carrier==Items.CROSSBOW)p.durability(465).enchantable(1).component(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.EMPTY)
                .cookingFuel(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOD_ITEMS_SMALL);
        else if(carrier==Items.TRIDENT)p.durability(250).rarity(Rarity.RARE).enchantable(1).attributes(TridentItem.createAttributes())
                .component(DataComponents.TOOL,TridentItem.createToolProperties()).component(DataComponents.WEAPON,new Weapon(1));
        else if(carrier==Items.IRON_AXE)p.axe(ToolMaterial.IRON,6,-3.1f);
        else if(carrier==Items.GOLDEN_AXE)p.axe(ToolMaterial.GOLD,6,-3);
        else if(carrier==Items.NETHERITE_AXE)p.axe(ToolMaterial.NETHERITE,5,-3).fireResistant();
        else if(carrier==Items.IRON_SHOVEL)p.shovel(ToolMaterial.IRON,1.5f,-3);
        else if(carrier==Items.NETHERITE_SHOVEL)p.shovel(ToolMaterial.NETHERITE,1.5f,-3).fireResistant();
        else if(carrier==Items.IRON_PICKAXE)p.pickaxe(ToolMaterial.IRON,1,-2.8f);
        else if(carrier==Items.DIAMOND_PICKAXE)p.pickaxe(ToolMaterial.DIAMOND,1,-2.8f);
        else if(carrier==Items.IRON_HOE)p.hoe(ToolMaterial.IRON,-2,-1);
        else if(carrier==Items.FLINT_AND_STEEL)p.durability(64);
        else if(carrier==Items.WOODEN_SWORD)p.sword(ToolMaterial.WOOD,3,-2.4f).cookingFuel(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOD_ITEMS_LARGE);
        else if(carrier==Items.IRON_SWORD)p.sword(ToolMaterial.IRON,3,-2.4f);
        else if(carrier==Items.GOLDEN_SWORD)p.sword(ToolMaterial.GOLD,3,-2.4f);
        else if(carrier==Items.NETHERITE_SWORD)p.sword(ToolMaterial.NETHERITE,3,-2.4f).fireResistant();
        else if(carrier==Items.SHIELD)p.durability(336).component(DataComponents.BANNER_PATTERNS,net.minecraft.world.level.block.entity.BannerPatternLayers.EMPTY)
                .repairable(net.minecraft.tags.ItemTags.WOODEN_TOOL_MATERIALS).equippableUnswappable(net.minecraft.world.entity.EquipmentSlot.OFFHAND)
                .delayedComponent(DataComponents.BLOCKS_ATTACKS,lookup->new BlocksAttacks(.25f,1,
                        java.util.List.of(new BlocksAttacks.DamageReduction(90,java.util.Optional.empty(),0,1)),
                        new BlocksAttacks.ItemDamageFunction(3,1,1),java.util.Optional.of(lookup.getOrThrow(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)),
                        java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BLOCK),java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BREAK)))
                .component(DataComponents.BREAK_SOUND,net.minecraft.sounds.SoundEvents.SHIELD_BREAK);
        else if(carrier==Items.TOTEM_OF_UNDYING)p.stacksTo(1).rarity(Rarity.UNCOMMON)
                .component(DataComponents.DEATH_PROTECTION,DeathProtection.TOTEM_OF_UNDYING);
        else if(carrier==Items.RECOVERY_COMPASS)p.rarity(Rarity.UNCOMMON);
        else if(carrier==Items.REPEATING_COMMAND_BLOCK)p.rarity(Rarity.EPIC);
        else if(carrier==Items.CARROT_ON_A_STICK)p.durability(25);
        else if(carrier==Items.TIPPED_ARROW)p.component(DataComponents.POTION_CONTENTS,net.minecraft.world.item.alchemy.PotionContents.EMPTY).component(DataComponents.POTION_DURATION_SCALE,.125f);
        else if(carrier==Items.FIREWORK_ROCKET)p.component(DataComponents.FIREWORKS,new Fireworks(1,java.util.List.of()));
        else if(carrier==Items.GOLDEN_APPLE)p.food(net.minecraft.world.food.Foods.GOLDEN_APPLE,Consumables.GOLDEN_APPLE);
        else if(carrier==Items.SALMON)p.food(net.minecraft.world.food.Foods.SALMON);
        else if(carrier==Items.CHORUS_FRUIT)p.food(net.minecraft.world.food.Foods.CHORUS_FRUIT,Consumables.CHORUS_FRUIT).useCooldown(1);
    }
}
