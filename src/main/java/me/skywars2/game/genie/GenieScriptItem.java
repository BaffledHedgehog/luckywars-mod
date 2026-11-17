package me.skywars2.game.genie;

import com.google.gson.JsonObject;
import java.util.*;
import me.skywars2.game.ItemData;
import me.skywars2.game.genie.script.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.*;

/** Persistent item identity/state; static executable code stays in the server's content store. */
public final class GenieScriptItem {
    public static final String CODE="genie_code",STATE="genie_state";
    private GenieScriptItem(){}
    public static boolean matches(ItemStack stack){return ItemData.contains(stack,CODE);}
    public static String code(ItemStack stack){return ItemData.stringValue(stack,CODE,"");}
    public static String serial(ItemStack stack){return ItemData.stringValue(stack,GenieRuntime.SERIAL,"");}
    public static String encodedState(ItemStack stack){return ItemData.stringValue(stack,STATE,"{}");}
    public static JsonObject state(ItemStack stack){return ScriptJson.parse(encodedState(stack));}
    public static void state(ItemStack stack,String encoded){
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putString(STATE,encoded);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    public static ItemStack construct(ScriptRepository.Loaded code,String icon,String name,String description,int count){
        Objects.requireNonNull(code);if(count<1)throw new IllegalArgumentException("positive item count required");
        var type=BuiltInRegistries.ITEM.get(Identifier.withDefaultNamespace(icon)).orElseThrow(()->new IllegalArgumentException("unknown item icon")).value();
        var stack=new ItemStack(type,count);if(stack.isEmpty())throw new IllegalArgumentException("item icon cannot be empty");
        stack.set(DataComponents.CUSTOM_NAME,Component.literal(name).withStyle(style->style.withItalic(false)));
        stack.set(DataComponents.LORE,new ItemLore(List.of(Component.literal(description).withStyle(style->style.withItalic(false).withColor(0xffffff)))));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putString(CODE,code.id());
        tag.putString(GenieRuntime.SERIAL,UUID.randomUUID().toString());tag.putString(STATE,ScriptJson.canonical(code.program().initialState()));
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;
    }
}
