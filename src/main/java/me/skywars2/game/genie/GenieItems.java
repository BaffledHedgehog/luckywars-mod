package me.skywars2.game.genie;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import me.skywars2.SkyWars2;
import me.skywars2.game.ItemData;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

/** Both lamp views use inert vanilla carriers, so neither predicts a throw on the client. */
public final class GenieItems {
    public static final Identifier IDLE=Identifier.parse("lbc:genie/lamp"),THINKING=Identifier.parse("lbc:genie/thinking");
    static final Identifier TOOLTIP_STYLE=Identifier.parse("lbc:tier_q");
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
                Component.literal(description).withStyle(style -> style.withItalic(false).withColor(ChatFormatting.WHITE)),
                Component.translatableWithFallback("genie.gift.lore", "Подарок джина")
                        .withStyle(style -> style.withItalic(false).withColor(ChatFormatting.WHITE)))));
        stack.set(DataComponents.TOOLTIP_STYLE, TOOLTIP_STYLE);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
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
