package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

/** Native identities and original presentations for all four original Nexus wires. */
public final class LbcNexusWireItems {
    private static final CompoundTag RED_TRIGGER = trigger("nexus_wire_red");
    private static final CompoundTag BLACK_TRIGGER = trigger("nexus_wire_black");
    private static final CompoundTag BLUE_TRIGGER = trigger("nexus_wire_blue");
    private static final CompoundTag WHITE_TRIGGER = trigger("nexus_wire_white");
    public static final Item RED = wire("nexus_wire_red", "lbc:nexus_wire_red", 0xFF5555, 0xFF5555, true);
    public static final Item BLACK = wire("nexus_wire_black", "lbc:nexus_wire_onyx", 0xAAAAAA, 0xAAAAAA, true);

    /** Blue is the historical identifier of the green wire. Outside reward stays deferred with TMT. */
    public static final Item BLUE = wire("nexus_wire_blue", "lbc:nexus_wire_green", 0x2BD893, 0x11A38D, true);
    public static final Item WHITE = wire("nexus_wire_white", "lbc:nexus_wire_white", 0xFFFFFF, 0xFFFFFF, false);

    private LbcNexusWireItems() { }

    public static void register() { }

    private static CompoundTag trigger(String name) {
        var tag = new CompoundTag();
        tag.putInt(name, 1);
        return tag;
    }

    private static Item wire(String name,String modelName,int nameColor,int loreColor,boolean instant) {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:"+name));
        var model=Identifier.parse(modelName);var flags=trigger(name);
        if(instant)flags.putInt("instantitem",1);
        var lore=new ArrayList<Component>();
        for(int line=1;line<=2;line++)lore.add(Component.translatable(name+".lore."+line)
                .withStyle(style->style.withColor(loreColor).withItalic(false)));
        if(instant)lore.add(Component.translatable("nexus_wire.lore")
                .withStyle(style->style.withColor(ChatFormatting.GOLD).withItalic(false)));
        var properties=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}
                .setId(key).overrideDescription(name)
                .component(DataComponents.ITEM_MODEL,model)
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_q"))
                .component(DataComponents.CUSTOM_DATA,CustomData.of(flags))
                .component(DataComponents.CUSTOM_NAME,Component.translatable(name)
                        .withStyle(style->style.withColor(nameColor).withItalic(false)))
                .component(DataComponents.LORE,new ItemLore(lore));
        return Registry.register(BuiltInRegistries.ITEM,key,
                new SimplePolymerItem(properties,Items.REPEATING_COMMAND_BLOCK,true));
    }

    public static boolean isBlue(ItemStack stack){return !stack.isEmpty()&&(stack.is(BLUE)
            ||stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).matchedBy(BLUE_TRIGGER));}
    public static boolean isWhite(ItemStack stack){return !stack.isEmpty()&&(stack.is(WHITE)
            ||stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).matchedBy(WHITE_TRIGGER));}
    /** Source cage order: red, green, black, white; zero is an unrelated item. */
    public static int kind(ItemStack stack){return isRed(stack)?1:isBlue(stack)?2:isBlack(stack)?3:isWhite(stack)?4:0;}
    static Item item(int kind){return switch(kind){case 1->RED;case 2->BLUE;case 3->BLACK;case 4->WHITE;default->throw new IllegalArgumentException("Wire color "+kind);};}

    public static boolean isRed(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(RED)
                || stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(RED_TRIGGER));
    }

    public static boolean isBlack(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(BLACK)
                || stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(BLACK_TRIGGER));
    }
}
