package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

/** Real native bows retain vanilla charge, ammunition, enchantment and durability handling. */
public final class LbcChargedBowItems {
    public static final Item RAILGUN = register("railgun", false);
    public static final Item SHISHGUN = register("shishgun", true);
    private LbcChargedBowItems() { }
    public static void register() { LbcChargedBows.register(); }
    public static boolean railgun(ItemStack stack) { return !stack.isEmpty() && ItemData.intValue(stack,"railgun_bow",0)==1; }
    public static boolean shishgun(ItemStack stack) { return !stack.isEmpty() && ItemData.intValue(stack,"shish_gun",0)==1; }
    public static boolean matches(ItemStack stack) { return railgun(stack)||shishgun(stack); }
    private static final class NativeBow extends BowItem implements PolymerItem {
        NativeBow(Properties properties) { super(properties); }
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context) { return Items.BOW; }
    }
    private static Item register(String id,boolean shish) {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:"+id));
        var model=Identifier.parse(shish?"lbc:shishgun_bow":"lbc:railgun_bow");
        String name=shish?"shish_bow":"railgun_bow";
        var flags=new CompoundTag();
        for(var flag:List.of("craftusable","transformable","bow",shish?"shish_gun":"railgun_bow"))flags.putInt(flag,1);
        var title=Component.translatable(name).withStyle(s->s.withItalic(false).withBold(shish)
                .withColor(shish?ChatFormatting.GREEN:ChatFormatting.WHITE));
        var type=Component.translatable("type").append(Component.translatable("bow.lbc"))
                .withStyle(ChatFormatting.AQUA).withStyle(s->s.withItalic(false));
        List<Component> lore=shish?List.of(type,Component.translatable("shish_bow.lore")
                .withStyle(ChatFormatting.GOLD).withStyle(s->s.withItalic(false)))
                :List.of(type,Component.translatable("railgun_bow.lore").withStyle(ChatFormatting.WHITE).withStyle(s->s.withItalic(false)),
                Component.translatable("railgun_bow.lore.1").withStyle(ChatFormatting.WHITE).withStyle(s->s.withItalic(false)),
                Component.literal("Stendari").withStyle(ChatFormatting.GREEN).append(Component.literal(" Beta Server Harem event")
                        .withStyle(ChatFormatting.DARK_GRAY)).withStyle(s->s.withItalic(false)));
        var properties=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}
                .setId(key).durability(384).enchantable(1).overrideDescription(name)
                .component(DataComponents.ITEM_MODEL,model).component(DataComponents.TOOLTIP_STYLE,Identifier.parse(shish?"lbc:tier_s":"lbc:tier_b"))
                .component(DataComponents.CUSTOM_NAME,title).component(DataComponents.CUSTOM_DATA,CustomData.of(flags))
                .component(DataComponents.LORE,new ItemLore(lore));
        return Registry.register(BuiltInRegistries.ITEM,key,new NativeBow(properties));
    }
}
