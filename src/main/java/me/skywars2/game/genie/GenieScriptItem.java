package me.skywars2.game.genie;

import com.google.gson.JsonObject;
import java.util.*;
import me.skywars2.game.ItemData;
import me.skywars2.game.genie.script.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;

/** Persistent item identity/state; static executable code stays in the server's content store. */
public final class GenieScriptItem {
    public static final String CODE="genie_code",STATE="genie_state";
    static final String REPLACE_SHOT_EVENT="SHOT_REPLACE";
    private static final String REPLACE_SHOT="genie_replace_shot";
    static final String REPLACE_USE_EVENT="USE_REPLACE";
    private static final String REPLACE_USE="genie_replace_use";
    private GenieScriptItem(){}
    public static boolean matches(ItemStack stack){return ItemData.contains(stack,CODE);}
    public static boolean replacesShot(ItemStack stack){return matches(stack)&&ItemData.intValue(stack,REPLACE_SHOT,0)==1;}
    static boolean replacesUse(ItemStack stack){return matches(stack)&&ItemData.intValue(stack,REPLACE_USE,0)==1;}
    public static String code(ItemStack stack){return ItemData.stringValue(stack,CODE,"");}
    public static String serial(ItemStack stack){return ItemData.stringValue(stack,GenieRuntime.SERIAL,"");}
    public static String encodedState(ItemStack stack){return ItemData.stringValue(stack,STATE,"{}");}
    public static JsonObject state(ItemStack stack){return ScriptJson.parse(encodedState(stack));}
    public static void state(ItemStack stack,String encoded){
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putString(STATE,encoded);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    public static ItemStack construct(ScriptRepository.Loaded code,String icon,String name,String description,int count){
        Objects.requireNonNull(code);if(count<1)throw new IllegalArgumentException("positive item count required");
        var type=BuiltInRegistries.ITEM.get(Identifier.parse(icon)).orElseThrow(()->new IllegalArgumentException("unknown item icon")).value();
        boolean replacesShot=code.program().handler(REPLACE_SHOT_EVENT)!=null;
        if(replacesShot&&!(type instanceof BowItem||type instanceof CrossbowItem))
            throw new IllegalArgumentException("SHOT_REPLACE requires a bow or crossbow");
        var stack=new ItemStack(type,count);if(stack.isEmpty())throw new IllegalArgumentException("item icon cannot be empty");
        GenieItems.describeGift(stack, name, description);
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putString(CODE,code.id());
        if(replacesShot)tag.putInt(REPLACE_SHOT,1);
        if(code.program().handler(REPLACE_USE_EVENT)!=null)tag.putInt(REPLACE_USE,1);
        tag.putString(GenieRuntime.SERIAL,UUID.randomUUID().toString());tag.putString(STATE,ScriptJson.canonical(code.program().initialState()));
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;
    }
}
