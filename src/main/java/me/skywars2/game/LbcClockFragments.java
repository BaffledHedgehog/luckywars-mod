package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Inert ID for archived source loot. The author explicitly cancelled time-acceleration fragments. */
public final class LbcClockFragments {
    public static final String ID="skywars2:clock_fragment";
    public static final Item ITEM=Registry.register(BuiltInRegistries.ITEM,Identifier.parse(ID),new SimplePolymerItem(
            new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Identifier.parse(ID)))
                    .component(DataComponents.ITEM_MODEL,Identifier.parse("lbc:time_clock"))
                    .component(DataComponents.CUSTOM_NAME,Component.translatable("time_fragment").withStyle(s->s.withItalic(false))),
            Items.CARROT_ON_A_STICK, true));
    private LbcClockFragments(){}
    public static void register(){}
}
