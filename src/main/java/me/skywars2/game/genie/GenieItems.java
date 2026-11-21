package me.skywars2.game.genie;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import me.skywars2.SkyWars2;
import me.skywars2.game.items.ItemData;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import me.skywars2.game.GameController;

/** Both lamp views use inert vanilla carriers, so neither predicts a throw on the client. */
public final class GenieItems {
    public static final Identifier IDLE=Identifier.parse("lbc:genie/lamp"),THINKING=Identifier.parse("lbc:genie/thinking");
    static final Identifier TOOLTIP_STYLE=Identifier.parse("lbc:tier_q");
    private static final String GIFT_FOOTER_KEY="genie.gift.lore";
    private static final Component GIFT_FOOTER=Component.translatableWithFallback(GIFT_FOOTER_KEY, "Подарок джина")
            .withStyle(style -> style.withItalic(false).withColor(ChatFormatting.WHITE));
    static final Component IDLE_NAME=Component.translatable("genie.lamp").withStyle(ChatFormatting.GOLD).withStyle(s->s.withItalic(false));
    static final Component THINKING_NAME=Component.translatableWithFallback("genie.lamp.thinking", "Лампа джина (думает)")
            .withStyle(ChatFormatting.GOLD).withStyle(s->s.withItalic(false));
    public static final Item LAMP=create();
    private GenieItems(){}
    public static void register(){}
    public static boolean lamp(ItemStack stack){return !stack.isEmpty()&&stack.is(LAMP);}
    static void describeGift(ItemStack stack, String name, String description) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(style -> style.withItalic(false)));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal(description).withStyle(style -> style.withItalic(false).withColor(ChatFormatting.WHITE)))));
        applyGiftPresentation(stack);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    /** Called for existing marked gifts when their saved components are materialized and when creating a gift. */
    public static void applyGiftPresentation(ItemStack stack) {
        if (!TOOLTIP_STYLE.equals(stack.get(DataComponents.TOOLTIP_STYLE))) {
            stack.set(DataComponents.TOOLTIP_STYLE, TOOLTIP_STYLE);
        }
        var original=stack.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines();
        var lines=new ArrayList<Component>(original.size()+1);
        for (var line : original) {
            if (!(line.getContents() instanceof TranslatableContents text) || !GIFT_FOOTER_KEY.equals(text.getKey())) {
                lines.add(line);
            }
        }
        // A full native lore cannot gain a new line without deleting existing text.
        if (lines.size() < ItemLore.MAX_LINES) {
            lines.add(GIFT_FOOTER);
            if (!original.equals(lines)) stack.set(DataComponents.LORE,new ItemLore(List.copyOf(lines)));
        }
    }

    private static Item create(){
        var id=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:genie_lamp"));
        var properties=new Item.Properties().setId(id).stacksTo(1).component(DataComponents.ITEM_MODEL,IDLE)
                .component(DataComponents.ITEM_NAME,IDLE_NAME)
                .component(DataComponents.TOOLTIP_STYLE,TOOLTIP_STYLE);
        return Registry.register(BuiltInRegistries.ITEM,id,new SimplePolymerItem(properties,Items.GOLD_NUGGET,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                var game=SkyWars2.game();
                if(game==null||!(player instanceof ServerPlayer owner))return InteractionResult.FAIL;
                return game.items().genie().open(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
}
