package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
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
import java.util.List;

/** Source drop tokens; raw custom-data carriers remain accepted. */
public final class LbcFamilyDropItems {
    private LbcFamilyDropItems() {}
    public enum Kind {
        MOM("mom", "drop_mom", "lbc:drop_not_trap"), OTCHIM("otchim", "drop_otchim", "lbc:drop_boss");
        private final CompoundTag trigger = new CompoundTag();
        private final Item item;
        private final String model;
        Kind(String flag, String name, String model) {
            this.model=model; trigger.putInt(flag,1);trigger.putInt("drop",1);
            var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:"+flag+"_drop"));
            item=Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(new Item.Properties().setId(key)
                    .overrideDescription(name).component(DataComponents.CUSTOM_DATA,CustomData.of(trigger))
                    .component(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_ff"))
                    .component(DataComponents.CUSTOM_NAME,Component.translatable(name).withStyle(s->s.withColor(0xffff55).withItalic(false)))
                    .component(DataComponents.LORE,new ItemLore(List.of(Component.translatable("trap_desc").withStyle(s->s.withColor(0xffffff).withItalic(false))))),Items.REPEATING_COMMAND_BLOCK,true));
        }
        public Item item(){return item;}
    }
    public static void register(){Kind.values();DefaultItemComponentEvents.MODIFY.register(context->{for(var k:Kind.values())context.modify(k.item,b->b.set(DataComponents.ITEM_MODEL,Identifier.parse(k.model)));});}
    public static boolean matches(ItemStack stack,Kind kind){return !stack.isEmpty()&&stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).matchedBy(kind.trigger);}
    public static Kind kind(ItemStack stack){for(var kind:Kind.values())if(matches(stack,kind))return kind;return null;}
    public static boolean matches(ItemStack stack){return kind(stack)!=null;}
}
