package me.skywars2.game;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.component.*;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ItemLore;

/** Source metadata fills missing native defaults once, before any user stack patches exist. */
public final class AuthoredItemPresentation {
    private static JsonObject read(String name){
        try(var input=AuthoredItemPresentation.class.getResourceAsStream("/skywars2/"+name+".json")){
            if(input==null)throw new IOException(name);
            return JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    private static final JsonObject SOURCES=read("authored_presentations"),ACADEMY=read("academy_presentations");
    private static final Map<String,Component> NAMES=new HashMap<>();
    private static final Map<String,ItemLore> LORE=new HashMap<>();
    static{
        for(var entry:SOURCES.entrySet()){
            var value=entry.getValue().getAsJsonObject();
            NAMES.put(entry.getKey(),ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,value.get("minecraft:custom_name")).getOrThrow());
            var lines=new ArrayList<Component>();
            for(var line:value.getAsJsonArray("minecraft:lore"))lines.add(ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,line).getOrThrow());
            LORE.put(entry.getKey(),new ItemLore(lines));
        }
    }
    public static void fill(DataComponentMap.Builder components,Component nativeName,Identifier itemId,boolean explicitItemName){
        if(itemId.getPath().equals("oguzok_sword")&&components.build().get(DataComponents.TOOLTIP_STYLE)==null)
            components.set(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_d"));
        if(!(nativeName.getContents() instanceof TranslatableContents translated))return;
        String key=translated.getKey();var source=SOURCES.getAsJsonObject(key);if(source==null)return;
        var existing=components.build();
        if(!explicitItemName&&existing.get(DataComponents.CUSTOM_NAME)==null)components.set(DataComponents.CUSTOM_NAME,NAMES.get(key));
        boolean academy=ACADEMY.has(itemId.getPath());
        if(existing.get(DataComponents.TOOLTIP_STYLE)==null&&(academy||source.has("minecraft:tooltip_style")))components.set(DataComponents.TOOLTIP_STYLE,Identifier.parse(academy?"lbc:tier_"+ACADEMY.get(itemId.getPath()).getAsString():source.get("minecraft:tooltip_style").getAsString()));
        if(source.has("minecraft:tooltip_display")){
            var authored=DataComponents.TOOLTIP_DISPLAY.codec().parse(JsonOps.INSTANCE,source.get("minecraft:tooltip_display")).getOrThrow();
            var display=existing.getOrDefault(DataComponents.TOOLTIP_DISPLAY,net.minecraft.world.item.component.TooltipDisplay.DEFAULT);
            for(var hidden:authored.hiddenComponents())display=display.withHidden(hidden,true);
            if(authored.hideTooltip()&&!display.hideTooltip())display=new net.minecraft.world.item.component.TooltipDisplay(true,display.hiddenComponents());
            components.set(DataComponents.TOOLTIP_DISPLAY,display);
        }
        // 26.3 supplies EMPTY lore and DEFAULT tooltip display even when the item never authored them.
        if(existing.get(DataComponents.LORE)==null||existing.get(DataComponents.LORE).lines().isEmpty()){
            var lore=LORE.get(key);
            if(academy)lore=lore.withLineAdded(Component.translatable("upgraded_item").withStyle(s->s.withColor(0x55ff55).withItalic(false)));
            components.set(DataComponents.LORE,lore);
        }
    }
}
