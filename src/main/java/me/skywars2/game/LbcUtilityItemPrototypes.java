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
        try(var input=LbcUtilityItemPrototypes.class.getResourceAsStream(resource)){
            if(input==null)throw new IllegalStateException("Missing utility prototype "+resource);
            var entry=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject()
                    .getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
            var rows=entry.has("modifier")?entry.getAsJsonArray("modifier"):entry.getAsJsonArray("functions");
            Identifier sourceModel=BuiltInRegistries.ITEM.getKey(carrier);
            for(var value:rows){var row=value.getAsJsonObject();var type=row.has("type")?row.get("type"):row.get("function");
                if(type.getAsString().equals("minecraft:set_components")&&row.getAsJsonObject("components").has("minecraft:item_model"))
                    sourceModel=Identifier.parse(row.getAsJsonObject("components").get("minecraft:item_model").getAsString());}
            final var model=sourceModel;var key=ResourceKey.create(Registries.ITEM,Identifier.parse(nativeId));
            var result=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}.setId(key).stacksTo(maximum);
            if(nativeDefaults)vanillaVariant(result,carrier);
            for(var value:rows){var row=value.getAsJsonObject();String type=(row.has("type")?row.get("type"):row.get("function")).getAsString();
                switch(type){
                    case "minecraft:set_count" -> {} // Loot owns count; it is not an item property.
                    case "minecraft:set_components" -> {for(var component:row.getAsJsonObject("components").entrySet()){
                        if(lootComponents.contains(component.getKey()))continue;
                        var componentType=BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse(component.getKey()));
                        if(componentType==null)throw new IllegalStateException("Unknown utility component "+component.getKey());
                        component(result,componentType,component.getValue());}}
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
    /** Vanilla holder components bind after item registration in 26.3; use its native property builders here. */
    private static void vanillaVariant(Item.Properties p,Item carrier){
        p.overrideDescription("item.minecraft."+BuiltInRegistries.ITEM.getKey(carrier).getPath());
        if(carrier==Items.ELYTRA)p.durability(432).rarity(Rarity.EPIC).component(DataComponents.GLIDER,net.minecraft.util.Unit.INSTANCE)
                .component(DataComponents.EQUIPPABLE,net.minecraft.world.item.equipment.Equippable.builder(net.minecraft.world.entity.EquipmentSlot.CHEST)
                        .setEquipSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_ELYTRA).setAsset(net.minecraft.world.item.equipment.EquipmentAssets.ELYTRA).setDamageOnHurt(false).build())
                .repairable(Items.PHANTOM_MEMBRANE);
        else if(carrier==Items.TIPPED_ARROW)p.component(DataComponents.POTION_CONTENTS,net.minecraft.world.item.alchemy.PotionContents.EMPTY).component(DataComponents.POTION_DURATION_SCALE,.125f);
        else if(carrier==Items.FIREWORK_ROCKET)p.component(DataComponents.FIREWORKS,new Fireworks(1,java.util.List.of()));
    }
}
