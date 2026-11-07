package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

/** Snowball is only the client carrier for immediate gadgets; the grenade uses the shared arrow transport. */
public final class LbcGadgetItems {
    public static final Item ISLAND_TELEPORTER=create("island_teleporter","lbc:islandtp",false);
    public static final Item SWAPPER=create("swapper","lbc:swapper",false);
    public static final Item LUCKY_GRENADE=create("lucky_grenade","lbc:luckygrenade",true);
    private LbcGadgetItems(){}
    public static void register(){LbcKillButton.register();}
    private static Item create(String name,String model,boolean grenade){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:"+name));
        var properties=new Item.Properties(){@Override public Identifier effectiveModel(){return Identifier.parse(model);}}
                .setId(key).stacksTo(16).overrideDescription(name)
                .component(DataComponents.ITEM_MODEL,Identifier.parse(model));
        var flags=new CompoundTag();flags.putInt(grenade?"special":"gadget",1);
        flags.putInt(grenade?"luckygrenade":name.equals("swapper")?"randtp":"islandtp",1);
        var title=Component.translatable(name).withStyle(s->s.withItalic(false));
        if(!grenade)title.withColor(name.equals("swapper")?0xff55ff:0x55ffff);
        var lore=name.equals("swapper")?List.of("swapper.lore.1","swapper.lore.2"):
                grenade?List.of("lucky_grenade.lore.1","lucky_grenade.lore.2","lucky_grenade.lore.3"):List.of("island_teleporter.lore");
        properties.component(DataComponents.CUSTOM_DATA,CustomData.of(flags)).component(DataComponents.CUSTOM_NAME,title)
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse(name.equals("swapper")?"lbc:tier_a":"lbc:tier_b"))
                .component(DataComponents.LORE,new ItemLore(lore.stream().map(k->(Component)Component.translatable(k).withStyle(s->s.withColor(0xffffff).withItalic(false))).toList()));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,Items.SNOWBALL,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                var game=me.skywars2.SkyWars2.game();
                if(game==null||!(player instanceof ServerPlayer owner))return InteractionResult.FAIL;
                boolean used=grenade?game.items().hedgehogSpikes().useLegacy(owner,hand):game.items().teleportGadgets().use(owner,hand);
                return used?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
}
